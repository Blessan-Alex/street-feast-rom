-- Fix typo in orders_log_event trigger function (new_status -> new.status)
-- Migration 0010 was applied with a typo that caused "column new_status does not exist" errors

create or replace function orders_log_event()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
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
$$;

