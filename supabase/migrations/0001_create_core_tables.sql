-- Core table definitions for Supabase migration.
set check_function_bodies = off;
set search_path = public;

create extension if not exists "pgcrypto";

create table if not exists stores (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    timezone text not null default 'UTC',
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists users (
    id uuid primary key,
    email text not null unique,
    display_name text,
    role text not null check (role in ('admin', 'chef', 'waiter')),
    store_id uuid references stores (id) on delete cascade,
    onesignal_player_id text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists orders (
    id uuid primary key default gen_random_uuid(),
    store_id uuid not null references stores (id) on delete cascade,
    number integer,
    status text not null default 'Created',
    customer jsonb,
    total numeric(10,2),
    source text,
    notes text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists orders_store_status_idx on orders (store_id, status);

create table if not exists order_items (
    id uuid primary key default gen_random_uuid(),
    order_id uuid not null references orders (id) on delete cascade,
    sku text,
    name text not null,
    quantity integer not null default 1 check (quantity > 0),
    price numeric(10,2),
    modifiers jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists order_items_order_idx on order_items (order_id);

create table if not exists order_events (
    id uuid primary key default gen_random_uuid(),
    order_id uuid not null references orders (id) on delete cascade,
    old_status text,
    new_status text not null,
    actor_id uuid references users (id),
    metadata jsonb,
    created_at timestamptz not null default now()
);

create index if not exists order_events_order_idx on order_events (order_id, created_at desc);




