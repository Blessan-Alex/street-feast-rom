<!-- Notification strategy for OneSignal + Supabase migration. -->

# Notification Architecture (OneSignal)

## Goals
- Provide reliable foreground and background push updates for order lifecycle events.
- Maintain existing local notification UX (titles, sounds, deep links).
- Support role-based targeting (chef vs waiter) and multi-store deployments.

## Device Registration Flow
1. **User Authenticates**:
   - Supabase session includes `user_id`, `role`, `store_id`.
2. **OneSignal SDK Initialization**:
   - `StreetFeastApplication` initializes OneSignal with App ID.
   - On login, app calls `OneSignal.login(user_id)` and sets tags:
     - `role`: `admin` / `chef` / `waiter`
     - `store_id`: UUID
     - `locale`: device locale (for localization later)
3. **Player ID Persistence**:
   - Application posts OneSignal `player_id` to Supabase via RPC (`register_device`) storing mapping in `users.onesignal_player_id`.
   - On logout, call `OneSignal.logout()` and remove mapping.

## Notification Types
| Event | Recipient Tag | Payload Summary |
|-------|---------------|-----------------|
| New Order (Created/Accepted) | `role = chef`, `store_id = X` | Title: “New Order #NN”; Body: first items; Data: `{orderId, status}` |
| Order Prepared | `role = waiter`, `store_id = X` | Title: “Order #NN ready”; Body: pickup window; Data: same |
| Order Canceled | `role in {chef, waiter}`, `store_id = X` | Alert both roles; include cancel reason. |
| Admin Alerts | `role = admin` | Optional operational notices. |

## Edge Function → OneSignal Payload
```json
POST https://onesignal.com/api/v1/notifications
{
  "app_id": "...",
  "include_player_ids": ["..."], // or tag filters
  "headings": {"en": "Order #42 ready"},
  "contents": {"en": "Mark as delivered when picked up."},
  "data": {
    "orderId": "uuid",
    "status": "Prepared",
    "storeId": "uuid"
  },
  "android_channel_id": "orders_updates",
  "ttl": 300
}
```
- Use tag filters when groups are small (< 2k) to avoid repeated REST calls.
- Attach `android_background_layout` if we customize heads-up style.

## Android Handling
- `NotificationHelper` continues to build local notifications.
- OneSignal notification opened handler routes to `MainActivity` with extras (`orderId`, `status`).
- Data-only notifications (for silent refresh) handled via `OneSignal.Notifications.addForegroundLifeCycleListener`.

## Background Considerations
- Android 13+ requires POST_NOTIFICATIONS permission; prompt on first launch using OneSignal `requestPermission`.
- Enable `NotificationExtenderService` if we need custom rendering while app in background.
- Ensure deep links work when app is terminated: add intent handling in manifest (`activity` with `android:launchMode="singleTop"`).

## Monitoring & Retry
- Supabase Edge Function logs response from OneSignal; retry on HTTP 5xx using exponential backoff (max 3 attempts).
- Metrics: per-store delivery counts, bounce rate; expose via Supabase Logflare export.
- Alerting: if pushes fail consecutively, raise Ops alert.

## Migration Checklist
- [ ] Create OneSignal dev & prod apps; configure Android package, Firebase Sender ID (for transport).
- [ ] Set environment variables: `ONESIGNAL_APP_ID`, `ONESIGNAL_REST_KEY`, `SUPABASE_SERVICE_ROLE_KEY`.
- [ ] Implement device registration RPC and update client to call during login/logout.
- [ ] Verify push delivery across foreground, background, and terminated states on Android 12 & 14.
- [ ] Document customer opt-out handling and data retention (GDPR / privacy).




