-- Flockr Database Schema
-- Run this in your Supabase SQL editor

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- PROFILES TABLE
-- ============================================
CREATE TABLE profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT NOT NULL,
    full_name TEXT,
    has_completed_onboarding BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- RLS for profiles
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own profile"
    ON profiles FOR SELECT
    USING (auth.uid() = id);

CREATE POLICY "Users can update their own profile"
    ON profiles FOR UPDATE
    USING (auth.uid() = id);

CREATE POLICY "Users can insert their own profile"
    ON profiles FOR INSERT
    WITH CHECK (auth.uid() = id);

-- ============================================
-- HOUSES TABLE
-- ============================================
CREATE TABLE houses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    owner_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    address TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- RLS for houses
ALTER TABLE houses ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view houses they are members of"
    ON houses FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM house_members
            WHERE house_members.house_id = houses.id
            AND house_members.user_id = auth.uid()
        )
    );

CREATE POLICY "House owners can update their houses"
    ON houses FOR UPDATE
    USING (owner_id = auth.uid());

CREATE POLICY "Authenticated users can create houses"
    ON houses FOR INSERT
    WITH CHECK (auth.uid() = owner_id);

-- ============================================
-- HOUSE MEMBERS TABLE
-- ============================================
CREATE TABLE house_members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    house_id UUID NOT NULL REFERENCES houses(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(house_id, user_id)
);

-- RLS for house_members
ALTER TABLE house_members ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view members of their houses"
    ON house_members FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM house_members hm
            WHERE hm.house_id = house_members.house_id
            AND hm.user_id = auth.uid()
        )
    );

CREATE POLICY "House owners can add members"
    ON house_members FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM houses
            WHERE houses.id = house_members.house_id
            AND houses.owner_id = auth.uid()
        )
    );

-- ============================================
-- HOUSE INVITATIONS TABLE
-- ============================================
CREATE TABLE house_invitations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    house_id UUID NOT NULL REFERENCES houses(id) ON DELETE CASCADE,
    inviter_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    invitee_email TEXT NOT NULL,
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'rejected')),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- RLS for house_invitations
ALTER TABLE house_invitations ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view invitations for their houses"
    ON house_invitations FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM house_members
            WHERE house_members.house_id = house_invitations.house_id
            AND house_members.user_id = auth.uid()
        )
        OR invitee_email = (SELECT email FROM profiles WHERE id = auth.uid())
    );

-- ============================================
-- DOCUMENTS TABLE
-- ============================================
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    house_id UUID REFERENCES houses(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    storage_path TEXT NOT NULL,
    file_name TEXT NOT NULL,
    file_size BIGINT,
    mime_type TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- RLS for documents
ALTER TABLE documents ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their personal documents"
    ON documents FOR SELECT
    USING (
        (house_id IS NULL AND user_id = auth.uid())
        OR
        (house_id IS NOT NULL AND EXISTS (
            SELECT 1 FROM house_members
            WHERE house_members.house_id = documents.house_id
            AND house_members.user_id = auth.uid()
        ))
    );

CREATE POLICY "Users can insert documents"
    ON documents FOR INSERT
    WITH CHECK (user_id = auth.uid());

CREATE POLICY "Users can delete their own documents"
    ON documents FOR DELETE
    USING (user_id = auth.uid());

-- ============================================
-- RECURRING EXPENSES TABLE
-- ============================================
CREATE TABLE recurring_expenses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    house_id UUID NOT NULL REFERENCES houses(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    due_day INTEGER NOT NULL CHECK (due_day >= 1 AND due_day <= 31),
    category TEXT NOT NULL,
    created_by UUID NOT NULL REFERENCES profiles(id),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- RLS for recurring_expenses
ALTER TABLE recurring_expenses ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view recurring expenses for their houses"
    ON recurring_expenses FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM house_members
            WHERE house_members.house_id = recurring_expenses.house_id
            AND house_members.user_id = auth.uid()
        )
    );

CREATE POLICY "House members can create recurring expenses"
    ON recurring_expenses FOR INSERT
    WITH CHECK (
        created_by = auth.uid() AND
        EXISTS (
            SELECT 1 FROM house_members
            WHERE house_members.house_id = recurring_expenses.house_id
            AND house_members.user_id = auth.uid()
        )
    );

