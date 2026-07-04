-- Mark a recurring bill paid in a single transaction: insert the one-time expense,
-- its splits, the payment-history row, and update last_paid_date. Previously three
-- separate client calls that could half-apply (orphan history row + a retry
-- double-inserting the expense).
create or replace function public.mark_recurring_bill_paid(
    p_recurring_id uuid,
    p_house_id uuid,
    p_paid_by uuid,
    p_name text,
    p_amount numeric,
    p_category text,
    p_date date,
    p_notes text,
    p_splits jsonb
) returns uuid
language plpgsql
security definer
set search_path to 'public'
as $function$
declare
    v_house_id uuid;
    v_expense_id uuid;
    v_split jsonb;
    v_split_total numeric;
begin
    -- Authorize against the bill's actual house, not caller-supplied input: otherwise a
    -- member of house A could pass their own p_house_id plus a p_recurring_id from house B
    -- and tamper with house B's bill (SECURITY DEFINER bypasses RLS).
    select house_id into v_house_id
    from recurring_expenses
    where id = p_recurring_id;

    if v_house_id is null then
        raise exception 'Recurring bill not found';
    end if;
    if v_house_id <> p_house_id then
        raise exception 'Recurring bill does not belong to the given house';
    end if;
    if not auth_is_house_member(v_house_id) then
        raise exception 'Not a member of this house';
    end if;
    if not is_house_member(v_house_id, p_paid_by) then
        raise exception 'Payer must be a member of this house';
    end if;
    if p_amount is null or p_amount <= 0 then
        raise exception 'Amount must be greater than zero';
    end if;

    select coalesce(sum((s->>'amount')::numeric), 0)
    into v_split_total
    from jsonb_array_elements(coalesce(p_splits, '[]'::jsonb)) s;

    if v_split_total < 0 or v_split_total > p_amount + 0.005 then
        raise exception 'Split amounts (%) cannot exceed the expense amount (%)', v_split_total, p_amount;
    end if;

    insert into one_time_expenses (house_id, paid_by, name, amount, category, date, notes)
    values (v_house_id, p_paid_by, p_name, p_amount, p_category, p_date, p_notes)
    returning id into v_expense_id;

    for v_split in select * from jsonb_array_elements(coalesce(p_splits, '[]'::jsonb))
    loop
        insert into expense_splits (expense_id, user_id, amount_owed)
        values (v_expense_id, (v_split->>'user_id')::uuid, (v_split->>'amount')::numeric);
    end loop;

    insert into payment_history (recurring_expense_id, paid_by, amount, payment_date)
    values (p_recurring_id, p_paid_by, p_amount, p_date);

    update recurring_expenses set last_paid_date = p_date where id = p_recurring_id;

    return v_expense_id;
end;
$function$;

revoke all on function public.mark_recurring_bill_paid(uuid, uuid, uuid, text, numeric, text, date, text, jsonb) from public, anon;
grant execute on function public.mark_recurring_bill_paid(uuid, uuid, uuid, text, numeric, text, date, text, jsonb) to authenticated;
