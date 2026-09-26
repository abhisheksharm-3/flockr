-- The ledger's rules, checked end to end against a real schema inside one transaction that is always
-- rolled back, so it leaves nothing behind. Every check that fails raises and stops the run.
--
--   psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f supabase/tests/ledger_test.sql

begin;

create function pg_temp.act_as(p_user uuid) returns void language sql as $$
    select set_config('request.jwt.claims', json_build_object('sub', p_user, 'role', 'authenticated')::text, true);
$$;

create function pg_temp.refuses(p_sql text, p_message text) returns void language plpgsql as $$
begin
    execute p_sql;
    raise exception 'expected a refusal containing "%", but it succeeded: %', p_message, p_sql;
exception when others then
    if sqlerrm not ilike '%' || p_message || '%' then
        raise exception 'expected a refusal containing "%", got: %', p_message, sqlerrm;
    end if;
end;
$$;

create function pg_temp.net(p_house uuid, p_user uuid) returns numeric language sql as $$
    select net from public.get_balances(p_house) where user_id = p_user;
$$;

create function pg_temp.shares(variadic p_rows text[]) returns jsonb language sql as $$
    select jsonb_agg(r::jsonb) from unnest(p_rows) r;
$$;

do $$
declare
    v_owner uuid := gen_random_uuid();
    v_mate uuid := gen_random_uuid();
    v_outsider uuid := gen_random_uuid();
    v_house uuid;
    v_expense uuid;
    v_plan record;
begin
    insert into auth.users (id, email, raw_user_meta_data) values
        (v_owner, 'owner@ledger.test', '{"full_name": "Owner"}'),
        (v_mate, 'mate@ledger.test', '{"full_name": "Mate"}'),
        (v_outsider, 'outsider@ledger.test', '{"full_name": "Outsider"}');

    perform pg_temp.act_as(v_owner);
    v_house := public.create_house('Ledger House', null, null, null, null, 'INR', 'dd/MM/yyyy', 1::smallint, 'Asia/Kolkata');
    perform pg_temp.act_as(v_mate);
    assert public.join_house_with_invite_code((select invite_code from public.houses where id = v_house)) = v_house,
        'a housemate joins with the invite code';

    perform pg_temp.act_as(v_owner);
    v_expense := public.save_expense(null, v_house, 'Groceries', 100, 'Groceries', '2026-09-20', null, 'equal', pg_temp.shares(
        format('{"user_id": "%s", "paid_share": 100, "owed_share": 50, "split_value": 1}', v_owner),
        format('{"user_id": "%s", "owed_share": 50, "split_value": 1}', v_mate)));
    assert pg_temp.net(v_house, v_owner) = 50 and pg_temp.net(v_house, v_mate) = -50, 'the payer is owed half';

    perform pg_temp.refuses(format($q$select public.save_expense(null, %L, 'Broken', 100, 'Groceries', '2026-09-20', null, 'exact',
        '[{"user_id": "%s", "paid_share": 100, "owed_share": 40}, {"user_id": "%s", "owed_share": 50}]')$q$, v_house, v_owner, v_mate),
        'add up');
    perform pg_temp.refuses(format($q$select public.save_expense(null, %L, 'Fraction', 10.005, 'Groceries', '2026-09-20', null, 'exact',
        '[{"user_id": "%s", "paid_share": 10.005, "owed_share": 10.005}]')$q$, v_house, v_owner),
        'INR');
    perform pg_temp.refuses(format($q$select public.save_expense(null, %L, 'Outsider', 10, 'Groceries', '2026-09-20', null, 'exact',
        '[{"user_id": "%s", "paid_share": 10, "owed_share": 5}, {"user_id": "%s", "owed_share": 5}]')$q$, v_house, v_owner, v_outsider),
        'belong to the house');

    select * into v_plan from public.get_settle_up_plan(v_house);
    assert v_plan.from_user_id = v_mate and v_plan.to_user_id = v_owner and v_plan.amount = 50,
        'one payment of 50 from the housemate settles the house';
    assert (select count(*) from public.get_settle_up_plan(v_house)) = 1, 'the plan has exactly one payment';

    perform pg_temp.act_as(v_outsider);
    perform pg_temp.refuses(format('select public.settle_up(%L, %L, %L, 1, %L, null)', v_house, v_mate, v_owner, '2026-09-21'), 'Not a member');

    perform pg_temp.act_as(v_owner);
    perform public.settle_up(v_house, v_mate, v_owner, 70, '2026-09-21', null);
    assert pg_temp.net(v_house, v_owner) = -20 and pg_temp.net(v_house, v_mate) = 20, 'overpaying by 20 flips the balance';
    assert (select total_spend from public.get_monthly_summary(v_house, '2026-09-01')) = 100, 'a payment is not spending';

    perform pg_temp.refuses(format('select public.set_expense_receipt(%L, %L)', v_expense, gen_random_uuid() || '/x.jpg'), 'receipt');
    perform public.set_expense_receipt(v_expense, v_house || '/' || v_owner || '/r.jpg');
    assert (select receipt_path from public.expenses where id = v_expense) like v_house || '/%', 'a receipt attaches';

    perform pg_temp.refuses('select public.delete_my_account()', 'Hand Ledger House over');

    perform pg_temp.act_as(v_mate);
    perform public.delete_my_account();
    assert not exists (select 1 from auth.users where id = v_mate), 'the account is gone';
    assert (select full_name from public.profiles where id = v_mate) = 'Deleted account', 'the profile is blanked';
    assert pg_temp.net(v_house, v_mate) = 20 and pg_temp.net(v_house, v_owner) = -20, 'the ledger still adds up without them';

    perform pg_temp.act_as(v_owner);
    perform public.delete_my_account();
    assert not exists (select 1 from public.houses where id = v_house), 'a house its last member leaves is deleted with them';

    raise notice 'ledger: all checks passed';
end;
$$;

rollback;
