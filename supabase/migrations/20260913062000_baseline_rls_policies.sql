-- Baseline: row-level security policies for the public schema.
--
-- RLS is the layer that was working correctly. It is recorded here so a reviewer can see
-- what it actually enforces, and so the policies survive a project rebuild.

-- chores
alter table public.chores enable row level security;
create policy "chores_delete" on public.chores for delete to public
  using (auth_is_house_admin(house_id));
create policy "chores_insert" on public.chores for insert to public
  with check (auth_is_house_member(house_id));
create policy "chores_select" on public.chores for select to public
  using (auth_is_house_member(house_id));
create policy "chores_update" on public.chores for update to public
  using (auth_is_house_member(house_id));

-- documents
alter table public.documents enable row level security;
create policy "documents_delete" on public.documents for delete to public
  using (((user_id = ( SELECT auth.uid() AS uid)) OR ((house_id IS NOT NULL) AND auth_is_house_admin(house_id))));
create policy "documents_insert" on public.documents for insert to public
  with check (((user_id = ( SELECT auth.uid() AS uid)) AND ((house_id IS NULL) OR auth_is_house_member(house_id))));
create policy "documents_select" on public.documents for select to public
  using ((((house_id IS NULL) AND (user_id = ( SELECT auth.uid() AS uid))) OR ((house_id IS NOT NULL) AND auth_is_house_member(house_id))));

-- expense_splits
alter table public.expense_splits enable row level security;
create policy "expense_splits_delete" on public.expense_splits for delete to public
  using ((EXISTS ( SELECT 1
   FROM one_time_expenses ote
  WHERE ((ote.id = expense_splits.expense_id) AND auth_is_house_admin(ote.house_id)))));
create policy "expense_splits_insert" on public.expense_splits for insert to public
  with check ((EXISTS ( SELECT 1
   FROM one_time_expenses ote
  WHERE ((ote.id = expense_splits.expense_id) AND auth_is_house_member(ote.house_id)))));
create policy "expense_splits_select" on public.expense_splits for select to public
  using ((EXISTS ( SELECT 1
   FROM one_time_expenses ote
  WHERE ((ote.id = expense_splits.expense_id) AND auth_is_house_member(ote.house_id)))));
create policy "expense_splits_update" on public.expense_splits for update to public
  using ((EXISTS ( SELECT 1
   FROM one_time_expenses ote
  WHERE ((ote.id = expense_splits.expense_id) AND auth_is_house_admin(ote.house_id)))));

-- house_audit_log
alter table public.house_audit_log enable row level security;
create policy "house_audit_log_insert" on public.house_audit_log for insert to public
  with check (auth_is_house_member(house_id));
create policy "house_audit_log_select" on public.house_audit_log for select to public
  using (auth_is_house_admin(house_id));

-- house_config
alter table public.house_config enable row level security;
create policy "house_config_delete" on public.house_config for delete to public
  using ((EXISTS ( SELECT 1
   FROM houses
  WHERE ((houses.id = house_config.house_id) AND (houses.owner_id = ( SELECT auth.uid() AS uid))))));
create policy "house_config_insert" on public.house_config for insert to public
  with check ((EXISTS ( SELECT 1
   FROM houses
  WHERE ((houses.id = house_config.house_id) AND (houses.owner_id = ( SELECT auth.uid() AS uid))))));
create policy "house_config_select" on public.house_config for select to public
  using (auth_is_house_member(house_id));
create policy "house_config_update" on public.house_config for update to public
  using ((EXISTS ( SELECT 1
   FROM houses
  WHERE ((houses.id = house_config.house_id) AND (houses.owner_id = ( SELECT auth.uid() AS uid))))));

-- house_invitations
alter table public.house_invitations enable row level security;
create policy "house_invitations_delete" on public.house_invitations for delete to public
  using (auth_is_house_admin(house_id));
create policy "house_invitations_insert" on public.house_invitations for insert to public
  with check (auth_is_house_member(house_id));
create policy "house_invitations_select" on public.house_invitations for select to public
  using ((auth_is_house_member(house_id) OR (invitee_email = (( SELECT users.email
   FROM auth.users
  WHERE (users.id = ( SELECT auth.uid() AS uid))))::text)));
create policy "house_invitations_update" on public.house_invitations for update to public
  using ((auth_is_house_admin(house_id) OR (invitee_email = (( SELECT users.email
   FROM auth.users
  WHERE (users.id = ( SELECT auth.uid() AS uid))))::text)));

-- house_members
alter table public.house_members enable row level security;
create policy "house_members_admin_delete" on public.house_members for delete to public
  using ((auth_is_house_admin(house_id) OR (user_id = ( SELECT auth.uid() AS uid))));
create policy "house_members_admin_insert" on public.house_members for insert to public
  with check (auth_is_house_admin(house_id));
create policy "house_members_admin_update" on public.house_members for update to public
  using ((auth_is_house_admin(house_id) OR (user_id = ( SELECT auth.uid() AS uid))));
create policy "house_members_select" on public.house_members for select to public
  using (((user_id = ( SELECT auth.uid() AS uid)) OR (house_id IN ( SELECT auth_user_house_ids() AS auth_user_house_ids))));

-- houses
alter table public.houses enable row level security;
create policy "houses_delete" on public.houses for delete to public
  using ((owner_id = ( SELECT auth.uid() AS uid)));
create policy "houses_insert" on public.houses for insert to public
  with check ((owner_id = ( SELECT auth.uid() AS uid)));
create policy "houses_select" on public.houses for select to public
  using (((owner_id = ( SELECT auth.uid() AS uid)) OR auth_is_house_member(id)));
