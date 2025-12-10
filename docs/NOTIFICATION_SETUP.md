# Notification Setup Guide

This guide explains how to set up and verify notifications for order alterations and item prepared events.

## Overview

The notification system supports two methods:
1. **Edge Function (Recommended)**: More reliable, centralized logic
2. **Direct OneSignal (Fallback)**: Direct API calls from database functions

## Configuration

### Method 1: Edge Function (Recommended)

Set these database settings:

```sql
ALTER DATABASE postgres SET app.project_ref = 'your-project-ref';
ALTER DATABASE postgres SET app.service_role_key = 'your-service-role-key';
```

To get your project ref:
- Go to your Supabase dashboard
- The project ref is in the URL: `https://[project-ref].supabase.co`
- Or find it in Project Settings > API

To get your service role key:
- Go to Project Settings > API
- Copy the "service_role" key (NOT the anon key)

### Method 2: Direct OneSignal (Fallback)

If Edge Function is not configured, the system will fall back to direct OneSignal:

```sql
ALTER DATABASE postgres SET app.onesignal_app_id = 'your-onesignal-app-id';
ALTER DATABASE postgres SET app.onesignal_rest_key = 'your-onesignal-rest-api-key';
```

## Verify Configuration

Run the diagnostic script to check your configuration:

```sql
-- Run this in Supabase SQL Editor
\i supabase/migrations/CHECK_NOTIFICATION_CONFIG.sql
```

Or manually check:

```sql
-- Check Edge Function config
SHOW app.project_ref;
SHOW app.service_role_key;

-- Check OneSignal config
SHOW app.onesignal_app_id;
SHOW app.onesignal_rest_key;

-- Check users with subscription IDs
SELECT 
    email, 
    role, 
    CASE WHEN onesignal_subscription_id IS NULL THEN 'NO SUBSCRIPTION' ELSE 'HAS SUBSCRIPTION' END
FROM users
WHERE store_id IS NOT NULL;
```

## Notification Events

### 1. Order Altered

When an order is edited via `alter_order_v2`, waiters and admins receive a notification:
- **Title**: "Order #[number] has been updated"
- **Message**: "Order #[number] has been modified. Please refresh to see changes."
- **Recipients**: Waiters and Admins

**Migration**: `0025_add_alter_order_notification.sql`

### 2. Item Prepared

When a chef marks an item as prepared (toggles item switch), waiters and admins receive a notification:
- **Title**: "Item ready for order #[number]"
- **Message**: "[Item Name] is prepared"
- **Recipients**: Waiters and Admins

**Function**: `mark_item_prepared` (migration `0022_fix_item_prepared_notification.sql`)

## Troubleshooting

### Notifications Not Working

1. **Check Configuration**:
   ```sql
   -- Run the check script
   \i supabase/migrations/CHECK_NOTIFICATION_CONFIG.sql
   ```

2. **Check User Subscriptions**:
   - Users must have `onesignal_subscription_id` set in the `users` table
   - This is set when the Android app registers the device with OneSignal
   - Verify users have registered their devices

3. **Check Supabase Logs**:
   - Go to Logs > Postgres Logs
   - Look for NOTICE messages from `mark_item_prepared` or `alter_order_v2`
   - Look for errors related to `net.http_post` or OneSignal

4. **Check Edge Function Logs**:
   - Go to Edge Functions > order-events > Logs
   - Look for errors when notifications are sent

5. **Check OneSignal Dashboard**:
   - Go to OneSignal dashboard > Delivery
   - Check if notifications are being sent
   - Check if there are delivery failures

### Common Issues

**Issue**: "No waiter/admin subscription IDs found"
- **Solution**: Ensure users have registered their devices. The Android app should call `register_device` when OneSignal is initialized.

**Issue**: "Edge Function notification failed"
- **Solution**: 
  - Verify `app.project_ref` and `app.service_role_key` are set correctly
  - Check Edge Function is deployed: `supabase functions deploy order-events`
  - Verify Edge Function secrets are set: `supabase secrets set ONESIGNAL_APP_ID=...`

**Issue**: "OneSignal notification failed"
- **Solution**:
  - Verify `app.onesignal_app_id` and `app.onesignal_rest_key` are set correctly
  - Check OneSignal API keys are valid
  - Ensure users have valid subscription IDs

## Testing

### Test Order Alteration Notification

1. Login as waiter on Android app
2. Login as waiter/chef on another device (or use admin panel)
3. Edit an existing order
4. Waiters should receive notification about the order update
5. Order list should refresh automatically (via realtime subscription)

### Test Item Prepared Notification

1. Login as chef on Android app
2. Login as waiter on another device
3. Chef marks an item as prepared (toggle switch)
4. Waiter should receive notification: "[Item Name] is prepared"
5. Each item toggle should send a separate notification

## Edge Function Deployment

Ensure the Edge Function is deployed:

```bash
cd supabase/functions/order-events
supabase functions deploy order-events
```

Set Edge Function secrets:

```bash
supabase secrets set ONESIGNAL_APP_ID=your-app-id
supabase secrets set ONESIGNAL_REST_KEY=your-rest-key
```

## Database Migrations

Run all migrations in order:

```bash
supabase migration up
```

Key migrations:
- `0022_fix_item_prepared_notification.sql` - Item prepared notifications
- `0025_add_alter_order_notification.sql` - Order alteration notifications

## Android App

The Android app automatically:
1. Registers device with OneSignal on login
2. Sets `onesignal_subscription_id` in the `users` table
3. Handles incoming notifications
4. Refreshes order lists when orders change (via realtime subscription)

No additional configuration needed on the Android side for basic functionality.

