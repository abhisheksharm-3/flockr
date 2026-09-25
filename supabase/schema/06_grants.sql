-- Privileges. Everything is revoked first and granted back explicitly, because Postgres gives PUBLIC
-- EXECUTE on every new function by default, and anon and authenticated inherit from PUBLIC. Revoking
-- from those two roles alone would leave every helper and trigger function callable over the API.

revoke all on all tables in schema public from public, anon, authenticated;
revoke all on all functions in schema public from public, anon, authenticated;
revoke all on all sequences in schema public from public, anon, authenticated;

grant usage on schema public to anon, authenticated, service_role;
grant all on all tables in schema public to service_role;
grant all on all functions in schema public to service_role;
grant all on all sequences in schema public to service_role;

grant select on
    public.currencies, public.notification_types, public.profiles, public.houses, public.house_config,
    public.house_members, public.house_invitations, public.house_audit_log, public.recurring_expenses,
    public.recurring_expense_shares, public.expenses, public.expense_shares, public.per_diem_config,
    public.per_diem_entries, public.chores, public.shopping_items, public.messages, public.documents,
    public.notifications, public.notification_preferences
to authenticated;

grant update (full_name, avatar_url, has_completed_onboarding) on public.profiles to authenticated;
grant update (name, address, latitude, longitude, header_image_url, max_members) on public.houses to authenticated;
grant update (currency_code, date_format, first_day_of_week, timezone) on public.house_config to authenticated;
grant update (role, default_split_weight) on public.house_members to authenticated;
grant delete on public.recurring_expenses to authenticated;

grant insert (house_id, item_name, rate, category, unit) on public.per_diem_config to authenticated;
grant update (item_name, rate, category, unit, is_active) on public.per_diem_config to authenticated;
grant delete on public.per_diem_config to authenticated;

grant insert (config_id, quantity, date, added_by, notes) on public.per_diem_entries to authenticated;
grant update (quantity, date, notes) on public.per_diem_entries to authenticated;
grant delete on public.per_diem_entries to authenticated;

grant insert (house_id, task_name, description, due_date, recurrence_pattern, rotation, effort_points, assigned_to, created_by)
    on public.chores to authenticated;
grant update (task_name, description, due_date, recurrence_pattern, rotation, effort_points, assigned_to,
              is_completed, completed_at, completed_by)
    on public.chores to authenticated;
grant delete on public.chores to authenticated;

grant insert (house_id, item_name, quantity, category, added_by) on public.shopping_items to authenticated;
grant update (item_name, quantity, category, is_purchased, purchased_by, purchased_at) on public.shopping_items to authenticated;
grant delete on public.shopping_items to authenticated;

grant insert (house_id, user_id, content) on public.messages to authenticated;
grant delete on public.messages to authenticated;

grant insert (house_id, user_id, storage_path, file_name, file_size, mime_type) on public.documents to authenticated;
grant delete on public.documents to authenticated;

grant update (is_read) on public.notifications to authenticated;
grant delete on public.notifications to authenticated;

-- Helpers the row-level security policies call as the signed-in user. Each derives identity from
-- auth.uid(), so exposing them reveals nothing about anyone else.
grant execute on function
    public.auth_is_house_member(uuid), public.auth_is_house_admin(uuid), public.auth_house_ids(),
    public.auth_shares_house_with(uuid)
to authenticated;

-- The API.
grant execute on function
    public.create_house(text, text, double precision, double precision, text, text, text, smallint, text),
    public.get_my_houses(),
    public.get_house_members(uuid),
    public.preview_house_by_invite_code(text),
    public.join_house_with_invite_code(text),
    public.regenerate_invite_code(uuid),
    public.remove_house_member(uuid, uuid),
    public.leave_house(uuid),
    public.transfer_house_ownership(uuid, uuid),
    public.delete_house(uuid),
    public.invite_to_house(uuid, text),
    public.get_my_pending_invitations(),
    public.respond_to_invitation(uuid, boolean),
    public.cancel_invitation(uuid),
    public.save_expense(uuid, uuid, text, numeric, text, date, text, text, jsonb),
    public.delete_expense(uuid),
    public.settle_up(uuid, uuid, numeric, date, text),
    public.get_balances(uuid),
    public.get_settle_up_plan(uuid),
    public.get_shared_history(uuid, uuid),
    public.get_monthly_summary(uuid, date),
    public.get_spend_by_member(uuid, date),
    public.get_spend_by_category(uuid, date),
    public.save_recurring_expense(uuid, uuid, text, numeric, text, text, smallint, date, boolean, smallint, boolean, text, jsonb, text),
    public.get_recurring_expenses(uuid),
    public.pay_recurring_expense(uuid, numeric, date, jsonb),
    public.get_per_diem_entries(uuid, date),
    public.get_per_diem_bill_itemized(uuid, date),
    public.get_per_diem_bill_by_member(uuid, date),
    public.mark_notifications_read(uuid[]),
    public.set_notification_preference(uuid, text, boolean),
    public.register_device_token(text, text)
to authenticated;

alter default privileges in schema public revoke all on tables from public, anon, authenticated;
alter default privileges in schema public revoke execute on functions from public, anon, authenticated;
