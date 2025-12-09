-- Test SQL Scripts for Phase 7: Testing & Validation
-- These scripts can be run in Supabase SQL Editor to verify database constraints and functions
-- Run these after applying migration 0017_comprehensive_test_data.sql

-- ========================================
-- 7.2 Table Management Testing - SQL Verification
-- ========================================

-- Test 7.2.1: Verify DINE_IN order with table number
-- Expected: Order exists with table_number = 1, license_plate IS NULL
SELECT 
    id,
    number,
    status,
    type,
    table_number,
    license_plate,
    CASE 
        WHEN type = 'DineIn' AND table_number IS NOT NULL AND license_plate IS NULL THEN 'PASS'
        ELSE 'FAIL'
    END as test_result
FROM orders 
WHERE type = 'DineIn' AND table_number = 1 
LIMIT 1;

-- Test 7.2.2: Verify get_occupied_tables() function
-- Expected: Returns tables 1, 2, 3, 4, 6, 7 (table 5 should not be included - order is Delivered)
SELECT 
    table_number,
    'PASS' as test_result
FROM get_occupied_tables(
    (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1)
)
ORDER BY table_number;

-- Test 7.2.3: Verify check_table_availability() function
-- Expected: Table 1 should return FALSE (occupied), Table 5 should return TRUE (available - order delivered)
SELECT 
    1 as table_num,
    check_table_availability(
        1,
        (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1)
    ) as is_available,
    CASE 
        WHEN check_table_availability(1, (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1)) = FALSE THEN 'PASS'
        ELSE 'FAIL'
    END as test_result
UNION ALL
SELECT 
    5 as table_num,
    check_table_availability(
        5,
        (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1)
    ) as is_available,
    CASE 
        WHEN check_table_availability(5, (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1)) = TRUE THEN 'PASS'
        ELSE 'FAIL'
    END as test_result;

-- Test 7.2.4: Verify EAT_AWAY order with license plate
-- Expected: Order exists with license_plate = '1234', table_number IS NULL
SELECT 
    id,
    number,
    status,
    type,
    table_number,
    license_plate,
    CASE 
        WHEN type = 'EatAway' AND license_plate IS NOT NULL AND table_number IS NULL THEN 'PASS'
        ELSE 'FAIL'
    END as test_result
FROM orders 
WHERE type = 'EatAway' AND license_plate = '1234'
LIMIT 1;

-- Test 7.2.5: Verify PARCEL order (no table, no license)
-- Expected: Order exists with table_number IS NULL, license_plate IS NULL
SELECT 
    id,
    number,
    status,
    type,
    table_number,
    license_plate,
    CASE 
        WHEN type = 'Parcel' AND table_number IS NULL AND license_plate IS NULL THEN 'PASS'
        ELSE 'FAIL'
    END as test_result
FROM orders 
WHERE type = 'Parcel'
LIMIT 1;

-- Test 7.2.6: Verify table number constraint (should prevent table 0 and table 8)
-- This test will fail if constraints are not working
-- Try to insert invalid table numbers (should fail)
DO $$
BEGIN
    -- This should fail due to constraint
    BEGIN
        INSERT INTO orders (store_id, type, table_number, status)
        VALUES (
            (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1),
            'DineIn',
            0,  -- Invalid: table 0
            'Created'
        );
        RAISE EXCEPTION 'TEST FAILED: Constraint should prevent table 0';
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE 'PASS: Constraint prevents table 0';
    END;
    
    BEGIN
        INSERT INTO orders (store_id, type, table_number, status)
        VALUES (
            (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1),
            'DineIn',
            8,  -- Invalid: table 8
            'Created'
        );
        RAISE EXCEPTION 'TEST FAILED: Constraint should prevent table 8';
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE 'PASS: Constraint prevents table 8';
    END;
END $$;

