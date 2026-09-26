-- Attaches the trigger functions in 02_functions.sql to the tables they guard.

create trigger on_auth_user_created after insert on auth.users
    for each row execute function public.handle_new_user();

create trigger touch_profiles before update on public.profiles for each row execute function public.touch_updated_at();
create trigger touch_houses before update on public.houses for each row execute function public.touch_updated_at();
create trigger touch_house_config before update on public.house_config for each row execute function public.touch_updated_at();
create trigger touch_recurring before update on public.recurring_expenses for each row execute function public.touch_updated_at();
create trigger touch_expenses before update on public.expenses for each row execute function public.touch_updated_at();

create trigger assign_invite_code before insert on public.houses for each row execute function public.assign_invite_code();
create trigger setup_new_house after insert on public.houses for each row execute function public.setup_new_house();
create trigger validate_house_config before insert or update on public.house_config for each row execute function public.validate_house_config();

create trigger enforce_member_limit before insert or update of left_at on public.house_members
    for each row execute function public.enforce_member_limit();
create trigger guard_member_role before update of role on public.house_members
    for each row execute function public.guard_member_role();
create trigger notify_membership_change after insert or update of left_at on public.house_members
    for each row execute function public.notify_membership_change();

create constraint trigger expense_balanced after insert or update or delete on public.expense_shares
    deferrable initially deferred for each row execute function public.check_expense_balanced();
create constraint trigger expense_amount_balanced after insert or update of amount on public.expenses
    deferrable initially deferred for each row execute function public.check_expense_balanced();
create trigger sync_recurring_bill after insert or delete or update of recurring_expense_id on public.expenses
    for each row execute function public.sync_recurring_bill();

create trigger recurring_minor_units before insert or update of amount on public.recurring_expenses
    for each row execute function public.check_minor_units('amount');
create trigger per_diem_minor_units before insert or update of rate on public.per_diem_config
    for each row execute function public.check_minor_units('rate');

create trigger guard_billed_usage before insert or update or delete on public.per_diem_entries
    for each row execute function public.guard_billed_usage();
create trigger price_per_diem_entry before insert or update on public.per_diem_entries
    for each row execute function public.price_per_diem_entry();

create trigger schedule_next_chore before update of is_completed on public.chores
    for each row execute function public.schedule_next_chore();
create trigger notify_chore_change after insert or update of assigned_to, is_completed on public.chores
    for each row execute function public.notify_chore_change();

create trigger notify_message after insert on public.messages for each row execute function public.notify_message();
create trigger notify_shopping_item after insert on public.shopping_items for each row execute function public.notify_shopping_item();
create trigger notify_location_shared after insert on public.member_locations for each row execute function public.notify_location_shared();
create trigger notify_document after insert on public.documents for each row execute function public.notify_document();
create trigger notify_invitation after insert on public.house_invitations for each row execute function public.notify_invitation();

create trigger audit_expenses after insert or update or delete on public.expenses for each row execute function public.log_house_activity();
create trigger audit_chores after insert or update or delete on public.chores for each row execute function public.log_house_activity();
create trigger audit_members after insert or update or delete on public.house_members for each row execute function public.log_house_activity();

create trigger push_notification after insert on public.notifications
    for each row execute function public.push_notification();
