import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useMenuStore } from '../store/menuStore';

export const Breadcrumbs: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { categories } = useMenuStore();

  const getBreadcrumbs = () => {
    const pathSegments = location.pathname.split('/').filter(Boolean);
    const breadcrumbs: Array<{ label: string; path?: string; clickable: boolean }> = [];

    // Map path segments to readable names
    const pathMap: Record<string, string> = {
      'dashboard': 'Dashboard',
      'create-order': 'Create Order',
      'manage-orders': 'Manage Orders',
      'menu': 'Menu',
      'upload': 'Upload',
      'create': 'Create',
      'summary': 'Summary',
      'settings': 'Settings'
    };

    // Build breadcrumb trail
    for (let i = 0; i < pathSegments.length; i++) {
      const segment = pathSegments[i];
      
      if (segment === 'menu' && pathSegments[i + 1] === 'create') {
        // Special case for category editing
        breadcrumbs.push(
          { label: 'Menu', path: '/menu', clickable: true },
          { label: 'Edit Category', clickable: false }
        );
        i++; // Skip the next segment
      } else if (pathMap[segment]) {
        const path = '/' + pathSegments.slice(0, i + 1).join('/');
        breadcrumbs.push({ 
          label: pathMap[segment], 
          path,
          clickable: i < pathSegments.length - 1 
        });
      }
    }

    // Special case: if we're in create-order and viewing a category via state
    if (location.pathname === '/create-order' && (location.state as any)?.categoryName) {
      breadcrumbs.push({ label: (location.state as any).categoryName, clickable: false });
    }

    return breadcrumbs;
  };

  const breadcrumbs = getBreadcrumbs();

  if (breadcrumbs.length === 0) {
    return null;
  }

  const handleBreadcrumbClick = (path: string) => {
    navigate(path);
  };

  return (
    <nav className="flex items-center space-x-2 text-sm">
      {breadcrumbs.map((crumb, index) => (
        <React.Fragment key={index}>
          {index > 0 && (
            <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
            </svg>
          )}
          {crumb.clickable && crumb.path ? (
            <button
              onClick={() => handleBreadcrumbClick(crumb.path!)}
              className="text-gray-600 hover:text-gray-900 hover:underline focus:outline-none focus:underline"
            >
              {crumb.label}
            </button>
          ) : (
            <span className={`${index === breadcrumbs.length - 1 ? 'text-gray-900 font-medium' : 'text-gray-600'}`}>
              {crumb.label}
            </span>
          )}
        </React.Fragment>
      ))}
    </nav>
  );
};
