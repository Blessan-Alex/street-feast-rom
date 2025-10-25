import { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Layout } from './components/Layout';
import { ToastContainer } from './components/Toast';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Login } from './pages/Login';
import { MenuChooser } from './pages/MenuChooser';
import { MenuUpload } from './pages/MenuUpload';
import { CategoryEditor } from './pages/CategoryEditor';
import { MenuSummary } from './pages/MenuSummary';
import { Settings } from './pages/Settings';
import { useMenuStore } from './store/menuStore';
import { loadFromStorage, saveToStorage } from './utils/storage';

function App() {
  // Load data from localStorage on mount
  useEffect(() => {
    const data = loadFromStorage();
    useMenuStore.setState(data);
  }, []);

  // Persist store changes to localStorage
  useEffect(() => {
    const unsubscribe = useMenuStore.subscribe((state) => {
      saveToStorage(state.categories, state.items, state.frequentItemIds);
    });
    return unsubscribe;
  }, []);

  return (
    <BrowserRouter>
      <ToastContainer />
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }>
          <Route index element={<Navigate to="/menu" replace />} />
          <Route path="menu" element={<MenuChooser />} />
          <Route path="menu/upload" element={<MenuUpload />} />
          <Route path="menu/create" element={<CategoryEditor />} />
          <Route path="menu/summary" element={<MenuSummary />} />
          <Route path="settings" element={<Settings />} />
        </Route>
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