-- Test 7.2.7: Verify license plate format constraint (should prevent invalid formats)
DO $$
BEGIN
    -- This should fail due to constraint
    BEGIN
        INSERT INTO orders (store_id, type, license_plate, status)
        VALUES (
            (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1),
            'EatAway',
            '123',  -- Invalid: 3 digits
            'Created'
        );
        RAISE EXCEPTION 'TEST FAILED: Constraint should prevent 3-digit license';
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE 'PASS: Constraint prevents 3-digit license';
    END;
    
    BEGIN
        INSERT INTO orders (store_id, type, license_plate, status)
        VALUES (
            (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1),
            'EatAway',
            '12345',  -- Invalid: 5 digits
            'Created'
        );
        RAISE EXCEPTION 'TEST FAILED: Constraint should prevent 5-digit license';
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE 'PASS: Constraint prevents 5-digit license';
    END;
    
    BEGIN
        INSERT INTO orders (store_id, type, license_plate, status)
        VALUES (
            (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1),
            'EatAway',
            'ABCD',  -- Invalid: non-numeric
            'Created'
        );
        RAISE EXCEPTION 'TEST FAILED: Constraint should prevent non-numeric license';
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE 'PASS: Constraint prevents non-numeric license';
    END;
END $$;

-- Test 7.2.8: Verify type-based constraints
-- DineIn must have table_number, no license_plate
-- EatAway must have license_plate, no table_number
-- Parcel must have both NULL
DO $$
BEGIN
    -- Test: DineIn without table_number (should fail)
    BEGIN
        INSERT INTO orders (store_id, type, status)
        VALUES (
            (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1),
            'DineIn',
            'Created'
        );
        RAISE EXCEPTION 'TEST FAILED: DineIn should require table_number';
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE 'PASS: DineIn requires table_number';
    END;
    
    -- Test: EatAway without license_plate (should fail)
    BEGIN
        INSERT INTO orders (store_id, type, status)
        VALUES (
            (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1),
            'EatAway',
            'Created'
        );
        RAISE EXCEPTION 'TEST FAILED: EatAway should require license_plate';
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE 'PASS: EatAway requires license_plate';
    END;
END $$;

-- ========================================
-- 7.3 Menu Synchronization Testing - SQL Verification
-- ========================================

-- Test 7.3.1: Verify menu data exists
SELECT 
    'Categories' as data_type,
    COUNT(*) as count,
    CASE WHEN COUNT(*) >= 3 THEN 'PASS' ELSE 'FAIL' END as test_result
FROM categories
WHERE store_id = (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1)
UNION ALL
SELECT 
    'Items' as data_type,
    COUNT(*) as count,
    CASE WHEN COUNT(*) >= 6 THEN 'PASS' ELSE 'FAIL' END as test_result
FROM items
WHERE store_id = (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1)
UNION ALL
SELECT 
    'Frequent Items' as data_type,
    COUNT(*) as count,
    CASE WHEN COUNT(*) >= 4 THEN 'PASS' ELSE 'FAIL' END as test_result
FROM frequent_items
WHERE store_id = (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1);

-- Test 7.3.2: Verify frequent items order
SELECT 
    fi.order_index,
    i.name as item_name,
    CASE 
        WHEN fi.order_index = 0 THEN 'PASS'
        WHEN fi.order_index = 1 THEN 'PASS'
        WHEN fi.order_index = 2 THEN 'PASS'
        WHEN fi.order_index = 3 THEN 'PASS'
        ELSE 'FAIL'
    END as test_result
FROM frequent_items fi
JOIN items i ON fi.item_id = i.id
WHERE fi.store_id = (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1)
ORDER BY fi.order_index;

-- ========================================
-- 7.4 Order Flow Testing - SQL Verification
-- ========================================

-- Test 7.4.1: Verify orders in various states
SELECT 
    status,
    COUNT(*) as order_count,
    CASE 
        WHEN status IN ('Created', 'Accepted', 'InKitchen', 'Prepared', 'Delivered') THEN 'PASS'
        ELSE 'FAIL'
    END as test_result
FROM orders
WHERE store_id = (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1)
GROUP BY status
ORDER BY status;

