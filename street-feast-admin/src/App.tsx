import { useEffect } from 'react';
import { HashRouter, Routes, Route, Navigate } from 'react-router-dom';
import { getAuth } from 'firebase/auth';
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
import { loadFromStorage, saveToStorage } from './utils/storage';
import { loadOrdersFromStorage, saveOrdersToStorage } from './utils/ordersStorage';
import { app } from './utils/firebase';

function App() {
  // Load menu data from localStorage on mount
  useEffect(() => {
    const data = loadFromStorage();
    useMenuStore.setState(data);
  }, []);

  // Initialize Firestore sync for orders
  useEffect(() => {
    const auth = getAuth(app);
    
    // Wait for auth to be ready, then initialize Firestore sync
    const initFirestore = async () => {
      // Wait for auth to complete (with timeout)
      let attempts = 0;
      while (!auth.currentUser && attempts < 10) {
        await new Promise(resolve => setTimeout(resolve, 100));
        attempts++;
      }

      const ordersStore = useOrdersStore.getState();
      
      // Load from Firestore first (source of truth)
      try {
        await ordersStore.loadOrdersFromFirestore();
      } catch (error) {
        console.error('Failed to load orders from Firestore, falling back to localStorage:', error);
        // Fallback to localStorage if Firestore fails
    const data = loadOrdersFromStorage();
    useOrdersStore.setState(data);
      }
      
      // Start real-time listener
      ordersStore.syncFromFirestore();
    };

    initFirestore();

    // Cleanup on unmount
    return () => {
      const ordersStore = useOrdersStore.getState();
      if (ordersStore.firestoreUnsubscribe) {
        ordersStore.firestoreUnsubscribe();
      }
    };
  }, []);

  // Persist menu store changes to localStorage
  useEffect(() => {
    let isActive = true;
    const unsubscribe = useMenuStore.subscribe((state) => {
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
