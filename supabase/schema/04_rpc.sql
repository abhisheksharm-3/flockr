-- The API the app calls. Reads run as the caller (security invoker), so row-level security decides what
-- they see. Writes that span tables run as the definer and check the caller before doing anything.

-- Houses

create function public.create_house(
    p_name text, p_address text, p_latitude double precision, p_longitude double precision, p_header_image_url text,
    p_currency_code text, p_date_format text, p_first_day_of_week smallint, p_timezone text
) returns uuid language plpgsql security definer set search_path = public as $$
declare
    v_house_id uuid;
begin
    if auth.uid() is null then raise exception 'Sign in to create a house'; end if;
    insert into houses (name, owner_id, address, latitude, longitude, header_image_url)
    values (btrim(p_name), auth.uid(), p_address, p_latitude, p_longitude, p_header_image_url)
    returning id into v_house_id;
    update house_config
    set currency_code = p_currency_code, date_format = p_date_format,
        first_day_of_week = p_first_day_of_week, timezone = p_timezone
    where house_id = v_house_id;
    return v_house_id;
end;
$$;

-- The caller's houses for the home screen, with this month's spend in each house's own time zone.
create function public.get_my_houses()
returns table (
    id uuid, name text, owner_id uuid, address text, latitude double precision, longitude double precision,
    header_image_url text, invite_code text, member_count bigint, currency_code text, monthly_spend numeric,
    my_net numeric
) language sql stable security invoker set search_path = public as $$
    select h.id, h.name, h.owner_id, h.address, h.latitude, h.longitude, h.header_image_url, h.invite_code,
           (select count(*) from house_members m where m.house_id = h.id and m.left_at is null),
           hc.currency_code,
           (select total_spend from get_monthly_summary(h.id, (now() at time zone hc.timezone)::date)),
           (select coalesce(sum(s.paid_share - s.owed_share), 0)
            from expense_shares s join expenses e on e.id = s.expense_id
            where e.house_id = h.id and s.user_id = (select auth.uid()))
    from houses h
    join house_config hc on hc.house_id = h.id
    where h.id in (select auth_house_ids())
    order by h.name;
$$;

create function public.get_house_members(p_house_id uuid)
returns table (
    user_id uuid, role text, joined_at timestamptz, left_at timestamptz, default_split_weight numeric,
    email text, full_name text, avatar_url text
) language sql stable security invoker set search_path = public as $$
    select m.user_id, m.role, m.joined_at, m.left_at, m.default_split_weight, p.email, p.full_name, p.avatar_url
    from house_members m
    join profiles p on p.id = m.user_id
    where m.house_id = p_house_id
    order by m.left_at nulls first, m.joined_at;
$$;

create function public.preview_house_by_invite_code(p_code text)
returns table (id uuid, name text, header_image_url text, owner_name text, member_count bigint, is_full boolean)
language sql stable security definer set search_path = public as $$
    select h.id, h.name, h.header_image_url, display_name(h.owner_id),
           (select count(*) from house_members m where m.house_id = h.id and m.left_at is null),
           (select count(*) from house_members m where m.house_id = h.id and m.left_at is null) >= h.max_members
    from houses h
    where auth.uid() is not null
      and h.invite_code = upper(btrim(p_code))
      and (h.invite_code_expires_at is null or h.invite_code_expires_at > now());
$$;

create function public.join_house_with_invite_code(p_code text) returns uuid
language plpgsql security definer set search_path = public as $$
declare
    v_house_id uuid;
begin
    if auth.uid() is null then raise exception 'Sign in to join a house'; end if;
    select id into v_house_id from houses
    where invite_code = upper(btrim(p_code)) and (invite_code_expires_at is null or invite_code_expires_at > now());
    if v_house_id is null then raise exception 'This invite code is invalid or has expired'; end if;
    insert into house_members (house_id, user_id) values (v_house_id, auth.uid())
    on conflict (house_id, user_id) do update set left_at = null, joined_at = now(), role = 'Member'
        where house_members.left_at is not null;
    return v_house_id;
