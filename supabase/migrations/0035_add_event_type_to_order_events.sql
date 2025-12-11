-- Migration: 0035_add_event_type_to_order_events.sql
-- Purpose: Add event_type to order_events to support ORDER_ALTERED / ORDER_ADD_ITEMS inserts.
-- Dependencies: order_events table exists.

BEGIN;

ALTER TABLE order_events
ADD COLUMN IF NOT EXISTS event_type TEXT;

-- Optional backfill from payload->>'type' if desired
-- UPDATE order_events SET event_type = payload->>'type' WHERE event_type IS NULL AND payload ? 'type';

CREATE INDEX IF NOT EXISTS idx_order_events_event_type ON order_events(event_type);

COMMIT;

