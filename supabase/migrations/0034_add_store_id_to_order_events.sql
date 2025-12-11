-- Migration: 0034_add_store_id_to_order_events.sql
-- Purpose: Add store_id to order_events to support alter_order_v2 and notifications.
-- Dependencies: ensure orders table has store_id.

BEGIN;

-- Add store_id column if missing
ALTER TABLE order_events
ADD COLUMN IF NOT EXISTS store_id UUID;

-- Backfill store_id from orders where possible
UPDATE order_events e
SET store_id = o.store_id
FROM orders o
WHERE e.store_id IS NULL
  AND e.order_id IS NOT NULL
  AND e.order_id = o.id;

-- Optional: index for faster lookups
CREATE INDEX IF NOT EXISTS idx_order_events_store_id ON order_events(store_id);

-- (Optional FK; skip if you don't want to enforce)
-- ALTER TABLE order_events
--   ADD CONSTRAINT order_events_store_id_fkey
--   FOREIGN KEY (store_id) REFERENCES stores(id);

COMMIT;