end;
$$;

create function public.regenerate_invite_code(p_house_id uuid) returns text
language plpgsql security definer set search_path = public as $$
declare
    v_code text := generate_invite_code();
begin
    if not auth_is_house_admin(p_house_id) then raise exception 'Only an admin can change the invite code'; end if;
    update houses set invite_code = v_code, invite_code_generated_at = now(), invite_code_expires_at = now() + interval '7 days'
    where id = p_house_id;
    return v_code;
end;
$$;

create function public.remove_house_member(p_house_id uuid, p_user_id uuid) returns void
language plpgsql security definer set search_path = public as $$
begin
    if not auth_is_house_admin(p_house_id) then raise exception 'Only an admin can remove a member'; end if;
    if exists (select 1 from house_members where house_id = p_house_id and user_id = p_user_id and role = 'Owner') then
        raise exception 'The owner cannot be removed';
    end if;
    update house_members set left_at = now() where house_id = p_house_id and user_id = p_user_id and left_at is null;
end;
$$;

-- Leaving keeps the member's history. The owner must hand the house over first unless nobody else is left.
create function public.leave_house(p_house_id uuid) returns void
language plpgsql security definer set search_path = public as $$
begin
    if not auth_is_house_member(p_house_id) then raise exception 'Not a member of this house'; end if;
    if exists (select 1 from house_members where house_id = p_house_id and user_id = auth.uid() and role = 'Owner')
       and exists (select 1 from house_members where house_id = p_house_id and left_at is null and user_id <> auth.uid()) then
        raise exception 'Transfer ownership before leaving';
    end if;
    update house_members set left_at = now() where house_id = p_house_id and user_id = auth.uid();
end;
$$;

create function public.transfer_house_ownership(p_house_id uuid, p_new_owner_id uuid) returns void
language plpgsql security definer set search_path = public as $$
begin
    if not exists (select 1 from house_members where house_id = p_house_id and user_id = auth.uid() and role = 'Owner' and left_at is null) then
        raise exception 'Only the owner can transfer the house';
    end if;
    if not is_active_house_member(p_house_id, p_new_owner_id) then
        raise exception 'The new owner must be a member';
    end if;
    perform set_config('flockr.transferring_ownership', 'on', true);
    update house_members set role = 'Admin' where house_id = p_house_id and user_id = auth.uid();
    update house_members set role = 'Owner' where house_id = p_house_id and user_id = p_new_owner_id;
    update houses set owner_id = p_new_owner_id where id = p_house_id;
end;
$$;

create function public.delete_house(p_house_id uuid) returns void
language plpgsql security definer set search_path = public as $$
begin
    if not exists (select 1 from houses where id = p_house_id and owner_id = auth.uid()) then
        raise exception 'Only the owner can delete the house';
    end if;
    delete from houses where id = p_house_id;
end;
$$;

-- Invitations

create function public.invite_to_house(p_house_id uuid, p_email text) returns uuid
language plpgsql security definer set search_path = public as $$
declare
    v_id uuid;
    v_window invitation_rate_limit;
begin
    if not auth_is_house_member(p_house_id) then raise exception 'Not a member of this house'; end if;
    insert into invitation_rate_limit (user_id, house_id) values (auth.uid(), p_house_id) on conflict do nothing;
    select * into v_window from invitation_rate_limit where user_id = auth.uid() and house_id = p_house_id for update;
    if v_window.window_start < now() - interval '1 hour' then
        update invitation_rate_limit set invitations_sent = 0, window_start = now() where user_id = auth.uid() and house_id = p_house_id;
        v_window.invitations_sent := 0;
    end if;
    if v_window.invitations_sent >= 10 then raise exception 'Too many invitations. Try again in an hour.'; end if;
    update invitation_rate_limit set invitations_sent = invitations_sent + 1 where user_id = auth.uid() and house_id = p_house_id;
    insert into house_invitations (house_id, inviter_id, invitee_email)
    values (p_house_id, auth.uid(), lower(btrim(p_email)))
    returning id into v_id;
    return v_id;
