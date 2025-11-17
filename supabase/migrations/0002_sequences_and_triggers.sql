-- Sequences, triggers, and helper functions for orders domain.
set search_path = public;

create table if not exists order_number_counters (
    store_id uuid primary key references stores (id) on delete cascade,
    current_number integer not null default 0,
    updated_at timestamptz not null default now()
);

create or replace function orders_assign_number()
returns trigger as $$
declare
    new_number integer;
begin
    if new.number is not null then
        return new;
    end if;

    update order_number_counters
    set current_number = current_number + 1,
        updated_at = now()
    where store_id = new.store_id
    returning current_number into new_number;

    if not found then
        insert into order_number_counters (store_id, current_number)
        values (new.store_id, 1)
        returning current_number into new_number;
    end if;

    new.number := new_number;
    return new;
end;
$$ language plpgsql;

create or replace function touch_updated_at()
returns trigger as $$
begin
    new.updated_at := now();
    return new;
end;
$$ language plpgsql;

create or replace function order_items_touch_updated_at()
returns trigger as $$
begin
    new.updated_at := now();
    return new;
end;
$$ language plpgsql;

create or replace function orders_status_guard()
returns trigger as $$
declare
    allowed boolean := false;
begin
    if tg_op = 'INSERT' then
        return new;
    end if;

    if new.status = old.status then
        return new;
    end if;

    case old.status
        when 'Created' then
            allowed := new.status in ('Accepted', 'InKitchen', 'Prepared', 'Delivered', 'Canceled');
        when 'Accepted' then
            allowed := new.status in ('InKitchen', 'Prepared', 'Delivered', 'Canceled');
        when 'InKitchen' then
            allowed := new.status in ('Prepared', 'Delivered', 'Canceled');
        when 'Prepared' then
            allowed := new.status in ('Delivered');
        when 'Delivered' then
            allowed := false;
        when 'Canceled' then
            allowed := false;
        else
            allowed := false;
    end case;

    if not allowed then
        raise exception 'Illegal order status transition from % to %', old.status, new.status
            using errcode = 'P0001';
    end if;

    return new;
end;
$$ language plpgsql;

create or replace function orders_log_event()
returns trigger as $$
declare
    actor uuid;
begin
    begin
        actor := current_setting('app.current_user_id', true)::uuid;
    exception when others then
        actor := null;
    end;

    if tg_op = 'INSERT' then
        insert into order_events (order_id, old_status, new_status, actor_id, metadata)
        values (new.id, null, new.status, actor, jsonb_build_object('source', 'insert'));
        return new;
    end if;

    if new.status is distinct from old.status then
        insert into order_events (order_id, old_status, new_status, actor_id, metadata)
        values (new.id, old.status, new.status, actor, jsonb_build_object('source', 'update'));
    end if;

    return new;
end;
$$ language plpgsql;

drop trigger if exists orders_assign_number_trg on orders;
create trigger orders_assign_number_trg
before insert on orders
for each row execute function orders_assign_number();

drop trigger if exists orders_touch_updated_at_trg on orders;
create trigger orders_touch_updated_at_trg
before update on orders
for each row execute function touch_updated_at();

drop trigger if exists orders_status_guard_trg on orders;
create trigger orders_status_guard_trg
before update on orders
for each row execute function orders_status_guard();

drop trigger if exists orders_log_event_trg on orders;
create trigger orders_log_event_trg
after insert or update on orders
for each row execute function orders_log_event();

drop trigger if exists order_items_touch_updated_at_trg on order_items;
create trigger order_items_touch_updated_at_trg
before update on order_items
for each row execute function order_items_touch_updated_at();


