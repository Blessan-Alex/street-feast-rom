-- Migration: 0037_fix_order_events_status.sql
-- Purpose: ensure order_events inserts populate non-null status columns for typed events
-- Context: ORDER_ALTERED / ORDER_ADD_ITEMS inserts previously omitted old_status/new_status,
--          violating NOT NULL on order_events.new_status and causing 23502 errors.
BEGIN;

-- Recreate alter_order_v2 with status fields in ORDER_ALTERED event
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
    IF v_current_user_id IS NOT NULL THEN
        PERFORM set_config('app.current_user_id', v_current_user_id::text, true);
    END IF;

    SELECT store_id, status, type, table_number, license_plate, number
    INTO v_store_id, v_status, v_type, v_table_number, v_license_plate, v_old_order_number
    FROM orders
    WHERE id = v_old_order_id;

    IF v_store_id IS NULL THEN
        RAISE EXCEPTION 'Order % does not exist', v_old_order_id;
    END IF;

    IF v_current_user_id IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM user_stores
            WHERE user_id = v_current_user_id
              AND store_id = v_store_id
        ) THEN
            RAISE EXCEPTION 'User does not have permission to alter this order';
        END IF;
    END IF;

    IF v_status IN ('Delivered', 'Closed', 'Canceled') THEN
        RAISE EXCEPTION 'Cannot alter order with status: %', v_status;
    END IF;

    UPDATE orders
    SET status = 'Canceled',
        updated_at = now()
    WHERE id = v_old_order_id;

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
        updated_at,
        is_edited
    )
    VALUES (
        v_store_id,
        v_status,
        v_type,
        v_table_number,
        v_license_plate,
        COALESCE(p_chef_tip, (SELECT chef_tip FROM orders WHERE id = v_old_order_id)),
        v_current_user_id,
        v_old_order_id,
        0,
        now(),
        now(),
        true
    )
    RETURNING id, number INTO v_new_order_id, v_new_order_number;

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
            COALESCE((item->>'created_at')::TIMESTAMPTZ, now()),
            COALESCE((item->>'updated_at')::TIMESTAMPTZ, now())
        );
    END LOOP;

    UPDATE orders
    SET total = (
        SELECT COALESCE(SUM(price * quantity), 0)
        FROM order_items
        WHERE order_id = v_new_order_id
    )
    WHERE id = v_new_order_id;

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
            'new_order_id', v_new_order_id,
            'new_order_number', v_new_order_number,
            'items_count', jsonb_array_length(COALESCE(p_items, '[]'::jsonb)),
            'table_number', v_table_number,
            'license_plate', v_license_plate
        )
    );

    -- Ensure NOT NULL status columns are populated for typed events
    INSERT INTO order_events (store_id, order_id, event_type, old_status, new_status, payload)
    VALUES (
        v_store_id,
        v_new_order_id,
        'ORDER_ALTERED',
        v_status,
        v_status,
        jsonb_build_object(
            'oldOrderId', v_old_order_id,
            'newOrderId', v_new_order_id,
            'oldOrderNumber', v_old_order_number,
            'newOrderNumber', v_new_order_number,
            'tableNumber', v_table_number,
            'licensePlate', v_license_plate
        )
    );

    PERFORM set_config('app.current_user_id', null, true);
    RETURN QUERY SELECT v_new_order_id, v_new_order_number;
END;
$$;