end;
$$;

create function public.get_my_pending_invitations()
returns table (id uuid, house_id uuid, house_name text, header_image_url text, inviter_name text, created_at timestamptz)
language sql stable security definer set search_path = public as $$
    select i.id, i.house_id, h.name, h.header_image_url, display_name(i.inviter_id), i.created_at
    from house_invitations i
    join houses h on h.id = i.house_id
    where i.status = 'pending' and i.invitee_email = (select email from profiles where id = auth.uid())
    order by i.created_at desc;
$$;

create function public.respond_to_invitation(p_invitation_id uuid, p_accept boolean) returns uuid
language plpgsql security definer set search_path = public as $$
declare
    v_invitation house_invitations;
begin
    select * into v_invitation from house_invitations
    where id = p_invitation_id and status = 'pending' and invitee_email = (select email from profiles where id = auth.uid())
    for update;
    if not found then raise exception 'Invitation not found'; end if;
    update house_invitations set status = case when p_accept then 'accepted' else 'rejected' end where id = p_invitation_id;
    if p_accept then
        insert into house_members (house_id, user_id) values (v_invitation.house_id, auth.uid())
        on conflict (house_id, user_id) do update set left_at = null, joined_at = now(), role = 'Member'
            where house_members.left_at is not null;
    end if;
    return v_invitation.house_id;
end;
$$;

create function public.cancel_invitation(p_invitation_id uuid) returns void
language plpgsql security definer set search_path = public as $$
begin
    update house_invitations set status = 'cancelled'
    where id = p_invitation_id and status = 'pending'
      and (inviter_id = auth.uid() or auth_is_house_admin(house_id));
    if not found then raise exception 'Invitation not found'; end if;
end;
$$;

-- Expenses and balances

-- Writes the shares of [p_expense_id] from [p_shares], a JSON array of
-- {user_id, paid_share, owed_share, split_value}. The deferred balance check validates the result.
create function public.write_expense_shares(p_expense_id uuid, p_shares jsonb) returns void
language plpgsql security definer set search_path = public as $$
begin
    delete from expense_shares where expense_id = p_expense_id;
    insert into expense_shares (expense_id, user_id, paid_share, owed_share, split_value)
    select p_expense_id, (s ->> 'user_id')::uuid,
           coalesce((s ->> 'paid_share')::numeric, 0), coalesce((s ->> 'owed_share')::numeric, 0),
           (s ->> 'split_value')::numeric
    from jsonb_array_elements(p_shares) s;
end;
$$;

-- Adds an expense, or replaces [p_expense_id]'s details and shares. Everyone on it is notified.
create function public.save_expense(
    p_expense_id uuid, p_house_id uuid, p_name text, p_amount numeric, p_category text, p_date date,
    p_notes text, p_split_method text, p_shares jsonb
) returns uuid language plpgsql security definer set search_path = public as $$
declare
    v_id uuid := p_expense_id;
begin
    if not auth_is_house_member(p_house_id) then raise exception 'Not a member of this house'; end if;
    if v_id is null then
        insert into expenses (house_id, name, amount, category, date, notes, split_method, created_by)
        values (p_house_id, btrim(p_name), p_amount, p_category, p_date, p_notes, p_split_method, auth.uid())
        returning id into v_id;
    else
        if exists (select 1 from expenses where id = v_id and per_diem_month is not null) then
            raise exception 'A usage bill is worked out from the usage log. Delete it and bill the month again to change it.';
        end if;
        update expenses
        set name = btrim(p_name), amount = p_amount, category = p_category, date = p_date, notes = p_notes, split_method = p_split_method
        where id = v_id and house_id = p_house_id and kind = 'expense';
        if not found then raise exception 'Expense not found'; end if;
    end if;
    perform write_expense_shares(v_id, p_shares);
    perform notify_expense(v_id, auth.uid(), case when p_expense_id is null then 'expense_added' else 'expense_updated' end);
    return v_id;
