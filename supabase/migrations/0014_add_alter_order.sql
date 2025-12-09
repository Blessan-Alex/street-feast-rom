-- Migration: 0014_add_alter_order.sql
-- Description: Create alter_order RPC function to allow modifying order items
-- Dependencies: 0001, 0004, 0012

BEGIN;

-- Function to alter an existing order by replacing its items
CREATE OR REPLACE FUNCTION alter_order(
    p_order_id UUID,
    p_items JSONB,
    p_actor_id UUID DEFAULT NULL
) RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_order_id UUID := p_order_id;
    v_store_id UUID;
    v_status TEXT;
    v_current_user_id UUID := COALESCE(p_actor_id, auth.uid());
    item JSONB;
BEGIN
    -- Set current user for RLS
    IF v_current_user_id IS NOT NULL THEN
        PERFORM set_config('app.current_user_id', v_current_user_id::text, true);
    END IF;

    -- Fetch order to validate it exists and get store_id/status
    SELECT store_id, status INTO v_store_id, v_status
    FROM orders
    WHERE id = v_order_id;

    IF v_store_id IS NULL THEN
        RAISE EXCEPTION 'Order % does not exist', v_order_id;
    END IF;

    -- Validate actor has permission (same store)
    IF v_current_user_id IS NOT NULL THEN
        -- Check if user belongs to the same store
        IF NOT EXISTS (
            SELECT 1 FROM user_stores
            WHERE user_id = v_current_user_id
            AND store_id = v_store_id
        ) THEN
            RAISE EXCEPTION 'User does not have permission to alter this order';
        END IF;
    END IF;

    -- Validate order status allows alteration
    -- Cannot alter orders that are Delivered, Closed, or Canceled
    IF v_status IN ('Delivered', 'Closed', 'Canceled') THEN
        RAISE EXCEPTION 'Cannot alter order with status: %', v_status;
    END IF;

    -- Delete existing order_items for the order
    DELETE FROM order_items WHERE order_id = v_order_id;

    -- Insert new items from p_items JSONB
    FOR item IN SELECT value FROM jsonb_array_elements(COALESCE(p_items, '[]'::jsonb))
    LOOP
        INSERT INTO order_items (id, order_id, sku, name, size, veg_flag, quantity, price, modifiers, created_at, updated_at)
        VALUES (
            COALESCE((item->>'id')::UUID, gen_random_uuid()),
            v_order_id,
            item->>'sku',
            COALESCE(item->>'name', 'Unnamed Item'),
            item->>'size',
            item->>'veg_flag',
            COALESCE((item->>'quantity')::INTEGER, 1),
            NULLIF(item->>'price', '')::NUMERIC,
            item->'modifiers',
            COALESCE((item->>'created_at')::TIMESTAMPTZ, now()),
            COALESCE((item->>'updated_at')::TIMESTAMPTZ, now())
        );
    END LOOP;

    -- Recalculate order total from items
    UPDATE orders
    SET total = (
        SELECT COALESCE(SUM(price * quantity), 0)
        FROM order_items
        WHERE order_id = v_order_id
    ),
    updated_at = now()
    WHERE id = v_order_id;

    PERFORM set_config('app.current_user_id', null, true);

    RETURN v_order_id;
END;
$$;

-- Grant execute permissions to authenticated users
GRANT EXECUTE ON FUNCTION alter_order(UUID, JSONB, UUID) TO authenticated;

COMMIT;


