-- Fix RLS violation: Make orders_log_event trigger function SECURITY DEFINER
-- This allows the trigger to insert into order_events even when RLS is enabled
-- The function will run with the function owner's privileges, bypassing RLS

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


