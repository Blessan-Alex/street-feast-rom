<!-- Supabase schema and realtime design for Street Feast migration. -->

# Supabase Data & Realtime Design

## Schema Overview
The Supabase/Postgres database replaces nested Firestore documents with normalized tables:

| Table | Key Columns | Description |
|-------|-------------|-------------|
| `stores` | `id UUID PK`, `name`, `timezone`, `is_active` | Master store list. |
| `orders` | `id UUID PK`, `store_id FK`, `number INT`, `status TEXT`, `customer JSONB`, `total NUMERIC`, `created_at TIMESTAMPTZ`, `updated_at TIMESTAMPTZ`, `source TEXT`, `notes TEXT` | Core order records. |
| `order_items` | `id UUID PK`, `order_id FK`, `sku`, `name`, `quantity INT`, `price NUMERIC`, `modifiers JSONB` | Items within each order. |
| `order_events` | `id UUID PK`, `order_id FK`, `old_status`, `new_status`, `actor_id UUID`, `created_at TIMESTAMPTZ` | Audit log for status transitions. |
| `users` | `id UUID PK`, `email`, `role TEXT`, `store_id FK`, `onesignal_player_id TEXT` | Application users linked to OneSignal devices. |
| `kitchen_tickets` *(optional)* | `id UUID PK`, `order_id FK`, `expected_at` | Support for KDS displays. |

> Use generated columns or sequences to maintain `orders.number` per store (e.g., `orders_number_seq_{store}`).

## Row Level Security (RLS)
Enable RLS on all tables. Policies implemented via SQL migrations:

- **Stores**: `SELECT` allowed for authenticated users bound to the same `store_id`.
- **Orders**:
  - `SELECT`: roles `{admin, chef, waiter}` with matching `store_id`.
  - `INSERT`: system service role only (Edge Function `orders_upsert`).
  - `UPDATE`: chefs may update status to `Prepared`, waiters to `Delivered`, admins full control.
- **Order Items**: `SELECT` same store policy; `UPDATE` restricted to admin/service.
- **Order Events**: `INSERT` via trigger only; no direct client writes.

Policies rely on JWT claims: `store_id`, `role`, `sub` (user id).

## Triggers & Functions
1. **`orders_set_defaults()`**: Before insert, sets `number` using per-store sequence and ensures `status = 'Created'`.
2. **`orders_update_timestamp()`**: Before update, refreshes `updated_at`.
3. **`orders_status_guard()`**: Validates legal status transitions (Created → Accepted → InKitchen → Prepared → Delivered; Cancel allowed from `{Created, Accepted, InKitchen}`).
4. **`orders_log_event()`**: After insert/update, append to `order_events`.
5. **`orders_notify()`**: After update, publishes to `realtime.broadcast` channel `orders:{store_id}` with payload containing order id, status, diff.

## Realtime Subscription Strategy
- Clients subscribe to `realtime` channel `orders:{storeId}` using Phoenix channels. Payload includes:
  ```json
  {
    "order_id": "...",
    "status": "Prepared",
    "number": 42,
    "updated_at": "2025-11-12T10:15:40Z"
  }
  ```
- Android foreground updates rely on this channel to replace Firestore snapshot listeners.
- Background notifications handed off to Edge Function (see Notifications spec).

## Edge Functions
Implement in TypeScript (Deno runtime):
1. **`order-status-hook`**:
   - Trigger: Supabase webhook on `orders` update (filtered to status changes).
   - Logic: Fetch order + items; derive OneSignal segment (chef/waiter); call OneSignal REST to send push with data payload.
2. **`order-number-maintenance`**:
   - Exposed RPC to reseed sequences per store (admin use).

## Migration & Seeding
- Store SQL migrations in `[supabase/migrations]` with incremental files:
  1. `0001_create_core_tables.sql`
  2. `0002_sequences_and_triggers.sql`
  3. `0003_rls_policies.sql`
  4. `0004_edge_function_helpers.sql`
- Seed data stored in `[supabase/seed/initial_data.sql]` for staging environments.

## Observability
- Enable [pg_stat_statements](https://supabase.com/docs/guides/database/extensions/pg_stat_statements) to monitor query performance.
- Log Edge Function invocations to Supabase Logs; forward metrics to observability stack (e.g., Logflare → Grafana).

## Open Questions
- Authentication provider: use Supabase Auth or integrate existing IAM solution?
- Long-term data retention: do we archive completed orders (>90 days) to cold storage?




