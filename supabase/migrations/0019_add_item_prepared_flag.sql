-- Phase 3A: Durable per-item prepared state
alter table order_items
    add column if not exists is_prepared boolean not null default false;

-- Backfill legacy rows to false (safe no-op with default)
update order_items set is_prepared = coalesce(is_prepared, false);


