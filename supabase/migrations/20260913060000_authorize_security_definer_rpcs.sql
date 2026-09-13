-- Close the cross-house data leaks in the SECURITY DEFINER RPCs.
--
-- RLS is enabled and correct on all twenty tables, but a SECURITY DEFINER function runs with the
-- owner's rights and so bypasses it entirely. Twenty-two of these functions carried no membership
-- check, which made every one of them a way around the policies. Verified against the live
-- project with a throwaway account belonging to no house: it could read any house's member list
-- including emails, read any house's recurring bills, enumerate any user's houses, and delete any
-- user's notifications.
--
-- The guard used here is auth_is_house_member(), which the RLS policies themselves already rely
-- on in thirty places. It derives the caller from auth.uid() rather than trusting an argument,
-- which is the property the vulnerable functions were missing.
--
-- The notification writers gate on auth.role() rather than membership alone. They are also called
-- from the notify_new_member trigger, which runs with no JWT, and a bare membership check would
-- reject that legitimate internal call. Gating on the client roles keeps anon and authenticated
-- honest while leaving trigger and service_role paths working.

-- Leaked every member's email and full name to any authenticated caller.
create or replace function public.get_house_members_with_profiles(p_house_id uuid)
returns table(user_id uuid, role text, joined_at timestamp with time zone, email text, full_name text, avatar_url text)
language plpgsql
security definer
set search_path to 'public'
as $function$
begin
  if not public.auth_is_house_member(p_house_id) then
    raise exception 'Not a member of this house' using errcode = '42501';
  end if;

  return query
  select
    hm.user_id,
    hm.role,
    hm.joined_at,
    p.email,
    p.full_name,
    p.avatar_url
  from house_members hm
  join profiles p on hm.user_id = p.id
  where hm.house_id = p_house_id;
end;
$function$;

-- Leaked bill names, amounts, due dates and notes for any house.
create or replace function public.get_recurring_expenses_with_status(p_house_id uuid)
returns table(id uuid, house_id uuid, name text, amount numeric, due_day integer, category text, created_by uuid, is_active boolean, created_at timestamp with time zone, frequency text, next_due_date date, last_paid_date date, custom_frequency_days integer, reminder_days_before integer, reminder_enabled boolean, notes text, due_status text, days_until_due integer)
language plpgsql
security definer
set search_path to 'public'
as $function$
begin
    if not public.auth_is_house_member(p_house_id) then
        raise exception 'Not a member of this house' using errcode = '42501';
    end if;

    return query
    select
        re.id,
        re.house_id,
        re.name,
        re.amount,
        re.due_day,
        re.category,
        re.created_by,
        re.is_active,
        re.created_at,
        re.frequency,
        re.next_due_date,
        re.last_paid_date,
        re.custom_frequency_days,
        re.reminder_days_before,
        re.reminder_enabled,
        re.notes,
        case
            when re.next_due_date is null then 'pending'
            when re.next_due_date < current_date then 'overdue'
            when re.next_due_date = current_date then 'due_today'
            when re.next_due_date <= current_date + (re.reminder_days_before || ' days')::interval then 'upcoming'
            else 'scheduled'
        end as due_status,
        case
            when re.next_due_date is not null then (re.next_due_date - current_date)::integer
            else null
        end as days_until_due
    from public.recurring_expenses re
    where re.house_id = p_house_id
    order by
        case
            when re.next_due_date is null then 999999
            else (re.next_due_date - current_date)::integer
        end,
        re.name;
end;
$function$;

-- Let any caller enumerate which houses any other user belonged to.
create or replace function public.get_user_house_ids(p_user_id uuid)
returns table(house_id uuid)
language sql
stable
security definer
set search_path to 'public'
as $function$
    select hm.house_id
    from public.house_members hm
    where hm.user_id = p_user_id
      and hm.is_active = true
      and p_user_id = auth.uid()
    order by hm.joined_at desc;
$function$;

-- A bare delete keyed on an argument: any caller could wipe any other user's notifications.
create or replace function public.delete_all_notifications(p_user_id uuid)
returns void
language plpgsql
security definer
set search_path to 'public'
as $function$
begin
    if p_user_id is distinct from auth.uid() then
        raise exception 'Can only delete your own notifications' using errcode = '42501';
    end if;

    delete from notifications
    where user_id = p_user_id;
end;
$function$;

-- Let any caller write an arbitrary notification into any user's feed, which is a phishing surface.
create or replace function public.create_notification(p_user_id uuid, p_house_id uuid, p_title text, p_message text, p_type text, p_data text default '{}'::text)
returns void
language plpgsql
security definer
set search_path to 'public'
as $function$
begin
    if coalesce(auth.role(), '') in ('authenticated', 'anon')
       and not public.auth_is_house_member(p_house_id) then
        raise exception 'Not a member of this house' using errcode = '42501';
    end if;

    insert into notifications (
        user_id,
        house_id,
        title,
        message,
        type,
        data,
        is_read,
        created_at
    ) values (
        p_user_id,
        p_house_id,
        p_title,
        p_message,
        p_type,
        p_data::jsonb,
        false,
        now()
    );
end;
$function$;

-- Same surface, fanned out to every member of a house at once.
create or replace function public.create_notification_for_house(p_house_id uuid, p_title text, p_message text, p_type text, p_data text default '{}'::text, p_exclude_user_id uuid default null::uuid)
returns void
language plpgsql
security definer
set search_path to 'public'
as $function$
begin
    if coalesce(auth.role(), '') in ('authenticated', 'anon')
       and not public.auth_is_house_member(p_house_id) then
        raise exception 'Not a member of this house' using errcode = '42501';
    end if;

    insert into notifications (
        user_id,
        house_id,
        title,
        message,
        type,
        data,
        is_read,
        created_at
    )
    select
        hm.user_id,
        p_house_id,
        p_title,
        p_message,
        p_type,
        p_data,
        false,
        now()
    from house_members hm
    where hm.house_id = p_house_id
      and hm.is_active = true
      and (p_exclude_user_id is null or hm.user_id != p_exclude_user_id);
end;
$function$;

-- Maintenance routines that mass-delete notifications and audit history. They are cron work and
-- were callable by any client, which let anyone destroy the audit trail on demand.
--
-- These revoke from PUBLIC, not from anon and authenticated. Postgres grants EXECUTE on a new
-- function to PUBLIC by default, and those two roles inherit it from there, so revoking from the
-- roles alone leaves the function callable.
revoke execute on function public.auto_cleanup_house_data() from public;
revoke execute on function public.cleanup_old_notifications() from public;
grant execute on function public.auto_cleanup_house_data() to service_role;
grant execute on function public.cleanup_old_notifications() to service_role;

-- Membership oracles that answer questions about arbitrary users. The RLS policies use the
-- auth_* variants instead, so removing client access costs the app nothing.
revoke execute on function public.is_house_admin(uuid, uuid) from public;
revoke execute on function public.is_house_member(uuid, uuid) from public;
revoke execute on function public.is_house_owner(uuid, uuid) from public;
revoke execute on function public.shares_house_with(uuid, uuid) from public;
grant execute on function public.is_house_admin(uuid, uuid) to service_role;
grant execute on function public.is_house_member(uuid, uuid) to service_role;
grant execute on function public.is_house_owner(uuid, uuid) to service_role;
grant execute on function public.shares_house_with(uuid, uuid) to service_role;
