-- Seed test data: Store and test users setup instructions
-- This migration creates a test store and provides template for test users
set check_function_bodies = off;
set search_path = public;

-- ========================================
-- 1. Create Test Store
-- ========================================
-- Insert a test store if it doesn't exist
INSERT INTO stores (id, name, timezone, is_active, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    'Test Store',
    'UTC',
    true,
    now(),
    now()
WHERE NOT EXISTS (
    SELECT 1 FROM stores WHERE name = 'Test Store'
);

-- ========================================
-- 2. Get the Test Store UUID for user references
-- ========================================
-- This will be used when inserting users below
-- Run this query first to get the store UUID:
-- SELECT id FROM stores WHERE name = 'Test Store';

-- ========================================
-- 3. Create Test Users in Supabase Auth (Manual Step Required)
-- ========================================
-- You MUST create these users in Supabase Dashboard first:
-- 1. Go to: Authentication → Users → Add user → Create new user
-- 2. Create users with these credentials:
--    - Email: chef@test.com, Password: password123
--    - Email: waiter@test.com, Password: password123
--    - Email: admin@test.com, Password: password123
-- 3. Copy the UUID for each user from Authentication → Users
-- 4. Replace the placeholders below with actual UUIDs

-- ========================================
-- 4. Insert User Records (After Auth Users Created)
-- ========================================
-- Replace 'STORE_UUID_HERE' with the store ID from above query
-- Replace 'AUTH_UUID_HERE' with actual UUIDs from Supabase Auth

-- Helper function to insert user if not exists
CREATE OR REPLACE FUNCTION insert_test_user(
    p_auth_uuid uuid,
    p_email text,
    p_display_name text,
    p_role text,
    p_store_id uuid
) RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    INSERT INTO users (id, email, display_name, role, store_id, created_at, updated_at)
    VALUES (p_auth_uuid, p_email, p_display_name, p_role, p_store_id, now(), now())
    ON CONFLICT (id) DO UPDATE
    SET email = EXCLUDED.email,
        display_name = EXCLUDED.display_name,
        role = EXCLUDED.role,
        store_id = EXCLUDED.store_id,
        updated_at = now();
END;
$$;

-- ========================================
-- 5. Example Usage (Run after creating Auth users)
-- ========================================
-- First, get the store UUID:
-- SELECT id INTO variable_store_id FROM stores WHERE name = 'Test Store';
--
-- Then insert users (replace UUIDs with actual Auth user UUIDs):
-- 
-- DO $$
-- DECLARE
--     v_store_id uuid;
--     v_chef_uuid uuid := '00000000-0000-0000-0000-000000000001';  -- Replace with actual
--     v_waiter_uuid uuid := '00000000-0000-0000-0000-000000000002'; -- Replace with actual
--     v_admin_uuid uuid := '00000000-0000-0000-0000-000000000003';  -- Replace with actual
-- BEGIN
--     SELECT id INTO v_store_id FROM stores WHERE name = 'Test Store' LIMIT 1;
--     
--     PERFORM insert_test_user(v_chef_uuid, 'chef@test.com', 'Test Chef', 'chef', v_store_id);
--     PERFORM insert_test_user(v_waiter_uuid, 'waiter@test.com', 'Test Waiter', 'waiter', v_store_id);
--     PERFORM insert_test_user(v_admin_uuid, 'admin@test.com', 'Test Admin', 'admin', v_store_id);
-- END $$;

-- ========================================
-- Test Credentials Summary
-- ========================================
-- Email: chef@test.com     Password: password123  Role: chef
-- Email: waiter@test.com   Password: password123  Role: waiter  
-- Email: admin@test.com    Password: password123  Role: admin
--
-- After running this migration:
-- 1. Create users in Supabase Auth (Dashboard → Authentication → Users)
-- 2. Get the UUID for each user
-- 3. Run the DO block above with actual UUIDs to insert user records

