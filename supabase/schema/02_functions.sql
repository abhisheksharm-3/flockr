-- Helper functions and the trigger functions that keep the data consistent. Nothing here is callable
-- by a client; 06_grants.sql exposes only the RPCs in 04_rpc.sql. Every raise here and in 04_rpc.sql
-- uses the default SQLSTATE P0001 with a sentence written for the user, and the app shows exactly
-- those messages, so a raise must never carry internal detail.

-- Access helpers. Each derives the caller from auth.uid() and never trusts an argument for identity.

create function public.auth_is_house_member(p_house_id uuid) returns boolean
language sql stable security definer set search_path = public as $$
    select exists (
        select 1 from house_members
        where house_id = p_house_id and user_id = auth.uid() and left_at is null
    );
$$;

create function public.auth_is_house_admin(p_house_id uuid) returns boolean
language sql stable security definer set search_path = public as $$
    select exists (
        select 1 from house_members
        where house_id = p_house_id and user_id = auth.uid() and left_at is null and role in ('Owner', 'Admin')
    );
$$;

create function public.auth_house_ids() returns setof uuid
language sql stable security definer set search_path = public as $$
    select house_id from house_members where user_id = auth.uid() and left_at is null;
$$;

-- Whether [p_user_id] is, or once was, a member of a house the caller belongs to, which decides whose
-- profile the caller can see. Past members count, so their names stay on the expenses they shared.
create function public.auth_shares_house_with(p_user_id uuid) returns boolean
language sql stable security definer set search_path = public as $$
    select exists (
        select 1 from house_members mine
        join house_members theirs on theirs.house_id = mine.house_id
        where mine.user_id = auth.uid() and mine.left_at is null and theirs.user_id = p_user_id
    );
$$;

-- Whether [p_user_id] has ever belonged to [p_house_id]. Past members stay valid on old expenses.
create function public.was_house_member(p_house_id uuid, p_user_id uuid) returns boolean
language sql stable security definer set search_path = public as $$
    select exists (select 1 from house_members where house_id = p_house_id and user_id = p_user_id);
$$;

create function public.is_active_house_member(p_house_id uuid, p_user_id uuid) returns boolean
language sql stable security definer set search_path = public as $$
    select exists (select 1 from house_members where house_id = p_house_id and user_id = p_user_id and left_at is null);
$$;

-- The number of minor-unit digits the house's currency has: 2 for cents, 0 for yen.
create function public.house_minor_digits(p_house_id uuid) returns smallint
language sql stable security definer set search_path = public as $$
    select c.minor_digits from house_config hc join currencies c on c.code = hc.currency_code where hc.house_id = p_house_id;
$$;

-- The due date that follows [p_from] for a bill of [p_frequency]. Monthly and longer cadences land on
-- [p_due_day], clamped to the month's length, so a bill due on the 31st falls on the 30th in April.
create function public.next_due_date(p_from date, p_frequency text, p_custom_days smallint, p_due_day smallint)
returns date language plpgsql immutable as $$
declare
    v_months int;
    v_target date;
begin
    case p_frequency
        when 'daily' then return p_from + 1;
        when 'weekly' then return p_from + 7;
        when 'biweekly' then return p_from + 14;
        when 'custom' then return p_from + p_custom_days;
        when 'monthly' then v_months := 1;
        when 'quarterly' then v_months := 3;
        when 'semiannual' then v_months := 6;
        when 'yearly' then v_months := 12;
    end case;
    v_target := (date_trunc('month', p_from) + make_interval(months => v_months))::date;
    return v_target + (least(p_due_day, extract(day from (v_target + interval '1 month' - interval '1 day'))::int) - 1);
end;
$$;

-- Generic trigger functions.

create function public.touch_updated_at() returns trigger language plpgsql as $$
begin
    new.updated_at := now();
    return new;
end;
$$;

create function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
begin
    insert into profiles (id, email, full_name)
    values (new.id, lower(new.email), coalesce(new.raw_user_meta_data ->> 'full_name', ''))
    on conflict (id) do nothing;
    return new;
end;
$$;

