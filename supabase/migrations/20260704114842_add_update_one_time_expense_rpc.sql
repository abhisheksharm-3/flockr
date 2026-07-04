-- Atomically update a one-time expense and replace its split rows in a single
-- transaction, so a partial failure can no longer wipe every split for the expense
-- (the client previously did a non-transactional delete-then-insert). Payer exclusion
-- is handled server-side.
create or replace function public.update_one_time_expense(
    p_expense_id uuid,
    p_name text,
    p_amount numeric,
    p_category text,
    p_date date,
    p_notes text,
    p_splits jsonb
) returns void
language plpgsql
security definer
set search_path to 'public'
as $function$
declare
    v_house_id uuid;
    v_paid_by uuid;
    v_amount numeric;
    v_split_total numeric;
begin
    select house_id, paid_by, amount
    into v_house_id, v_paid_by, v_amount
    from one_time_expenses
    where id = p_expense_id;

    if v_house_id is null then
        raise exception 'Expense not found';
    end if;
    if not auth_is_house_member(v_house_id) then
        raise exception 'Not a member of this house';
    end if;
    if p_amount is not null and p_amount <= 0 then
        raise exception 'Amount must be greater than zero';
    end if;

    update one_time_expenses set
        name = coalesce(p_name, name),
        amount = coalesce(p_amount, amount),
        category = coalesce(p_category, category),
        date = coalesce(p_date, date),
        notes = coalesce(p_notes, notes)
    where id = p_expense_id;

    if p_splits is not null then
        select coalesce(sum((s->>'amount')::numeric), 0)
        into v_split_total
        from jsonb_array_elements(p_splits) s
        where (s->>'user_id')::uuid <> v_paid_by;

        if v_split_total < 0 or v_split_total > coalesce(p_amount, v_amount) + 0.005 then
            raise exception 'Split amounts (%) cannot exceed the expense amount (%)',
                v_split_total, coalesce(p_amount, v_amount);
        end if;

        delete from expense_splits where expense_id = p_expense_id;

        insert into expense_splits (expense_id, user_id, amount_owed)
        select p_expense_id, (s->>'user_id')::uuid, (s->>'amount')::numeric
        from jsonb_array_elements(p_splits) s
        where (s->>'user_id')::uuid <> v_paid_by;
    end if;
end;
$function$;

revoke all on function public.update_one_time_expense(uuid, text, numeric, text, date, text, jsonb) from public, anon;
grant execute on function public.update_one_time_expense(uuid, text, numeric, text, date, text, jsonb) to authenticated;
