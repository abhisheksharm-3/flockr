-- Tables, keys and constraints for the public schema. Every rule that can be a constraint is one,
-- so a row that breaks the app's model cannot be written by any client.

-- Currencies a house can use, with the number of minor-unit digits each has. The database and the
-- app both round money to these digits, so this table is the one place that knowledge lives here.
create table public.currencies (
    code         text primary key check (code ~ '^[A-Z]{3}$'),
    minor_digits smallint not null check (minor_digits between 0 and 3)
);

insert into public.currencies (code, minor_digits) values
    ('USD', 2), ('EUR', 2), ('GBP', 2), ('JPY', 0), ('INR', 2), ('CAD', 2), ('AUD', 2), ('CNY', 2);

create table public.profiles (
    id                       uuid primary key references auth.users (id) on delete cascade,
    email                    text not null,
    full_name                text not null default '',
    avatar_url               text,
    has_completed_onboarding boolean not null default false,
    created_at               timestamptz not null default now(),
    updated_at               timestamptz not null default now()
);

create table public.houses (
    id                       uuid primary key default gen_random_uuid(),
    name                     text not null check (length(btrim(name)) between 1 and 100),
    owner_id                 uuid not null references public.profiles (id),
    address                  text,
    latitude                 double precision check (latitude between -90 and 90),
    longitude                double precision check (longitude between -180 and 180),
    header_image_url         text,
    invite_code              text unique,
    invite_code_generated_at timestamptz,
    invite_code_expires_at   timestamptz,
    max_members              smallint not null default 20 check (max_members between 1 and 100),
    created_at               timestamptz not null default now(),
    updated_at               timestamptz not null default now(),
    check ((latitude is null) = (longitude is null))
);

-- One row per house. first_day_of_week counts from 0 for Sunday.
create table public.house_config (
    house_id          uuid primary key references public.houses (id) on delete cascade,
    currency_code     text not null default 'USD' references public.currencies (code),
    date_format       text not null default 'yyyy-MM-dd' check (date_format in ('dd/MM/yyyy', 'MM/dd/yyyy', 'yyyy-MM-dd')),
    first_day_of_week smallint not null default 0 check (first_day_of_week between 0 and 6),
    timezone          text not null default 'UTC',
    created_at        timestamptz not null default now(),
    updated_at        timestamptz not null default now()
);

-- Membership keeps its row after someone leaves, because their expense history still points at it.
-- is_active is derived from left_at so the two can never disagree. default_split_weight is the
-- member's share when an expense is split by shares, set once for the house (a larger room, say).
create table public.house_members (
    house_id             uuid not null references public.houses (id) on delete cascade,
    user_id              uuid not null references public.profiles (id) on delete cascade,
    role                 text not null default 'Member' check (role in ('Owner', 'Admin', 'Member')),
    default_split_weight numeric(8, 3) not null default 1 check (default_split_weight > 0),
    joined_at            timestamptz not null default now(),
    left_at              timestamptz,
    is_active            boolean generated always as (left_at is null) stored,
    primary key (house_id, user_id)
);

create unique index house_members_one_owner on public.house_members (house_id) where role = 'Owner' and left_at is null;
create index house_members_user on public.house_members (user_id) where left_at is null;

create table public.house_invitations (
    id            uuid primary key default gen_random_uuid(),
    house_id      uuid not null references public.houses (id) on delete cascade,
    inviter_id    uuid not null references public.profiles (id) on delete cascade,
    invitee_email text not null check (invitee_email = lower(btrim(invitee_email)) and invitee_email like '%_@_%'),
    status        text not null default 'pending' check (status in ('pending', 'accepted', 'rejected', 'cancelled')),
    created_at    timestamptz not null default now()
);

create unique index house_invitations_one_pending on public.house_invitations (house_id, invitee_email) where status = 'pending';

create table public.invitation_rate_limit (
    user_id          uuid not null references public.profiles (id) on delete cascade,
    house_id         uuid not null references public.houses (id) on delete cascade,
    invitations_sent smallint not null default 0 check (invitations_sent >= 0),
    window_start     timestamptz not null default now(),
    primary key (user_id, house_id)
);

create table public.house_audit_log (
    id             uuid primary key default gen_random_uuid(),
    house_id       uuid not null references public.houses (id) on delete cascade,
    user_id        uuid references public.profiles (id) on delete set null,
    action         text not null,
    target_user_id uuid references public.profiles (id) on delete set null,
    details        jsonb not null default '{}',
    created_at     timestamptz not null default now()
);

create index house_audit_log_house_time on public.house_audit_log (house_id, created_at desc);

