-- Tables whose changes are streamed to open apps. Recreating the schema drops them from the
-- publication, so they are added back here.

alter publication supabase_realtime add table
    public.houses, public.house_members, public.house_config, public.expenses, public.expense_shares,
    public.recurring_expenses, public.per_diem_config, public.per_diem_entries, public.chores, public.shopping_items, public.messages,
    public.documents, public.notifications, public.member_locations;

-- A stopped share is a delete, and realtime can only filter a delete by house when the old row is
-- sent whole, so this small table keeps its full old row in the log.
alter table public.member_locations replica identity full;