-- ============================================
-- ONE-TIME EXPENSES TABLE
-- ============================================
CREATE TABLE one_time_expenses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    house_id UUID NOT NULL REFERENCES houses(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    date DATE NOT NULL DEFAULT CURRENT_DATE,
    paid_by UUID NOT NULL REFERENCES profiles(id),
    category TEXT NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- RLS for one_time_expenses
ALTER TABLE one_time_expenses ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view one-time expenses for their houses"
    ON one_time_expenses FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM house_members
            WHERE house_members.house_id = one_time_expenses.house_id
            AND house_members.user_id = auth.uid()
        )
    );

CREATE POLICY "House members can create one-time expenses"
    ON one_time_expenses FOR INSERT
    WITH CHECK (
        paid_by = auth.uid() AND
        EXISTS (
            SELECT 1 FROM house_members
            WHERE house_members.house_id = one_time_expenses.house_id
            AND house_members.user_id = auth.uid()
        )
    );

-- ============================================
-- EXPENSE SPLITS TABLE
-- ============================================
CREATE TABLE expense_splits (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    expense_id UUID NOT NULL REFERENCES one_time_expenses(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    amount_owed DECIMAL(10, 2) NOT NULL,
    is_settled BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- RLS for expense_splits
ALTER TABLE expense_splits ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view expense splits for their houses"
    ON expense_splits FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM one_time_expenses ote
            JOIN house_members hm ON hm.house_id = ote.house_id
            WHERE ote.id = expense_splits.expense_id
            AND hm.user_id = auth.uid()
        )
    );

-- ============================================
-- TRANSACTIONS TABLE (IOUs)
-- ============================================
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    house_id UUID NOT NULL REFERENCES houses(id) ON DELETE CASCADE,
    payer_id UUID NOT NULL REFERENCES profiles(id),
    payee_id UUID NOT NULL REFERENCES profiles(id),
    amount DECIMAL(10, 2) NOT NULL,
    is_settlement BOOLEAN DEFAULT FALSE,
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- RLS for transactions
ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view transactions for their houses"
    ON transactions FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM house_members
            WHERE house_members.house_id = transactions.house_id
            AND house_members.user_id = auth.uid()
        )
    );

CREATE POLICY "House members can create transactions"
    ON transactions FOR INSERT
    WITH CHECK (
        payer_id = auth.uid() AND
        EXISTS (
            SELECT 1 FROM house_members
            WHERE house_members.house_id = transactions.house_id
            AND house_members.user_id = auth.uid()
        )
    );

-- ============================================
-- PER DIEM CONFIG TABLE
-- ============================================
CREATE TABLE per_diem_config (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    house_id UUID NOT NULL REFERENCES houses(id) ON DELETE CASCADE,
    item_name TEXT NOT NULL,
    rate DECIMAL(10, 2) NOT NULL,
    category TEXT NOT NULL,
    unit TEXT DEFAULT 'unit',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- RLS for per_diem_config
ALTER TABLE per_diem_config ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view per diem config for their houses"
    ON per_diem_config FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM house_members
            WHERE house_members.house_id = per_diem_config.house_id
            AND house_members.user_id = auth.uid()
        )
    );

-- ============================================
-- PER DIEM ENTRIES TABLE
-- ============================================
CREATE TABLE per_diem_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    config_id UUID NOT NULL REFERENCES per_diem_config(id) ON DELETE CASCADE,
    quantity DECIMAL(10, 2) NOT NULL,
    date DATE NOT NULL DEFAULT CURRENT_DATE,
    added_by UUID NOT NULL REFERENCES profiles(id),
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- RLS for per_diem_entries
ALTER TABLE per_diem_entries ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view per diem entries for their houses"
    ON per_diem_entries FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM per_diem_config pdc
            JOIN house_members hm ON hm.house_id = pdc.house_id
            WHERE pdc.id = per_diem_entries.config_id
            AND hm.user_id = auth.uid()
        )
    );

CREATE POLICY "House members can create per diem entries"
    ON per_diem_entries FOR INSERT
    WITH CHECK (
        added_by = auth.uid() AND
        EXISTS (
            SELECT 1 FROM per_diem_config pdc
            JOIN house_members hm ON hm.house_id = pdc.house_id
            WHERE pdc.id = per_diem_entries.config_id
            AND hm.user_id = auth.uid()
        )
    );

-- ============================================
-- PAYMENT HISTORY TABLE
-- ============================================
CREATE TABLE payment_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    recurring_expense_id UUID NOT NULL REFERENCES recurring_expenses(id) ON DELETE CASCADE,
    paid_by UUID NOT NULL REFERENCES profiles(id),
    amount DECIMAL(10, 2) NOT NULL,
    payment_date DATE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- RLS for payment_history
