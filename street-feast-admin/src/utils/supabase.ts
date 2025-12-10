import { createClient, SupabaseClient } from '@supabase/supabase-js';

// Get Supabase configuration from environment variables
const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || '';
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY || '';

if (!supabaseUrl || !supabaseAnonKey) {
  console.error('Missing Supabase configuration. Please set VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY');
}

// Create Supabase client instance with realtime configuration
export const supabase: SupabaseClient = createClient(supabaseUrl, supabaseAnonKey, {
  realtime: {
    params: {
      eventsPerSecond: 10
    }
  }
});

// Get current store ID (fetch from stores table - first active store)
export async function getCurrentStoreId(): Promise<string | null> {
  try {
    const { data, error } = await supabase
      .from('stores')
      .select('id')
      .eq('is_active', true)
      .limit(1)
      .single();

    if (error) {
      console.error('Failed to fetch store ID:', error);
      return null;
    }

    return data?.id || null;
  } catch (error) {
    console.error('Error fetching store ID:', error);
    return null;
  }
}

// Helper to get store ID with fallback to env var
export async function getStoreId(): Promise<string> {
  // Try to fetch from database first
  const storeId = await getCurrentStoreId();
  if (storeId) {
    return storeId;
  }

  // Fallback to env var
  const envStoreId = (import.meta as any).env?.VITE_STORE_ID;
  if (envStoreId) {
    return envStoreId;
  }

  // Last resort: throw error
  throw new Error('Store ID not found. Please set up a store in the database or set VITE_STORE_ID environment variable.');
}

// Get occupied tables for a store
export async function getOccupiedTables(storeId: string): Promise<number[]> {
  try {
    const { data, error } = await supabase.rpc('get_occupied_tables', {
      p_store_id: storeId
    });

    if (error) {
      console.error('Failed to fetch occupied tables:', error);
      return [];
    }

    // Map response from [{table_number: 1}, ...] to [1, ...]
    return (data || []).map((row: { table_number: number }) => row.table_number);
  } catch (error) {
    console.error('Error fetching occupied tables:', error);
    return [];
  }
}

