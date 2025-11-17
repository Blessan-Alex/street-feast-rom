-- Enable realtime replication for orders table
-- This fixes "Unable to subscribe to changes" error in Android app
-- The error occurs when trying to subscribe to public.orders with store_id filters
set search_path = public;

-- Enable the orders table for realtime replication
-- supabase_realtime publication is created by Supabase automatically
alter publication supabase_realtime add table public.orders;

comment on table orders is 
'Orders table with realtime replication enabled. Subscriptions with store_id filters are supported.';


