# Test Execution Guide - Phase 7

This guide provides step-by-step procedures for executing all test cases in Phase 7: Testing & Validation.

## Prerequisites

1. **Test Environment Setup**
   - Test Supabase project configured
   - Test users created (chef@test.com, waiter@test.com, admin@test.com)
   - Test data migration (0017_comprehensive_test_data.sql) applied
   - Android app installed on test device(s)
   - Admin dashboard accessible
   - OneSignal configured for push notifications

2. **Test Credentials**
   - Chef: `chef@test.com` / `password123`
   - Waiter: `waiter@test.com` / `password123`
   - Admin: `admin@test.com` / `password123`

## Test Execution Order

Execute tests in the following order for best results:

1. **Test Infrastructure** (if not already done)
2. **Table Management Tests**
3. **Menu Synchronization Tests**
4. **Order Flow Tests**
5. **Order Editing Tests**
6. **Notification Tests**
7. **Integration Tests**
8. **Performance Tests**
9. **Error Scenario Tests**

## Test Execution Procedures

### 7.2 Table Management Testing

#### Test 7.2.1: Create DINE_IN Order with Table Number

**Steps:**
1. Log in to waiter app as `waiter@test.com`
2. Navigate to order creation flow
3. Select "Dine In" order type
4. Select table 1
5. Add items to order
6. Place order

**Expected Results:**
- Order created successfully
- Order appears in admin dashboard with "Table 01" badge
- Table 1 appears in `get_occupied_tables()` query
- `check_table_availability(1, store_id)` returns FALSE

**Verification:**
```sql
-- Run in Supabase SQL Editor
SELECT * FROM orders WHERE table_number = 1 AND status = 'Created';
SELECT * FROM get_occupied_tables('YOUR_STORE_ID');
SELECT check_table_availability(1, 'YOUR_STORE_ID');
```

#### Test 7.2.2: Try to Create Order on Occupied Table

**Steps:**
1. Ensure table 1 has an active order (from Test 7.2.1)
2. Log in to waiter app
3. Create new DINE_IN order
4. Try to select table 1

**Expected Results:**
- Table 1 shows as "Occupied" in UI
- Table 1 is disabled/grayed out
- Error message if attempting to create order: "Table 1 is already occupied"
- Order creation fails

**Verification:**
- Check UI shows occupied state
- Check error message displayed
- Verify order not created in database

#### Test 7.2.3: Deliver Order and Verify Table Availability

**Steps:**
1. Find order on table 1 with status "Prepared"
2. Log in to waiter app
3. Navigate to "Ready Orders" page
4. Click "Deliver" on the order
5. Verify order status changes to "Delivered"
6. Try to create new order on table 1

**Expected Results:**
- Order status updates to "Delivered"
- Table 1 no longer in `get_occupied_tables()` result
- `check_table_availability(1, store_id)` returns TRUE
- New order can be created on table 1

**Verification:**
```sql
SELECT * FROM orders WHERE table_number = 1 ORDER BY updated_at DESC LIMIT 1;
SELECT * FROM get_occupied_tables('YOUR_STORE_ID');
SELECT check_table_availability(1, 'YOUR_STORE_ID');
```

#### Test 7.2.4: Create EAT_AWAY Order with License Plate

**Steps:**
1. Log in to waiter app
2. Select "Eat Away" order type
3. Enter license plate: "1234"
4. Add items
5. Place order

**Expected Results:**
- Order created with `license_plate = '1234'`
- `table_number IS NULL`
- Order appears in admin with "License: 1234" badge

**Verification:**
```sql
SELECT * FROM orders WHERE license_plate = '1234';
```

#### Test 7.2.5: Invalid License Plate Validation

**Steps:**
1. Try to create EAT_AWAY order with license "123" (3 digits)
2. Try with "12345" (5 digits)
3. Try with "ABCD" (non-numeric)

**Expected Results:**
- Validation error: "License plate must be exactly 4 digits"
- Order not created
- Database constraint prevents invalid format

#### Test 7.2.6: All 7 Tables Occupied

**Steps:**
1. Create orders on tables 1-7 (all with status Created/Accepted/InKitchen/Prepared)
2. Try to create 8th DINE_IN order

**Expected Results:**
- All tables show as occupied
- Error: "No available tables"
- Order creation blocked

### 7.3 Menu Synchronization Testing

#### Test 7.3.1: Admin Menu Upload

**Steps:**
1. Log in to admin dashboard
2. Navigate to Menu Upload
3. Upload CSV with new categories and items
4. Verify data in Supabase

**Expected Results:**
- CSV parsed correctly
- Categories and items inserted into database
- `menu_upsert()` RPC executes successfully
- Admin menu display updates

**Verification:**
```sql
SELECT * FROM categories WHERE store_id = 'YOUR_STORE_ID';
SELECT * FROM items WHERE store_id = 'YOUR_STORE_ID';
```

#### Test 7.3.2: Real-time Menu Sync

**Steps:**
1. Open waiter app and admin dashboard simultaneously
2. Admin adds new menu item
3. Observe waiter app

**Expected Results:**
- Waiter sees new item within 2 seconds
- Room database cache updated
- No manual refresh needed

**Verification:**
- Check waiter app menu updates automatically
- Check Room database has new item

#### Test 7.3.3: Frequent Items Reordering

**Steps:**
1. Admin reorders "Most Bought" items
2. Observe waiter app

