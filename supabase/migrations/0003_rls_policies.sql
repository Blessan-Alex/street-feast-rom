-- Row Level Security policies and helper functions.
set search_path = public;

create or replace function app_user_store_id()
returns uuid
language sql
stable
security definer
set search_path = public
as $$
    select store_id from users where id = auth.uid();
$$;

create or replace function app_user_role()
returns text
language sql
stable
security definer
set search_path = public
as $$
    select role from users where id = auth.uid();
$$;

create or replace function app_is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select coalesce(app_user_role(), '') = 'admin';
$$;

alter table stores enable row level security;
alter table users enable row level security;
alter table orders enable row level security;
alter table order_items enable row level security;
alter table order_events enable row level security;

-- Stores policies
drop policy if exists stores_select_same_store on stores;
create policy stores_select_same_store
on stores
for select
using (
    app_is_admin() or id = app_user_store_id()
);

-- Users policies
drop policy if exists users_select_self on users;
create policy users_select_self
on users
for select
using (id = auth.uid() or app_is_admin());

drop policy if exists users_update_self on users;
create policy users_update_self
on users
for update
using (id = auth.uid() or app_is_admin())
with check (id = auth.uid() or app_is_admin());

-- Orders policies
drop policy if exists orders_select_same_store on orders;
create policy orders_select_same_store
on orders
for select
using (
    app_is_admin() or store_id = app_user_store_id()
);

drop policy if exists orders_update_allowed on orders;
create policy orders_update_allowed
on orders
for update
using (
    app_is_admin() or store_id = app_user_store_id()
)
with check (
    app_is_admin() or store_id = app_user_store_id()
);

drop policy if exists orders_insert_service_role on orders;
create policy orders_insert_service_role
on orders
for insert
with check (auth.role() = 'service_role');

-- Order items policies
drop policy if exists order_items_select_same_store on order_items;
create policy order_items_select_same_store
on order_items
for select
using (
    app_is_admin() or order_id in (
        select id from orders where store_id = app_user_store_id()
    )
);

drop policy if exists order_items_mutate_service_role on order_items;
create policy order_items_mutate_service_role
on order_items
for all
using (auth.role() = 'service_role')
with check (auth.role() = 'service_role');

-- Order events policies
drop policy if exists order_events_select_same_store on order_events;
create policy order_events_select_same_store
on order_events
for select
using (
    app_is_admin() or order_id in (
        select id from orders where store_id = app_user_store_id()
    )
);




