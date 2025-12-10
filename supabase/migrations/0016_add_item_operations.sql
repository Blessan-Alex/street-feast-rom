-- Migration: 0016_add_item_operations.sql
-- Description: Add RPC functions for updating and deleting individual order items
-- Dependencies: 0001, 0004, 0012, 0014

BEGIN;

-- Function to update an individual order item
CREATE OR REPLACE FUNCTION update_order_item(
    p_item_id UUID,
    p_quantity INTEGER DEFAULT NULL,
    p_size TEXT DEFAULT NULL,
    p_chef_tip TEXT DEFAULT NULL,
    p_actor_id UUID DEFAULT NULL
) RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_order_id UUID;
    v_order_status TEXT;
    v_store_id UUID;
    v_current_user_id UUID := COALESCE(p_actor_id, auth.uid());
BEGIN
    -- Set current user for RLS
    IF v_current_user_id IS NOT NULL THEN
        PERFORM set_config('app.current_user_id', v_current_user_id::text, true);
    END IF;

    -- Fetch order_id and status from order_items
    SELECT oi.order_id, o.status, o.store_id
    INTO v_order_id, v_order_status, v_store_id
    FROM order_items oi
    JOIN orders o ON o.id = oi.order_id
    WHERE oi.id = p_item_id;

    IF v_order_id IS NULL THEN
        RAISE EXCEPTION 'Order item with ID % not found', p_item_id;
    END IF;

    -- Validate actor has permission (same store)
    IF v_current_user_id IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM user_stores
            WHERE user_id = v_current_user_id
            AND store_id = v_store_id
        ) THEN
            RAISE EXCEPTION 'User does not have permission to modify this order item';
        END IF;
    END IF;

    -- Validate order status allows modification
    IF v_order_status IN ('Delivered', 'Closed', 'Canceled') THEN
        RAISE EXCEPTION 'Cannot modify item in order with status: %', v_order_status;
    END IF;

    -- Update order_items with provided values (only update non-null parameters)
    UPDATE order_items
    SET 
        quantity = COALESCE(p_quantity, quantity),
        size = COALESCE(p_size, size),
        modifiers = CASE 
            WHEN p_chef_tip IS NOT NULL THEN 
                jsonb_set(COALESCE(modifiers, '{}'::jsonb), '{chefTip}', to_jsonb(p_chef_tip))
            ELSE modifiers
        END,
        updated_at = now()
    WHERE id = p_item_id;

    -- Recalculate order total
    UPDATE orders
    SET total = (
        SELECT COALESCE(SUM(price * quantity), 0)
        FROM order_items
        WHERE order_id = v_order_id
    ),
    updated_at = now()
    WHERE id = v_order_id;

    -- Insert event into order_events table
    INSERT INTO order_events (order_id, old_status, new_status, actor_id, metadata)
    VALUES (
        v_order_id,
        v_order_status,  -- old_status (no change)
        v_order_status,  -- new_status (no change)
        v_current_user_id,
        jsonb_build_object(
            'action', 'item_updated',
            'item_id', p_item_id,
            'quantity_changed', p_quantity IS NOT NULL,
            'size_changed', p_size IS NOT NULL,
            'chef_tip_changed', p_chef_tip IS NOT NULL
        )
    );

    PERFORM set_config('app.current_user_id', null, true);
END;
$$;

-- Function to delete an individual order item
CREATE OR REPLACE FUNCTION delete_order_item(
    p_item_id UUID,
    p_actor_id UUID DEFAULT NULL
) RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_order_id UUID;
    v_order_status TEXT;
    v_store_id UUID;
    v_current_user_id UUID := COALESCE(p_actor_id, auth.uid());
    v_item_count INTEGER;
BEGIN
    -- Set current user for RLS
    IF v_current_user_id IS NOT NULL THEN
        PERFORM set_config('app.current_user_id', v_current_user_id::text, true);
    END IF;

    -- Fetch order_id and status from order_items
    SELECT oi.order_id, o.status, o.store_id
    INTO v_order_id, v_order_status, v_store_id
    FROM order_items oi
    JOIN orders o ON o.id = oi.order_id
    WHERE oi.order_id = (SELECT order_id FROM order_items WHERE id = p_item_id);

    IF v_order_id IS NULL THEN
        RAISE EXCEPTION 'Order item with ID % not found', p_item_id;
    END IF;

    -- Validate actor has permission (same store)
    IF v_current_user_id IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM user_stores
            WHERE user_id = v_current_user_id
            AND store_id = v_store_id
        ) THEN
            RAISE EXCEPTION 'User does not have permission to delete this order item';
        END IF;
    END IF;

    -- Validate order status allows deletion
    IF v_order_status IN ('Delivered', 'Closed', 'Canceled') THEN
        RAISE EXCEPTION 'Cannot delete item from order with status: %', v_order_status;
    END IF;

    -- Count remaining items after deletion
    SELECT COUNT(*) INTO v_item_count
    FROM order_items
    WHERE order_id = v_order_id AND id != p_item_id;

    -- Prevent deleting the last item
    IF v_item_count = 0 THEN
        RAISE EXCEPTION 'Cannot delete the last item in an order';
    END IF;

    -- Delete the order item
    DELETE FROM order_items WHERE id = p_item_id;

    -- Recalculate order total
    UPDATE orders
    SET total = (
        SELECT COALESCE(SUM(price * quantity), 0)
        FROM order_items
        WHERE order_id = v_order_id
    ),
    updated_at = now()
    WHERE id = v_order_id;

    -- Insert event into order_events table
    INSERT INTO order_events (order_id, old_status, new_status, actor_id, metadata)
    VALUES (
        v_order_id,
        v_order_status,  -- old_status (no change)
        v_order_status,  -- new_status (no change)
        v_current_user_id,
        jsonb_build_object(
            'action', 'item_deleted',
            'item_id', p_item_id
        )
    );

    PERFORM set_config('app.current_user_id', null, true);
END;
$$;

-- Grant execute permissions to authenticated users
GRANT EXECUTE ON FUNCTION update_order_item(UUID, INTEGER, TEXT, TEXT, UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION delete_order_item(UUID, UUID) TO authenticated;

COMMIT;


