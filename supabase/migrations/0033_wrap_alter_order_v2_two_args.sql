-- Migration: 0033_wrap_alter_order_v2_two_args.sql
-- Purpose: Remove the old 2-arg alter_order_v2 overload and reintroduce it as a
--          thin wrapper that delegates to the safe 4-arg implementation.
-- Rationale: Avoid calling legacy implementation that reused order_items IDs and
--            caused PK collisions; maintain client compatibility with 2-arg RPC.
-- Dependencies: 0032_fix_alter_order_v2_item_ids.sql

BEGIN;

-- Drop the legacy 2-arg overload if it exists
DROP FUNCTION IF EXISTS alter_order_v2(UUID, JSONB);

-- Recreate a wrapper 2-arg version that forwards to the 4-arg implementation
CREATE OR REPLACE FUNCTION alter_order_v2(
    p_order_id UUID,
    p_items JSONB
) RETURNS TABLE(new_order_id UUID, new_order_number INTEGER)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    -- Delegate to the main implementation; actor and chef tip default to NULL
    RETURN QUERY
    SELECT * FROM alter_order_v2(p_order_id, p_items, NULL, NULL);
END;
$$;

-- Ensure execution permission
GRANT EXECUTE ON FUNCTION alter_order_v2(UUID, JSONB) TO authenticated;

COMMIT;


