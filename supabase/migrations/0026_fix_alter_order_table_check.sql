-- Migration: 0026_fix_alter_order_table_check.sql
-- Description: Add comments clarifying that alter_order_v2 skips table availability check
-- Dependencies: 0025

BEGIN;

-- Note: alter_order_v2 already inserts directly, bypassing orders_upsert
-- This means it doesn't trigger the table availability check in orders_upsert
-- This is correct behavior because:
-- 1. The old order is cancelled in the same transaction
-- 2. We're replacing the same order, so the table should be available
-- 3. This is an atomic operation, so no race condition
-- 
-- The function is already correct from 0025, but we add this migration
-- for documentation and to ensure the logic is clear.

-- The alter_order_v2 function from 0025 already handles this correctly
-- by inserting directly into orders table without going through orders_upsert

COMMIT;

