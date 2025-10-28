import React, { useState } from 'react';
import { Button } from './Button';
import { useMenuStore, Category, Item } from '../store/menuStore';

interface MenuActionModalProps {
  isOpen: boolean;
  onClose: () => void;
  action: 'add' | 'edit' | 'delete';
  onConfirm: (data?: any) => void;
  title: string;
  message: string;
  categoryName?: string;
  category?: Category | null;
}

export const MenuActionModal: React.FC<MenuActionModalProps> = ({
  isOpen,
  onClose,
  action,
  onConfirm,
  title,
  message,
  categoryName,
  category
}) => {
  const { addCategory, addItems } = useMenuStore();
  const [currentStep, setCurrentStep] = useState(1);
  const [formData, setFormData] = useState({
    categoryName: category?.name || '',
    itemName: '',
    sizes: [] as string[],
    vegFlag: 'Veg' as 'Veg' | 'NonVeg',
  });

  if (!isOpen) return null;

  const handleNext = () => {
    if (currentStep < 4) setCurrentStep(currentStep + 1);
  };
  const handleBack = () => {
    if (currentStep > 1) setCurrentStep(currentStep - 1);
  };

  const handleSubmit = () => {
    // Create category
    const now = Date.now();
    const newCategory: Category = {
      id: crypto.randomUUID(),
      name: formData.categoryName.trim(),
      isActive: true,
      createdAt: now,
      updatedAt: now,
    };
    addCategory(newCategory);

    // Create item
    const newItem: Item = {
      id: crypto.randomUUID(),
      categoryId: newCategory.id,
      name: formData.itemName.trim(),
      sizes: formData.sizes,
      vegFlag: formData.vegFlag,
      flavors: undefined,
      isActive: true,
      createdAt: now,
      updatedAt: now,
    };
    addItems([newItem]);

    onConfirm({ category: newCategory, item: newItem });
    onClose();
  };

  const renderStep = () => {
    switch (currentStep) {
      case 1:
        return (
          <div>
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Category Name</h3>
            <input
              type="text"
              value={formData.categoryName}
              onChange={(e) => setFormData({ ...formData, categoryName: e.target.value })}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-action-primary"
              placeholder="e.g., Chinese"
            />
          </div>
        );
      case 2:
        return (
          <div>
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Item Name</h3>
            <input
              type="text"
              value={formData.itemName}
              onChange={(e) => setFormData({ ...formData, itemName: e.target.value })}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-action-primary"
              placeholder="e.g., Chicken Momos"
            />
          </div>
        );
      case 3:
        return (
          <div>
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Sizes</h3>
            <input
              type="text"
              value={formData.sizes.join(', ')}
              onChange={(e) => setFormData({ ...formData, sizes: e.target.value.split(',').map(s => s.trim()).filter(Boolean) })}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-action-primary"
              placeholder="e.g., Small, Medium, Large"
            />
            <p className="text-xs text-gray-500 mt-2">Leave empty if no sizes</p>
          </div>
        );
      case 4:
        return (
          <div>
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Type</h3>
            <div className="flex gap-3">
              {(['Veg', 'NonVeg'] as const).map(option => (
                <label key={option} className={`px-3 py-2 border rounded-lg cursor-pointer ${formData.vegFlag === option ? 'border-action-primary bg-action-primary/5' : 'border-gray-300'}`}>
                  <input
                    type="radio"
                    className="hidden"
                    checked={formData.vegFlag === option}
                    onChange={() => setFormData({ ...formData, vegFlag: option })}
                  />
                  <span className="text-sm font-medium text-gray-800">{option === 'Veg' ? 'Vegetarian' : 'Non-Vegetarian'}</span>
                </label>
              ))}
            </div>
          </div>
        );
      default:
        return null;
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div 
        className="absolute inset-0 bg-black bg-opacity-50 backdrop-blur-sm"
        onClick={onClose}
      />
      
      <div className="relative bg-white rounded-lg shadow-xl max-w-lg w-full mx-4 max-h-[90vh] overflow-hidden flex flex-col">
        <div className="px-6 py-4 border-b bg-gray-50">
          <h2 className="text-xl font-bold text-gray-900">{title}</h2>
          <p className="text-gray-600 text-sm">{message}</p>
        </div>

        <div className="px-6 py-4 flex-1 overflow-y-auto">
          {/* Progress Bar */}
          <div className="mb-6">
            <div className="flex items-center justify-between text-xs text-gray-500 mb-2">
              <span>Step {currentStep} of 4</span>
              <span>{Math.round((currentStep / 4) * 100)}%</span>
            </div>
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div 
                className="bg-action-primary h-2 rounded-full transition-all duration-300"
                style={{ width: `${(currentStep / 4) * 100}%` }}
              />
            </div>
          </div>

          {renderStep()}
        </div>

        <div className="px-6 py-4 border-t bg-gray-50 flex gap-3">
          {currentStep > 1 ? (
            <Button variant="secondary" onClick={handleBack} className="flex-1">
              Back
            </Button>
          ) : (
            <Button variant="secondary" onClick={onClose} className="flex-1">
              Cancel
            </Button>
          )}
          {currentStep < 4 ? (
            <Button 
              variant="primary" 
              onClick={handleNext} 
              className="flex-1"
              disabled={(currentStep === 1 && !formData.categoryName.trim()) || (currentStep === 2 && !formData.itemName.trim())}
            >
              Next
            </Button>
          ) : (
            <Button 
              variant="primary" 
              onClick={handleSubmit} 
              className="flex-1"
              disabled={!formData.categoryName.trim() || !formData.itemName.trim()}
            >
              Create
            </Button>
          )}
        </div>
      </div>
    </div>
  );
};
