import React from 'react';

interface ButtonProps {
  children: React.ReactNode;
  variant?: 'primary' | 'danger' | 'secondary';
  onClick?: () => void;
  disabled?: boolean;
  type?: 'button' | 'submit' | 'reset';
  className?: string;
  size?: 'small' | 'medium' | 'large';
}

export const Button: React.FC<ButtonProps> = ({
  children,
  variant = 'secondary',
  onClick,
  disabled = false,
  type = 'button',
  className = '',
  size = 'medium'
}) => {
  const baseClasses = 'font-medium rounded-lg transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed';
  
  const variantClasses = {
    primary: 'bg-action-primary hover:bg-green-600 text-white focus:ring-action-primary',
    danger: 'bg-action-danger hover:bg-red-600 text-white focus:ring-action-danger',
    secondary: 'bg-gray-200 hover:bg-gray-300 text-gray-800 focus:ring-gray-400'
  };
  
  const sizeClasses = {
    small: 'px-3 py-1.5 text-sm min-h-[36px]',
    medium: 'px-4 py-2 text-base min-h-[44px]',
    large: 'px-6 py-3 text-lg min-h-[52px]'
  };
  
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={`${baseClasses} ${variantClasses[variant]} ${sizeClasses[size]} ${className}`}
    >
      {children}
    </button>
  );
};