-- A recurring bill. Its split lives in recurring_expense_shares; split_method is null when the bill
-- is not split. next_due_date is advanced by a trigger each time a payment is recorded.
create table public.recurring_expenses (
    id                    uuid primary key default gen_random_uuid(),
    house_id              uuid not null references public.houses (id) on delete cascade,
    name                  text not null check (length(btrim(name)) between 1 and 200),
    amount                numeric not null check (amount > 0),
    category              text not null check (length(btrim(category)) > 0),
    frequency             text not null default 'monthly'
        check (frequency in ('daily', 'weekly', 'biweekly', 'monthly', 'quarterly', 'semiannual', 'yearly', 'custom')),
    custom_frequency_days smallint check (custom_frequency_days between 1 and 366),
    first_due_date        date not null,
    next_due_date         date not null,
    last_paid_date        date,
    reminder_enabled      boolean not null default true,
    reminder_days_before  smallint not null default 3 check (reminder_days_before between 0 and 30),
    allow_prepayment      boolean not null default false,
    split_method          text check (split_method in ('equal', 'exact', 'percent', 'shares')),
    notes                 text,
    is_active             boolean not null default true,
    created_by            uuid not null references public.profiles (id),
    created_at            timestamptz not null default now(),
    updated_at            timestamptz not null default now(),
    check ((frequency = 'custom') = (custom_frequency_days is not null)),
    check (next_due_date >= first_due_date)
);

create index recurring_expenses_house on public.recurring_expenses (house_id) where is_active;

-- split_value is the member's input for the bill's split method: 1 for equal, the amount for
-- exact, the percentage for percent, the share count for shares. Each payment divides that
-- month's amount in proportion to these values.
create table public.recurring_expense_shares (
    recurring_expense_id uuid not null references public.recurring_expenses (id) on delete cascade,
    user_id              uuid not null references public.profiles (id),
    split_value          numeric(14, 4) not null check (split_value > 0),
    primary key (recurring_expense_id, user_id)
);

-- Every movement of money in a house, including settle-up payments (kind = 'settlement') and the
-- payments of recurring bills (recurring_expense_id set). Who paid and who owes is in
-- expense_shares; a member's balance is the sum of what they paid minus what they owe.
create table public.expenses (
    id                   uuid primary key default gen_random_uuid(),
    house_id             uuid not null references public.houses (id) on delete cascade,
    kind                 text not null default 'expense' check (kind in ('expense', 'settlement')),
    name                 text not null check (length(btrim(name)) between 1 and 200),
    amount               numeric not null check (amount > 0),
    category             text check (length(btrim(category)) > 0),
    split_method         text check (split_method in ('equal', 'exact', 'percent', 'shares')),
    date                 date not null,
    notes                text,
    recurring_expense_id uuid references public.recurring_expenses (id) on delete set null,
    created_by           uuid not null references public.profiles (id),
    created_at           timestamptz not null default now(),
    updated_at           timestamptz not null default now(),
    check ((kind = 'settlement') = (category is null)),
    check (kind = 'expense' or (split_method is null and recurring_expense_id is null))
);

create index expenses_house_date on public.expenses (house_id, date desc);
create index expenses_recurring on public.expenses (recurring_expense_id) where recurring_expense_id is not null;

-- One row per person on an expense. Paid shares sum to the amount, and so do owed shares; a deferred
-- constraint trigger checks both at commit. split_value keeps the input the owed share came from,
-- so reopening the expense restores what the user entered.
create table public.expense_shares (
    expense_id  uuid not null references public.expenses (id) on delete cascade,
    user_id     uuid not null references public.profiles (id),
    paid_share  numeric not null default 0 check (paid_share >= 0),
    owed_share  numeric not null default 0 check (owed_share >= 0),
    split_value numeric(14, 4) check (split_value > 0),
    primary key (expense_id, user_id),
    check (paid_share > 0 or owed_share > 0)
);

create index expense_shares_user on public.expense_shares (user_id);

-- Items the house pays for by usage, such as milk or water cans, at a unit price per unit.
create table public.per_diem_config (
    id         uuid primary key default gen_random_uuid(),
    house_id   uuid not null references public.houses (id) on delete cascade,
    item_name  text not null check (length(btrim(item_name)) between 1 and 100),
    rate       numeric not null check (rate > 0),
    category   text not null check (length(btrim(category)) > 0),
    unit       text not null default 'unit' check (length(btrim(unit)) > 0),
    is_active  boolean not null default true,
    created_at timestamptz not null default now()
);

-- A recorded use of a per-diem item. rate and total_cost are captured when the entry is written, so
-- changing an item's price later leaves past entries and past bills as they were. total_cost is
-- rounded to the house currency's minor units, and a bill is the sum of these stored costs.
create table public.per_diem_entries (
    id         uuid primary key default gen_random_uuid(),
    config_id  uuid not null references public.per_diem_config (id) on delete cascade,
    quantity   numeric(12, 3) not null check (quantity > 0),
    rate       numeric not null check (rate > 0),
    total_cost numeric not null check (total_cost >= 0),
    date       date not null,
    added_by   uuid not null references public.profiles (id),
    notes      text,
    created_at timestamptz not null default now()
);

create index per_diem_entries_config_date on public.per_diem_entries (config_id, date);

