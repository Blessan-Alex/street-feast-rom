-- Migration: 0013_create_menu_tables.sql
-- Description: Create menu tables (categories, items, frequent_items) with RLS and realtime
-- Dependencies: 0001, 0003

set check_function_bodies = off;
set search_path = public;

BEGIN;

-- Step 1: Create categories table
CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(store_id, name)
);

-- Step 2: Create items table
CREATE TABLE IF NOT EXISTS items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    sizes JSONB, -- Array: ["Small", "Large"]
    veg_flag TEXT, -- "Veg", "NonVeg", or "Both"
    flavors JSONB, -- Array: ["Spicy", "Sweet"] or null
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Step 3: Create frequent_items table
CREATE TABLE IF NOT EXISTS frequent_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    item_id UUID NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    order_index INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(store_id, item_id)
);

-- Step 4: Create indexes
CREATE INDEX IF NOT EXISTS idx_items_category ON items(category_id);
CREATE INDEX IF NOT EXISTS idx_items_store ON items(store_id);
CREATE INDEX IF NOT EXISTS idx_frequent_items_store ON frequent_items(store_id);
CREATE INDEX IF NOT EXISTS idx_frequent_items_order ON frequent_items(store_id, order_index);
CREATE INDEX IF NOT EXISTS idx_categories_store ON categories(store_id);

-- Step 5: Enable RLS
ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE items ENABLE ROW LEVEL SECURITY;
ALTER TABLE frequent_items ENABLE ROW LEVEL SECURITY;

-- Step 6: RLS Policies for categories
-- Admin: full access
DROP POLICY IF EXISTS categories_admin_all ON categories;
CREATE POLICY categories_admin_all
ON categories
FOR ALL
USING (app_is_admin())
WITH CHECK (app_is_admin());

-- Waiter/Chef: SELECT only (same store)
DROP POLICY IF EXISTS categories_select_same_store ON categories;
CREATE POLICY categories_select_same_store
ON categories
FOR SELECT
USING (
    store_id = app_user_store_id()
);

-- Step 7: RLS Policies for items
-- Admin: full access
DROP POLICY IF EXISTS items_admin_all ON items;
CREATE POLICY items_admin_all
ON items
FOR ALL
USING (app_is_admin())
WITH CHECK (app_is_admin());

-- Waiter/Chef: SELECT only (same store)
DROP POLICY IF EXISTS items_select_same_store ON items;
CREATE POLICY items_select_same_store
ON items
FOR SELECT
USING (
    store_id = app_user_store_id()
);

-- Step 8: RLS Policies for frequent_items
-- Admin: full access
DROP POLICY IF EXISTS frequent_items_admin_all ON frequent_items;
CREATE POLICY frequent_items_admin_all
ON frequent_items
FOR ALL
USING (app_is_admin())
WITH CHECK (app_is_admin());

-- Waiter/Chef: SELECT only (same store)
DROP POLICY IF EXISTS frequent_items_select_same_store ON frequent_items;
CREATE POLICY frequent_items_select_same_store
ON frequent_items
FOR SELECT
USING (
    store_id = app_user_store_id()
);

-- Step 9: Enable realtime replication
ALTER PUBLICATION supabase_realtime ADD TABLE categories;
ALTER PUBLICATION supabase_realtime ADD TABLE items;
ALTER PUBLICATION supabase_realtime ADD TABLE frequent_items;

-- Step 10: Create RPC function: menu_upsert
CREATE OR REPLACE FUNCTION menu_upsert(
    p_store_id UUID,
    p_categories JSONB DEFAULT '[]'::jsonb,
    p_items JSONB DEFAULT '[]'::jsonb,
    p_frequent_items JSONB DEFAULT '[]'::jsonb
) RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    category_item JSONB;
    item_data JSONB;
    frequent_item JSONB;
