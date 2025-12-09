import { useEffect } from 'react';
import { HashRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Layout } from './components/Layout';
import { ToastContainer } from './components/Toast';
import { ProtectedRoute } from './components/ProtectedRoute';
import { ConditionalRedirect } from './components/ConditionalRedirect';
import { Login } from './pages/Login';
import { MenuUpload } from './pages/MenuUpload';
import { CategoryEditor } from './pages/CategoryEditor';
import { MenuSummary } from './pages/MenuSummary';
import { Settings } from './pages/Settings';
import { Dashboard } from './pages/Dashboard';
import { CreateOrder } from './pages/CreateOrder';
import { ManageOrders } from './pages/ManageOrders';
import { useMenuStore } from './store/menuStore';
import { useOrdersStore } from './store/ordersStore';
import { initOrdersRealtime } from './store/ordersStore';
import { loadFromStorage, saveToStorage } from './utils/storage';
import { loadOrdersFromStorage, saveOrdersToStorage } from './utils/ordersStorage';
import { getStoreId, supabase } from './utils/supabase';
import { RealtimeChannel } from '@supabase/supabase-js';

function App() {
  // Load menu data from backend first, fallback to localStorage
  useEffect(() => {
    const loadMenu = async () => {
      try {
        const result = await useMenuStore.getState().fetchMenuFromBackend();
        if (!result.ok) {
          // Fallback to localStorage if backend fetch fails
          console.warn('Failed to fetch menu from backend, using localStorage fallback');
          const data = loadFromStorage();
          useMenuStore.setState(data);
        }
      } catch (error) {
        console.error('Error loading menu:', error);
        // Fallback to localStorage
        const data = loadFromStorage();
        useMenuStore.setState(data);
      }
    };
    loadMenu();
  }, []);

  // Load orders from localStorage on mount (Supabase sync happens via API calls in ordersStore)
  useEffect(() => {
    const data = loadOrdersFromStorage();
    useOrdersStore.setState(data);
  }, []);

  // Persist menu store changes to localStorage
  useEffect(() => {
    let isActive = true;
    const unsubscribe = useMenuStore.subscribe((state: ReturnType<typeof useMenuStore.getState>) => {
      if (isActive) {
        saveToStorage(state.categories, state.items, state.frequentItemIds);
      }
    });
    return () => {
      isActive = false;
      unsubscribe();
    };
  }, []);

  // Persist orders store changes to localStorage
  useEffect(() => {
    let isActive = true;
    const unsubscribe = useOrdersStore.subscribe((state) => {
      if (isActive) {
        saveOrdersToStorage(state.orders, state.draft);
      }
    });
    return () => {
      isActive = false;
      unsubscribe();
    };
  }, []);

  // Initialize realtime subscription for orders
  useEffect(() => {
    console.log('[App] Setting up realtime subscription...');
    let cleanup: (() => void) | null = null;

    const setupRealtime = async (): Promise<void> => {
      try {
        // Wait a bit for auth session to be established after login
        await new Promise(resolve => setTimeout(resolve, 500));
        
        // Check authentication first
        const { data: { session }, error: authError } = await supabase.auth.getSession();
        if (authError) {
          console.error('[App] Auth check error:', authError);
        }
        console.log('[App] Current session:', session ? 'Authenticated' : 'Not authenticated');
        
        if (!session) {
          console.warn('[App] No user session - realtime subscription may fail due to RLS policies');
          console.warn('[App] Please ensure you are logged in via the login page');
          return; // Don't initialize realtime if not authenticated
        }
        
        console.log('[App] Getting store ID...');
        const storeId = await getStoreId();
        console.log('[App] Store ID:', storeId);
        if (storeId) {
          // Fetch orders initially from Supabase to ensure fresh data
          console.log('[App] Fetching orders from Supabase...');
          await useOrdersStore.getState().fetchOrders();
          console.log('[App] Orders fetched from Supabase');
          
          // Clean up existing subscription if any
          if (cleanup) {
            console.log('[App] Cleaning up existing realtime subscription');
            cleanup();
          }
          console.log('[App] Initializing new realtime subscription for store:', storeId);
          cleanup = initOrdersRealtime(storeId);
          console.log('[App] Orders realtime subscription initialized for store:', storeId);
        } else {
          console.warn('[App] No store ID found, skipping realtime subscription');
        }
      } catch (error) {
        console.error('[App] Failed to initialize orders realtime subscription:', error);
        console.error('[App] Error details:', error instanceof Error ? error.message : String(error));
      }
    };

    setupRealtime();

    // Listen for auth state changes (e.g., when user logs in)
    const { data: { subscription } } = supabase.auth.onAuthStateChange((event, session) => {
      console.log('[App] Auth state changed:', event, session ? 'Authenticated' : 'Not authenticated');
      
      if (event === 'SIGNED_IN' && session) {
        console.log('[App] User signed in, reinitializing realtime subscription...');
        // Fetch fresh orders and setup new subscription
        setupRealtime();
      } else if (event === 'SIGNED_OUT') {
        console.log('[App] User signed out, cleaning up realtime subscription...');
        if (cleanup) {
          cleanup();
          cleanup = null;
        }
      }
    });

    return () => {
      // Cleanup auth listener
      subscription.unsubscribe();
      // Cleanup realtime subscription
      if (cleanup) {
        console.log('[App] Cleaning up realtime subscription on unmount');
        cleanup();
      }
    };
  }, []);

  // Initialize realtime subscription for menu tables
  useEffect(() => {
    console.log('[App] Setting up menu realtime subscription...');
    let menuChannel: RealtimeChannel | null = null;

    const setupMenuRealtime = async (): Promise<void> => {
      try {
        // Wait a bit for auth session to be established
        await new Promise(resolve => setTimeout(resolve, 500));
        
        const { data: { session } } = await supabase.auth.getSession();
        if (!session) {
          console.warn('[App] No user session - menu realtime subscription skipped');
          return;
        }
        
        const storeId = await getStoreId();
        if (!storeId) {
          console.warn('[App] No store ID found, skipping menu realtime subscription');
          return;
        }

        console.log('[App] Initializing menu realtime subscription for store:', storeId);
        
        // Subscribe to categories, items, and frequent_items tables
        menuChannel = supabase
          .channel('menu-changes')
          .on(
            'postgres_changes',
            {
              event: '*',
              schema: 'public',
              table: 'categories',
              filter: `store_id=eq.${storeId}`
            },
            (payload) => {
              console.log('[App] Menu change detected (categories):', payload);
              // Refresh menu from backend
              useMenuStore.getState().fetchMenuFromBackend();
            }
          )
          .on(
            'postgres_changes',
            {
              event: '*',
              schema: 'public',
              table: 'items',
              filter: `store_id=eq.${storeId}`
            },
            (payload) => {
              console.log('[App] Menu change detected (items):', payload);
              // Refresh menu from backend
              useMenuStore.getState().fetchMenuFromBackend();
            }
          )
          .on(
            'postgres_changes',
            {
              event: '*',
              schema: 'public',
              table: 'frequent_items',
              filter: `store_id=eq.${storeId}`
            },
            (payload) => {
              console.log('[App] Menu change detected (frequent_items):', payload);
              // Refresh menu from backend
              useMenuStore.getState().fetchMenuFromBackend();
            }
          )
          .subscribe((status) => {
            console.log('[App] Menu realtime subscription status:', status);
          });

        console.log('[App] Menu realtime subscription initialized');
      } catch (error) {
        console.error('[App] Failed to initialize menu realtime subscription:', error);
      }
    };

    setupMenuRealtime();

    // Listen for auth state changes
    const { data: { subscription } } = supabase.auth.onAuthStateChange((event, session) => {
      if (event === 'SIGNED_IN' && session) {
        console.log('[App] User signed in, reinitializing menu realtime subscription...');
        if (menuChannel) {
          supabase.removeChannel(menuChannel);
        }
        setupMenuRealtime();
      } else if (event === 'SIGNED_OUT') {
        console.log('[App] User signed out, cleaning up menu realtime subscription...');
        if (menuChannel) {
          supabase.removeChannel(menuChannel);
          menuChannel = null;
        }
      }
    });

    return () => {
      subscription.unsubscribe();
      if (menuChannel) {
        console.log('[App] Cleaning up menu realtime subscription on unmount');
        supabase.removeChannel(menuChannel);
      }
    };
  }, []);

  return (
    <HashRouter>
      <ToastContainer />
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }>
          <Route index element={<ConditionalRedirect />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="create-order" element={<CreateOrder />} />
          <Route path="manage-orders" element={<ManageOrders />} />
          <Route path="menu" element={<Navigate to="/menu/summary" replace />} />
          <Route path="menu/upload" element={<MenuUpload />} />
          <Route path="menu/create" element={<CategoryEditor />} />
          <Route path="menu/summary" element={<MenuSummary />} />
          <Route path="settings" element={<Settings />} />
        </Route>
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </HashRouter>
  );
}

export default App;