ALTER TABLE payment_history ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view payment history for their houses"
    ON payment_history FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM recurring_expenses re
            JOIN house_members hm ON hm.house_id = re.house_id
            WHERE re.id = payment_history.recurring_expense_id
            AND hm.user_id = auth.uid()
        )
    );

-- ============================================
-- SHOPPING ITEMS TABLE
-- ============================================
CREATE TABLE shopping_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    house_id UUID NOT NULL REFERENCES houses(id) ON DELETE CASCADE,
    item_name TEXT NOT NULL,
    quantity TEXT,
    is_purchased BOOLEAN DEFAULT FALSE,
    added_by UUID NOT NULL REFERENCES profiles(id),
    purchased_by UUID REFERENCES profiles(id),
    purchased_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- RLS for shopping_items
ALTER TABLE shopping_items ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view shopping items for their houses"
    ON shopping_items FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM house_members
            WHERE house_members.house_id = shopping_items.house_id
            AND house_members.user_id = auth.uid()
        )
    );

CREATE POLICY "House members can create shopping items"
    ON shopping_items FOR INSERT
    WITH CHECK (
        added_by = auth.uid() AND
        EXISTS (
            SELECT 1 FROM house_members
            WHERE house_members.house_id = shopping_items.house_id
            AND house_members.user_id = auth.uid()
        )
    );

CREATE POLICY "House members can update shopping items"
    ON shopping_items FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM house_members
            WHERE house_members.house_id = shopping_items.house_id
            AND house_members.user_id = auth.uid()
        )
    );

CREATE POLICY "House members can delete shopping items"
    ON shopping_items FOR DELETE
    USING (
        EXISTS (
            SELECT 1 FROM house_members
            WHERE house_members.house_id = shopping_items.house_id
            AND house_members.user_id = auth.uid()
        )
    );

-- ============================================
-- CHORES TABLE
-- ============================================
CREATE TABLE chores (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    house_id UUID NOT NULL REFERENCES houses(id) ON DELETE CASCADE,
    task_name TEXT NOT NULL,
    description TEXT,
    due_date DATE,
    is_recurring BOOLEAN DEFAULT FALSE,
    recurrence_pattern TEXT,
    assigned_to UUID REFERENCES profiles(id),
    is_completed BOOLEAN DEFAULT FALSE,
    completed_at TIMESTAMPTZ,
    completed_by UUID REFERENCES profiles(id),
    created_by UUID NOT NULL REFERENCES profiles(id),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- RLS for chores
ALTER TABLE chores ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view chores for their houses"
    ON chores FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM house_members
            WHERE house_members.house_id = chores.house_id
            AND house_members.user_id = auth.uid()
        )
    );

CREATE POLICY "House members can create chores"
    ON chores FOR INSERT
    WITH CHECK (
        created_by = auth.uid() AND
        EXISTS (
            SELECT 1 FROM house_members
            WHERE house_members.house_id = chores.house_id
            AND house_members.user_id = auth.uid()
        )
    );

CREATE POLICY "House members can update chores"
    ON chores FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM house_members
            WHERE house_members.house_id = chores.house_id
            AND house_members.user_id = auth.uid()
        )
    );

-- ============================================
-- MESSAGES TABLE
-- ============================================
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    house_id UUID NOT NULL REFERENCES houses(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- RLS for messages
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view messages for their houses"
    ON messages FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM house_members
            WHERE house_members.house_id = messages.house_id
            AND house_members.user_id = auth.uid()
        )
    );

CREATE POLICY "House members can create messages"
    ON messages FOR INSERT
    WITH CHECK (
        user_id = auth.uid() AND
        EXISTS (
            SELECT 1 FROM house_members
            WHERE house_members.house_id = messages.house_id
            AND house_members.user_id = auth.uid()
        )
    );

