-- Migration: 0024_fix_alter_order_prepared_status.sql
-- Description: Fix alter_order_v2 to handle Prepared status orders by temporarily disabling trigger
-- Dependencies: 0021

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

    -- For 'Prepared' status, we need to bypass the trigger that prevents
    -- Prepared -> Canceled transition. We'll temporarily disable the trigger.
    -- For other statuses, the normal trigger will work fine.
    IF v_status = 'Prepared' THEN
        -- Temporarily disable the status validation trigger
        ALTER TABLE orders DISABLE TRIGGER orders_status_guard_trg;
        
        -- Mark old order as Cancelled (soft cancel)
        UPDATE orders
        SET status = 'Canceled',
            updated_at = now()
        WHERE id = v_old_order_id;
        
        -- Re-enable the trigger
        ALTER TABLE orders ENABLE TRIGGER orders_status_guard_trg;
    ELSE
        -- For other statuses, use normal update (trigger will validate)
        UPDATE orders
        SET status = 'Canceled',
            updated_at = now()
        WHERE id = v_old_order_id;
    END IF;

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
    -- Always generate new UUIDs for items since this is a new order (not updating existing items)
    -- This prevents duplicate key errors when item IDs from the old order are reused
    FOR item IN SELECT value FROM jsonb_array_elements(COALESCE(p_items, '[]'::jsonb))
    LOOP
        INSERT INTO order_items (id, order_id, sku, name, size, veg_flag, quantity, price, modifiers, created_at, updated_at)
        VALUES (
            gen_random_uuid(),  -- Always generate new UUID for new order items, ignore any ID from item JSON
            v_new_order_id,
            item->>'sku',
            COALESCE(item->>'name', 'Unnamed Item'),
            item->>'size',
            item->>'veg_flag',
            COALESCE((item->>'quantity')::INTEGER, 1),
            NULLIF(item->>'price', '')::NUMERIC,
            item->'modifiers',
            now(),  -- Use current timestamp for new items
            now()
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
EXCEPTION
    WHEN OTHERS THEN
        -- Ensure trigger is re-enabled even if there's an error
        ALTER TABLE orders ENABLE TRIGGER orders_status_guard_trg;
        RAISE;
END;
$$;

COMMIT;

