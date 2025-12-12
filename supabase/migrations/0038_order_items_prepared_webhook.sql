-- Migration: 0038_order_items_prepared_webhook.sql
-- Description: Add database webhook trigger for order_items.is_prepared changes
-- Fires only when a single item becomes prepared (false -> true)

set check_function_bodies = off;
set search_path = public;

-- Ensure pg_net extension is available
create extension if not exists pg_net;

-- Function to call the order-events Edge Function when an item becomes prepared
create or replace function public.trigger_order_item_prepared_webhook()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_url text := 'https://crtgffoqirnotpazfbdl.supabase.co/functions/v1/order-events';
  v_headers jsonb;
  v_body jsonb;
  v_webhook_secret text;
  v_request_id bigint;
begin
  -- Only when it becomes prepared (avoid noise / extra webhook calls)
  if new.is_prepared = true and coalesce(old.is_prepared, false) = false then

    -- Read secret from Vault (hosted-safe)
    select decrypted_secret
      into v_webhook_secret
    from vault.decrypted_secrets
    where name = 'db_webhook_secret'
    limit 1;

    -- Fail closed if secret missing (prevents insecure public webhook calls)
    if v_webhook_secret is null or v_webhook_secret = '' then
      raise warning 'Vault secret db_webhook_secret missing. Webhook skipped.';
      return new;
    end if;

    v_headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'X-Webhook-Secret', v_webhook_secret
    );

    v_body := jsonb_build_object(
      'type', 'UPDATE',
      'table', 'order_items',
      'record', to_jsonb(new),
      'old_record', to_jsonb(old)
    );

    begin
      select net.http_post(
        url := v_url,
        headers := v_headers,
        body := v_body
      ) into v_request_id;

      raise notice 'Item prepared webhook sent (request_id: %)', v_request_id;
    exception when others then
      raise warning 'Failed to trigger item prepared webhook: %', sqlerrm;
    end;
  end if;

  return new;
end;
$$;

-- Create trigger on order_items table
drop trigger if exists order_items_prepared_webhook_trg on public.order_items;
create trigger order_items_prepared_webhook_trg
after update of is_prepared on public.order_items
for each row
execute function public.trigger_order_item_prepared_webhook();

comment on function public.trigger_order_item_prepared_webhook() is 
'Calls the order-events Edge Function when an order item becomes prepared (is_prepared changes from false to true). Requires db_webhook_secret in Vault. Replace <PROJECT_REF> in the function with your actual Supabase project reference.';

