# Test Users Setup Guide

This guide explains how to set up test users for the Street Feast Android app.

## Quick Setup Steps

### 1. Run the Seed Migration

Apply the seed migration to create a test store:
```bash
supabase db push
```

This will create a test store automatically.

### 2. Create Users in Supabase Auth Dashboard

1. Go to your Supabase project: [https://supabase.com/dashboard](https://supabase.com/dashboard)
2. Navigate to **Authentication** → **Users**
3. Click **"Add user"** → **"Create new user"**
4. Create three users with the following credentials:

   **Chef User:**
   - Email: `chef@test.com`
   - Password: `password123`
   - Auto Confirm User: ✅ (checked)

   **Waiter User:**
   - Email: `waiter@test.com`
   - Password: `password123`
   - Auto Confirm User: ✅ (checked)

   **Admin User:**
   - Email: `admin@test.com`
   - Password: `password123`
   - Auto Confirm User: ✅ (checked)

5. **Copy the UUID** for each user (you'll see it in the users list)

### 3. Insert User Records into Database

Run this SQL in the Supabase SQL Editor, replacing the UUIDs with the actual ones from step 2:

```sql
-- First, get the store UUID
DO $$
DECLARE
    v_store_id uuid;
    -- Replace these UUIDs with the actual Auth user UUIDs from Supabase Dashboard
    v_chef_uuid uuid := 'PASTE_CHEF_UUID_HERE';
    v_waiter_uuid uuid := 'PASTE_WAITER_UUID_HERE';
    v_admin_uuid uuid := 'PASTE_ADMIN_UUID_HERE';
BEGIN
    -- Get the test store ID
    SELECT id INTO v_store_id FROM stores WHERE name = 'Test Store' LIMIT 1;
    
    IF v_store_id IS NULL THEN
        RAISE EXCEPTION 'Test store not found. Run the seed migration first.';
    END IF;
    
    -- Insert chef user
    INSERT INTO users (id, email, display_name, role, store_id, created_at, updated_at)
    VALUES (v_chef_uuid, 'chef@test.com', 'Test Chef', 'chef', v_store_id, now(), now())
    ON CONFLICT (id) DO UPDATE
    SET email = EXCLUDED.email,
        display_name = EXCLUDED.display_name,
        role = EXCLUDED.role,
        store_id = EXCLUDED.store_id,
        updated_at = now();
    
    -- Insert waiter user
    INSERT INTO users (id, email, display_name, role, store_id, created_at, updated_at)
    VALUES (v_waiter_uuid, 'waiter@test.com', 'Test Waiter', 'waiter', v_store_id, now(), now())
    ON CONFLICT (id) DO UPDATE
    SET email = EXCLUDED.email,
        display_name = EXCLUDED.display_name,
        role = EXCLUDED.role,
        store_id = EXCLUDED.store_id,
        updated_at = now();
    
    -- Insert admin user
    INSERT INTO users (id, email, display_name, role, store_id, created_at, updated_at)
    VALUES (v_admin_uuid, 'admin@test.com', 'Test Admin', 'admin', v_store_id, now(), now())
    ON CONFLICT (id) DO UPDATE
    SET email = EXCLUDED.email,
        display_name = EXCLUDED.display_name,
        role = EXCLUDED.role,
        store_id = EXCLUDED.store_id,
        updated_at = now();
    
    RAISE NOTICE 'Test users created successfully!';
END $$;
```

### 4. Verify Setup

Run this query to verify everything is set up correctly:

```sql
SELECT 
    u.id,
    u.email,
    u.display_name,
    u.role,
    s.name as store_name
FROM users u
JOIN stores s ON u.store_id = s.id
WHERE u.email IN ('chef@test.com', 'waiter@test.com', 'admin@test.com')
ORDER BY u.role;
```

You should see all three users with their store assignments.

## Test Credentials Summary

After setup, you can use these credentials to log into the Android app:

| Role | Email | Password |
|------|-------|----------|
| Chef | `chef@test.com` | `password123` |
| Waiter | `waiter@test.com` | `password123` |
| Admin | `admin@test.com` | `password123` |

## Troubleshooting

### "Test store not found" error
- Make sure you've run `supabase db push` to apply all migrations
- Check that the store exists: `SELECT * FROM stores WHERE name = 'Test Store';`

### "Duplicate key value violates unique constraint" error
- The user record already exists, this is okay - the `ON CONFLICT` clause will update it
- Or delete existing users first: `DELETE FROM users WHERE email LIKE '%@test.com';`

### Login fails with "User profile missing"
- Make sure the user UUID in the `users` table matches the UUID from Supabase Auth
- Verify the user exists: `SELECT * FROM users WHERE email = 'chef@test.com';`
- Check that the user has a valid `store_id`

### Cannot see orders after login
- Make sure the user's `store_id` matches an existing store
- Verify the store has orders: `SELECT * FROM orders WHERE store_id = (SELECT store_id FROM users WHERE email = 'chef@test.com');`

## Notes

- The `id` in the `users` table **must** match the user's UUID from Supabase Auth
- Each user must have a valid `store_id` that references an existing store
- Users can only see orders for their assigned store (enforced by Row Level Security)
- The seed migration creates a single "Test Store" - you can create additional stores as needed

