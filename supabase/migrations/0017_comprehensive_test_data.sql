-- Migration: 0017_comprehensive_test_data.sql
-- Description: Comprehensive test data for Phase 7 testing including menu, orders in various states
-- Dependencies: 0001, 0005, 0012, 0013
-- 
-- This migration creates:
-- 1. Test menu data (categories, items, frequent_items)
-- 2. Test orders in all statuses (Created, Accepted, InKitchen, Prepared, Delivered)
-- 3. Test orders with different types (DINE_IN, EAT_AWAY, PARCEL)
-- 4. Test orders on different tables (1-7) for table management testing
-- 5. Order items for test orders
--
-- NOTE: This assumes test users have been created via 0005_seed_test_data.sql
-- and the test store exists. Run this after setting up test users.

set check_function_bodies = off;
set search_path = public;

BEGIN;

-- ========================================
-- 1. Get Test Store ID
-- ========================================
DO $$
DECLARE
    v_store_id UUID;
    v_chef_id UUID;
    v_waiter_id UUID;
    v_admin_id UUID;
    
    -- Fixed UUIDs for test data (reproducible)
    v_cat_chinese UUID := '11111111-1111-1111-1111-111111111111';
    v_cat_indian UUID := '22222222-2222-2222-2222-222222222222';
    v_cat_desserts UUID := '33333333-3333-3333-3333-333333333333';
    
    v_item_soup UUID := 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
    v_item_rolls UUID := 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb';
    v_item_paneer UUID := 'cccccccc-cccc-cccc-cccc-cccccccccccc';
    v_item_chicken UUID := 'dddddddd-dddd-dddd-dddd-dddddddddddd';
    v_item_cake UUID := 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee';
    v_item_icecream UUID := 'ffffffff-ffff-ffff-ffff-ffffffffffff';
    
    v_order_1 UUID := '10000000-0000-0000-0000-000000000001';
    v_order_2 UUID := '20000000-0000-0000-0000-000000000002';
    v_order_3 UUID := '30000000-0000-0000-0000-000000000003';
    v_order_4 UUID := '40000000-0000-0000-0000-000000000004';
    v_order_5 UUID := '50000000-0000-0000-0000-000000000005';
    v_order_6 UUID := '60000000-0000-0000-0000-000000000006';
    v_order_7 UUID := '70000000-0000-0000-0000-000000000007';
    v_order_8 UUID := '80000000-0000-0000-0000-000000000008';
    v_order_9 UUID := '90000000-0000-0000-0000-000000000009';
    v_order_10 UUID := 'a0000000-0000-0000-0000-00000000000a';
    v_order_11 UUID := 'b0000000-0000-0000-0000-00000000000b';
    v_order_12 UUID := 'c0000000-0000-0000-0000-00000000000c';
    
