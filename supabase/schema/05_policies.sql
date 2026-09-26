-- Row-level security. Every table has it enabled; a table with no policy for an action refuses it,
-- which is how writes are funnelled through the RPCs in 04_rpc.sql.

alter table public.currencies enable row level security;
alter table public.notification_types enable row level security;
alter table public.profiles enable row level security;
alter table public.houses enable row level security;
alter table public.house_config enable row level security;
alter table public.house_members enable row level security;
alter table public.house_invitations enable row level security;
alter table public.invitation_rate_limit enable row level security;
alter table public.house_audit_log enable row level security;
alter table public.recurring_expenses enable row level security;
alter table public.recurring_expense_shares enable row level security;
alter table public.expenses enable row level security;
alter table public.expense_shares enable row level security;
alter table public.per_diem_config enable row level security;
alter table public.per_diem_entries enable row level security;
alter table public.chores enable row level security;
alter table public.shopping_items enable row level security;
alter table public.messages enable row level security;
alter table public.documents enable row level security;
alter table public.notifications enable row level security;
alter table public.notification_preferences enable row level security;
alter table public.device_tokens enable row level security;
alter table public.member_locations enable row level security;

create policy "anyone signed in reads currencies" on public.currencies for select to authenticated using (true);
create policy "anyone signed in reads notification types" on public.notification_types for select to authenticated using (true);

create policy "read own and housemates' profiles" on public.profiles for select to authenticated
    using (id = (select auth.uid()) or auth_shares_house_with(id));
create policy "update own profile" on public.profiles for update to authenticated
    using (id = (select auth.uid())) with check (id = (select auth.uid()));

create policy "members read their houses" on public.houses for select to authenticated
    using (id in (select auth_house_ids()));
create policy "admins update their houses" on public.houses for update to authenticated
    using (auth_is_house_admin(id)) with check (auth_is_house_admin(id));

create policy "members read house config" on public.house_config for select to authenticated
    using (auth_is_house_member(house_id));
create policy "admins update house config" on public.house_config for update to authenticated
    using (auth_is_house_admin(house_id)) with check (auth_is_house_admin(house_id));

create policy "members read the roster" on public.house_members for select to authenticated
    using (auth_is_house_member(house_id));
create policy "admins update members" on public.house_members for update to authenticated
    using (auth_is_house_admin(house_id)) with check (auth_is_house_admin(house_id));

create policy "inviters, admins and invitees read invitations" on public.house_invitations for select to authenticated
    using (inviter_id = (select auth.uid()) or auth_is_house_admin(house_id)
           or invitee_email = (select email from public.profiles where id = (select auth.uid())));

create policy "members read the activity log" on public.house_audit_log for select to authenticated
    using (auth_is_house_member(house_id));

create policy "members read bills" on public.recurring_expenses for select to authenticated
    using (auth_is_house_member(house_id));
create policy "creators and admins delete bills" on public.recurring_expenses for delete to authenticated
    using (auth_is_house_member(house_id) and (created_by = (select auth.uid()) or auth_is_house_admin(house_id)));
create policy "members read bill splits" on public.recurring_expense_shares for select to authenticated
    using (exists (select 1 from public.recurring_expenses r where r.id = recurring_expense_id and auth_is_house_member(r.house_id)));

create policy "members read expenses" on public.expenses for select to authenticated
    using (auth_is_house_member(house_id));
create policy "members read expense shares" on public.expense_shares for select to authenticated
    using (exists (select 1 from public.expenses e where e.id = expense_id and auth_is_house_member(e.house_id)));

create policy "members read per-diem items" on public.per_diem_config for select to authenticated
    using (auth_is_house_member(house_id));
create policy "members add per-diem items" on public.per_diem_config for insert to authenticated
    with check (auth_is_house_member(house_id));
create policy "members edit per-diem items" on public.per_diem_config for update to authenticated
    using (auth_is_house_member(house_id)) with check (auth_is_house_member(house_id));
