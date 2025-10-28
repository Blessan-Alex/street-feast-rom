import React, { useState } from 'react';
import logoSrc from '../assets/logo.png';

interface LogoProps {
  variant: 'full' | 'compact';
  className?: string;
}

export const Logo: React.FC<LogoProps> = ({ variant, className = '' }) => {
  const [imageError, setImageError] = useState(false);
  
  const sizeClasses = {
    full: 'w-48 h-auto', // ~200px width for login page
    compact: 'w-36 h-auto' // ~150px width for sidebar
  };

  if (imageError) {
    return (
      <div className={`text-center ${className}`}>
        <h1 className={`font-bold text-gray-800 ${variant === 'full' ? 'text-4xl' : 'text-xl'}`}>
          Global Street Feast
        </h1>
        {variant === 'full' && (
          <p className="text-sm text-gray-500">Admin Panel</p>
        )}
      </div>
    );
  }

  return (
    <div className={`flex justify-center ${className}`}>
      <img
        src={logoSrc}
        alt="Global Street Feast Logo"
        className={`${sizeClasses[variant]} object-contain`}
        onError={(e) => {
          console.error('Logo image failed to load:', e);
          setImageError(true);
        }}
        onLoad={() => {
          console.log('Logo image loaded successfully');
          setImageError(false);
        }}
      />
    </div>
  );
};