end;
$$;

create function public.delete_expense(p_expense_id uuid) returns void
language plpgsql security definer set search_path = public as $$
begin
    delete from expenses e
    where e.id = p_expense_id and auth_is_house_member(e.house_id)
      and (e.created_by = auth.uid() or auth_is_house_admin(e.house_id));
    if not found then raise exception 'Only whoever added it, or an admin, can delete this'; end if;
end;
$$;

-- Records that [p_from_user_id] paid [p_to_user_id] back [p_amount], as a settlement in the ledger.
-- Either of the two may record it and the other is told. Paying more than is owed carries forward
-- as a balance the other way.
create function public.settle_up(
    p_house_id uuid, p_from_user_id uuid, p_to_user_id uuid, p_amount numeric, p_date date, p_note text
) returns uuid language plpgsql security definer set search_path = public as $$
declare
    v_id uuid;
begin
    if not auth_is_house_member(p_house_id) then raise exception 'Not a member of this house'; end if;
    if auth.uid() not in (p_from_user_id, p_to_user_id) then raise exception 'Only the two people in a payment can record it'; end if;
    if p_from_user_id = p_to_user_id then raise exception 'A payment needs two different people'; end if;
    insert into expenses (house_id, kind, name, amount, date, notes, created_by)
    values (p_house_id, 'settlement', 'Payment', p_amount, p_date, p_note, auth.uid())
    returning id into v_id;
    insert into expense_shares (expense_id, user_id, paid_share, owed_share) values
        (v_id, p_from_user_id, p_amount, 0),
        (v_id, p_to_user_id, 0, p_amount);
    perform notify_expense(v_id, auth.uid(), 'settlement_received');
    return v_id;
end;
$$;

-- Each member's standing: [paid] and [owed] are their spending and share of expenses alone, and
-- [net] is what everything, payments between housemates included, leaves them owed or owing.
-- Past members appear only while they are not square.
create function public.get_balances(p_house_id uuid)
returns table (user_id uuid, full_name text, is_active boolean, paid numeric, owed numeric, net numeric)
language sql stable security invoker set search_path = public as $$
    select m.user_id, p.full_name, m.left_at is null,
           coalesce(sum(s.paid_share) filter (where e.kind = 'expense'), 0),
           coalesce(sum(s.owed_share) filter (where e.kind = 'expense'), 0),
           coalesce(sum(s.paid_share), 0) - coalesce(sum(s.owed_share), 0)
    from house_members m
    join profiles p on p.id = m.user_id
    left join expense_shares s on s.user_id = m.user_id
        and s.expense_id in (select id from expenses where house_id = p_house_id)
    left join expenses e on e.id = s.expense_id
    where m.house_id = p_house_id
    group by m.user_id, p.full_name, m.left_at
    having m.left_at is null or coalesce(sum(s.paid_share), 0) <> coalesce(sum(s.owed_share), 0)
    order by 6 desc;
$$;

-- The fewest payments that settle every balance in the house: the largest debtor pays the largest
-- creditor, repeatedly. Amounts are exact because the balances are.
create function public.get_settle_up_plan(p_house_id uuid)
returns table (from_user_id uuid, to_user_id uuid, amount numeric)
language plpgsql stable security invoker set search_path = public as $$
declare
    v_debtors uuid[];
    v_debts numeric[];
    v_creditors uuid[];
    v_credits numeric[];
    i int := 1;
    j int := 1;
    v_pay numeric;