-- The currency is fixed once the house has recorded money, because amounts carry no currency of their
-- own and switching it would relabel every past amount.
create function public.validate_house_config() returns trigger language plpgsql as $$
begin
    if not exists (select 1 from pg_timezone_names where name = new.timezone) then
        raise exception 'Unknown time zone %', new.timezone;
    end if;
    if tg_op = 'UPDATE' and new.currency_code <> old.currency_code
       and (exists (select 1 from public.expenses where house_id = new.house_id)
            or exists (select 1 from public.recurring_expenses where house_id = new.house_id)
            or exists (select 1 from public.per_diem_config where house_id = new.house_id)) then
        raise exception 'The currency cannot change once the house has recorded money';
    end if;
    return new;
end;
$$;

-- Refuses a row whose money columns, named as the trigger's arguments, have more decimals than the
-- house currency allows. Columns are unconstrained numeric so a bad value reaches this check instead
-- of being rounded silently on the way in.
create function public.check_minor_units() returns trigger
language plpgsql security definer set search_path = public as $$
declare
    v_row jsonb := to_jsonb(new);
    v_digits smallint := house_minor_digits((v_row ->> 'house_id')::uuid);
    v_column text;
    v_value numeric;
begin
    foreach v_column in array tg_argv loop
        v_value := (v_row ->> v_column)::numeric;
        if v_value <> round(v_value, v_digits) then
            raise exception 'Amounts must be whole units of the house currency';
        end if;
    end loop;
    return new;
end;
$$;

-- A new house gets its config, an invite code, and its owner as the first member.
create function public.setup_new_house() returns trigger
language plpgsql security definer set search_path = public as $$
begin
    insert into house_config (house_id) values (new.id);
    insert into house_members (house_id, user_id, role) values (new.id, new.owner_id, 'Owner');
    return new;
end;
$$;

