import { STORAGE_KEYS } from './storage';

export interface User {
  email: string;
  role: 'admin' | 'chef' | 'waiter';
}

export const login = (email: string, password: string): { success: boolean; user?: User; error?: string } => {
  // Mock authentication
  if (email === 'admin@test.com' && password === 'password123') {
    const user: User = { email, role: 'admin' };
    localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(user));
    return { success: true, user };
  }
  return { success: false, error: 'Invalid email or password' };
};

export const logout = (): void => {
  localStorage.removeItem(STORAGE_KEYS.USER);
};

export const isAuthenticated = (): boolean => {
  return !!localStorage.getItem(STORAGE_KEYS.USER);
};

export const getCurrentUser = (): User | null => {
  try {
    const userStr = localStorage.getItem(STORAGE_KEYS.USER);
    return userStr ? JSON.parse(userStr) : null;
  } catch (e) {
    return null;
  }
};