begin
    select array_agg(b.user_id order by b.net, b.user_id), array_agg(-b.net order by b.net, b.user_id)
    into v_debtors, v_debts from get_balances(p_house_id) b where b.net < 0;
    select array_agg(b.user_id order by b.net desc, b.user_id), array_agg(b.net order by b.net desc, b.user_id)
    into v_creditors, v_credits from get_balances(p_house_id) b where b.net > 0;

    while i <= coalesce(cardinality(v_debtors), 0) and j <= coalesce(cardinality(v_creditors), 0) loop
        v_pay := least(v_debts[i], v_credits[j]);
        from_user_id := v_debtors[i];
        to_user_id := v_creditors[j];
        amount := v_pay;
        return next;
        v_debts[i] := v_debts[i] - v_pay;
        v_credits[j] := v_credits[j] - v_pay;
        if v_debts[i] = 0 then i := i + 1; end if;
        if v_credits[j] = 0 then j := j + 1; end if;
    end loop;
end;
$$;

-- The expenses and payments the caller shares with [p_other_user_id], each with [between_us]: how much
-- it added to what the other person owes the caller, negative when it added to what the caller owes
-- them. Each person's debt is owed to the payers in proportion to what they paid, so with one payer
-- this is exactly the other person's share, or the caller's; with several it is rounded to the currency.
create function public.get_shared_history(p_house_id uuid, p_other_user_id uuid)
returns table (expense_id uuid, kind text, name text, date date, amount numeric, between_us numeric)
language sql stable security invoker set search_path = public as $$
    select e.id, e.kind, e.name, e.date, e.amount,
           round((theirs.owed_share * mine.paid_share - mine.owed_share * theirs.paid_share) / e.amount, c.minor_digits)
    from expenses e
    join house_config hc on hc.house_id = e.house_id
    join currencies c on c.code = hc.currency_code
    join expense_shares mine on mine.expense_id = e.id and mine.user_id = auth.uid()
    join expense_shares theirs on theirs.expense_id = e.id and theirs.user_id = p_other_user_id
    where e.house_id = p_house_id
    order by e.date desc, e.created_at desc;
$$;

-- Spending in a calendar month, excluding payments between housemates. Recurring bill payments are
-- counted once, as the expenses they are. Per-diem is counted from its usage entries, so a usage bill,
-- which is those same entries turned into an expense, is left out rather than counted twice.
create function public.get_monthly_summary(p_house_id uuid, p_month date)
returns table (total_spend numeric, recurring_spend numeric, one_time_spend numeric, per_diem_spend numeric)
language sql stable security invoker set search_path = public as $$
    with month_bounds as (
        select date_trunc('month', p_month)::date as starts, (date_trunc('month', p_month) + interval '1 month')::date as ends
    ), spend as (
        select coalesce(sum(e.amount) filter (where e.recurring_expense_id is not null), 0) as recurring,
               coalesce(sum(e.amount) filter (where e.recurring_expense_id is null), 0) as one_time
        from expenses e, month_bounds b
        where e.house_id = p_house_id and e.kind = 'expense' and e.per_diem_month is null
          and e.date >= b.starts and e.date < b.ends
    ), per_diem as (
        select coalesce(sum(pe.total_cost), 0) as total
        from per_diem_entries pe
        join per_diem_config pc on pc.id = pe.config_id, month_bounds b
        where pc.house_id = p_house_id and pe.date >= b.starts and pe.date < b.ends
    )
    select s.recurring + s.one_time + d.total, s.recurring, s.one_time, d.total from spend s, per_diem d;
$$;

-- What each member paid and what they consumed (owed) in a month, excluding payments between them.
create function public.get_spend_by_member(p_house_id uuid, p_month date)
returns table (user_id uuid, full_name text, paid numeric, consumed numeric)
language sql stable security invoker set search_path = public as $$
    select s.user_id, p.full_name, sum(s.paid_share), sum(s.owed_share)
    from expenses e
    join expense_shares s on s.expense_id = e.id
    join profiles p on p.id = s.user_id
    where e.house_id = p_house_id and e.kind = 'expense'
      and e.date >= date_trunc('month', p_month)::date and e.date < (date_trunc('month', p_month) + interval '1 month')::date
    group by s.user_id, p.full_name
    order by 4 desc;
$$;