-- A chore. A recurring chore can rotate: when completed, it is reassigned to the next member in
-- rotation and its due date moves on. effort_points let the house see who carries how much.
create table public.chores (
    id                 uuid primary key default gen_random_uuid(),
    house_id           uuid not null references public.houses (id) on delete cascade,
    task_name          text not null check (length(btrim(task_name)) between 1 and 200),
    description        text,
    due_date           date,
    recurrence_pattern text check (recurrence_pattern in ('daily', 'weekly', 'monthly', 'yearly')),
    rotation           uuid[] not null default '{}',
    effort_points      smallint not null default 1 check (effort_points between 1 and 10),
    assigned_to        uuid references public.profiles (id) on delete set null,
    is_completed       boolean not null default false,
    completed_at       timestamptz,
    completed_by       uuid references public.profiles (id) on delete set null,
    created_by         uuid not null references public.profiles (id),
    created_at         timestamptz not null default now(),
    check (is_completed = (completed_at is not null)),
    check (cardinality(rotation) = 0 or recurrence_pattern is not null)
);

create index chores_house on public.chores (house_id, is_completed);

create table public.shopping_items (
    id           uuid primary key default gen_random_uuid(),
    house_id     uuid not null references public.houses (id) on delete cascade,
    item_name    text not null check (length(btrim(item_name)) between 1 and 200),
    quantity     text,
    category     text,
    is_purchased boolean not null default false,
    added_by     uuid not null references public.profiles (id),
    purchased_by uuid references public.profiles (id) on delete set null,
    purchased_at timestamptz,
    created_at   timestamptz not null default now(),
    check (is_purchased = (purchased_at is not null))
);

create index shopping_items_house on public.shopping_items (house_id, is_purchased);

create table public.messages (
    id         uuid primary key default gen_random_uuid(),
    house_id   uuid not null references public.houses (id) on delete cascade,
    user_id    uuid not null references public.profiles (id),
    content    text not null check (length(btrim(content)) between 1 and 4000),
    created_at timestamptz not null default now()
);

create index messages_house_time on public.messages (house_id, created_at desc);

-- A stored file. house_id is null for a document in the owner's personal vault.
create table public.documents (
    id           uuid primary key default gen_random_uuid(),
    house_id     uuid references public.houses (id) on delete cascade,
    user_id      uuid not null references public.profiles (id) on delete cascade,
    storage_path text not null unique,
    file_name    text not null,
    file_size    bigint not null check (file_size >= 0),
    mime_type    text,
    created_at   timestamptz not null default now()
);

create index documents_house on public.documents (house_id, created_at desc);

-- The kinds of notification the house can raise. The server raises every one of them from the event
-- itself, so no client can create or forge a notification.
create table public.notification_types (
    type        text primary key,
    description text not null
);

insert into public.notification_types (type, description) values
    ('member_joined',       'Someone joined the house'),
    ('member_left',         'Someone left the house'),
    ('expense_added',       'An expense you share was added'),
    ('expense_updated',     'An expense you share was changed'),
    ('settlement_received', 'A housemate paid you back'),
    ('settlement_recorded', 'A housemate recorded a payment you made them'),
    ('bill_due',            'A recurring bill is due soon'),
    ('bill_paid',           'A recurring bill you share was paid'),
    ('chore_assigned',      'A chore was assigned to you'),
    ('chore_completed',     'A chore you created was completed'),
    ('message',             'A new message in the house chat'),
    ('shopping_item_added', 'Something was added to the shopping list'),
    ('document_uploaded',   'A document was added to the house'),
    ('invitation',          'You were invited to a house');

create table public.notifications (
    id         uuid primary key default gen_random_uuid(),
    user_id    uuid not null references public.profiles (id) on delete cascade,
    house_id   uuid references public.houses (id) on delete cascade,
    actor_id   uuid references public.profiles (id) on delete set null,
    type       text not null references public.notification_types (type),
    title      text not null,
    body       text not null,
    data       jsonb not null default '{}',
    is_read    boolean not null default false,
    created_at timestamptz not null default now()
);

create index notifications_user_time on public.notifications (user_id, created_at desc);
create index notifications_user_unread on public.notifications (user_id) where not is_read;

-- A member's opt-out for one notification type in one house. No row means the type is on, so a new
-- type reaches everyone until they turn it off.
create table public.notification_preferences (
    user_id    uuid not null references public.profiles (id) on delete cascade,
    house_id   uuid not null references public.houses (id) on delete cascade,
    type       text not null references public.notification_types (type),
    is_enabled boolean not null,
    updated_at timestamptz not null default now(),
    primary key (user_id, house_id, type)
);

-- A device to deliver push notifications to. One user can have several.
create table public.device_tokens (
    token      text primary key,
    user_id    uuid not null references public.profiles (id) on delete cascade,
    platform   text not null default 'android' check (platform in ('android', 'ios', 'web')),
    updated_at timestamptz not null default now()
);

create index device_tokens_user on public.device_tokens (user_id);
