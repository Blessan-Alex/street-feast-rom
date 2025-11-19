import { supabase } from './supabase';
import { STORAGE_KEYS } from './storage';

export interface User {
  email: string;
  role: 'admin' | 'chef' | 'waiter';
  id?: string; // Add Supabase user ID
}

export const login = async (email: string, password: string): Promise<{ success: boolean; user?: User; error?: string }> => {
  try {
    console.log('[auth] Attempting login for:', email);
    // Sign in with Supabase Auth
    const { data, error } = await supabase.auth.signInWithPassword({
      email,
      password
    });

    if (error) {
      console.error('[auth] Login error:', error);
      return { success: false, error: error.message || 'Invalid email or password' };
    }

    if (!data.user) {
      return { success: false, error: 'Login failed - no user data' };
    }

    console.log('[auth] Supabase auth successful, fetching user role...');

    // Fetch user role from database
    const { data: userData, error: userError } = await supabase
      .from('users')
      .select('role, store_id')
      .eq('id', data.user.id)
      .single();

    if (userError || !userData) {
      console.error('[auth] Failed to fetch user role:', userError);
      // Still allow login, but log the error
    }

    const user: User = {
      email: data.user.email || email,
      role: (userData?.role as 'admin' | 'chef' | 'waiter') || 'admin',
      id: data.user.id
    };

    // Store user info in localStorage for compatibility with existing code
    localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(user));
    
    console.log('[auth] Login successful:', user);
    return { success: true, user };
  } catch (error: any) {
    console.error('[auth] Login exception:', error);
    return { success: false, error: error.message || 'Login failed' };
  }
};

export const logout = async (): Promise<void> => {
  try {
    await supabase.auth.signOut();
    localStorage.removeItem(STORAGE_KEYS.USER);
    console.log('[auth] Logout successful');
  } catch (error) {
    console.error('[auth] Logout error:', error);
    // Still clear localStorage even if signOut fails
    localStorage.removeItem(STORAGE_KEYS.USER);
  }
};

export const isAuthenticated = (): boolean => {
  // Check both localStorage and Supabase session
  const localUser = localStorage.getItem(STORAGE_KEYS.USER);
  if (!localUser) return false;
  
  // Also check if Supabase session exists (async check happens in App.tsx)
  return true;
};

export const getCurrentUser = (): User | null => {
  try {
    const userStr = localStorage.getItem(STORAGE_KEYS.USER);
    return userStr ? JSON.parse(userStr) : null;
  } catch (e) {
    return null;
  }
};

// New function to check Supabase session
export const checkSupabaseSession = async (): Promise<boolean> => {
  try {
    const { data: { session } } = await supabase.auth.getSession();
    return !!session;
  } catch (error) {
    console.error('[auth] Session check error:', error);
    return false;
  }
};