create function public.get_spend_by_category(p_house_id uuid, p_month date)
returns table (category text, total numeric)
language sql stable security invoker set search_path = public as $$
    with month_bounds as (
        select date_trunc('month', p_month)::date as starts, (date_trunc('month', p_month) + interval '1 month')::date as ends
    )
    select category, sum(total) from (
        select e.category, e.amount as total
        from expenses e, month_bounds b
        where e.house_id = p_house_id and e.kind = 'expense' and e.per_diem_month is null
          and e.date >= b.starts and e.date < b.ends
        union all
        select pc.category, pe.total_cost
        from per_diem_entries pe join per_diem_config pc on pc.id = pe.config_id, month_bounds b
        where pc.house_id = p_house_id and pe.date >= b.starts and pe.date < b.ends
    ) spend
    group by category
    order by 2 desc;
$$;

-- Recurring bills

-- Adds a recurring bill, or replaces [p_recurring_id]'s details and split. [p_shares] is a JSON array
-- of {user_id, split_value}; empty with a null split method means the bill is not split.
create function public.save_recurring_expense(
    p_recurring_id uuid, p_house_id uuid, p_name text, p_amount numeric, p_category text, p_frequency text,
    p_custom_frequency_days smallint, p_first_due_date date, p_reminder_enabled boolean, p_reminder_days_before smallint,
    p_allow_prepayment boolean, p_split_method text, p_shares jsonb, p_notes text
) returns uuid language plpgsql security definer set search_path = public as $$
declare
    v_id uuid := p_recurring_id;
begin
    if not auth_is_house_member(p_house_id) then raise exception 'Not a member of this house'; end if;
    if exists (select 1 from jsonb_array_elements(p_shares) s where not was_house_member(p_house_id, (s ->> 'user_id')::uuid)) then
        raise exception 'Everyone on a bill must belong to the house';
    end if;
    if v_id is null then
        insert into recurring_expenses (house_id, name, amount, category, frequency, custom_frequency_days, first_due_date,
                                        next_due_date, reminder_enabled, reminder_days_before, allow_prepayment, split_method,
                                        notes, created_by)
        values (p_house_id, btrim(p_name), p_amount, p_category, p_frequency, p_custom_frequency_days, p_first_due_date,
                p_first_due_date, p_reminder_enabled, p_reminder_days_before, p_allow_prepayment, p_split_method, p_notes, auth.uid())
        returning id into v_id;
    else
        update recurring_expenses
        set name = btrim(p_name), amount = p_amount, category = p_category, frequency = p_frequency,
            custom_frequency_days = p_custom_frequency_days, reminder_enabled = p_reminder_enabled,
            reminder_days_before = p_reminder_days_before, allow_prepayment = p_allow_prepayment,
            split_method = p_split_method, notes = p_notes
        where id = v_id and house_id = p_house_id;
        if not found then raise exception 'Bill not found'; end if;
    end if;
    delete from recurring_expense_shares where recurring_expense_id = v_id;
    insert into recurring_expense_shares (recurring_expense_id, user_id, split_value)
    select v_id, (s ->> 'user_id')::uuid, (s ->> 'split_value')::numeric from jsonb_array_elements(p_shares) s;
    return v_id;
end;
$$;

-- Active bills with their status measured against today in the house's time zone.
create function public.get_recurring_expenses(p_house_id uuid)
returns table (
    id uuid, name text, amount numeric, category text, frequency text, custom_frequency_days smallint,
    first_due_date date, next_due_date date, last_paid_date date, reminder_enabled boolean, reminder_days_before smallint,
    allow_prepayment boolean, split_method text, notes text, created_by uuid, shares jsonb, due_status text, days_until_due int
) language sql stable security invoker set search_path = public as $$
    with today as (
        select (now() at time zone hc.timezone)::date as d from house_config hc where hc.house_id = p_house_id
    )
    select r.id, r.name, r.amount, r.category, r.frequency, r.custom_frequency_days, r.first_due_date, r.next_due_date,
           r.last_paid_date, r.reminder_enabled, r.reminder_days_before, r.allow_prepayment, r.split_method, r.notes, r.created_by,
           coalesce((select jsonb_agg(jsonb_build_object('user_id', s.user_id, 'split_value', s.split_value) order by s.user_id)
                     from recurring_expense_shares s where s.recurring_expense_id = r.id), '[]'),
           case
               when r.next_due_date < t.d then 'overdue'
               when r.next_due_date = t.d then 'due_today'
               when r.next_due_date <= t.d + r.reminder_days_before then 'upcoming'
               else 'scheduled'
           end,
           r.next_due_date - t.d
    from recurring_expenses r, today t
    where r.house_id = p_house_id and r.is_active
    order by r.next_due_date, r.name;
