-- Migration: 0012_add_table_management.sql
-- Description: Add table_number and license_plate columns with validation
-- Dependencies: 0001, 0004, 0006

set check_function_bodies = off;
set search_path = public;

BEGIN;

-- Step 1: Add columns (nullable)
ALTER TABLE orders
  ADD COLUMN IF NOT EXISTS table_number INTEGER,
  ADD COLUMN IF NOT EXISTS license_plate TEXT;

-- Step 2: Add basic constraints
ALTER TABLE orders
  ADD CONSTRAINT check_table_number_range
    CHECK (table_number IS NULL OR (table_number >= 1 AND table_number <= 7)),
  ADD CONSTRAINT check_license_plate_format
    CHECK (license_plate IS NULL OR (license_plate ~ '^[0-9]{4}$'));

-- Step 2.5: Fix existing data before adding type-based constraint
-- This runs AFTER columns are added but BEFORE the constraint that requires valid data

-- First, migrate 'Delivery' to 'EatAway' (must happen before other fixes)
UPDATE orders
SET type = 'EatAway'
WHERE type = 'Delivery';

-- Fix DineIn orders: Set table_number and ensure license_plate is NULL
UPDATE orders
SET 
    table_number = CASE 
        WHEN number IS NOT NULL THEN ((number - 1) % 7) + 1
        ELSE 1
    END,
    license_plate = NULL
WHERE type = 'DineIn';

-- Fix EatAway orders (including migrated Delivery): Set license_plate and ensure table_number is NULL
UPDATE orders
SET 
    license_plate = CASE 
        WHEN number IS NOT NULL THEN LPAD((number % 10000)::text, 4, '0')
        ELSE '0000'
    END,
    table_number = NULL
WHERE type = 'EatAway';

-- Fix Parcel orders: Ensure both are NULL
UPDATE orders
SET 
    table_number = NULL,
    license_plate = NULL
WHERE type = 'Parcel';

-- Fix NULL type orders: Ensure both are NULL
UPDATE orders
SET 
    table_number = NULL,
    license_plate = NULL
WHERE type IS NULL;

-- Step 3: Add type-based constraints
ALTER TABLE orders
  ADD CONSTRAINT check_order_type_table_license
    CHECK (
      (type = 'DineIn' AND table_number IS NOT NULL AND license_plate IS NULL) OR
      (type = 'EatAway' AND license_plate IS NOT NULL AND table_number IS NULL) OR
      (type = 'Parcel' AND table_number IS NULL AND license_plate IS NULL) OR
      (type IS NULL)  -- Allow NULL type for backward compatibility
    );

-- Step 4: Create helper functions

-- Function to check if a table is available
CREATE OR REPLACE FUNCTION check_table_availability(
  p_table_num INTEGER,
  p_store_id UUID
) RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
BEGIN
  RETURN NOT EXISTS (
    SELECT 1 FROM orders
    WHERE store_id = p_store_id
      AND table_number = p_table_num
      AND status IN ('Created', 'Accepted', 'InKitchen', 'Prepared')
  );
END;
$$;

-- Function to get list of occupied tables
CREATE OR REPLACE FUNCTION get_occupied_tables(
  p_store_id UUID
) RETURNS TABLE(table_number INTEGER)
LANGUAGE plpgsql
AS $$
BEGIN
  RETURN QUERY
  SELECT DISTINCT o.table_number
  FROM orders o
  WHERE o.store_id = p_store_id
    AND o.table_number IS NOT NULL
    AND o.table_number BETWEEN 1 AND 7
    AND o.status IN ('Created', 'Accepted', 'InKitchen', 'Prepared')
  ORDER BY o.table_number;
END;
$$;

-- Grant execute permissions
GRANT EXECUTE ON FUNCTION check_table_availability(INTEGER, UUID) TO authenticated, service_role;
GRANT EXECUTE ON FUNCTION get_occupied_tables(UUID) TO authenticated, service_role;

-- Step 5: Update orders_upsert function to handle table_number and license_plate
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
        -- Use actor_id as created_by if not specified
        if v_created_by is null then
            v_created_by := p_actor_id;
        end if;
    elsif auth.uid() is not null then
        perform set_config('app.current_user_id', auth.uid()::text, true);
        -- Use auth.uid() as created_by if not specified
        if v_created_by is null then
            v_created_by := auth.uid();
        end if;
    end if;

    -- Validate table availability for DineIn orders
    if v_type = 'DineIn' and v_table_number is not null then
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

grant execute on function orders_upsert(uuid, jsonb, jsonb, uuid) to service_role;

-- Step 6: Create index for efficient table queries
CREATE INDEX IF NOT EXISTS idx_orders_table_status 
ON orders(store_id, table_number, status) 
WHERE table_number IS NOT NULL;

-- Step 7: Migrate existing "Delivery" orders to "EatAway"
UPDATE orders 
SET type = 'EatAway' 
WHERE type = 'Delivery';

COMMIT;


