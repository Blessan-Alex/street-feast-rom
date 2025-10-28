import React, { useState, useEffect } from 'react';
import { Link, useLocation, Outlet, useNavigate } from 'react-router-dom';
import { Logo } from './Logo';
import { Breadcrumbs } from './Breadcrumbs';
import { useMenuStore } from '../store/menuStore';
import { STORAGE_KEYS } from '../utils/storage';

export const Layout: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { categories } = useMenuStore();
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  // Load sidebar state from localStorage
  useEffect(() => {
    const savedState = localStorage.getItem('sidebarCollapsed');
    if (savedState !== null) {
      setSidebarCollapsed(JSON.parse(savedState));
    }
  }, []);

  // Save sidebar state to localStorage
  useEffect(() => {
    localStorage.setItem('sidebarCollapsed', JSON.stringify(sidebarCollapsed));
  }, [sidebarCollapsed]);

  const handleLogout = () => {
    localStorage.removeItem(STORAGE_KEYS.USER);
    navigate('/login');
  };

  const toggleSidebar = () => {
    setSidebarCollapsed(!sidebarCollapsed);
  };

  // Check if menu is loaded (first-time user detection)
  const hasMenu = categories.length > 0;

  const navItems = [
    { path: '/dashboard', label: 'Dashboard', enabled: hasMenu },
    { path: '/create-order', label: 'Create Order', enabled: hasMenu },
    { path: '/manage-orders', label: 'Manage Orders', enabled: hasMenu },
    { path: '/menu/summary', label: 'Menu', enabled: true },
    { path: '/settings', label: 'Settings', enabled: hasMenu },
  ];

  const isActive = (path: string) => location.pathname.startsWith(path);

  return (
    <div className="flex h-screen bg-gray-100">
      {/* Sidebar Toggle Button - Outside Sidebar */}
      <button
        onClick={toggleSidebar}
        className={`fixed top-16 left-4 z-50 p-2 bg-white hover:bg-gray-100 rounded-lg shadow-lg transition-colors focus:outline-none focus:ring-2 focus:ring-gray-400 ${sidebarCollapsed ? 'block' : 'hidden'}`}
        aria-label="Toggle sidebar"
      >
        <svg className="w-5 h-5 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
        </svg>
      </button>

      {/* Left Sidebar */}
      <aside className={`${sidebarCollapsed ? 'w-0' : 'w-64'} bg-white shadow-lg flex flex-col transition-all duration-300 overflow-hidden`}>
        <div className="p-4 border-b">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-3">
              <Logo variant="compact" />
              <button
                onClick={toggleSidebar}
                className="p-1 hover:bg-gray-100 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-gray-400"
                aria-label="Toggle sidebar"
              >
                <svg className="w-5 h-5 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                </svg>
              </button>
            </div>
          </div>
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
                    className={`flex items-center px-4 py-3 rounded-lg font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-action-primary ${
                      isActive(item.path)
                        ? 'bg-action-primary text-white'
                        : 'text-gray-700 hover:bg-gray-100'
                    }`}
                  >
                    <span>{item.label}</span>
                  </Link>
                ) : (
                  <div className="flex items-center px-4 py-3 rounded-lg font-medium text-gray-400 cursor-not-allowed">
                    <span>{item.label}</span>
                    <span className="text-xs ml-2">(Setup Required)</span>
                  </div>
                )}
              </li>
            ))}
          </ul>
        </nav>

        {/* Logout Button at Bottom */}
        <div className="p-4 border-t">
          <button
            onClick={handleLogout}
            className="flex items-center w-full px-4 py-3 text-sm font-medium text-gray-700 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-gray-400"
          >
            <span>Logout</span>
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Top Bar */}
        <header className="bg-white shadow-sm px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Breadcrumbs />
          </div>
        </header>

        {/* Content Area */}
        <main className="flex-1 overflow-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

