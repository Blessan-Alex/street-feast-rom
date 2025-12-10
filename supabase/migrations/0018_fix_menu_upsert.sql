-- Migration: 0018_fix_menu_upsert.sql
-- Description: Fix menu_upsert to delete all old menu data before inserting new data
-- This prevents 409 conflicts on (store_id, name) unique constraint and ensures clean menu replacement
-- Dependencies: 0013

set check_function_bodies = off;
set search_path = public;

BEGIN;

-- Replace menu_upsert function to delete old menu data before inserting new data
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
    -- Delete ALL old menu data for this store first
    -- Order matters: delete items before categories to avoid foreign key constraint issues
    DELETE FROM frequent_items WHERE store_id = p_store_id;
    DELETE FROM items WHERE store_id = p_store_id;
    DELETE FROM categories WHERE store_id = p_store_id;
    
    -- Insert new categories (no conflict handling needed since we deleted everything)
    FOR category_item IN SELECT * FROM jsonb_array_elements(p_categories) LOOP
        INSERT INTO categories (id, store_id, name, is_active, created_at, updated_at)
        VALUES (
            (category_item->>'id')::uuid,
            p_store_id,
            category_item->>'name',
            COALESCE((category_item->>'isActive')::boolean, true),
            COALESCE((category_item->>'createdAt')::timestamptz, now()),
            COALESCE((category_item->>'updatedAt')::timestamptz, now())
        );
    END LOOP;

    -- Insert new items (no conflict handling needed since we deleted everything)
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
        );
    END LOOP;

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

-- Grant execute permission (already granted, but ensuring it's still there)
GRANT EXECUTE ON FUNCTION menu_upsert(UUID, JSONB, JSONB, JSONB) TO authenticated;

COMMIT;

