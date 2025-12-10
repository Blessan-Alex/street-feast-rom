-- Migration: 0021_alter_order_v2.sql
-- Description: Create alter_order_v2 RPC that cancels old order and creates new one (atomic operation)
-- Dependencies: 0001, 0002, 0004, 0012, 0015

BEGIN;

-- Function to alter an existing order by cancelling old and creating new
-- This ensures old order disappears from active lists and table occupancy is correct
CREATE OR REPLACE FUNCTION alter_order_v2(
    p_order_id UUID,
    p_items JSONB,
    p_actor_id UUID DEFAULT NULL,
    p_chef_tip TEXT DEFAULT NULL
) RETURNS TABLE(new_order_id UUID, new_order_number INTEGER)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_old_order_id UUID := p_order_id;
    v_store_id UUID;
    v_status TEXT;
    v_type TEXT;
    v_table_number INTEGER;
    v_license_plate TEXT;
    v_old_order_number INTEGER;
    v_current_user_id UUID := COALESCE(p_actor_id, auth.uid());
    v_new_order_id UUID;
    v_new_order_number INTEGER;
    item JSONB;
BEGIN
    -- Set current user for RLS
    IF v_current_user_id IS NOT NULL THEN
        PERFORM set_config('app.current_user_id', v_current_user_id::text, true);
    END IF;

    -- Fetch old order details
    SELECT store_id, status, type, table_number, license_plate, number
    INTO v_store_id, v_status, v_type, v_table_number, v_license_plate, v_old_order_number
    FROM orders
    WHERE id = v_old_order_id;

    IF v_store_id IS NULL THEN
        RAISE EXCEPTION 'Order % does not exist', v_old_order_id;
    END IF;

    -- Validate actor has permission (same store)
    IF v_current_user_id IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM user_stores
            WHERE user_id = v_current_user_id
            AND store_id = v_store_id
        ) THEN
            RAISE EXCEPTION 'User does not have permission to alter this order';
        END IF;
    END IF;

    -- Validate order status allows alteration
    IF v_status IN ('Delivered', 'Closed', 'Canceled') THEN
        RAISE EXCEPTION 'Cannot alter order with status: %', v_status;
    END IF;

    -- Mark old order as Cancelled (soft cancel)
    UPDATE orders
    SET status = 'Canceled',
        updated_at = now()
    WHERE id = v_old_order_id;

    -- Log cancellation event
    INSERT INTO order_events (order_id, old_status, new_status, actor_id, metadata)
    VALUES (
        v_old_order_id,
        v_status,
        'Canceled',
        v_current_user_id,
        jsonb_build_object(
            'action', 'order_cancelled_for_alter',
            'new_order_will_be_created', true
        )
    );

    -- Create new order with same table/license/type
    -- The trigger will assign order number automatically
    INSERT INTO orders (
        store_id,
        status,
        type,
        table_number,
        license_plate,
        chef_tip,
        created_by,
        parent_order_id,
        total,
        created_at,
        updated_at
    )
    VALUES (
        v_store_id,
        v_status,  -- Keep same status (Created, Accepted, etc.)
        v_type,
        v_table_number,
        v_license_plate,
        COALESCE(p_chef_tip, (SELECT chef_tip FROM orders WHERE id = v_old_order_id)),
        v_current_user_id,
        v_old_order_id,  -- Link to old order as parent
        0,  -- Will be calculated from items
        now(),
        now()
    )
    RETURNING id, number INTO v_new_order_id, v_new_order_number;

    -- Insert items for the new order
    FOR item IN SELECT value FROM jsonb_array_elements(COALESCE(p_items, '[]'::jsonb))
    LOOP
        INSERT INTO order_items (id, order_id, sku, name, size, veg_flag, quantity, price, modifiers, created_at, updated_at)
        VALUES (
            COALESCE((item->>'id')::UUID, gen_random_uuid()),
            v_new_order_id,
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
        WHERE order_id = v_new_order_id
    )
    WHERE id = v_new_order_id;

    -- Log alter event for new order
    INSERT INTO order_events (order_id, old_status, new_status, actor_id, metadata)
    VALUES (
        v_new_order_id,
        NULL,  -- New order, no old status
        v_status,
        v_current_user_id,
        jsonb_build_object(
            'action', 'order_created_from_alter',
            'old_order_id', v_old_order_id,
            'old_order_number', v_old_order_number,
            'items_count', jsonb_array_length(COALESCE(p_items, '[]'::jsonb))
        )
    );

    PERFORM set_config('app.current_user_id', null, true);

    -- Return new order details
    RETURN QUERY SELECT v_new_order_id, v_new_order_number;
END;
$$;

-- Grant execute permissions to authenticated users
GRANT EXECUTE ON FUNCTION alter_order_v2(UUID, JSONB, UUID, TEXT) TO authenticated;

-- Ensure get_occupied_tables excludes Cancelled orders (should already be correct, but verify)
-- The function at line 106 in 0012_add_table_management.sql already filters by status
-- It only includes: 'Created', 'Accepted', 'InKitchen', 'Prepared'
-- So Cancelled orders are already excluded - no change needed

COMMIT;

