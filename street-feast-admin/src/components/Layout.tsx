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
  const isMenuSetup = location.pathname.startsWith('/menu');

  const navItems = [
    { path: '/dashboard', label: 'Dashboard', icon: '/icons/dashboard.png', enabled: hasMenu },
    { path: '/create-order', label: 'Create Order', icon: '/icons/create-order.png', enabled: hasMenu },
    { path: '/manage-orders', label: 'Manage Orders', icon: '/icons/manage-order.png', enabled: hasMenu },
    { path: '/menu', label: 'Menu', icon: '/icons/menu.png', enabled: true },
    { path: '/settings', label: 'Settings', icon: '/icons/setting.png', enabled: hasMenu },
  ];

  const isActive = (path: string) => location.pathname.startsWith(path);

  return (
    <div className="flex h-screen bg-gray-100">
      {/* Left Sidebar */}
      <aside className={`${sidebarCollapsed ? 'w-20' : 'w-64'} bg-white shadow-lg flex flex-col transition-all duration-300`}>
        <div className="p-4 border-b">
          <div className="flex items-center justify-between mb-3">
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
          {!sidebarCollapsed && (
            <>
              <h1 className="text-2xl font-bold text-gray-800">Streat Feast</h1>
              <p className="text-sm text-gray-500">Admin Panel</p>
            </>
          )}
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
                    } ${sidebarCollapsed ? 'justify-center' : ''}`}
                    title={sidebarCollapsed ? item.label : undefined}
                  >
                    <img 
                      src={item.icon} 
                      alt={item.label} 
                      className={`w-5 h-5 ${isActive(item.path) ? 'brightness-0 invert' : 'opacity-80'}`} 
                    />
                    {!sidebarCollapsed && <span className="ml-3">{item.label}</span>}
                  </Link>
                ) : (
                  <div className={`flex items-center px-4 py-3 rounded-lg font-medium text-gray-400 cursor-not-allowed ${
                    sidebarCollapsed ? 'justify-center' : ''
                  }`} title={sidebarCollapsed ? item.label : undefined}>
                    <img 
                      src={item.icon} 
                      alt={item.label} 
                      className={`w-5 h-5 ${isActive(item.path) ? 'brightness-0 invert' : 'opacity-60'}`} 
                    />
                    {!sidebarCollapsed && (
                      <>
                        <span className="ml-3">{item.label}</span>
                        <span className="text-xs ml-2">(Setup Required)</span>
                      </>
                    )}
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
            className={`flex items-center w-full px-4 py-3 text-sm font-medium text-gray-700 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-gray-400 ${
              sidebarCollapsed ? 'justify-center' : ''
            }`}
            title={sidebarCollapsed ? 'Logout' : undefined}
          >
            <img src="/icons/logout.png" alt="Logout" className="w-5 h-5" />
            {!sidebarCollapsed && <span className="ml-3">Logout</span>}
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