-- ============================================
-- NOTIFICATIONS TABLE (CRITICAL)
-- ============================================
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    house_id UUID REFERENCES houses(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    data JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for faster queries
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(user_id, is_read);

-- RLS for notifications
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own notifications"
    ON notifications FOR SELECT
    USING (user_id = auth.uid());

CREATE POLICY "Users can update their own notifications"
    ON notifications FOR UPDATE
    USING (user_id = auth.uid());

-- ============================================
-- DATABASE FUNCTIONS (RPCs)
-- ============================================

-- Function: Create notification for all house members (except triggering user)
CREATE OR REPLACE FUNCTION create_notification_for_house(
    p_house_id UUID,
    p_title TEXT,
    p_message TEXT,
    p_data JSONB DEFAULT NULL,
    p_exclude_user_id UUID DEFAULT NULL
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    INSERT INTO notifications (user_id, house_id, title, message, data)
    SELECT
        hm.user_id,
        p_house_id,
        p_title,
        p_message,
        p_data
    FROM house_members hm
    WHERE hm.house_id = p_house_id
    AND (p_exclude_user_id IS NULL OR hm.user_id != p_exclude_user_id);
END;
$$;

-- Function: Get monthly summary
CREATE OR REPLACE FUNCTION get_monthly_summary(
    p_house_id UUID,
    p_month DATE
)
RETURNS TABLE(
    total_expenses DECIMAL,
    recurring_expenses DECIMAL,
    one_time_expenses DECIMAL,
    per_diem_expenses DECIMAL
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    RETURN QUERY
    SELECT
        COALESCE(
            (SELECT SUM(ph.amount)
             FROM payment_history ph
             JOIN recurring_expenses re ON re.id = ph.recurring_expense_id
             WHERE re.house_id = p_house_id
             AND DATE_TRUNC('month', ph.payment_date) = DATE_TRUNC('month', p_month)),
            0
        ) +
        COALESCE(
            (SELECT SUM(ote.amount)
             FROM one_time_expenses ote
             WHERE ote.house_id = p_house_id
             AND DATE_TRUNC('month', ote.date) = DATE_TRUNC('month', p_month)),
            0
        ) +
        COALESCE(
            (SELECT SUM(pde.quantity * pdc.rate)
             FROM per_diem_entries pde
             JOIN per_diem_config pdc ON pdc.id = pde.config_id
             WHERE pdc.house_id = p_house_id
             AND DATE_TRUNC('month', pde.date) = DATE_TRUNC('month', p_month)),
            0
        ) as total_expenses,
        COALESCE(
            (SELECT SUM(ph.amount)
             FROM payment_history ph
             JOIN recurring_expenses re ON re.id = ph.recurring_expense_id
             WHERE re.house_id = p_house_id
             AND DATE_TRUNC('month', ph.payment_date) = DATE_TRUNC('month', p_month)),
            0
        ) as recurring_expenses,
        COALESCE(
            (SELECT SUM(ote.amount)
             FROM one_time_expenses ote
             WHERE ote.house_id = p_house_id
             AND DATE_TRUNC('month', ote.date) = DATE_TRUNC('month', p_month)),
            0
        ) as one_time_expenses,
        COALESCE(
            (SELECT SUM(pde.quantity * pdc.rate)
             FROM per_diem_entries pde
             JOIN per_diem_config pdc ON pdc.id = pde.config_id
             WHERE pdc.house_id = p_house_id
             AND DATE_TRUNC('month', pde.date) = DATE_TRUNC('month', p_month)),
            0
        ) as per_diem_expenses;
END;
$$;

-- Function: Get spend by member
CREATE OR REPLACE FUNCTION get_spend_by_member(
    p_house_id UUID,
    p_month DATE
)
RETURNS TABLE(
    user_id UUID,
    full_name TEXT,
    total_spent DECIMAL
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    RETURN QUERY
    SELECT
        p.id as user_id,
        p.full_name,
        COALESCE(SUM(ote.amount), 0) as total_spent
    FROM profiles p
    JOIN house_members hm ON hm.user_id = p.id
    LEFT JOIN one_time_expenses ote ON ote.paid_by = p.id
        AND ote.house_id = p_house_id
        AND DATE_TRUNC('month', ote.date) = DATE_TRUNC('month', p_month)
    WHERE hm.house_id = p_house_id
    GROUP BY p.id, p.full_name
    ORDER BY total_spent DESC;
END;
$$;

-- Function: Get spend by category
CREATE OR REPLACE FUNCTION get_spend_by_category(
    p_house_id UUID,
    p_month DATE
)
RETURNS TABLE(
    category TEXT,
    total_amount DECIMAL
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    RETURN QUERY
    SELECT
        ote.category,
        SUM(ote.amount) as total_amount
    FROM one_time_expenses ote
    WHERE ote.house_id = p_house_id
    AND DATE_TRUNC('month', ote.date) = DATE_TRUNC('month', p_month)
    GROUP BY ote.category
    ORDER BY total_amount DESC;
END;
$$;

-- Function: Get user balances (IOU)
CREATE OR REPLACE FUNCTION get_user_balances(
    p_house_id UUID
)
RETURNS TABLE(
    user_id UUID,
    full_name TEXT,
    balance DECIMAL
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    RETURN QUERY
    WITH balance_calc AS (
        -- Money owed to user from expense splits
        SELECT
            es.user_id,
            SUM(es.amount_owed) as owes
        FROM expense_splits es
        JOIN one_time_expenses ote ON ote.id = es.expense_id
        WHERE ote.house_id = p_house_id
        AND es.is_settled = FALSE
        GROUP BY es.user_id

        UNION ALL

        -- Money user paid for others
        SELECT
            ote.paid_by as user_id,
            -SUM(es.amount_owed) as owes
        FROM one_time_expenses ote
        JOIN expense_splits es ON es.expense_id = ote.id
        WHERE ote.house_id = p_house_id
        AND es.is_settled = FALSE
        GROUP BY ote.paid_by

        UNION ALL

        -- Transactions (settlements)
        SELECT
            t.payer_id as user_id,
            -t.amount as owes
        FROM transactions t
        WHERE t.house_id = p_house_id

        UNION ALL

        SELECT
            t.payee_id as user_id,
            t.amount as owes
        FROM transactions t
        WHERE t.house_id = p_house_id
    )
    SELECT
        p.id as user_id,
        p.full_name,
        COALESCE(SUM(bc.owes), 0) as balance
    FROM profiles p
    JOIN house_members hm ON hm.user_id = p.id
    LEFT JOIN balance_calc bc ON bc.user_id = p.id
    WHERE hm.house_id = p_house_id
    GROUP BY p.id, p.full_name
    ORDER BY balance DESC;
END;
$$;

-- Function: Get per diem bill itemized
CREATE OR REPLACE FUNCTION get_per_diem_bill_itemized(
    p_house_id UUID,
    p_month DATE
)
RETURNS TABLE(
    item_name TEXT,
    total_quantity DECIMAL,
    rate DECIMAL,
    unit TEXT,
    total_amount DECIMAL
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    RETURN QUERY
    SELECT
        pdc.item_name,
        SUM(pde.quantity) as total_quantity,
        pdc.rate,
        pdc.unit,
        SUM(pde.quantity * pdc.rate) as total_amount
    FROM per_diem_entries pde
    JOIN per_diem_config pdc ON pdc.id = pde.config_id
    WHERE pdc.house_id = p_house_id
    AND DATE_TRUNC('month', pde.date) = DATE_TRUNC('month', p_month)
    GROUP BY pdc.item_name, pdc.rate, pdc.unit
    ORDER BY total_amount DESC;
END;
$$;

-- Function: Get per diem bill by member
CREATE OR REPLACE FUNCTION get_per_diem_bill_by_member(
    p_house_id UUID,
    p_month DATE
)
RETURNS TABLE(
    user_id UUID,
    full_name TEXT,
    total_quantity DECIMAL,
    total_amount DECIMAL
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    RETURN QUERY
    SELECT
        p.id as user_id,
        p.full_name,
        SUM(pde.quantity) as total_quantity,
        SUM(pde.quantity * pdc.rate) as total_amount
    FROM per_diem_entries pde
    JOIN per_diem_config pdc ON pdc.id = pde.config_id
    JOIN profiles p ON p.id = pde.added_by
    WHERE pdc.house_id = p_house_id
    AND DATE_TRUNC('month', pde.date) = DATE_TRUNC('month', p_month)
    GROUP BY p.id, p.full_name
    ORDER BY total_amount DESC;
END;
$$;

-- ============================================
-- TRIGGERS
-- ============================================

-- Trigger: Auto-create profile on user signup
CREATE OR REPLACE FUNCTION handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    INSERT INTO profiles (id, email, full_name)
    VALUES (
        NEW.id,
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'full_name', '')
    );
    RETURN NEW;
END;
$$;

CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION handle_new_user();

-- Trigger: Update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TRIGGER update_profiles_updated_at
    BEFORE UPDATE ON profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_houses_updated_at
    BEFORE UPDATE ON houses
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger: Auto-add house owner as member
CREATE OR REPLACE FUNCTION add_owner_as_member()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    INSERT INTO house_members (house_id, user_id)
    VALUES (NEW.id, NEW.owner_id);
    RETURN NEW;
END;
$$;

CREATE TRIGGER on_house_created
    AFTER INSERT ON houses
    FOR EACH ROW
    EXECUTE FUNCTION add_owner_as_member();

