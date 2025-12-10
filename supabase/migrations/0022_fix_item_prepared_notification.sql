-- Migration: 0022_fix_item_prepared_notification.sql
-- Description: Add error handling and logging to mark_item_prepared RPC for OneSignal notifications
-- Dependencies: 0020

BEGIN;

-- Enhanced function with better error handling for OneSignal notifications
CREATE OR REPLACE FUNCTION mark_item_prepared(
    p_order_id uuid,
    p_item_id uuid,
    p_actor_id uuid default null
) returns table (
    all_prepared boolean,
    order_number int,
    table_number int,
    license_plate text,
    order_type text,
    item_name text
)
language plpgsql
security definer
set search_path = public
as $$
declare
    v_current_user uuid := coalesce(p_actor_id, auth.uid());
    v_store_id uuid;
    v_order_status text;
    v_subscription_ids text[];
    v_onesignal_app_id text := current_setting('app.onesignal_app_id', true);
    v_onesignal_rest_key text := current_setting('app.onesignal_rest_key', true);
    v_project_ref text := current_setting('app.project_ref', true);
    v_request_id bigint;
    v_notification_sent boolean := false;
    v_error_message text;
begin
    -- Set current user for RLS checks
    if v_current_user is not null then
        perform set_config('app.current_user_id', v_current_user::text, true);
    end if;

    -- Fetch order + item context
    select o.store_id, o.status, o.number, o.table_number, o.license_plate, o.type, oi.name
    into v_store_id, v_order_status, order_number, table_number, license_plate, order_type, item_name
    from orders o
    join order_items oi on oi.order_id = o.id
    where o.id = p_order_id
      and oi.id = p_item_id;

    if v_store_id is null then
        raise exception 'Order/item not found';
    end if;

    -- Permission: actor must belong to store
    if v_current_user is not null then
        if not exists (
            select 1 from user_stores
            where user_id = v_current_user
              and store_id = v_store_id
        ) then
            raise exception 'User does not have permission to mark this item prepared';
        end if;
    end if;

    -- Reject terminal orders
    if v_order_status in ('Delivered', 'Closed', 'Canceled') then
        raise exception 'Cannot mark items prepared for status %', v_order_status;
    end if;

    -- Mark the item prepared
    update order_items
    set is_prepared = true,
        updated_at = now()
    where id = p_item_id;

    -- Recompute all_prepared
    select bool_and(is_prepared) into all_prepared
    from order_items
    where order_id = p_order_id;

    -- Notify waiters/admins via OneSignal if configured
    -- Try Edge Function first (recommended), fall back to direct OneSignal
    if v_project_ref is not null and v_project_ref != '' then
        -- Use Edge Function for notifications (more reliable)
        begin
            select net.http_post(
                url := 'https://' || v_project_ref || '.supabase.co/functions/v1/order-events',
                headers := jsonb_build_object(
                    'Content-Type', 'application/json',
                    'Authorization', 'Bearer ' || current_setting('app.service_role_key', true)
                ),
                body := jsonb_build_object(
                    'type', 'ITEM_PREPARED',
                    'orderId', p_order_id,
                    'orderNumber', order_number,
                    'itemId', p_item_id,
                    'itemName', item_name,
                    'storeId', v_store_id,
                    'tableNumber', table_number,
                    'licensePlate', license_plate,
                    'orderType', order_type
                )::text
            ) into v_request_id;
            v_notification_sent := true;
            raise notice 'Item prepared notification sent via Edge Function (request_id: %)', v_request_id;
        exception when others then
            v_error_message := sqlerrm;
            raise notice 'Edge Function notification failed: %', v_error_message;
            -- Fall through to direct OneSignal attempt
        end;
    end if;

    -- Fallback: Direct OneSignal notification if Edge Function failed or not configured
    if not v_notification_sent and v_onesignal_app_id is not null and v_onesignal_rest_key is not null then
        begin
            select array_agg(onesignal_subscription_id)
            into v_subscription_ids
            from users
            where store_id = v_store_id
              and role in ('waiter', 'admin')
              and onesignal_subscription_id is not null;

            if coalesce(array_length(v_subscription_ids, 1), 0) > 0 then
                select net.http_post(
                    url := 'https://onesignal.com/api/v1/notifications',
                    headers := jsonb_build_object(
                        'Content-Type', 'application/json',
                        'Authorization', 'Basic ' || v_onesignal_rest_key
                    ),
                    body := jsonb_build_object(
                        'app_id', v_onesignal_app_id,
                        'include_subscription_ids', v_subscription_ids,
                        'headings', jsonb_build_object('en', format('Item ready for order #%s', order_number)),
                        'contents', jsonb_build_object('en', coalesce(item_name, 'Item') || ' is prepared'),
                        'data', jsonb_build_object(
                            'status', 'ItemPrepared',
                            'orderId', p_order_id,
                            'orderNumber', order_number,
                            'tableNumber', table_number,
                            'licensePlate', license_plate,
                            'orderType', order_type,
                            'itemId', p_item_id
                        ),
                        'android_sound', 'ping'
                    )::text
                ) into v_request_id;
                v_notification_sent := true;
                raise notice 'Item prepared notification sent via OneSignal (request_id: %, subscriptions: %)', v_request_id, array_length(v_subscription_ids, 1);
            else
                raise notice 'No waiter/admin subscription IDs found for store %', v_store_id;
            end if;
        exception when others then
            v_error_message := sqlerrm;
            raise warning 'OneSignal notification failed: %', v_error_message;
            -- Don't fail the transaction, just log the error
        end;
    elsif not v_notification_sent then
        raise notice 'OneSignal not configured (app_id: %, rest_key: %)', 
            case when v_onesignal_app_id is null then 'NULL' else 'SET' end,
            case when v_onesignal_rest_key is null then 'NULL' else 'SET' end;
    end if;

    return next;
end;
$$;

grant execute on function mark_item_prepared(uuid, uuid, uuid) to authenticated;

comment on function mark_item_prepared(uuid, uuid, uuid) is
'Marks a single order item as prepared, computes all_prepared, and notifies waiters/admins via OneSignal (via Edge Function or direct). Requires app.onesignal_app_id and app.onesignal_rest_key settings and pg_net extension.';

COMMIT;

