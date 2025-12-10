-- Migration: 0028_remove_menu_active_filter.sql
-- Description: Remove is_active filter from get_menu to return all menu items
-- Dependencies: 0013

BEGIN;

-- Update get_menu to return ALL categories and items, not just active ones
-- This ensures waiters see everything in the database
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
                ) ORDER BY name
            )
            FROM categories
            WHERE store_id = p_store_id
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
                ) ORDER BY name
            )
            FROM items
            WHERE store_id = p_store_id
        ),
        'frequent_items', (
            SELECT json_agg(
                json_build_object(
                    'item_id', item_id,
                    'order_index', order_index
                ) ORDER BY order_index
            )
            FROM frequent_items
            WHERE store_id = p_store_id
        )
    ) INTO result;
    
    RETURN COALESCE(result, json_build_object('categories', '[]'::json, 'items', '[]'::json, 'frequent_items', '[]'::json));
END;
$$;

COMMIT;

