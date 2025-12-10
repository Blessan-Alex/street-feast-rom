-- Check notification configuration for debugging
-- Run this to verify OneSignal and Edge Function settings are configured

-- Check Edge Function configuration (preferred method)
SELECT 
    name,
    setting,
    CASE 
        WHEN setting IS NULL OR setting = '' THEN 'NOT SET'
        ELSE 'SET (' || LEFT(setting, 20) || '...)'
    END as status
FROM pg_settings 
WHERE name IN ('app.project_ref', 'app.service_role_key')
ORDER BY name;

-- Check OneSignal configuration (fallback method)
SELECT 
    name,
    setting,
    CASE 
        WHEN setting IS NULL OR setting = '' THEN 'NOT SET'
        ELSE 'SET (' || LEFT(setting, 20) || '...)'
    END as status
FROM pg_settings 
WHERE name IN ('app.onesignal_app_id', 'app.onesignal_rest_key')
ORDER BY name;

-- Check users with subscription IDs
SELECT 
    u.id,
    u.email,
    u.role,
    u.store_id,
    CASE 
        WHEN u.onesignal_subscription_id IS NULL THEN 'NO SUBSCRIPTION'
        ELSE 'HAS SUBSCRIPTION'
    END as subscription_status,
    s.name as store_name
FROM users u
LEFT JOIN stores s ON u.store_id = s.id
WHERE u.store_id IS NOT NULL
ORDER BY u.store_id, u.role;

-- Summary: Count users with subscription IDs by role and store
SELECT 
    s.name as store_name,
    u.role,
    COUNT(*) as total_users,
    COUNT(u.onesignal_subscription_id) as users_with_subscription,
    COUNT(*) - COUNT(u.onesignal_subscription_id) as users_without_subscription
FROM users u
JOIN stores s ON u.store_id = s.id
GROUP BY s.id, s.name, u.role
ORDER BY s.name, u.role;

