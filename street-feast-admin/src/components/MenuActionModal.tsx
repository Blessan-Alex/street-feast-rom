import React, { useEffect, useState } from 'react';
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
  category
}) => {
  const { addCategory, addItems } = useMenuStore();
  const [currentStep, setCurrentStep] = useState(1);
  // Always declare add/edit wizard state hooks BEFORE any conditional returns
  const [categoryNameState, setCategoryNameState] = useState(category?.name || '');
  const [itemCount, setItemCount] = useState(1);
  const [itemForms, setItemForms] = useState<Array<{ name: string; sizes: string[]; sizesRaw?: string; vegFlag: 'Veg' | 'NonVeg' }>>([
    { name: '', sizes: [], sizesRaw: '', vegFlag: 'Veg' }
  ]);
  useEffect(() => {
    if (itemCount < 1) setItemCount(1);
    setItemForms(prev => {
      const next = [...prev];
      if (itemCount > next.length) {
        while (next.length < itemCount) next.push({ name: '', sizes: [], sizesRaw: '', vegFlag: 'Veg' });
      } else if (itemCount < next.length) {
        next.length = itemCount;
      }
      return next;
    });
  }, [itemCount]);

  // DELETE: simple confirm path, no creation
  if (!isOpen) return null;
  if (action === 'delete') {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center">
        <div 
          className="absolute inset-0 bg-black bg-opacity-50 backdrop-blur-sm"
          onClick={onClose}
        />
        <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
          <h2 className="text-xl font-bold text-gray-900 mb-2">{title}</h2>
          <p className="text-gray-600 mb-6">{message}</p>
          <div className="flex gap-3">
            <Button variant="secondary" onClick={onClose} className="flex-1">Cancel</Button>
            <Button variant="danger" onClick={() => { onConfirm(); onClose(); }} className="flex-1">Delete</Button>
          </div>
        </div>
      </div>
    );
  }

  // ADD/EDIT: wizard flow (Category → Number of Items → per-item steps)

  const totalSteps = 2 + itemCount; // 1=Category, 2=Number, 3..=items

  const handleNext = () => {
    if (currentStep < totalSteps) setCurrentStep(currentStep + 1);
  };
  const handleBack = () => {
    if (currentStep > 1) setCurrentStep(currentStep - 1);
  };

  const handleSubmit = () => {
    const now = Date.now();
    const newCategory: Category = {
      id: crypto.randomUUID(),
      name: categoryNameState.trim(),
      isActive: true,
      createdAt: now,
      updatedAt: now,
    };
    addCategory(newCategory);

    const itemsToAdd: Item[] = itemForms
      .filter(f => f.name.trim())
      .map(f => ({
        id: crypto.randomUUID(),
        categoryId: newCategory.id,
        name: f.name.trim(),
        sizes: f.sizes,
        vegFlag: f.vegFlag,
        flavors: undefined,
        isActive: true,
        createdAt: now,
        updatedAt: now,
      }));
    if (itemsToAdd.length) addItems(itemsToAdd);

    onConfirm({ category: newCategory, items: itemsToAdd });
    onClose();
  };

  const renderStep = () => {
    if (currentStep === 1) {
      return (
        <div>
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Category Name</h3>
          <input
            type="text"
            value={categoryNameState}
            onChange={(e) => setCategoryNameState(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-action-primary"
            placeholder="e.g., Chinese"
          />
        </div>
      );
    }
    if (currentStep === 2) {
      return (
        <div>
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Number of Items</h3>
          <input
            type="number"
            min={1}
            max={20}
            value={itemCount}
            onChange={(e) => setItemCount(Math.max(1, Math.min(20, Number(e.target.value) || 1)))}
            className="w-32 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-action-primary"
          />
        </div>
      );
    }
    const idx = currentStep - 3;
    const form = itemForms[idx];
    const updateForm = (patch: Partial<typeof form>) => {
      setItemForms(prev => prev.map((f, i) => i === idx ? { ...f, ...patch } : f));
    };
    
    const processSizesInput = (rawValue: string) => {
      const sizes = rawValue.split(',').map(s => s.trim()).filter(s => s);
      updateForm({ sizesRaw: rawValue, sizes: sizes });
    };
    return (
      <div>
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Item {idx + 1} of {itemCount}</h3>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Item Name *</label>
            <input
              value={form.name}
              onChange={(e) => updateForm({ name: e.target.value })}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-action-primary"
              placeholder="e.g., Chicken Momos"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Sizes (comma-separated)</label>
            <input
              value={form.sizesRaw || ''}
              onChange={(e) => processSizesInput(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-action-primary"
              placeholder="Small, Medium, Large"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Type</label>
            <div className="flex gap-3">
              {(['Veg','NonVeg'] as const).map(v => (
                <label key={v} className={`px-3 py-2 border rounded-lg cursor-pointer ${form.vegFlag === v ? 'border-action-primary bg-action-primary/5' : 'border-gray-300'}`}>
                  <input type="radio" className="hidden" checked={form.vegFlag === v} onChange={() => updateForm({ vegFlag: v })} />
                  <span className="text-sm">{v === 'Veg' ? 'Vegetarian' : 'Non-Vegetarian'}</span>
                </label>
              ))}
            </div>
          </div>
        </div>
      </div>
    );
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
          {currentStep < totalSteps ? (
            <Button 
              variant="primary" 
              onClick={handleNext} 
              className="flex-1"
              disabled={(currentStep === 1 && !categoryNameState.trim())}
            >
              Next
            </Button>
          ) : (
            <Button 
              variant="primary" 
              onClick={handleSubmit} 
              className="flex-1"
              disabled={!categoryNameState.trim() || itemForms.some(f => !f.name.trim())}
            >
              Create
            </Button>
          )}
        </div>
      </div>
    </div>
  );
};
