-- Migration: 0029_fix_child_order_table_check.sql
-- Description: Skip table availability check for child orders (parent already occupies table)
-- Dependencies: 0027

BEGIN;

-- Update orders_upsert to skip table availability check for child orders
-- Child orders share the same table as their parent, so no need to check availability
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

    -- Check if order already exists
    SELECT id INTO v_existing_order_id
    FROM orders
    WHERE id = v_order_id;

    -- Only validate table availability for NEW DineIn orders (not updates, not child orders)
    -- Skip check if:
    -- 1. Order already exists (updating)
    -- 2. Order has parent_order_id (child order - parent already occupies table)
    if v_type = 'DineIn' 
       and v_table_number is not null 
       and v_existing_order_id is null 
       and v_parent_order_id is null then  -- NEW: Skip check for child orders
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

    return v_order_id;
end;
$$;

COMMIT;

