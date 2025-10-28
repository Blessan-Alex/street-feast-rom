import React from 'react';
import { Navigate } from 'react-router-dom';
import { useMenuStore } from '../store/menuStore';

export const ConditionalRedirect: React.FC = () => {
  const { categories } = useMenuStore();
  
  // If no menu is loaded (first-time user), redirect to menu setup
  if (categories.length === 0) {
    return <Navigate to="/menu" replace />;
  }
  
  // If menu exists (existing user), redirect to create order
  return <Navigate to="/create-order" replace />;
};
