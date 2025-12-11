-- Migration: 0023_create_user_stores.sql
-- Description: Create user_stores table for permission checks
-- Dependencies: 0001

BEGIN;

-- Create user_stores junction table
CREATE TABLE IF NOT EXISTS user_stores (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, store_id)
);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS user_stores_user_idx ON user_stores(user_id);
CREATE INDEX IF NOT EXISTS user_stores_store_idx ON user_stores(store_id);

-- Populate user_stores from existing users table
INSERT INTO user_stores (user_id, store_id)
SELECT id, store_id
FROM users
WHERE store_id IS NOT NULL
ON CONFLICT (user_id, store_id) DO NOTHING;

-- Enable RLS
ALTER TABLE user_stores ENABLE ROW LEVEL SECURITY;

-- RLS Policy: Users can see their own store associations
CREATE POLICY "Users can view their own store associations"
    ON user_stores
    FOR SELECT
    USING (auth.uid() = user_id);

COMMIT;



