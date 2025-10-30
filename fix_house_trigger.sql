-- Fix for "column 'role' of relation 'house_members' does not exist"
-- This drops and recreates the trigger without the role column

-- Drop the existing trigger and function
DROP TRIGGER IF EXISTS on_house_created ON houses;
DROP FUNCTION IF EXISTS add_owner_as_member();

-- Recreate the function correctly (without role column)
CREATE OR REPLACE FUNCTION add_owner_as_member()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    INSERT INTO house_members (house_id, user_id)
    VALUES (NEW.id, NEW.owner_id);
    RETURN NEW;
END;
$$;

-- Recreate the trigger
CREATE TRIGGER on_house_created
    AFTER INSERT ON houses
    FOR EACH ROW
    EXECUTE FUNCTION add_owner_as_member();

