import React from 'react';
import { Link, useLocation, Outlet, useNavigate } from 'react-router-dom';
import { STORAGE_KEYS } from '../utils/storage';

export const Layout: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem(STORAGE_KEYS.USER);
    navigate('/login');
  };

  const navItems = [
    { path: '/menu', label: 'Menu', enabled: true },
    { path: '/create-order', label: 'Create Order', enabled: false },
    { path: '/manage-orders', label: 'Manage Orders', enabled: false },
    { path: '/settings', label: 'Settings', enabled: true },
  ];

  const isActive = (path: string) => location.pathname.startsWith(path);

  return (
    <div className="flex h-screen bg-gray-100">
      {/* Left Sidebar */}
      <aside className="w-64 bg-white shadow-lg flex flex-col">
        <div className="p-6 border-b">
          <h1 className="text-2xl font-bold text-gray-800">Streat Feast</h1>
          <p className="text-sm text-gray-500">Admin Panel</p>
        </div>
        
        <nav className="flex-1 p-4">
          <ul className="space-y-2">
            {navItems.map((item) => (
              <li key={item.path}>
                {item.enabled ? (
                  <Link
                    to={item.path}
                    className={`block px-4 py-3 rounded-lg font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-action-primary ${
                      isActive(item.path)
                        ? 'bg-action-primary text-white'
                        : 'text-gray-700 hover:bg-gray-100'
                    }`}
                  >
                    {item.label}
                  </Link>
                ) : (
                  <div className="px-4 py-3 rounded-lg font-medium text-gray-400 cursor-not-allowed">
                    {item.label}
                    <span className="text-xs ml-2">(Coming Soon)</span>
                  </div>
                )}
              </li>
            ))}
          </ul>
        </nav>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Top Bar */}
        <header className="bg-white shadow-sm px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <h2 className="text-xl font-semibold text-gray-800">Streat Feast Admin</h2>
          </div>
          
          <button
            onClick={handleLogout}
            className="px-4 py-2 text-sm font-medium text-gray-700 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-gray-400"
          >
            Logout
          </button>
        </header>

        {/* Content Area */}
        <main className="flex-1 overflow-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

