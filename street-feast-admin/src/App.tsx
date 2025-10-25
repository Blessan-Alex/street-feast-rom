import { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Layout } from './components/Layout';
import { ToastContainer } from './components/Toast';
import { useMenuStore } from './store/menuStore';
import { loadFromStorage, saveToStorage } from './utils/storage';

import { useState } from 'react';
import { Button } from './components/Button';
import { Dialog } from './components/Dialog';
import { toast } from './components/Toast';

// Placeholder components (will be created next)
const Login = () => <div className="flex items-center justify-center h-full"><h1 className="text-3xl font-bold">Login Page (Coming Soon)</h1></div>;

// Test component to verify all components work
const MenuChooser = () => {
  const [dialogOpen, setDialogOpen] = useState(false);

  return (
    <div className="max-w-4xl">
      <h1 className="text-3xl font-bold text-gray-900 mb-6">Welcome to Menu Management</h1>
      
      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <h2 className="text-xl font-semibold mb-4">Test Components</h2>
        <div className="flex gap-3 flex-wrap">
          <Button variant="primary" onClick={() => toast.success('Success! Everything works.')}>
            Test Success Toast
          </Button>
          <Button variant="danger" onClick={() => toast.error('Error! Something went wrong.')}>
            Test Error Toast
          </Button>
          <Button variant="secondary" onClick={() => toast.warning('Warning! Be careful.')}>
            Test Warning Toast
          </Button>
          <Button onClick={() => toast.info('Info: This is informational.')}>
            Test Info Toast
          </Button>
          <Button variant="primary" onClick={() => setDialogOpen(true)}>
            Test Dialog
          </Button>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-xl font-semibold mb-4">Get Started</h2>
        <p className="text-gray-700 mb-4">Choose how you want to create your menu:</p>
        <div className="flex gap-4">
          <Button variant="primary" size="large">Upload from CSV</Button>
          <Button variant="secondary" size="large">Create Manually</Button>
        </div>
      </div>

      <Dialog
        isOpen={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title="Test Dialog"
        message="This is a test dialog. It can be used for confirmations and alerts."
        confirmText="Got it!"
        onConfirm={() => toast.success('Dialog confirmed!')}
        confirmVariant="primary"
      />
    </div>
  );
};

const Settings = () => <div className="flex items-center justify-center h-full"><h1 className="text-3xl font-bold">Settings (Coming Soon)</h1></div>;

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
        <Route path="/" element={<Layout />}>
          <Route index element={<Navigate to="/menu" replace />} />
          <Route path="menu/*" element={<MenuChooser />} />
          <Route path="settings" element={<Settings />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
