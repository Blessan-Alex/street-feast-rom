# Database Webhook Setup for OneSignal Notifications

The Edge Function `order-events` needs to be triggered when orders are inserted or updated. You have two options:

## Option 1: Supabase Dashboard Webhook (Recommended)

This is the easiest and most reliable method:

1. **Deploy the Edge Function** (if not already deployed):
   ```bash
   cd /mnt/c/Users/blaze/Downloads/Street-Feast
   npx supabase functions deploy order-events
   ```

2. **Set OneSignal Secrets**:
   ```bash
   npx supabase secrets set ONESIGNAL_APP_ID=your_onesignal_app_id
   npx supabase secrets set ONESIGNAL_REST_KEY=your_onesignal_rest_api_key
   ```

3. **Configure Database Webhook in Supabase Dashboard**:
   - Go to your Supabase Dashboard
   - Navigate to **Database → Webhooks**
   - Click **Create a new webhook**
   - Configure:
     - **Name**: `order-events-webhook`
     - **Table**: `orders`
     - **Events**: Check `INSERT` and `UPDATE`
     - **HTTP Request**:
       - **URL**: `https://[YOUR-PROJECT-REF].supabase.co/functions/v1/order-events`
       - Replace `[YOUR-PROJECT-REF]` with your actual Supabase project reference
       - **Method**: `POST`
       - **Headers**:
         ```
         Authorization: Bearer [YOUR-SERVICE-ROLE-KEY]
         Content-Type: application/json
         ```
       - **Body** (JSON):
         ```json
         {
           "type": "{{event_type}}",
           "table": "orders",
           "record": {{record}},
           "old_record": {{old_record}}
         }
         ```

4. **Get your Project Reference**:
   - Go to Settings → API
   - Your project URL is: `https://[PROJECT-REF].supabase.co`
   - Use the `[PROJECT-REF]` part in the webhook URL

5. **Get your Service Role Key**:
   - Go to Settings → API
   - Copy the `service_role` key (keep it secret!)
   - Use it in the Authorization header

## Option 2: Database Trigger with pg_net (Alternative)

If you prefer database-level triggers, you can use the migration `0007_add_order_notification_webhook.sql`, but you'll need to configure:

1. **Set database configuration**:
   ```sql
   ALTER DATABASE postgres SET app.project_ref = 'your-project-ref';
   ALTER DATABASE postgres SET app.service_role_key = 'your-service-role-key';
   ```

   **Note**: Storing the service role key in the database is less secure. Option 1 (Dashboard webhook) is recommended.

2. **Apply the migration**:
   ```bash
   npx supabase db push
   ```

## Testing

After setup, test by creating an order from the admin app. Check:

1. **Edge Function Logs**: Go to Edge Functions → order-events → Logs
2. **OneSignal Dashboard**: Check if notifications were sent
3. **Android App**: Verify push notification is received

## Troubleshooting

- **No notifications**: Check Edge Function logs for errors
- **Edge Function not called**: Verify webhook is active in Dashboard
- **OneSignal errors**: Verify secrets are set correctly
- **Device not receiving**: Check that `onesignal_player_id` is set in `users` table for logged-in users