create policy "houses_update" on public.houses for update to public
  using ((owner_id = ( SELECT auth.uid() AS uid)));

-- invitation_rate_limit
alter table public.invitation_rate_limit enable row level security;
create policy "Users can view own rate limits" on public.invitation_rate_limit for select to public
  using ((user_id = ( SELECT auth.uid() AS uid)));

-- messages
alter table public.messages enable row level security;
create policy "messages_delete" on public.messages for delete to public
  using ((user_id = ( SELECT auth.uid() AS uid)));
create policy "messages_insert" on public.messages for insert to public
  with check (((user_id = ( SELECT auth.uid() AS uid)) AND auth_is_house_member(house_id)));
create policy "messages_select" on public.messages for select to public
  using (auth_is_house_member(house_id));

-- notification_preferences
alter table public.notification_preferences enable row level security;
create policy "notification_preferences_all" on public.notification_preferences for all to public
  using ((user_id = ( SELECT auth.uid() AS uid)))
  with check ((user_id = ( SELECT auth.uid() AS uid)));

-- notifications
alter table public.notifications enable row level security;
create policy "notifications_delete" on public.notifications for delete to public
  using ((user_id = ( SELECT auth.uid() AS uid)));
create policy "notifications_select" on public.notifications for select to public
  using ((user_id = ( SELECT auth.uid() AS uid)));
create policy "notifications_update" on public.notifications for update to public
  using ((user_id = ( SELECT auth.uid() AS uid)));

-- one_time_expenses
alter table public.one_time_expenses enable row level security;
create policy "one_time_expenses_delete" on public.one_time_expenses for delete to public
  using (auth_is_house_admin(house_id));
create policy "one_time_expenses_insert" on public.one_time_expenses for insert to public
  with check (auth_is_house_member(house_id));
create policy "one_time_expenses_select" on public.one_time_expenses for select to public
  using (auth_is_house_member(house_id));
create policy "one_time_expenses_update" on public.one_time_expenses for update to public
  using (((paid_by = ( SELECT auth.uid() AS uid)) AND auth_is_house_member(house_id)));

-- payment_history
alter table public.payment_history enable row level security;
create policy "payment_history_insert" on public.payment_history for insert to public
  with check (((paid_by = ( SELECT auth.uid() AS uid)) AND (EXISTS ( SELECT 1
   FROM recurring_expenses re
  WHERE ((re.id = payment_history.recurring_expense_id) AND auth_is_house_member(re.house_id))))));
create policy "payment_history_select" on public.payment_history for select to public
  using ((EXISTS ( SELECT 1
   FROM recurring_expenses re
  WHERE ((re.id = payment_history.recurring_expense_id) AND auth_is_house_member(re.house_id)))));

-- per_diem_config
alter table public.per_diem_config enable row level security;
create policy "per_diem_config_delete" on public.per_diem_config for delete to public
  using (auth_is_house_admin(house_id));
create policy "per_diem_config_insert" on public.per_diem_config for insert to public
  with check (auth_is_house_admin(house_id));
create policy "per_diem_config_select" on public.per_diem_config for select to public
  using (auth_is_house_member(house_id));
create policy "per_diem_config_update" on public.per_diem_config for update to public
  using (auth_is_house_admin(house_id));

-- per_diem_entries
alter table public.per_diem_entries enable row level security;
create policy "per_diem_entries_insert" on public.per_diem_entries for insert to public
  with check ((EXISTS ( SELECT 1
   FROM per_diem_config pdc
  WHERE ((pdc.id = per_diem_entries.config_id) AND auth_is_house_member(pdc.house_id)))));
create policy "per_diem_entries_select" on public.per_diem_entries for select to public
  using ((EXISTS ( SELECT 1
   FROM per_diem_config pdc
  WHERE ((pdc.id = per_diem_entries.config_id) AND auth_is_house_member(pdc.house_id)))));

-- profiles
alter table public.profiles enable row level security;
create policy "insert_own_profile" on public.profiles for insert to public
  with check ((id = ( SELECT auth.uid() AS uid)));
create policy "members_can_view_housemate_profiles" on public.profiles for select to public
  using (((id = ( SELECT auth.uid() AS uid)) OR shares_house_with(id, ( SELECT auth.uid() AS uid))));
create policy "update_own_profile" on public.profiles for update to public
  using ((id = ( SELECT auth.uid() AS uid)));

-- recurring_expenses
alter table public.recurring_expenses enable row level security;
create policy "recurring_expenses_delete" on public.recurring_expenses for delete to public
  using (auth_is_house_admin(house_id));
create policy "recurring_expenses_insert" on public.recurring_expenses for insert to public
  with check (auth_is_house_member(house_id));
create policy "recurring_expenses_select" on public.recurring_expenses for select to public
  using (auth_is_house_member(house_id));
create policy "recurring_expenses_update" on public.recurring_expenses for update to public
  using (auth_is_house_admin(house_id));

-- shopping_items
alter table public.shopping_items enable row level security;
create policy "shopping_items_delete" on public.shopping_items for delete to public
  using (auth_is_house_member(house_id));
create policy "shopping_items_insert" on public.shopping_items for insert to public
  with check (auth_is_house_member(house_id));
create policy "shopping_items_select" on public.shopping_items for select to public
  using (auth_is_house_member(house_id));
create policy "shopping_items_update" on public.shopping_items for update to public
  using (auth_is_house_member(house_id));

-- transactions
alter table public.transactions enable row level security;
create policy "transactions_insert" on public.transactions for insert to public
  with check (((payer_id = ( SELECT auth.uid() AS uid)) AND auth_is_house_member(house_id)));
create policy "transactions_select" on public.transactions for select to public
  using (auth_is_house_member(house_id));