$$;

-- Records a payment of [p_recurring_id] by the caller as an expense with the given shares, and moves
-- the bill to its next due date. The app divides the amount by the bill's split so the preview and
-- the record match; the balance check verifies the result.
create function public.pay_recurring_expense(p_recurring_id uuid, p_amount numeric, p_date date, p_shares jsonb) returns uuid
language plpgsql security definer set search_path = public as $$
declare
    v_bill recurring_expenses;
    v_id uuid;
begin
    select * into v_bill from recurring_expenses where id = p_recurring_id;
    if not found or not auth_is_house_member(v_bill.house_id) then raise exception 'Bill not found'; end if;
    if not v_bill.allow_prepayment and p_date < v_bill.next_due_date - v_bill.reminder_days_before then
        raise exception 'This bill is not due yet';
    end if;
    insert into expenses (house_id, name, amount, category, date, notes, split_method, recurring_expense_id, created_by)
    values (v_bill.house_id, v_bill.name, p_amount, v_bill.category, p_date, v_bill.notes, v_bill.split_method, v_bill.id, auth.uid())
    returning id into v_id;
    perform write_expense_shares(v_id, p_shares);
    perform notify_expense(v_id, auth.uid(), 'bill_paid');
    return v_id;
end;
$$;

-- Per-diem

create function public.get_per_diem_entries(p_house_id uuid, p_month date)
returns table (
    id uuid, config_id uuid, item_name text, category text, unit text, rate numeric, quantity numeric, total_cost numeric,
    date date, added_by uuid, added_by_name text, notes text, created_at timestamptz
) language sql stable security invoker set search_path = public as $$
    select pe.id, pe.config_id, pc.item_name, pc.category, pc.unit, pe.rate, pe.quantity, pe.total_cost,
           pe.date, pe.added_by, p.full_name, pe.notes, pe.created_at
    from per_diem_entries pe
    join per_diem_config pc on pc.id = pe.config_id
    join profiles p on p.id = pe.added_by
    where pc.house_id = p_house_id
      and pe.date >= date_trunc('month', p_month)::date and pe.date < (date_trunc('month', p_month) + interval '1 month')::date
    order by pe.date desc, pe.created_at desc;
$$;

-- Per item for a month: total quantity and the sum of each entry's stored cost, at every price it had.
create function public.get_per_diem_bill_itemized(p_house_id uuid, p_month date)
returns table (config_id uuid, item_name text, category text, unit text, total_quantity numeric, total_cost numeric)
language sql stable security invoker set search_path = public as $$
    select pc.id, pc.item_name, pc.category, pc.unit, sum(pe.quantity), sum(pe.total_cost)
    from per_diem_entries pe
    join per_diem_config pc on pc.id = pe.config_id
    where pc.house_id = p_house_id
      and pe.date >= date_trunc('month', p_month)::date and pe.date < (date_trunc('month', p_month) + interval '1 month')::date
    group by pc.id, pc.item_name, pc.category, pc.unit
    order by 6 desc;
$$;

