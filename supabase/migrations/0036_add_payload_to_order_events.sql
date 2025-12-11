-- Migration: 0036_add_payload_to_order_events.sql
-- Purpose: Add payload column required by alter_order_v2 and child-order events.
-- Context: order_events currently lacks payload, but functions insert into it,
--          causing 42703 errors when altering orders.
BEGIN;

ALTER TABLE order_events
ADD COLUMN IF NOT EXISTS payload JSONB;

-- Optional backfill if you want historical metadata copied over:
-- UPDATE order_events SET payload = metadata WHERE payload IS NULL AND metadata IS NOT NULL;

-- Optional composite index for store/event queries:
-- CREATE INDEX IF NOT EXISTS idx_order_events_store_event ON order_events(store_id, event_type);

COMMIT;