BEGIN
    -- Upsert categories
    FOR category_item IN SELECT * FROM jsonb_array_elements(p_categories) LOOP
        INSERT INTO categories (id, store_id, name, is_active, created_at, updated_at)
        VALUES (
            (category_item->>'id')::uuid,
            p_store_id,
            category_item->>'name',
            COALESCE((category_item->>'isActive')::boolean, true),
            COALESCE((category_item->>'createdAt')::timestamptz, now()),
            COALESCE((category_item->>'updatedAt')::timestamptz, now())
        )
        ON CONFLICT (id) DO UPDATE SET
            name = EXCLUDED.name,
            is_active = EXCLUDED.is_active,
            updated_at = EXCLUDED.updated_at;
    END LOOP;

    -- Upsert items
    FOR item_data IN SELECT * FROM jsonb_array_elements(p_items) LOOP
        INSERT INTO items (id, category_id, store_id, name, sizes, veg_flag, flavors, is_active, created_at, updated_at)
        VALUES (
            (item_data->>'id')::uuid,
            (item_data->>'categoryId')::uuid,
            p_store_id,
            item_data->>'name',
            item_data->'sizes', -- JSONB array
            item_data->>'vegFlag',
            item_data->'flavors', -- JSONB array or null
            COALESCE((item_data->>'isActive')::boolean, true),
            COALESCE((item_data->>'createdAt')::timestamptz, now()),
            COALESCE((item_data->>'updatedAt')::timestamptz, now())
        )
        ON CONFLICT (id) DO UPDATE SET
            category_id = EXCLUDED.category_id,
            name = EXCLUDED.name,
            sizes = EXCLUDED.sizes,
            veg_flag = EXCLUDED.veg_flag,
            flavors = EXCLUDED.flavors,
            is_active = EXCLUDED.is_active,
            updated_at = EXCLUDED.updated_at;
    END LOOP;

    -- Delete old frequent items for this store
    DELETE FROM frequent_items WHERE store_id = p_store_id;

    -- Insert new frequent items
    FOR frequent_item IN SELECT * FROM jsonb_array_elements(p_frequent_items) LOOP
        INSERT INTO frequent_items (id, store_id, item_id, order_index, created_at)
        VALUES (
            gen_random_uuid(),
            p_store_id,
            (frequent_item->>'itemId')::uuid,
            COALESCE((frequent_item->>'orderIndex')::integer, 0),
            now()
        );
    END LOOP;
END;
$$;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION menu_upsert(UUID, JSONB, JSONB, JSONB) TO authenticated;

-- Step 11: Create RPC function: get_menu
CREATE OR REPLACE FUNCTION get_menu(
    p_store_id UUID
) RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    result JSON;
BEGIN
    SELECT json_build_object(
        'categories', (
            SELECT json_agg(
                json_build_object(
                    'id', id,
                    'name', name,
                    'is_active', is_active,
                    'created_at', created_at,
                    'updated_at', updated_at
                )
            )
            FROM categories
            WHERE store_id = p_store_id AND is_active = true
            ORDER BY name
        ),
        'items', (
            SELECT json_agg(
                json_build_object(
                    'id', id,
                    'category_id', category_id,
                    'name', name,
                    'sizes', sizes,
                    'veg_flag', veg_flag,
                    'flavors', flavors,
                    'is_active', is_active,
                    'created_at', created_at,
                    'updated_at', updated_at
                )
            )
            FROM items
            WHERE store_id = p_store_id AND is_active = true
            ORDER BY name
        ),
        'frequent_items', (
            SELECT json_agg(
                json_build_object(
                    'item_id', item_id,
                    'order_index', order_index
                )
            )
            FROM frequent_items
            WHERE store_id = p_store_id
            ORDER BY order_index
        )
    ) INTO result;
    
    RETURN COALESCE(result, json_build_object('categories', '[]'::json, 'items', '[]'::json, 'frequent_items', '[]'::json));
END;
$$;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION get_menu(UUID) TO authenticated;

-- Step 12: Create RPC function: update_frequent_items
CREATE OR REPLACE FUNCTION update_frequent_items(
    p_store_id UUID,
    p_item_ids UUID[]
) RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    item_id UUID;
    idx INTEGER := 0;
BEGIN
    -- Delete old frequent items for this store
    DELETE FROM frequent_items WHERE store_id = p_store_id;

    -- Insert new frequent items with order_index
    FOREACH item_id IN ARRAY p_item_ids LOOP
        INSERT INTO frequent_items (id, store_id, item_id, order_index, created_at)
        VALUES (
            gen_random_uuid(),
            p_store_id,
            item_id,
            idx,
            now()
        );
        idx := idx + 1;
    END LOOP;
END;
$$;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION update_frequent_items(UUID, UUID[]) TO authenticated;

COMMIT;