BEGIN
    -- Get test store
    SELECT id INTO v_store_id FROM stores WHERE name = 'Test Store' LIMIT 1;
    
    IF v_store_id IS NULL THEN
        RAISE EXCEPTION 'Test store not found. Please run migration 0005_seed_test_data.sql first.';
    END IF;
    
    -- Get test user IDs (they should exist from 0005 migration)
    SELECT id INTO v_chef_id FROM users WHERE email = 'chef@test.com' LIMIT 1;
    SELECT id INTO v_waiter_id FROM users WHERE email = 'waiter@test.com' LIMIT 1;
    SELECT id INTO v_admin_id FROM users WHERE email = 'admin@test.com' LIMIT 1;
    
    -- ========================================
    -- 2. Create Test Menu Categories
    -- ========================================
    INSERT INTO categories (id, store_id, name, is_active, created_at, updated_at)
    VALUES
        (v_cat_chinese, v_store_id, 'Chinese', true, now(), now()),
        (v_cat_indian, v_store_id, 'Indian', true, now(), now()),
        (v_cat_desserts, v_store_id, 'Desserts', true, now(), now())
    ON CONFLICT (id) DO UPDATE SET
        name = EXCLUDED.name,
        is_active = EXCLUDED.is_active,
        updated_at = now();
    
    -- ========================================
    -- 3. Create Test Menu Items
    -- ========================================
    INSERT INTO items (id, category_id, store_id, name, sizes, veg_flag, flavors, is_active, created_at, updated_at)
    VALUES
        -- Chinese items
        (v_item_soup, v_cat_chinese, v_store_id, 'Chicken Soup', '["Small", "Large"]'::jsonb, 'NonVeg', NULL, true, now(), now()),
        (v_item_rolls, v_cat_chinese, v_store_id, 'Spring Rolls', '[]'::jsonb, 'Veg', NULL, true, now(), now()),
        -- Indian items
        (v_item_paneer, v_cat_indian, v_store_id, 'Paneer Tikka', '["Small", "Large"]'::jsonb, 'Veg', NULL, true, now(), now()),
        (v_item_chicken, v_cat_indian, v_store_id, 'Butter Chicken', '["Small", "Large"]'::jsonb, 'NonVeg', NULL, true, now(), now()),
        -- Desserts
        (v_item_cake, v_cat_desserts, v_store_id, 'Chocolate Cake', '[]'::jsonb, 'Veg', NULL, true, now(), now()),
        (v_item_icecream, v_cat_desserts, v_store_id, 'Ice Cream', '["Small", "Large"]'::jsonb, 'Veg', NULL, true, now(), now())
    ON CONFLICT (id) DO UPDATE SET
        category_id = EXCLUDED.category_id,
        name = EXCLUDED.name,
        sizes = EXCLUDED.sizes,
        veg_flag = EXCLUDED.veg_flag,
        flavors = EXCLUDED.flavors,
        is_active = EXCLUDED.is_active,
        updated_at = now();
    
    -- ========================================
    -- 4. Create Frequent Items
    -- ========================================
    -- Clear existing frequent items for test store
    DELETE FROM frequent_items WHERE store_id = v_store_id;
    
    -- Insert frequent items in order
    INSERT INTO frequent_items (id, store_id, item_id, order_index, created_at)
    VALUES
        (gen_random_uuid(), v_store_id, v_item_chicken, 0, now()),
        (gen_random_uuid(), v_store_id, v_item_paneer, 1, now()),
        (gen_random_uuid(), v_store_id, v_item_soup, 2, now()),
        (gen_random_uuid(), v_store_id, v_item_icecream, 3, now())
    ON CONFLICT (store_id, item_id) DO UPDATE SET
        order_index = EXCLUDED.order_index;
    
    -- ========================================
    -- 5. Create Test Orders in Various States
    -- ========================================
    
    -- Order 1: DINE_IN, Table 1, Status: Created
    INSERT INTO orders (id, store_id, number, status, type, table_number, license_plate, chef_tip, created_by, total, created_at, updated_at)
    VALUES (v_order_1, v_store_id, 1001, 'Created', 'DineIn', 1, NULL, '', v_waiter_id, 25.50, now() - interval '2 hours', now() - interval '2 hours')
    ON CONFLICT (id) DO UPDATE SET
        status = EXCLUDED.status,
        updated_at = now();
    
    -- Order 2: DINE_IN, Table 2, Status: Accepted
    INSERT INTO orders (id, store_id, number, status, type, table_number, license_plate, chef_tip, created_by, total, created_at, updated_at)
    VALUES (v_order_2, v_store_id, 1002, 'Accepted', 'DineIn', 2, NULL, '', v_waiter_id, 18.00, now() - interval '1 hour', now() - interval '30 minutes')
    ON CONFLICT (id) DO UPDATE SET
        status = EXCLUDED.status,
        updated_at = now();
    
    -- Order 3: DINE_IN, Table 3, Status: InKitchen
    INSERT INTO orders (id, store_id, number, status, type, table_number, license_plate, chef_tip, created_by, total, created_at, updated_at)
    VALUES (v_order_3, v_store_id, 1003, 'InKitchen', 'DineIn', 3, NULL, 'Extra spicy', v_waiter_id, 32.00, now() - interval '45 minutes', now() - interval '20 minutes')
    ON CONFLICT (id) DO UPDATE SET
        status = EXCLUDED.status,
        updated_at = now();
    
    -- Order 4: DINE_IN, Table 4, Status: Prepared
    INSERT INTO orders (id, store_id, number, status, type, table_number, license_plate, chef_tip, created_by, total, created_at, updated_at)
    VALUES (v_order_4, v_store_id, 1004, 'Prepared', 'DineIn', 4, NULL, '', v_waiter_id, 15.50, now() - interval '30 minutes', now() - interval '5 minutes')
    ON CONFLICT (id) DO UPDATE SET
        status = EXCLUDED.status,
        updated_at = now();
    
    -- Order 5: DINE_IN, Table 5, Status: Delivered
    INSERT INTO orders (id, store_id, number, status, type, table_number, license_plate, chef_tip, created_by, total, created_at, updated_at)
    VALUES (v_order_5, v_store_id, 1005, 'Delivered', 'DineIn', 5, NULL, '', v_waiter_id, 28.00, now() - interval '3 hours', now() - interval '2 hours')
    ON CONFLICT (id) DO UPDATE SET
        status = EXCLUDED.status,
        updated_at = now();
    
    -- Order 6: EAT_AWAY, License 1234, Status: Created
    INSERT INTO orders (id, store_id, number, status, type, table_number, license_plate, chef_tip, created_by, total, created_at, updated_at)
    VALUES (v_order_6, v_store_id, 1006, 'Created', 'EatAway', NULL, '1234', '', v_waiter_id, 22.00, now() - interval '1 hour', now() - interval '1 hour')
    ON CONFLICT (id) DO UPDATE SET
        status = EXCLUDED.status,
        updated_at = now();
    
    -- Order 7: EAT_AWAY, License 5678, Status: InKitchen
    INSERT INTO orders (id, store_id, number, status, type, table_number, license_plate, chef_tip, created_by, total, created_at, updated_at)
    VALUES (v_order_7, v_store_id, 1007, 'InKitchen', 'EatAway', NULL, '5678', 'No onions', v_waiter_id, 19.50, now() - interval '30 minutes', now() - interval '15 minutes')
    ON CONFLICT (id) DO UPDATE SET
        status = EXCLUDED.status,
        updated_at = now();
    
    -- Order 8: PARCEL, Status: Created
    INSERT INTO orders (id, store_id, number, status, type, table_number, license_plate, chef_tip, created_by, total, created_at, updated_at)
    VALUES (v_order_8, v_store_id, 1008, 'Created', 'Parcel', NULL, NULL, '', v_waiter_id, 12.00, now() - interval '20 minutes', now() - interval '20 minutes')
    ON CONFLICT (id) DO UPDATE SET
        status = EXCLUDED.status,
        updated_at = now();
    
    -- Order 9: DINE_IN, Table 6, Status: Accepted
    INSERT INTO orders (id, store_id, number, status, type, table_number, license_plate, chef_tip, created_by, total, created_at, updated_at)
    VALUES (v_order_9, v_store_id, 1009, 'Accepted', 'DineIn', 6, NULL, '', v_admin_id, 35.00, now() - interval '15 minutes', now() - interval '10 minutes')
    ON CONFLICT (id) DO UPDATE SET
        status = EXCLUDED.status,
        updated_at = now();
    
    -- Order 10: DINE_IN, Table 7, Status: Prepared
    INSERT INTO orders (id, store_id, number, status, type, table_number, license_plate, chef_tip, created_by, total, created_at, updated_at)
    VALUES (v_order_10, v_store_id, 1010, 'Prepared', 'DineIn', 7, NULL, 'Extra sauce', v_waiter_id, 20.00, now() - interval '10 minutes', now() - interval '2 minutes')
    ON CONFLICT (id) DO UPDATE SET
        status = EXCLUDED.status,
        updated_at = now();
    
    -- Order 11: EAT_AWAY, License 9012, Status: Prepared
    INSERT INTO orders (id, store_id, number, status, type, table_number, license_plate, chef_tip, created_by, total, created_at, updated_at)
    VALUES (v_order_11, v_store_id, 1011, 'Prepared', 'EatAway', NULL, '9012', '', v_waiter_id, 16.50, now() - interval '8 minutes', now() - interval '1 minute')
    ON CONFLICT (id) DO UPDATE SET
        status = EXCLUDED.status,
        updated_at = now();
    
    -- Order 12: PARCEL, Status: Delivered
    INSERT INTO orders (id, store_id, number, status, type, table_number, license_plate, chef_tip, created_by, total, created_at, updated_at)
    VALUES (v_order_12, v_store_id, 1012, 'Delivered', 'Parcel', NULL, NULL, '', v_waiter_id, 14.00, now() - interval '4 hours', now() - interval '3 hours')
    ON CONFLICT (id) DO UPDATE SET
        status = EXCLUDED.status,
        updated_at = now();
    
    -- ========================================
    -- 6. Create Order Items for Test Orders
    -- ========================================
    
    -- Order 1 items (Table 1, Created)
    INSERT INTO order_items (id, order_id, sku, name, size, veg_flag, quantity, price, modifiers, created_at, updated_at)
    VALUES
        (gen_random_uuid(), v_order_1, v_item_chicken::text, 'Butter Chicken', 'Large', 'NonVeg', 1, 15.50, '{"chefTip": ""}'::jsonb, now() - interval '2 hours', now() - interval '2 hours'),
        (gen_random_uuid(), v_order_1, v_item_paneer::text, 'Paneer Tikka', 'Small', 'Veg', 1, 10.00, '{"chefTip": ""}'::jsonb, now() - interval '2 hours', now() - interval '2 hours')
    ON CONFLICT DO NOTHING;
    
    -- Order 2 items (Table 2, Accepted)
    INSERT INTO order_items (id, order_id, sku, name, size, veg_flag, quantity, price, modifiers, created_at, updated_at)
    VALUES
        (gen_random_uuid(), v_order_2, v_item_soup::text, 'Chicken Soup', 'Small', 'NonVeg', 2, 18.00, '{"chefTip": ""}'::jsonb, now() - interval '1 hour', now() - interval '1 hour')
    ON CONFLICT DO NOTHING;
    
    -- Order 3 items (Table 3, InKitchen)
    INSERT INTO order_items (id, order_id, sku, name, size, veg_flag, quantity, price, modifiers, created_at, updated_at)
    VALUES
        (gen_random_uuid(), v_order_3, v_item_chicken::text, 'Butter Chicken', 'Large', 'NonVeg', 2, 31.00, '{"chefTip": "Extra spicy"}'::jsonb, now() - interval '45 minutes', now() - interval '45 minutes'),
        (gen_random_uuid(), v_order_3, v_item_icecream::text, 'Ice Cream', 'Small', 'Veg', 1, 1.00, '{"chefTip": ""}'::jsonb, now() - interval '45 minutes', now() - interval '45 minutes')
    ON CONFLICT DO NOTHING;
    
    -- Order 4 items (Table 4, Prepared)
    INSERT INTO order_items (id, order_id, sku, name, size, veg_flag, quantity, price, modifiers, created_at, updated_at)
    VALUES
        (gen_random_uuid(), v_order_4, v_item_rolls::text, 'Spring Rolls', NULL, 'Veg', 1, 8.00, '{"chefTip": ""}'::jsonb, now() - interval '30 minutes', now() - interval '30 minutes'),
        (gen_random_uuid(), v_order_4, v_item_cake::text, 'Chocolate Cake', NULL, 'Veg', 1, 7.50, '{"chefTip": ""}'::jsonb, now() - interval '30 minutes', now() - interval '30 minutes')
    ON CONFLICT DO NOTHING;
    
    -- Order 5 items (Table 5, Delivered)
    INSERT INTO order_items (id, order_id, sku, name, size, veg_flag, quantity, price, modifiers, created_at, updated_at)
    VALUES
        (gen_random_uuid(), v_order_5, v_item_paneer::text, 'Paneer Tikka', 'Large', 'Veg', 2, 28.00, '{"chefTip": ""}'::jsonb, now() - interval '3 hours', now() - interval '3 hours')
    ON CONFLICT DO NOTHING;
    
    -- Order 6 items (EAT_AWAY, License 1234, Created)
    INSERT INTO order_items (id, order_id, sku, name, size, veg_flag, quantity, price, modifiers, created_at, updated_at)
    VALUES
        (gen_random_uuid(), v_order_6, v_item_soup::text, 'Chicken Soup', 'Large', 'NonVeg', 1, 12.00, '{"chefTip": ""}'::jsonb, now() - interval '1 hour', now() - interval '1 hour'),
        (gen_random_uuid(), v_order_6, v_item_rolls::text, 'Spring Rolls', NULL, 'Veg', 1, 10.00, '{"chefTip": ""}'::jsonb, now() - interval '1 hour', now() - interval '1 hour')
    ON CONFLICT DO NOTHING;
    
    -- Order 7 items (EAT_AWAY, License 5678, InKitchen)
    INSERT INTO order_items (id, order_id, sku, name, size, veg_flag, quantity, price, modifiers, created_at, updated_at)
    VALUES
        (gen_random_uuid(), v_order_7, v_item_chicken::text, 'Butter Chicken', 'Small', 'NonVeg', 1, 12.50, '{"chefTip": "No onions"}'::jsonb, now() - interval '30 minutes', now() - interval '30 minutes'),
        (gen_random_uuid(), v_order_7, v_item_icecream::text, 'Ice Cream', 'Small', 'Veg', 1, 7.00, '{"chefTip": ""}'::jsonb, now() - interval '30 minutes', now() - interval '30 minutes')
    ON CONFLICT DO NOTHING;
    
    -- Order 8 items (PARCEL, Created)
    INSERT INTO order_items (id, order_id, sku, name, size, veg_flag, quantity, price, modifiers, created_at, updated_at)
    VALUES
        (gen_random_uuid(), v_order_8, v_item_cake::text, 'Chocolate Cake', NULL, 'Veg', 1, 12.00, '{"chefTip": ""}'::jsonb, now() - interval '20 minutes', now() - interval '20 minutes')
    ON CONFLICT DO NOTHING;
    
    -- Order 9 items (Table 6, Accepted)
    INSERT INTO order_items (id, order_id, sku, name, size, veg_flag, quantity, price, modifiers, created_at, updated_at)
    VALUES
        (gen_random_uuid(), v_order_9, v_item_chicken::text, 'Butter Chicken', 'Large', 'NonVeg', 2, 31.00, '{"chefTip": ""}'::jsonb, now() - interval '15 minutes', now() - interval '15 minutes'),
        (gen_random_uuid(), v_order_9, v_item_paneer::text, 'Paneer Tikka', 'Large', 'Veg', 1, 14.00, '{"chefTip": ""}'::jsonb, now() - interval '15 minutes', now() - interval '15 minutes')
    ON CONFLICT DO NOTHING;
    
    -- Order 10 items (Table 7, Prepared)
    INSERT INTO order_items (id, order_id, sku, name, size, veg_flag, quantity, price, modifiers, created_at, updated_at)
    VALUES
        (gen_random_uuid(), v_order_10, v_item_soup::text, 'Chicken Soup', 'Small', 'NonVeg', 1, 8.00, '{"chefTip": "Extra sauce"}'::jsonb, now() - interval '10 minutes', now() - interval '10 minutes'),
        (gen_random_uuid(), v_order_10, v_item_rolls::text, 'Spring Rolls', NULL, 'Veg', 1, 12.00, '{"chefTip": ""}'::jsonb, now() - interval '10 minutes', now() - interval '10 minutes')
    ON CONFLICT DO NOTHING;
    
    -- Order 11 items (EAT_AWAY, License 9012, Prepared)
    INSERT INTO order_items (id, order_id, sku, name, size, veg_flag, quantity, price, modifiers, created_at, updated_at)
    VALUES
        (gen_random_uuid(), v_order_11, v_item_paneer::text, 'Paneer Tikka', 'Small', 'Veg', 1, 10.00, '{"chefTip": ""}'::jsonb, now() - interval '8 minutes', now() - interval '8 minutes'),
        (gen_random_uuid(), v_order_11, v_item_icecream::text, 'Ice Cream', 'Large', 'Veg', 1, 6.50, '{"chefTip": ""}'::jsonb, now() - interval '8 minutes', now() - interval '8 minutes')
    ON CONFLICT DO NOTHING;
    
    -- Order 12 items (PARCEL, Delivered)
    INSERT INTO order_items (id, order_id, sku, name, size, veg_flag, quantity, price, modifiers, created_at, updated_at)
    VALUES
        (gen_random_uuid(), v_order_12, v_item_rolls::text, 'Spring Rolls', NULL, 'Veg', 2, 14.00, '{"chefTip": ""}'::jsonb, now() - interval '4 hours', now() - interval '4 hours')
    ON CONFLICT DO NOTHING;
    
    RAISE NOTICE 'Test data created successfully!';
    RAISE NOTICE 'Test store ID: %', v_store_id;
    RAISE NOTICE 'Created 3 categories, 6 items, 4 frequent items';
    RAISE NOTICE 'Created 12 test orders in various states';
    RAISE NOTICE 'Occupied tables: 1, 2, 3, 4, 6, 7 (tables 5 is available - order delivered)';
    
END $$;

COMMIT;

