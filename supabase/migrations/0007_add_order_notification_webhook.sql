-- Database webhook trigger to call order-events Edge Function
-- This uses pg_net extension (available in Supabase) to make HTTP calls
set check_function_bodies = off;
set search_path = public;

-- Enable pg_net extension for HTTP requests
create extension if not exists pg_net;

-- Function to call the order-events Edge Function via HTTP
create or replace function trigger_order_notification()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  edge_function_url text;
  payload jsonb;
  service_role_key text;
  request_id bigint;
  project_ref text;
begin
  -- Get project reference from Supabase URL or config
  -- In Supabase, we can extract it from current_database() or use a config variable
  project_ref := current_setting('app.project_ref', true);
  
  -- If not set, try to get from Supabase settings
  -- For production, you'll need to set this via: ALTER DATABASE postgres SET app.project_ref = 'your-project-ref';
  -- OR use the Supabase Dashboard webhook feature instead
  
  -- Build Edge Function URL
  -- Format: https://[project-ref].supabase.co/functions/v1/order-events
  if project_ref is null or project_ref = '' then
    -- Skip if project ref not configured
    -- This means you should use Supabase Dashboard webhooks instead
    raise warning 'Project ref not configured. Use Supabase Dashboard to set up webhook.';
    return coalesce(new, old);
  end if;
  
  edge_function_url := 'https://' || project_ref || '.supabase.co/functions/v1/order-events';
  
  -- Get service role key (from vault or config)
  -- Note: In production, store this securely in Supabase Vault
  service_role_key := current_setting('app.service_role_key', true);
  
  if service_role_key is null or service_role_key = '' then
    raise warning 'Service role key not configured. Edge Function call skipped.';
    return coalesce(new, old);
  end if;

  -- Build payload matching Edge Function expected format
  payload := jsonb_build_object(
    'type', case when tg_op = 'INSERT' then 'INSERT' else 'UPDATE' end,
    'table', 'orders',
    'record', to_jsonb(coalesce(new, old)),
    'old_record', case when tg_op = 'UPDATE' then to_jsonb(old) else null end
  );

  -- Call Edge Function via HTTP using pg_net
  select net.http_post(
    url := edge_function_url,
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'Authorization', 'Bearer ' || service_role_key
    ),
    body := payload::text
  ) into request_id;

  return coalesce(new, old);
exception
  when others then
    -- Log error but don't fail the order insert/update transaction
    raise warning 'Failed to trigger order notification webhook: %', sqlerrm;
    return coalesce(new, old);
end;
$$;

-- Create trigger on orders table
drop trigger if exists orders_notification_webhook_trg on orders;
create trigger orders_notification_webhook_trg
after insert or update on orders
for each row
execute function trigger_order_notification();

comment on function trigger_order_notification() is 
'Calls the order-events Edge Function when orders are inserted or updated. Requires app.project_ref and app.service_role_key to be set via: ALTER DATABASE postgres SET app.project_ref = ''your-ref''; ALTER DATABASE postgres SET app.service_role_key = ''your-key'';';

-- Note: An alternative (and recommended) approach is to use Supabase Dashboard:
-- 1. Go to Database → Webhooks
-- 2. Create webhook for 'orders' table
-- 3. Events: INSERT, UPDATE  
-- 4. HTTP Request URL: https://[project-ref].supabase.co/functions/v1/order-events
-- 5. HTTP Headers: Authorization: Bearer [SERVICE_ROLE_KEY]
-- 6. HTTP Body: {"type":"{{event_type}}","table":"orders","record":{{record}},"old_record":{{old_record}}}
--
-- The Dashboard webhook is easier to configure and doesn't require database-level settings.