create function public.get_per_diem_bill_by_member(p_house_id uuid, p_month date)
returns table (user_id uuid, full_name text, total_cost numeric)
language sql stable security invoker set search_path = public as $$
    select pe.added_by, p.full_name, sum(pe.total_cost)
    from per_diem_entries pe
    join per_diem_config pc on pc.id = pe.config_id
    join profiles p on p.id = pe.added_by
    where pc.house_id = p_house_id
      and pe.date >= date_trunc('month', p_month)::date and pe.date < (date_trunc('month', p_month) + interval '1 month')::date
    group by pe.added_by, p.full_name
    order by 3 desc;
$$;

-- Turns [p_month]'s usage into one expense the caller paid, owed by each member exactly as their
-- entries cost, so per-diem reaches everyone's balance. A month is billed once; its entries are then
-- fixed until the bill is deleted.
create function public.bill_per_diem_month(p_house_id uuid, p_month date) returns uuid
language plpgsql security definer set search_path = public as $$
declare
    v_month date := date_trunc('month', p_month)::date;
    v_total numeric;
    v_id uuid;
begin
    if not auth_is_house_member(p_house_id) then raise exception 'Not a member of this house'; end if;
    if exists (select 1 from expenses where house_id = p_house_id and per_diem_month = v_month) then
        raise exception 'This month''s usage has already been billed';
    end if;
    select sum(pe.total_cost) into v_total
    from per_diem_entries pe join per_diem_config pc on pc.id = pe.config_id
    where pc.house_id = p_house_id and pe.date >= v_month and pe.date < v_month + interval '1 month';
    if coalesce(v_total, 0) <= 0 then raise exception 'There is no usage to bill this month'; end if;

    insert into expenses (house_id, name, amount, category, date, split_method, per_diem_month, created_by)
    values (p_house_id, 'Usage for ' || to_char(v_month, 'FMMonth YYYY'), v_total, 'Usage',
            least((v_month + interval '1 month' - interval '1 day')::date, (now() at time zone (select timezone from house_config where house_id = p_house_id))::date),
            'exact', v_month, auth.uid())
    returning id into v_id;
    insert into expense_shares (expense_id, user_id, paid_share, owed_share, split_value)
    select v_id, coalesce(owed.user_id, auth.uid()),
           case when coalesce(owed.user_id, auth.uid()) = auth.uid() then v_total else 0 end,
           coalesce(owed.cost, 0), owed.cost
    from (select pe.added_by as user_id, sum(pe.total_cost) as cost
          from per_diem_entries pe join per_diem_config pc on pc.id = pe.config_id
          where pc.house_id = p_house_id and pe.date >= v_month and pe.date < v_month + interval '1 month'
          group by pe.added_by) owed
    full join (select auth.uid() as user_id) payer on payer.user_id = owed.user_id;
    perform notify_expense(v_id, auth.uid(), 'expense_added');
    return v_id;
end;
$$;

-- Notifications

create function public.mark_notifications_read(p_ids uuid[]) returns void
language sql security invoker set search_path = public as $$
    update notifications set is_read = true
    where user_id = auth.uid() and not is_read and (p_ids is null or id = any (p_ids));
$$;

create function public.set_notification_preference(p_house_id uuid, p_type text, p_enabled boolean) returns void
language plpgsql security definer set search_path = public as $$
begin
    if not auth_is_house_member(p_house_id) then raise exception 'Not a member of this house'; end if;
    insert into notification_preferences (user_id, house_id, type, is_enabled) values (auth.uid(), p_house_id, p_type, p_enabled)
    on conflict (user_id, house_id, type) do update set is_enabled = excluded.is_enabled, updated_at = now();
end;
$$;

create function public.register_device_token(p_token text, p_platform text) returns void
language sql security definer set search_path = public as $$
    insert into device_tokens (token, user_id, platform) values (p_token, auth.uid(), p_platform)
    on conflict (token) do update set user_id = auth.uid(), platform = excluded.platform, updated_at = now();
$$;

-- Stops pushes to this device for the signed-in user, which signing out must do first.
create function public.unregister_device_token(p_token text) returns void
language sql security definer set search_path = public as $$
    delete from device_tokens where token = p_token and user_id = auth.uid();
$$;