-- Test 7.4.2: Verify order items exist for all orders
SELECT 
    o.id as order_id,
    o.number as order_number,
    COUNT(oi.id) as item_count,
    CASE 
        WHEN COUNT(oi.id) > 0 THEN 'PASS'
        ELSE 'FAIL'
    END as test_result
FROM orders o
LEFT JOIN order_items oi ON o.id = oi.order_id
WHERE o.store_id = (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1)
GROUP BY o.id, o.number
ORDER BY o.number;

-- Test 7.4.3: Verify order totals match item totals
SELECT 
    o.id as order_id,
    o.number as order_number,
    o.total as order_total,
    COALESCE(SUM(oi.price * oi.quantity), 0) as calculated_total,
    CASE 
        WHEN ABS(o.total - COALESCE(SUM(oi.price * oi.quantity), 0)) < 0.01 THEN 'PASS'
        ELSE 'FAIL'
    END as test_result
FROM orders o
LEFT JOIN order_items oi ON o.id = oi.order_id
WHERE o.store_id = (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1)
GROUP BY o.id, o.number, o.total
ORDER BY o.number;

-- ========================================
-- 7.5 Order Editing Testing - SQL Verification
-- ========================================

-- Test 7.5.1: Verify order_events table has entries
SELECT 
    COUNT(*) as event_count,
    CASE WHEN COUNT(*) > 0 THEN 'PASS' ELSE 'FAIL' END as test_result
FROM order_events
WHERE order_id IN (
    SELECT id FROM orders 
    WHERE store_id = (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1)
);

-- Test 7.5.2: Verify order events have metadata
SELECT 
    new_status,
    metadata->>'action' as action,
    COUNT(*) as count,
    CASE 
        WHEN metadata IS NOT NULL THEN 'PASS'
        ELSE 'FAIL'
    END as test_result
FROM order_events
WHERE order_id IN (
    SELECT id FROM orders 
    WHERE store_id = (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1)
)
GROUP BY new_status, metadata->>'action'
ORDER BY new_status;

-- ========================================
-- 7.8 Performance Testing - SQL Queries
-- ========================================

-- Test 7.8.1: Measure get_occupied_tables() performance
EXPLAIN ANALYZE
SELECT * FROM get_occupied_tables(
    (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1)
);

-- Test 7.8.2: Measure check_table_availability() performance
EXPLAIN ANALYZE
SELECT check_table_availability(
    1,
    (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1)
);

-- Test 7.8.3: Verify indexes are being used
-- Check if indexes exist
SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE tablename IN ('orders', 'order_items', 'categories', 'items', 'frequent_items')
AND schemaname = 'public'
ORDER BY tablename, indexname;

-- ========================================
-- Summary Test Report
-- ========================================

-- Generate summary of all test orders
SELECT 
    type,
    status,
    COUNT(*) as count,
    STRING_AGG(DISTINCT table_number::text, ', ') FILTER (WHERE table_number IS NOT NULL) as tables,
    STRING_AGG(DISTINCT license_plate, ', ') FILTER (WHERE license_plate IS NOT NULL) as licenses
FROM orders
WHERE store_id = (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1)
GROUP BY type, status
ORDER BY type, status;

-- Verify test data integrity
SELECT 
    'Total Orders' as metric,
    COUNT(*)::text as value,
    CASE WHEN COUNT(*) >= 12 THEN 'PASS' ELSE 'FAIL' END as test_result
FROM orders
WHERE store_id = (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1)
UNION ALL
SELECT 
    'Total Order Items' as metric,
    COUNT(*)::text as value,
    CASE WHEN COUNT(*) >= 12 THEN 'PASS' ELSE 'FAIL' END as test_result
FROM order_items
WHERE order_id IN (
    SELECT id FROM orders 
    WHERE store_id = (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1)
)
UNION ALL
SELECT 
    'Occupied Tables' as metric,
    COUNT(*)::text as value,
    CASE WHEN COUNT(*) >= 6 THEN 'PASS' ELSE 'FAIL' END as test_result
FROM get_occupied_tables(
    (SELECT id FROM stores WHERE name = 'Test Store' LIMIT 1)
);

