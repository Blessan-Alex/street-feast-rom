-- Add new column for OneSignal v5 subscription IDs
-- Migration from onesignal_player_id (v4) to onesignal_subscription_id (v5)
-- OneSignal v5 uses subscription IDs (per-device) instead of player IDs (user-level)
set search_path = public;

-- Add new column for OneSignal v5 subscription IDs
alter table users add column if not exists onesignal_subscription_id text;

-- Migrate existing data from old column to new column
update users 
set onesignal_subscription_id = onesignal_player_id 
where onesignal_player_id is not null and onesignal_subscription_id is null;

-- Update register_device function to use new column
create or replace function register_device(player_id text, platform text default 'android')
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    update users
    set onesignal_subscription_id = player_id,  -- Changed to new column for OneSignal v5
        updated_at = now()
    where id = auth.uid();
end;
$$;

comment on column users.onesignal_subscription_id is 
'OneSignal v5 subscription ID (per-device). Previously stored as onesignal_player_id. Android app sends OneSignal.User.pushSubscription.id which is a subscription ID.';


