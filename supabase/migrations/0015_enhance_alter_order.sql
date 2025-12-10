-- Migration: 0015_enhance_alter_order.sql
-- Description: Enhance alter_order RPC to support chef_tip parameter and log to order_events
-- Dependencies: 0001, 0004, 0012, 0014

BEGIN;

-- Enhanced function to alter an existing order by replacing its items
CREATE OR REPLACE FUNCTION alter_order(
    p_order_id UUID,
    p_items JSONB,
    p_actor_id UUID DEFAULT NULL,
    p_chef_tip TEXT DEFAULT NULL
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
    v_old_chef_tip TEXT;
    v_items_count INTEGER;
    v_chef_tip_updated BOOLEAN := false;
    item JSONB;
BEGIN
    -- Set current user for RLS
    IF v_current_user_id IS NOT NULL THEN
        PERFORM set_config('app.current_user_id', v_current_user_id::text, true);
    END IF;

    -- Fetch order to validate it exists and get store_id/status/chef_tip
    SELECT store_id, status, chef_tip INTO v_store_id, v_status, v_old_chef_tip
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

    -- Count items for logging
    v_items_count := jsonb_array_length(COALESCE(p_items, '[]'::jsonb));

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

    -- Update chef_tip if provided
    IF p_chef_tip IS NOT NULL THEN
        v_chef_tip_updated := (p_chef_tip IS DISTINCT FROM v_old_chef_tip);
    END IF;

    -- Recalculate order total from items and update chef_tip if provided
    UPDATE orders
    SET total = (
        SELECT COALESCE(SUM(price * quantity), 0)
        FROM order_items
        WHERE order_id = v_order_id
    ),
    chef_tip = COALESCE(p_chef_tip, chef_tip),
    updated_at = now()
    WHERE id = v_order_id;

    -- Insert event into order_events table
    INSERT INTO order_events (order_id, old_status, new_status, actor_id, metadata)
    VALUES (
        v_order_id,
        v_status,  -- old_status (no change)
        v_status,  -- new_status (no change, order status remains the same)
        v_current_user_id,
        jsonb_build_object(
            'action', 'order_altered',
            'items_count', v_items_count,
            'chef_tip_updated', v_chef_tip_updated
        )
    );

    PERFORM set_config('app.current_user_id', null, true);

    RETURN v_order_id;
END;
$$;

-- Grant execute permissions to authenticated users (updated signature)
GRANT EXECUTE ON FUNCTION alter_order(UUID, JSONB, UUID, TEXT) TO authenticated;

COMMIT;


