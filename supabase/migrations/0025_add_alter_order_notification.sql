-- Migration: 0025_add_alter_order_notification.sql
-- Description: Add notification to waiters when order is altered via alter_order_v2
-- Dependencies: 0024

BEGIN;

-- Enhanced alter_order_v2 with notification support
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
    v_subscription_ids text[];
    v_onesignal_app_id text := current_setting('app.onesignal_app_id', true);
    v_onesignal_rest_key text := current_setting('app.onesignal_rest_key', true);
    v_project_ref text := current_setting('app.project_ref', true);
    v_service_role_key text := current_setting('app.service_role_key', true);
    v_request_id bigint;
    v_notification_sent boolean := false;
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
    FOR item IN SELECT value FROM jsonb_array_elements(COALESCE(p_items, '[]'::jsonb))
    LOOP
        INSERT INTO order_items (id, order_id, sku, name, size, veg_flag, quantity, price, modifiers, created_at, updated_at)
        VALUES (
            gen_random_uuid(),
            v_new_order_id,
            item->>'sku',
            COALESCE(item->>'name', 'Unnamed Item'),
            item->>'size',
            item->>'veg_flag',
            COALESCE((item->>'quantity')::INTEGER, 1),
            NULLIF(item->>'price', '')::NUMERIC,
            item->'modifiers',
            now(),
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
        NULL,
        v_status,
        v_current_user_id,
        jsonb_build_object(
            'action', 'order_created_from_alter',
            'old_order_id', v_old_order_id,
            'old_order_number', v_old_order_number,
            'items_count', jsonb_array_length(COALESCE(p_items, '[]'::jsonb))
        )
    );

    -- Send notification to waiters/admins about order alteration
    -- Try Edge Function first (recommended)
    IF v_project_ref IS NOT NULL AND v_project_ref != '' AND v_service_role_key IS NOT NULL AND v_service_role_key != '' THEN
        BEGIN
            SELECT net.http_post(
                url := 'https://' || v_project_ref || '.supabase.co/functions/v1/order-events',
                headers := jsonb_build_object(
                    'Content-Type', 'application/json',
                    'Authorization', 'Bearer ' || v_service_role_key
                ),
                body := jsonb_build_object(
                    'type', 'ORDER_ALTERED',
                    'orderId', v_new_order_id,
                    'orderNumber', v_new_order_number,
                    'storeId', v_store_id,
                    'status', v_status,
                    'orderType', v_type,
                    'tableNumber', v_table_number,
                    'licensePlate', v_license_plate
                )::text
            ) INTO v_request_id;
            v_notification_sent := true;
            RAISE NOTICE 'Order alter notification sent via Edge Function (request_id: %)', v_request_id;
        EXCEPTION WHEN OTHERS THEN
            RAISE NOTICE 'Edge Function notification failed: %', sqlerrm;
        END;
    END IF;

    -- Fallback: Direct OneSignal notification if Edge Function failed or not configured
    IF NOT v_notification_sent AND v_onesignal_app_id IS NOT NULL AND v_onesignal_rest_key IS NOT NULL THEN
        BEGIN
            SELECT array_agg(onesignal_subscription_id)
            INTO v_subscription_ids
            FROM users
            WHERE store_id = v_store_id
              AND role IN ('waiter', 'admin')
              AND onesignal_subscription_id IS NOT NULL;

            IF COALESCE(array_length(v_subscription_ids, 1), 0) > 0 THEN
                SELECT net.http_post(
                    url := 'https://onesignal.com/api/v1/notifications',
                    headers := jsonb_build_object(
                        'Content-Type', 'application/json',
                        'Authorization', 'Basic ' || v_onesignal_rest_key
                    ),
                    body := jsonb_build_object(
                        'app_id', v_onesignal_app_id,
                        'include_subscription_ids', v_subscription_ids,
                        'headings', jsonb_build_object('en', format('Order #%s has been updated', v_new_order_number)),
                        'contents', jsonb_build_object('en', format('Order #%s has been modified. Please refresh to see changes.', v_new_order_number)),
                        'data', jsonb_build_object(
                            'status', 'OrderAltered',
                            'orderId', v_new_order_id,
                            'orderNumber', v_new_order_number,
                            'tableNumber', v_table_number,
                            'licensePlate', v_license_plate,
                            'orderType', v_type
                        ),
                        'android_sound', 'ping'
                    )::text
                ) INTO v_request_id;
                RAISE NOTICE 'Order alter notification sent via OneSignal (request_id: %, subscriptions: %)', v_request_id, array_length(v_subscription_ids, 1);
            END IF;
        EXCEPTION WHEN OTHERS THEN
            RAISE WARNING 'OneSignal notification failed: %', sqlerrm;
        END;
    END IF;

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