create policy "admins delete per-diem items" on public.per_diem_config for delete to authenticated
    using (auth_is_house_admin(house_id));

create policy "members read per-diem entries" on public.per_diem_entries for select to authenticated
    using (exists (select 1 from public.per_diem_config c where c.id = config_id and auth_is_house_member(c.house_id)));
create policy "members log their own per-diem use" on public.per_diem_entries for insert to authenticated
    with check (added_by = (select auth.uid())
                and exists (select 1 from public.per_diem_config c where c.id = config_id and auth_is_house_member(c.house_id)));
create policy "authors and admins edit per-diem entries" on public.per_diem_entries for update to authenticated
    using (exists (select 1 from public.per_diem_config c where c.id = config_id
                   and (added_by = (select auth.uid()) or auth_is_house_admin(c.house_id))));
create policy "authors and admins delete per-diem entries" on public.per_diem_entries for delete to authenticated
    using (exists (select 1 from public.per_diem_config c where c.id = config_id
                   and (added_by = (select auth.uid()) or auth_is_house_admin(c.house_id))));

create policy "members read chores" on public.chores for select to authenticated
    using (auth_is_house_member(house_id));
create policy "members add chores" on public.chores for insert to authenticated
    with check (auth_is_house_member(house_id) and created_by = (select auth.uid()));
create policy "members update chores" on public.chores for update to authenticated
    using (auth_is_house_member(house_id)) with check (auth_is_house_member(house_id));
create policy "creators and admins delete chores" on public.chores for delete to authenticated
    using (auth_is_house_member(house_id) and (created_by = (select auth.uid()) or auth_is_house_admin(house_id)));

create policy "members read the shopping list" on public.shopping_items for select to authenticated
    using (auth_is_house_member(house_id));
create policy "members add to the shopping list" on public.shopping_items for insert to authenticated
    with check (auth_is_house_member(house_id) and added_by = (select auth.uid()));
create policy "members update the shopping list" on public.shopping_items for update to authenticated
    using (auth_is_house_member(house_id)) with check (auth_is_house_member(house_id));
create policy "members remove from the shopping list" on public.shopping_items for delete to authenticated
    using (auth_is_house_member(house_id));

create policy "members read messages" on public.messages for select to authenticated
    using (auth_is_house_member(house_id));
create policy "members send messages as themselves" on public.messages for insert to authenticated
    with check (auth_is_house_member(house_id) and user_id = (select auth.uid()));
create policy "authors delete their messages" on public.messages for delete to authenticated
    using (user_id = (select auth.uid()));

create policy "read personal and house documents" on public.documents for select to authenticated
    using ((house_id is null and user_id = (select auth.uid())) or (house_id is not null and auth_is_house_member(house_id)));
create policy "upload documents as yourself" on public.documents for insert to authenticated
    with check (user_id = (select auth.uid()) and (house_id is null or auth_is_house_member(house_id)));
create policy "owners and admins delete documents" on public.documents for delete to authenticated
    using (user_id = (select auth.uid()) or (house_id is not null and auth_is_house_admin(house_id)));

create policy "read own notifications" on public.notifications for select to authenticated
    using (user_id = (select auth.uid()));
create policy "update own notifications" on public.notifications for update to authenticated
    using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
create policy "delete own notifications" on public.notifications for delete to authenticated
    using (user_id = (select auth.uid()));

create policy "read own notification preferences" on public.notification_preferences for select to authenticated
    using (user_id = (select auth.uid()));

-- A shared point is visible to the house while it's live and its sharer still lives there. Writes go
-- only through the location functions, which check the caller themselves.
create policy "members see live locations" on public.member_locations for select to authenticated
    using (
        auth_is_house_member(house_id) and expires_at > now()
        and exists (select 1 from house_members m where m.house_id = member_locations.house_id and m.user_id = member_locations.user_id and m.left_at is null)
    );
