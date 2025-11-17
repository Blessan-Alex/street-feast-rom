## Supabase Rollout Validation

### Pre-requisites
- Supabase project with migrations `0001`–`0004` applied.
- Edge Function `order-events` deployed and environment variables (`ONESIGNAL_APP_ID`, `ONESIGNAL_REST_KEY`, `SUPABASE_SERVICE_ROLE_KEY`) configured.
- Android app configured with `local.properties` keys:
  ```
  SUPABASE_URL=https://<project>.supabase.co
  SUPABASE_ANON_KEY=<anon_key>
  ONESIGNAL_APP_ID=<onesignal_app_id>
  ```
- Staging OneSignal app linked to Firebase Sender ID.

### Test Matrix
| Scenario | Steps | Expected |
|----------|-------|----------|
| Login Success | Launch app → login with Supabase credentials | Navigates to role-specific home; OneSignal logs in device |
| Login Failure | Invalid password | Error toast, no navigation |
| Chef New Order | Insert order via SQL or migration script | Appears in Chef "New Orders" tab; push delivered to chef device (foreground/background) |
| Chef Accept Order | Tap Accept | Order moves to Preparing; push for waiter not sent |
| Chef Mark Prepared | Tap Mark Prepared | Order moves to Waiter tab; OneSignal push sent to waiter |
| Waiter Mark Delivered | Tap Mark Delivered | Order removed from Ready tab; no further pushes |
| Offline Cache | Toggle airplane mode → relaunch app | Shows last synced orders; actions blocked with error |
| Realtime Sync | Update order status from second device | UI updates within 2s on first device |
| Device Logout | Logout → login as different role | OneSignal tags updated; correct tabs shown |

### Regression Checklist
- [ ] Notification sounds play on new/prepared events.
- [ ] Deep links open `MainActivity` with correct order loaded.
- [ ] No Firebase classes loaded (logcat clean of `Firebase` warnings).
- [ ] Room database file `streetfeast.db` created.
- [ ] Supabase `order_events` table contains entries for status transitions.
- [ ] Edge Function logs OneSignal responses (success/failure).

### Rollout Steps
1. Run `python tools/migrate_firestore_to_supabase.py --dry-run` to validate mapping.
2. Execute migration without `--dry-run`, monitor Supabase tables.
3. Cut a staging build; QA using test matrix.
4. Enable Supabase-backed build for pilot kitchen (feature flag or config).
5. Monitor Supabase logs, OneSignal delivery dashboards, and Crashlytics for 48 hours.
6. Once stable, tag release and archive Firebase credentials.


