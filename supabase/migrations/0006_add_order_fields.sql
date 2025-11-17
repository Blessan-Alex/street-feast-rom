-- Add missing order fields to match Android app expectations
set check_function_bodies = off;
set search_path = public;

-- Add missing columns to orders table
alter table orders
    add column if not exists type text,
    add column if not exists chef_tip text,
    add column if not exists created_by uuid references users(id) on delete set null,
    add column if not exists parent_order_id uuid references orders(id) on delete set null;

-- Add missing columns to order_items table
alter table order_items
    add column if not exists size text,
    add column if not exists veg_flag text;

-- Update orders_upsert function will be updated in 0004_edge_function_helpers.sql