-- An invite code admits whoever holds it, so its characters come from a cryptographic source. 32
-- divides 256, so taking each byte modulo 32 favours no character.
create function public.generate_invite_code() returns text language sql volatile as $$
    select string_agg(substr('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + get_byte(b, i) % 32, 1), '')
    from (select extensions.gen_random_bytes(8) as b) g, generate_series(0, 7) i;
$$;

create function public.assign_invite_code() returns trigger language plpgsql as $$
begin
    if new.invite_code is null then
        new.invite_code := generate_invite_code();
        new.invite_code_generated_at := now();
        new.invite_code_expires_at := now() + interval '7 days';
    end if;
    return new;
end;
$$;

-- Joining or rejoining counts against the house's member limit.
create function public.enforce_member_limit() returns trigger
language plpgsql security definer set search_path = public as $$
declare
    v_limit smallint;
    v_active int;
begin
    if new.left_at is not null then return new; end if;
    if tg_op = 'UPDATE' and old.left_at is null then return new; end if;
    select max_members into v_limit from houses where id = new.house_id;
    select count(*) into v_active from house_members where house_id = new.house_id and left_at is null and user_id <> new.user_id;
    if v_active >= v_limit then
        raise exception 'This house is full (% members)', v_limit;
    end if;
    return new;
end;
$$;

-- Money invariants, checked at commit so an RPC can write the expense and its shares in any order:
-- paid shares and owed shares each sum to the amount, every person on it belongs to the house, and
-- every amount fits the house currency's minor units.
create function public.check_expense_balanced() returns trigger
language plpgsql security definer set search_path = public as $$
declare
    v_row jsonb := to_jsonb(coalesce(new, old));
    v_expense_id uuid := coalesce((v_row ->> 'expense_id')::uuid, (v_row ->> 'id')::uuid);
    v_expense record;
    v_paid numeric;
    v_owed numeric;
    v_digits smallint;
begin
    select * into v_expense from expenses where id = v_expense_id;
    if not found then return null; end if;

    select coalesce(sum(paid_share), 0), coalesce(sum(owed_share), 0) into v_paid, v_owed
    from expense_shares where expense_id = v_expense_id;
    if v_paid <> v_expense.amount or v_owed <> v_expense.amount then
        raise exception 'Shares must add up to the expense: paid %, owed %, amount %', v_paid, v_owed, v_expense.amount;
    end if;

    v_digits := house_minor_digits(v_expense.house_id);
    if v_expense.amount <> round(v_expense.amount, v_digits)
       or exists (select 1 from expense_shares where expense_id = v_expense_id
                  and (paid_share <> round(paid_share, v_digits) or owed_share <> round(owed_share, v_digits))) then
        raise exception 'Amounts must be whole units of the house currency';
    end if;

    if exists (select 1 from expense_shares s where s.expense_id = v_expense_id
               and not was_house_member(v_expense.house_id, s.user_id)) then
        raise exception 'Everyone on an expense must belong to the house';
    end if;
    return null;
end;
$$;

-- A per-diem entry takes the item's current price and computes its cost when it is written. Later
-- edits recompute the cost from the entry's own price, never the item's new one.
create function public.price_per_diem_entry() returns trigger
language plpgsql security definer set search_path = public as $$
declare
    v_house_id uuid;
begin
    if tg_op = 'INSERT' then
        select rate, house_id into new.rate, v_house_id from per_diem_config where id = new.config_id;
    else
        if new.config_id <> old.config_id then
            raise exception 'An entry cannot move to another item';
        end if;
        new.rate := old.rate;
        select house_id into v_house_id from per_diem_config where id = new.config_id;
    end if;
    new.total_cost := round(new.quantity * new.rate, house_minor_digits(v_house_id));
    return new;
end;
$$;

-- A bill's next due date is its first due date advanced once per recorded payment, and its last paid
-- date is its latest payment's. Recording a payment moves the bill forward; deleting one moves it back.
create function public.sync_recurring_bill() returns trigger
language plpgsql security definer set search_path = public as $$
declare
    v_bill recurring_expenses;
    v_payments int;
    v_last_paid date;
    v_due date;
begin
    select * into v_bill from recurring_expenses
    where id = coalesce((to_jsonb(new) ->> 'recurring_expense_id')::uuid, (to_jsonb(old) ->> 'recurring_expense_id')::uuid);
    if not found then return null; end if;
    select count(*), max(date) into v_payments, v_last_paid from expenses where recurring_expense_id = v_bill.id;
    v_due := v_bill.first_due_date;
    for i in 1..v_payments loop
        v_due := next_due_date(v_due, v_bill.frequency, v_bill.custom_frequency_days, extract(day from v_bill.first_due_date)::smallint);
    end loop;
    update recurring_expenses set next_due_date = v_due, last_paid_date = v_last_paid where id = v_bill.id;
    return null;
end;
$$;

-- Completing a recurring chore schedules the next one, passed to the next person in its rotation.
-- The completed chore stays as history.
create function public.schedule_next_chore() returns trigger
language plpgsql security definer set search_path = public as $$
declare
    v_next_assignee uuid;
    v_position int;
begin
    if not new.is_completed or old.is_completed or new.recurrence_pattern is null then return new; end if;
    v_next_assignee := new.assigned_to;
    if cardinality(new.rotation) > 0 then
        v_position := coalesce(array_position(new.rotation, new.assigned_to), 0);
        v_next_assignee := new.rotation[(v_position % cardinality(new.rotation)) + 1];
    end if;
    insert into chores (house_id, task_name, description, due_date, recurrence_pattern, rotation, effort_points,
                        assigned_to, created_by)
    values (new.house_id, new.task_name, new.description,
            case new.recurrence_pattern
                when 'daily' then coalesce(new.due_date, current_date) + 1
                when 'weekly' then coalesce(new.due_date, current_date) + 7
                when 'monthly' then (coalesce(new.due_date, current_date) + interval '1 month')::date
                when 'yearly' then (coalesce(new.due_date, current_date) + interval '1 year')::date
            end,
            new.recurrence_pattern, new.rotation, new.effort_points, v_next_assignee, new.created_by);
    return new;
end;
$$;

-- Only admins change roles, nobody changes their own, and ownership moves only by transfer_house_ownership.
create function public.guard_member_role() returns trigger
language plpgsql security definer set search_path = public as $$
begin
    if new.role = old.role then return new; end if;
    if current_setting('flockr.transferring_ownership', true) = 'on' then return new; end if;
    if old.role = 'Owner' or new.role = 'Owner' then
        raise exception 'Ownership changes only through a transfer';
    end if;
    if auth.uid() is not null and (auth.uid() = new.user_id or not auth_is_house_admin(new.house_id)) then
        raise exception 'Only an admin can change another member''s role';
    end if;
    return new;
end;
$$;

create function public.log_house_activity() returns trigger
language plpgsql security definer set search_path = public as $$
declare
    v_row jsonb := to_jsonb(coalesce(new, old));
begin
    if not exists (select 1 from houses where id = (v_row ->> 'house_id')::uuid) then
        return coalesce(new, old);
    end if;
    insert into house_audit_log (house_id, user_id, action, target_user_id, details)
    values ((v_row ->> 'house_id')::uuid, auth.uid(), lower(tg_op) || '_' || tg_table_name,
            nullif(v_row ->> 'user_id', '')::uuid,
            jsonb_build_object('id', v_row ->> 'id'));
    return coalesce(new, old);
end;
$$;

-- Notifications. The server raises every notification; each respects the recipient's preference
-- for that type in that house, and nobody is notified about their own action.

create function public.notify(
    p_user_id uuid, p_house_id uuid, p_actor_id uuid, p_type text, p_title text, p_body text, p_data jsonb default '{}'
) returns void language plpgsql security definer set search_path = public as $$
begin
    if p_user_id is null or p_user_id = p_actor_id then return; end if;
    if exists (select 1 from notification_preferences
               where user_id = p_user_id and house_id = p_house_id and type = p_type and not is_enabled) then
        return;
    end if;
    insert into notifications (user_id, house_id, actor_id, type, title, body, data)
    values (p_user_id, p_house_id, p_actor_id, p_type, p_title, p_body, p_data);
end;
$$;

create function public.notify_house(
    p_house_id uuid, p_actor_id uuid, p_type text, p_title text, p_body text, p_data jsonb default '{}'
) returns void language plpgsql security definer set search_path = public as $$
declare
    v_member uuid;
begin
    for v_member in select user_id from house_members where house_id = p_house_id and left_at is null loop
        perform notify(v_member, p_house_id, p_actor_id, p_type, p_title, p_body, p_data);
    end loop;
end;
$$;

create function public.display_name(p_user_id uuid) returns text
language sql stable security definer set search_path = public as $$
    select coalesce(nullif(btrim(full_name), ''), split_part(email, '@', 1), 'Someone') from profiles where id = p_user_id;
$$;

create function public.notify_membership_change() returns trigger
language plpgsql security definer set search_path = public as $$
declare
    v_house text;
begin
    select name into v_house from houses where id = new.house_id;
    if tg_op = 'INSERT' or (old.left_at is not null and new.left_at is null) then
        perform notify_house(new.house_id, new.user_id, 'member_joined', 'New housemate',
                             display_name(new.user_id) || ' joined ' || v_house, jsonb_build_object('user_id', new.user_id));
    elsif old.left_at is null and new.left_at is not null then
        perform notify_house(new.house_id, new.user_id, 'member_left', 'Housemate left',
                             display_name(new.user_id) || ' left ' || v_house, jsonb_build_object('user_id', new.user_id));
    end if;
    return new;
end;
$$;

create function public.notify_chore_change() returns trigger
language plpgsql security definer set search_path = public as $$
begin
    if new.assigned_to is not null and (tg_op = 'INSERT' or new.assigned_to is distinct from old.assigned_to) then
        perform notify(new.assigned_to, new.house_id, auth.uid(), 'chore_assigned', 'Chore for you',
                       new.task_name, jsonb_build_object('chore_id', new.id));
    end if;
    if tg_op = 'UPDATE' and new.is_completed and not old.is_completed then
        perform notify(new.created_by, new.house_id, coalesce(new.completed_by, auth.uid()), 'chore_completed', 'Chore done',
                       display_name(coalesce(new.completed_by, auth.uid())) || ' finished ' || new.task_name,
                       jsonb_build_object('chore_id', new.id));
    end if;
    return new;
end;
$$;

create function public.notify_message() returns trigger
language plpgsql security definer set search_path = public as $$
begin
    perform notify_house(new.house_id, new.user_id, 'message', display_name(new.user_id), left(new.content, 140),
                         jsonb_build_object('message_id', new.id));
    return new;
end;
$$;

create function public.notify_shopping_item() returns trigger
language plpgsql security definer set search_path = public as $$
begin
    perform notify_house(new.house_id, new.added_by, 'shopping_item_added', 'Added to the list',
                         display_name(new.added_by) || ' added ' || new.item_name, jsonb_build_object('item_id', new.id));
    return new;
end;
$$;

create function public.notify_document() returns trigger
language plpgsql security definer set search_path = public as $$
begin
    if new.house_id is null then return new; end if;
    perform notify_house(new.house_id, new.user_id, 'document_uploaded', 'New document',
                         display_name(new.user_id) || ' added ' || new.file_name, jsonb_build_object('document_id', new.id));
    return new;
end;
$$;

create function public.notify_invitation() returns trigger
language plpgsql security definer set search_path = public as $$
declare
    v_invitee uuid;
begin
    select id into v_invitee from profiles where email = new.invitee_email;
    if v_invitee is not null then
        perform notify(v_invitee, null, new.inviter_id, 'invitation', 'House invitation',
                       display_name(new.inviter_id) || ' invited you to ' || (select name from houses where id = new.house_id),
                       jsonb_build_object('invitation_id', new.id, 'house_id', new.house_id));
    end if;
    return new;
end;
$$;

-- [p_amount] as the notification text shows it: the currency code, then the amount with thousands
-- separators and exactly the currency's decimals, such as "INR 1,050.00" or "JPY 1,000".
create function public.format_money(p_amount numeric, p_currency text) returns text
language sql stable set search_path = public as $$
    select p_currency || ' ' || to_char(round(p_amount, c.minor_digits),
                                        'FM999,999,999,990' || case when c.minor_digits > 0 then '.' || repeat('0', c.minor_digits) else '' end)
    from currencies c where c.code = p_currency;
$$;

-- Notifies everyone on [p_expense_id] except [p_actor_id]. Called by the expense RPCs once the shares
-- exist, which a trigger on the expense row alone could not see.
create function public.notify_expense(p_expense_id uuid, p_actor_id uuid, p_type text) returns void
language plpgsql security definer set search_path = public as $$
declare
    v_expense record;
    v_share record;
    v_currency text;
    v_payer uuid;
begin
    select * into v_expense from expenses where id = p_expense_id;
    select currency_code into v_currency from house_config where house_id = v_expense.house_id;
    select user_id into v_payer from expense_shares where expense_id = p_expense_id order by paid_share desc limit 1;
    for v_share in select * from expense_shares where expense_id = p_expense_id loop
        if v_expense.kind = 'settlement' and v_share.owed_share > 0 then
            perform notify(v_share.user_id, v_expense.house_id, p_actor_id, 'settlement_received', 'You were paid back',
                           display_name(v_payer) || ' paid you ' || format_money(v_expense.amount, v_currency),
                           jsonb_build_object('expense_id', p_expense_id));
        elsif v_expense.kind = 'settlement' then
            perform notify(v_share.user_id, v_expense.house_id, p_actor_id, 'settlement_recorded', 'Payment recorded',
                           display_name(p_actor_id) || ' recorded that you paid them ' || format_money(v_expense.amount, v_currency),
                           jsonb_build_object('expense_id', p_expense_id));
        elsif v_share.owed_share > 0 then
            perform notify(v_share.user_id, v_expense.house_id, p_actor_id, p_type,
                           case p_type when 'expense_updated' then 'Expense changed' when 'bill_paid' then 'Bill paid' else 'New expense' end,
                           v_expense.name || ': your share ' || format_money(v_share.owed_share, v_currency),
                           jsonb_build_object('expense_id', p_expense_id));
        end if;
    end loop;
end;
$$;

-- Reminds each person sharing a recurring bill when it falls within its reminder window. Run hourly,
-- it acts only in the hour a house's clock reads 9am, so each house gets one reminder at a sensible
-- local time.
create function public.send_bill_reminders() returns void
language plpgsql security definer set search_path = public as $$
declare
    v_bill record;
    v_member uuid;
begin
    for v_bill in
        select r.* from recurring_expenses r
        join house_config hc on hc.house_id = r.house_id
        where r.is_active and r.reminder_enabled
          and extract(hour from now() at time zone hc.timezone) = 9
          and r.next_due_date = (now() at time zone hc.timezone)::date + r.reminder_days_before
    loop
        for v_member in
            select m.user_id from house_members m
            where m.house_id = v_bill.house_id and m.left_at is null
              and (v_bill.split_method is null
                   or m.user_id = v_bill.created_by
                   or exists (select 1 from recurring_expense_shares s
                              where s.recurring_expense_id = v_bill.id and s.user_id = m.user_id))
        loop
            perform notify(v_member, v_bill.house_id, null, 'bill_due', 'Bill due soon',
                           v_bill.name || ' is due ' || to_char(v_bill.next_due_date, 'DD Mon'),
                           jsonb_build_object('recurring_expense_id', v_bill.id));
        end loop;
    end loop;
end;
$$;
