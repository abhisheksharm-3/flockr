-- Fix for "infinite recursion detected in policy for relation house_members"
-- This completely rewrites the RLS policies to eliminate circular dependencies

-- First, drop ALL existing policies on both tables
DROP POLICY IF EXISTS "Users can view houses they are members of" ON houses;
DROP POLICY IF EXISTS "Users can view houses they own or are members of" ON houses;
DROP POLICY IF EXISTS "House owners can update their houses" ON houses;
DROP POLICY IF EXISTS "Authenticated users can create houses" ON houses;
DROP POLICY IF EXISTS "Users can view members of their houses" ON house_members;
DROP POLICY IF EXISTS "Users can view members of houses they own or are members of" ON house_members;
DROP POLICY IF EXISTS "House owners can add members" ON house_members;
DROP POLICY IF EXISTS "House owners and system can add members" ON house_members;

-- ============================================
-- HOUSES TABLE POLICIES (NO RECURSION)
-- ============================================

-- SELECT: Users can view houses they own (owner_id check only, no house_members lookup)
-- The trigger adds owners as members, so they'll see it through membership after creation
CREATE POLICY "Owners can view their houses"
    ON houses FOR SELECT
    USING (owner_id = auth.uid());

-- INSERT: Authenticated users can create houses (they must be the owner)
CREATE POLICY "Users can create houses"
    ON houses FOR INSERT
    WITH CHECK (auth.uid() = owner_id);

-- UPDATE: House owners can update their houses
CREATE POLICY "Owners can update their houses"
    ON houses FOR UPDATE
    USING (owner_id = auth.uid());

-- ============================================
-- HOUSE MEMBERS TABLE POLICIES (NO RECURSION)
-- ============================================

-- SELECT: Users can view house_members records where they are the member OR the owner
CREATE POLICY "Users can view house members"
    ON house_members FOR SELECT
    USING (
        user_id = auth.uid()
        OR
        house_id IN (SELECT id FROM houses WHERE owner_id = auth.uid())
    );

-- INSERT: Allow house owners to add members AND allow the trigger to add the owner
-- The trigger runs with SECURITY DEFINER so it bypasses RLS, but we keep this policy for manual adds
CREATE POLICY "Owners can add house members"
    ON house_members FOR INSERT
    WITH CHECK (
        house_id IN (SELECT id FROM houses WHERE owner_id = auth.uid())
    );

-- DELETE: House owners can remove members
CREATE POLICY "Owners can remove house members"
    ON house_members FOR DELETE
    USING (
        house_id IN (SELECT id FROM houses WHERE owner_id = auth.uid())
    );

