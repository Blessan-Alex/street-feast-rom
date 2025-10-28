import { useEffect } from 'react';
import { HashRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Layout } from './components/Layout';
import { ToastContainer } from './components/Toast';
import { ProtectedRoute } from './components/ProtectedRoute';
import { ConditionalRedirect } from './components/ConditionalRedirect';
import { Login } from './pages/Login';
import { MenuChooser } from './pages/MenuChooser';
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

function App() {
  // Load menu data from localStorage on mount
  useEffect(() => {
    const data = loadFromStorage();
    useMenuStore.setState(data);
  }, []);

  // Load orders data from localStorage on mount
  useEffect(() => {
    const data = loadOrdersFromStorage();
    useOrdersStore.setState(data);
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