**Expected Results:**
- Waiter sees updated order immediately
- `frequent_items` table updated
- Waiter "Most Bought" section reflects new order

### 7.4 Order Flow Testing

#### Test 7.4.1: Complete Order Lifecycle

**Steps:**
1. Waiter creates DINE_IN order on table 2
2. Verify table 2 occupied
3. Chef accepts order (status → InKitchen)
4. Chef prepares order (status → Prepared)
5. Verify waiter receives notification
6. Waiter delivers order (status → Delivered)
7. Verify admin receives toast
8. Verify table 2 becomes available

**Expected Results:**
- Each status transition updates everywhere within 1 second
- Notifications delivered correctly
- Table availability updates correctly

### 7.5 Order Editing Testing

#### Test 7.5.1: Alter Order

**Steps:**
1. Open existing order in waiter app
2. Click "Alter Order"
3. Change item quantities
4. Remove some items
5. Add new items
6. Save changes

**Expected Results:**
- `alter_order` RPC called
- Changes reflect in admin immediately
- Order total recalculated
- `order_events` table has "order_altered" entry

**Verification:**
```sql
SELECT * FROM order_events WHERE metadata->>'action' = 'order_altered' ORDER BY created_at DESC LIMIT 1;
```

#### Test 7.5.2: Edit Individual Item

**Steps:**
1. Open order in edit mode
2. Edit item quantity from 2 to 3
3. Save

**Expected Results:**
- `update_order_item` RPC called
- Quantity updated in database
- Order total recalculated
- Changes reflect in admin

#### Test 7.5.3: Delete Item

**Steps:**
1. Open order in edit mode
2. Delete an item
3. Confirm deletion

**Expected Results:**
- Confirmation dialog shown
- `delete_order_item` RPC called
- Item removed from database
- Order total recalculated

#### Test 7.5.4: Status Validation

**Steps:**
1. Try to edit order with status "Delivered"
2. Try to edit order with status "Closed"
3. Try to edit order with status "Canceled"
4. Try to edit order with status "InKitchen"

**Expected Results:**
- Delivered/Closed/Canceled: Error message, edit blocked
- InKitchen: Warning dialog, but modification allowed

### 7.6 Notification Testing

#### Test 7.6.1: Prepared Order Notification

**Steps:**
1. Chef marks order as "Prepared"
2. Observe waiter device

**Expected Results:**
- OneSignal notification sent
- Waiter receives push notification
- Notification title: "Order #1234 ready"
- Notification sound plays

#### Test 7.6.2: Notification Deep Link

**Steps:**
1. Receive notification on waiter device
2. Click notification

**Expected Results:**
- App opens (if closed)
- Navigates to "Ready Orders" page
- Order appears in ready list

#### Test 7.6.3: Admin Toast Notification

**Steps:**
1. Waiter marks order as "Delivered"
2. Observe admin dashboard

**Expected Results:**
- Toast appears within 2 seconds
- Toast message: "Waiter has delivered Order #1234"

### 7.7 Integration Testing

#### Test 7.7.1: Menu Sync During Active Ordering

**Steps:**
1. Waiter opens item selection screen
2. Admin adds new menu item
3. Waiter adds new item to order

**Expected Results:**
- Waiter sees new item appear in real-time
- Order created with new item successfully

#### Test 7.7.2: Network Disconnection

**Steps:**
1. Disable network on waiter device
2. Attempt to create order
3. Enable network
4. Retry order creation

**Expected Results:**
- Error message: "No connection"
- Order creation fails gracefully
- After reconnection, order creation succeeds

### 7.8 Performance Testing

#### Test 7.8.1: Real-time Update Latency

**Steps:**
1. Record timestamp when chef changes order status
2. Record timestamp when waiter app UI updates
3. Calculate latency

**Expected Results:**
- Latency < 2 seconds

#### Test 7.8.2: Database Query Performance

**Steps:**
1. Create 100 test orders
2. Run `get_occupied_tables()` query
3. Measure execution time

**Expected Results:**
- Query execution time < 100ms

### 7.9 Error Scenario Testing

#### Test 7.9.1: Concurrent Table Booking

**Steps:**
1. Two waiters simultaneously try to create order on table 5
2. Observe results

**Expected Results:**
- Only one succeeds
- Other receives error: "Table 5 is already occupied"
- Database constraint prevents race condition

## Test Result Documentation

For each test case:
1. Record pass/fail status
2. Document actual results vs expected results
3. Note any deviations or issues
4. Capture screenshots for UI tests
5. Record performance metrics
6. Document error messages

Use `TEST_RESULTS_TEMPLATE.md` for structured documentation.

## Troubleshooting

### Test Data Not Found
- Verify migration 0017_comprehensive_test_data.sql was applied
- Check test store exists: `SELECT * FROM stores WHERE name = 'Test Store';`
- Verify test users exist: `SELECT * FROM users WHERE email LIKE '%@test.com';`

### Real-time Updates Not Working
- Check Supabase realtime is enabled for tables
- Verify network connection
- Check subscription is active in app logs

### Notifications Not Received
- Verify OneSignal configuration
- Check device has notification permissions
- Verify user has `onesignal_player_id` set in database

## Next Steps

After completing all tests:
1. Document results in `TEST_RESULTS_TEMPLATE.md`
2. Report bugs using `BUG_REPORT_TEMPLATE.md`
3. Create test execution report with summary
4. Document performance benchmarks
5. List known issues

