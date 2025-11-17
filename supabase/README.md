# Supabase Backend

This directory contains database migrations, Edge Functions, and integration tests that replace the legacy Firebase stack.

## Structure
- `migrations/` – SQL files applied in order via `supabase db push`.
- `functions/order-events/` – Edge Function that bridges Supabase realtime updates with OneSignal notifications.
- `tests/` – HTTP requests runnable with the Supabase CLI or REST client to validate the stack.

## Getting Started
1. Install the [Supabase CLI](https://supabase.com/docs/guides/cli).
2. Create a local `.env` populated with:
   ```
   SUPABASE_URL=https://xyzcompany.supabase.co
   SUPABASE_ANON_KEY=...
   SERVICE_ROLE_KEY=...
   ONESIGNAL_APP_ID=...
   ONESIGNAL_REST_KEY=...
   ```
3. Start the local stack: `supabase start`.
4. Apply migrations: `supabase db push`.
5. Deploy Edge Function: `supabase functions deploy order-events`.
6. **Set up test users**: See [TEST_USERS_SETUP.md](./TEST_USERS_SETUP.md) for instructions.
7. Run tests (update placeholders in `tests/order_events.http`): `supabase functions serve order-events` then execute the HTTP requests via REST client.

## Test Users

To set up test credentials for the Android app, follow the guide in [TEST_USERS_SETUP.md](./TEST_USERS_SETUP.md).

**Quick Reference:**
- Chef: `chef@test.com` / `password123`
- Waiter: `waiter@test.com` / `password123`
- Admin: `admin@test.com` / `password123`

## Notes
- RPC `orders_upsert` uses `service_role` privilege; call from Edge Functions or secure backend only.
- `register_device` is exposed to authenticated clients; ensure the mobile app calls it during login/logout.
- Remember to configure OneSignal to accept device registrations from the Android bundle ID.




