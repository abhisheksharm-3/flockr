-- Tables whose changes are streamed to open apps. Recreating the schema drops them from the
-- publication, so they are added back here.

alter publication supabase_realtime add table
    public.houses, public.house_members, public.house_config, public.expenses, public.expense_shares,
    public.recurring_expenses, public.per_diem_entries, public.chores, public.shopping_items, public.messages,
    public.documents, public.notifications;
