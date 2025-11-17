<!-- Document the existing Firebase/Firestore + FCM implementation to inform migration decisions. -->

# Current Firebase / Firestore Implementation

## Overview
- **Initialization**: `StreetFeastApplication` initializes `FirebaseApp`, enables Firestore offline persistence, and sets up Android notification channels.
- **Realtime Updates (Foreground)**: `OrderPushWatcher` attaches a Firestore snapshot listener on `stores/{storeId}/orders`, issuing local notifications through `NotificationHelper` when documents change.
- **Background Polling**: `OrderPollWorker` (WorkManager, 15 min cadence) queries Firestore for orders with `updatedAt` newer than the last seen timestamp and triggers matching local notifications. The worker handles resilience against listener drops but does not provide true push in Doze.
- **Remote Push (FCM)**: `AppMessagingService` extends `FirebaseMessagingService` to display notifications sent via FCM topics; however, Cloud Functions generating those pushes are minimal and do not include background payloads for Android 13+.

## Firestore Data Model
| Collection | Document | Key Fields | Notes |
|------------|----------|-----------|-------|
| `stores` | `{storeId}` | `name`, `location`, ... | Root document per store. |
| `stores/{storeId}/orders` | `{orderId}` | `status`, `orderNumber`, `updatedAt`, `items[]`, `customer`, `total`, ... | Primary collection observed in app. |
| `stores/{storeId}/orderItems` *(inferred)* | `{itemId}` | `name`, `quantity`, `options` | Referenced by orders. |

> Additional collections (`users`, `menu`) exist but are not part of the notification flow and remain unchanged by this migration.

## Supporting Components
- **NotificationHelper**: Centralizes local notification rendering for new, prepared, and canceled orders with role-specific sounds and icons.
- **Constants**: Holds channel IDs, default store IDs, order status enums (`Created`, `Accepted`, `InKitchen`, `Prepared`, `Delivered`, `Canceled`).
- **SoundManager**: Plays audio cues alongside notifications.

## Server-Side Logic (Cloud Functions)
- `createOrderNumber`: Generates incremental order numbers.
- `onOrderStatusWrite`: Automatically advances `Accepted → InKitchen`, dispatches FCM notifications for status changes.
- `dangerDeleteAll`: Maintenance function to purge store data (disabled in production).

> Functions rely on Firestore triggers and Firebase Admin SDK. They must be re-implemented using Supabase SQL/Edge Functions.

## Identified Pain Points
1. **Background Notifications**: FCM implementation depends on notification payloads without data messages, failing to wake the app reliably.
2. **Polling Latency**: WorkManager fallback introduces up to 15 min delay and consumes battery.
3. **Offline Cache Coupling**: Firestore provides transparent persistence currently; replacement needs equivalent caching guarantees.
4. **Role & Topic Management**: Subscription management for chefs/waiters is manual and prone to drift; no central mapping exists.

## Migration Implications
- Any Supabase solution must preserve order status semantics, notification sounds, and deep links into `MainActivity`.
- Existing Firestore rules enforce per-role access; Supabase Row Level Security must model the same constraints.
- Cutover requires dual writes or a downtime window to ensure order history continuity.



