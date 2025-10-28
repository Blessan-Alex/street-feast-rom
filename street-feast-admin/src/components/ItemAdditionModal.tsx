import React, { useState } from 'react';
import { Item } from '../store/menuStore';
import { Button } from './Button';

interface ItemAdditionModalProps {
  item: Item;
  isOpen: boolean;
  onClose: () => void;
  onAdd: (data: { size: string | null; chefTip: string; quantity: number }) => void;
  editData?: { size: string | null; chefTip: string; quantity: number };
}

export const ItemAdditionModal: React.FC<ItemAdditionModalProps> = ({ 
  item, 
  isOpen, 
  onClose, 
  onAdd,
  editData 
}) => {
  const [currentStep, setCurrentStep] = useState(1);
  const [selectedSize, setSelectedSize] = useState<string | null>(
    editData?.size || (item.sizes.length > 0 ? item.sizes[0] : null)
  );
  const [chefTip, setChefTip] = useState(editData?.chefTip || '');
  const [quantity, setQuantity] = useState(editData?.quantity || 1);

  // Always have 3 steps: Size (if applicable), Chef Tip, Quantity
  const totalSteps = item.sizes.length > 0 ? 3 : 2;
  const isEditMode = !!editData;

  const handleNext = () => {
    if (currentStep < totalSteps) {
      setCurrentStep(currentStep + 1);
    }
  };

  const handleBack = () => {
    if (currentStep > 1) {
      setCurrentStep(currentStep - 1);
    }
  };

  const handleAdd = () => {
    onAdd({
      size: selectedSize,
      chefTip,
      quantity
    });
    onClose();
  };

  const resetModal = () => {
    setCurrentStep(1);
    setSelectedSize(item.sizes.length > 0 ? item.sizes[0] : null);
    setChefTip('');
    setQuantity(1);
  };

  const handleClose = () => {
    resetModal();
    onClose();
  };

  if (!isOpen) return null;

  const renderStep = () => {
    // Step 1: Size selection (only if item has sizes)
    if (currentStep === 1 && item.sizes.length > 0) {
      return (
        <div>
          <h2 className="text-xl font-bold text-gray-900 mb-4">Select Size</h2>
          <div className="space-y-3 mb-6">
            {item.sizes.map(size => (
              <label key={size} className="flex items-center p-4 border-2 rounded-lg cursor-pointer hover:bg-gray-50 transition-colors"
                style={{ borderColor: selectedSize === size ? '#22C55E' : '#E5E7EB' }}
              >
                <input
                  type="radio"
                  checked={selectedSize === size}
                  onChange={() => setSelectedSize(size)}
                  className="w-5 h-5 text-action-primary border-gray-300 focus:ring-2 focus:ring-action-primary"
                />
                <span className="ml-3 text-lg font-medium text-gray-900">{size}</span>
              </label>
            ))}
          </div>
        </div>
      );
    }

    // Step 2: Chef Tip (always shown)
    if ((currentStep === 2 && item.sizes.length > 0) || (currentStep === 1 && item.sizes.length === 0)) {
      return (
        <div>
          <h2 className="text-xl font-bold text-gray-900 mb-4">Chef Tip (Optional)</h2>
          <p className="text-sm text-gray-600 mb-4">
            Add special instructions for the chef
          </p>
          <textarea
            value={chefTip}
            onChange={(e) => setChefTip(e.target.value)}
            placeholder="e.g., Extra spicy, No onions, Well done..."
            rows={3}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-action-primary text-sm"
          />
          <div className="mt-2 text-xs text-gray-500">
            Suggestions: Extra spicy, No onions, Well done, Medium rare, Extra sauce
          </div>
        </div>
      );
    }

    // Step 3: Quantity (always the last step)
    if ((currentStep === 3 && item.sizes.length > 0) || (currentStep === 2 && item.sizes.length === 0)) {
      return (
        <div>
          <h2 className="text-xl font-bold text-gray-900 mb-4">Quantity</h2>
          <div className="flex items-center justify-center gap-4 mb-6">
            <button
              onClick={() => setQuantity(Math.max(1, quantity - 1))}
              className="w-10 h-10 flex items-center justify-center border border-gray-300 rounded-lg hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-action-primary text-lg font-bold"
            >
              −
            </button>
            <span className="text-3xl font-bold text-gray-900 w-16 text-center">{quantity}</span>
            <button
              onClick={() => setQuantity(quantity + 1)}
              className="w-10 h-10 flex items-center justify-center border border-gray-300 rounded-lg hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-action-primary text-lg font-bold"
            >
              +
            </button>
          </div>
          <div className="text-center text-sm text-gray-600">
            {quantity} × {item.name} {selectedSize && `(${selectedSize})`}
          </div>
        </div>
      );
    }

    return null;
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* Backdrop */}
      <div 
        className="absolute inset-0 bg-black bg-opacity-50 backdrop-blur-sm"
        onClick={handleClose}
      />
      
      {/* Modal */}
      <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6 animate-fade-in">
        {/* Progress Bar */}
        <div className="mb-6">
          <div className="flex items-center justify-between text-xs text-gray-500 mb-2">
            <span>Step {currentStep} of {totalSteps}</span>
            <span>{Math.round((currentStep / totalSteps) * 100)}%</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-2">
            <div 
              className="bg-action-primary h-2 rounded-full transition-all duration-300"
              style={{ width: `${(currentStep / totalSteps) * 100}%` }}
            />
          </div>
        </div>

        {/* Step Content */}
        <div className="mb-6 min-h-[200px]">
          {renderStep()}
        </div>

        {/* Navigation Buttons */}
        <div className="flex gap-3">
          {currentStep > 1 ? (
            <Button variant="secondary" onClick={handleBack} className="flex-1">
              Back
            </Button>
          ) : (
            <Button variant="secondary" onClick={handleClose} className="flex-1">
              Cancel
            </Button>
          )}
          
          {currentStep < totalSteps ? (
            <Button variant="primary" onClick={handleNext} className="flex-1">
              Next
            </Button>
          ) : (
            <Button variant="primary" onClick={handleAdd} className="flex-1">
              {isEditMode ? 'Update Item' : 'Add to Order'}
            </Button>
          )}
        </div>
      </div>
    </div>
  );
};