import React, { useState, useEffect } from 'react';

interface LicensePlateInputProps {
  value?: string;
  onChange: (value: string) => void;
  error?: string;
  className?: string;
}

export const LicensePlateInput: React.FC<LicensePlateInputProps> = ({
  value = '',
  onChange,
  error: externalError,
  className = ''
}) => {
  const [internalError, setInternalError] = useState<string>('');
  const [hasBlurred, setHasBlurred] = useState(false);

  // Validate on blur
  const handleBlur = () => {
    setHasBlurred(true);
    if (value.length > 0 && value.length !== 4) {
      setInternalError('License plate must be exactly 4 digits');
    } else {
      setInternalError('');
    }
  };

  // Real-time validation: only allow digits
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const inputValue = e.target.value;
    
    // Only allow digits
    const digitsOnly = inputValue.replace(/\D/g, '');
    
    // Limit to 4 characters
    const limited = digitsOnly.slice(0, 4);
    
    onChange(limited);
    
    // Clear error if valid
    if (limited.length === 4) {
      setInternalError('');
    } else if (hasBlurred && limited.length > 0) {
      setInternalError('License plate must be exactly 4 digits');
    }
  };

  // Clear error when value becomes valid
  useEffect(() => {
    if (value.length === 4 && internalError) {
      setInternalError('');
    }
  }, [value, internalError]);

  const displayError = externalError || internalError;
  const showError = hasBlurred && displayError;

  return (
    <div className={className}>
      <input
        type="text"
        inputMode="numeric"
        maxLength={4}
        value={value}
        onChange={handleChange}
        onBlur={handleBlur}
        placeholder="Enter 4-digit license plate"
        className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-action-primary text-base ${
          showError
            ? 'border-red-500 focus:ring-red-500'
            : 'border-gray-300'
        }`}
      />
      {showError && (
        <p className="mt-1 text-sm text-red-600">{displayError}</p>
      )}
    </div>
  );
};


