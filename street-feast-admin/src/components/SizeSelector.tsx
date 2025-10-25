import React, { useState } from 'react';
import { Item } from '../store/menuStore';
import { Button } from './Button';

interface SizeSelectorProps {
  item: Item;
  isOpen: boolean;
  onClose: () => void;
  onSelect: (size: 'Small' | 'Large' | null) => void;
}

export const SizeSelector: React.FC<SizeSelectorProps> = ({ item, isOpen, onClose, onSelect }) => {
  const [selectedSize, setSelectedSize] = useState<'Small' | 'Large' | null>(
    item.sizes.length > 0 ? item.sizes[0] as 'Small' | 'Large' : null
  );

  if (!isOpen) return null;

  const handleAdd = () => {
    onSelect(selectedSize);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* Backdrop */}
      <div 
        className="absolute inset-0 bg-black bg-opacity-50"
        onClick={onClose}
      />
      
      {/* Modal */}
      <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6 animate-fade-in">
        <h2 className="text-2xl font-bold text-gray-900 mb-2">{item.name}</h2>
        <p className="text-gray-600 mb-6">Select size for this item</p>
        
        {item.sizes.length > 0 ? (
          <div className="space-y-2 mb-6">
            {item.sizes.map(size => (
              <label key={size} className="flex items-center p-3 border-2 rounded-lg cursor-pointer hover:bg-gray-50 transition-colors"
                style={{ borderColor: selectedSize === size ? '#22C55E' : '#E5E7EB' }}
              >
                <input
                  type="radio"
                  checked={selectedSize === size}
                  onChange={() => setSelectedSize(size as 'Small' | 'Large')}
                  className="w-4 h-4 text-action-primary border-gray-300 focus:ring-2 focus:ring-action-primary"
                />
                <span className="ml-3 text-lg font-medium text-gray-900">{size}</span>
              </label>
            ))}
          </div>
        ) : (
          <div className="mb-6">
            <p className="text-gray-600">This item has no size variants</p>
          </div>
        )}
        
        <div className="flex gap-3">
          <Button variant="secondary" onClick={onClose} className="flex-1">
            Cancel
          </Button>
          <Button variant="primary" onClick={handleAdd} className="flex-1">
            Add to Order
          </Button>
        </div>
      </div>
    </div>
  );
};