-- Recreate orders_upsert to populate status in ORDER_ADD_ITEMS events for child orders
CREATE OR REPLACE FUNCTION orders_upsert(
    p_store_id uuid,
    p_order jsonb,
    p_items jsonb default '[]'::jsonb,
    p_actor_id uuid default null
) returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
    v_order_id uuid := coalesce((p_order->>'id')::uuid, gen_random_uuid());
    v_existing_order_id uuid;
    v_total numeric(10,2) := nullif(p_order->>'total', '')::numeric;
    v_created_at timestamptz := coalesce((p_order->>'created_at')::timestamptz, now());
    v_updated_at timestamptz := coalesce((p_order->>'updated_at')::timestamptz, now());
    v_status text := coalesce(p_order->>'status', 'Created');
    v_type text := p_order->>'type';
    v_chef_tip text := p_order->>'chef_tip';
    v_created_by uuid := nullif((p_order->>'created_by')::uuid, null);
    v_parent_order_id uuid := nullif((p_order->>'parent_order_id')::uuid, null);
    v_table_number INTEGER := nullif((p_order->>'table_number')::integer, null);
    v_license_plate TEXT := nullif(p_order->>'license_plate', null);
    item jsonb;
begin
    if p_actor_id is not null then
        perform set_config('app.current_user_id', p_actor_id::text, true);
        if v_created_by is null then
            v_created_by := p_actor_id;
        end if;
    elsif auth.uid() is not null then
        perform set_config('app.current_user_id', auth.uid()::text, true);
        if v_created_by is null then
            v_created_by := auth.uid();
        end if;
    end if;

    select id into v_existing_order_id
    from orders
    where id = v_order_id;

    if v_type = 'DineIn' 
       and v_table_number is not null 
       and v_existing_order_id is null 
       and v_parent_order_id is null then
        if not check_table_availability(v_table_number, p_store_id) then
            raise exception 'Table % is already occupied', v_table_number;
        end if;
    end if;

    insert into orders (id, store_id, number, status, type, chef_tip, created_by, parent_order_id, customer, total, source, notes, table_number, license_plate, created_at, updated_at)
    values (
        v_order_id,
        p_store_id,
        nullif(p_order->>'number', '')::integer,
        v_status,
        v_type,
        v_chef_tip,
        v_created_by,
        v_parent_order_id,
        p_order->'customer',
        v_total,
        p_order->>'source',
        p_order->>'notes',
        v_table_number,
        v_license_plate,
        v_created_at,
        v_updated_at
    )
    on conflict (id) do update set
        status = excluded.status,
        type = excluded.type,
        chef_tip = excluded.chef_tip,
        parent_order_id = excluded.parent_order_id,
        customer = excluded.customer,
        total = excluded.total,
        source = excluded.source,
        notes = excluded.notes,
        table_number = excluded.table_number,
        license_plate = excluded.license_plate,
        updated_at = excluded.updated_at,
        store_id = excluded.store_id
    returning id into v_order_id;

    delete from order_items where order_id = v_order_id;

    for item in select * from jsonb_array_elements(coalesce(p_items, '[]'::jsonb)) loop
        insert into order_items (id, order_id, sku, name, size, veg_flag, quantity, price, modifiers, created_at, updated_at)
        values (
            coalesce((item->>'id')::uuid, gen_random_uuid()),
            v_order_id,
            item->>'sku',
            coalesce(item->>'name', 'Unnamed Item'),
            item->>'size',
            item->>'veg_flag',
            coalesce((item->>'quantity')::integer, 1),
            nullif(item->>'price', '')::numeric,
            item->'modifiers',
            coalesce((item->>'created_at')::timestamptz, now()),
            coalesce((item->>'updated_at')::timestamptz, now())
        );
    end loop;

    perform set_config('app.current_user_id', null, true);

    -- Emit ORDER_ADD_ITEMS event when creating a child order (add-on)
    if v_parent_order_id is not null then
        insert into order_events (store_id, order_id, event_type, old_status, new_status, payload)
        values (
            p_store_id,
            v_order_id,
            'ORDER_ADD_ITEMS',
            v_status,
            v_status,
            jsonb_build_object(
                'baseOrderId', v_parent_order_id,
                'childOrderId', v_order_id,
                'tableNumber', v_table_number,
                'licensePlate', v_license_plate,
                'itemsAddedCount', jsonb_array_length(coalesce(p_items, '[]'::jsonb))
            )
        );
    end if;

    return v_order_id;
end;
$$;

COMMIT;


