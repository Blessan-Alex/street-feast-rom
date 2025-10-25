import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Button } from '../components/Button';
import { useMenuStore } from '../store/menuStore';
import { toast } from '../components/Toast';

interface ItemFormData {
  id: string;
  name: string;
  sizes: string[];
  vegFlag: 'Veg' | 'NonVeg';
}

export const CategoryEditor: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { addCategory, addItems, updateCategory, items: storeItems } = useMenuStore();
  
  // Check if we're editing an existing category
  const editingCategory = location.state?.category || null;
  const editingCategoryId = editingCategory?.id || null;
  
  const [categoryName, setCategoryName] = useState(editingCategory?.name || '');
  const [items, setItems] = useState<ItemFormData[]>(() => {
    if (editingCategoryId) {
      // Load existing items for this category
      const categoryItems = storeItems.filter(item => item.categoryId === editingCategoryId);
      return categoryItems.map(item => ({
        id: item.id,
        name: item.name,
        sizes: item.sizes,
        vegFlag: item.vegFlag
      }));
    }
    return [{
      id: `temp-${Date.now()}`,
      name: '',
      sizes: [],
      vegFlag: 'Veg' as const
    }];
  });

  const [errors, setErrors] = useState<{ [key: string]: string }>({});

  const addItem = () => {
    setItems([...items, {
      id: `temp-${Date.now()}-${Math.random()}`,
      name: '',
      sizes: [],
      vegFlag: 'Veg'
    }]);
  };

  const removeItem = (id: string) => {
    if (items.length === 1) {
      toast.warning('Category must have at least one item');
      return;
    }
    setItems(items.filter(item => item.id !== id));
  };

  const updateItem = (id: string, field: keyof ItemFormData, value: any) => {
    setItems(items.map(item => 
      item.id === id ? { ...item, [field]: value } : item
    ));
    // Clear error for this field
    delete errors[`${id}-${field}`];
    setErrors({ ...errors });
  };

  const toggleSize = (itemId: string, size: string) => {
    const item = items.find(i => i.id === itemId);
    if (!item) return;

    const newSizes = item.sizes.includes(size)
      ? item.sizes.filter(s => s !== size)
      : [...item.sizes, size];
    
    updateItem(itemId, 'sizes', newSizes);
  };

  const validate = (): boolean => {
    const newErrors: { [key: string]: string } = {};

    // Validate category name
    if (!categoryName.trim()) {
      newErrors.categoryName = 'Category name is required';
    }

    // Validate items
    items.forEach((item) => {
      if (!item.name.trim()) {
        newErrors[`${item.id}-name`] = 'Item name is required';
      }
    });

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSave = () => {
    if (!validate()) {
      toast.error('Please fix validation errors');
      return;
    }

    const now = Date.now();

    if (editingCategoryId) {
      // Update existing category
      updateCategory(editingCategoryId, {
        name: categoryName,
        updatedAt: now
      });

      // Update items (remove old ones and add new ones)
      // For simplicity on Day 1, we'll just add new items
      // A full implementation would handle updates/deletes properly
      items
        .filter(item => !item.id.startsWith('temp-'))
        .map(item => ({
          ...item,
          categoryId: editingCategoryId,
          isActive: true,
          createdAt: now,
          updatedAt: now
        }));
      
      toast.success('Category updated successfully!');
    } else {
      // Create new category
      const categoryId = `cat-${Date.now()}-${Math.random().toString(36).substring(7)}`;
      
      addCategory({
        id: categoryId,
        name: categoryName,
        isActive: true,
        createdAt: now,
        updatedAt: now
      });

      // Add items
      const newItems = items.map((item, index) => ({
        id: `item-${Date.now()}-${index}-${Math.random().toString(36).substring(7)}`,
        categoryId,
        name: item.name,
        sizes: item.sizes,
        vegFlag: item.vegFlag,
        isActive: true,
        createdAt: now,
        updatedAt: now
      }));

      addItems(newItems);
      toast.success(`Category "${categoryName}" created with ${items.length} items!`);
    }

    navigate('/menu/summary');
  };

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          {editingCategoryId ? 'Edit Category' : 'Create Category'}
        </h1>
        <p className="text-gray-600">
          {editingCategoryId 
            ? 'Update your category and items below.'
            : 'Create a new category and add items to it.'}
        </p>
      </div>

      {/* Category Name */}
      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <label htmlFor="categoryName" className="block text-lg font-semibold text-gray-900 mb-2">
          Category Name <span className="text-red-500">*</span>
        </label>
        <input
          id="categoryName"
          type="text"
          value={categoryName}
          onChange={(e) => {
            setCategoryName(e.target.value);
            delete errors.categoryName;
            setErrors({ ...errors });
          }}
          placeholder="e.g., Chinese, Indian, Desserts"
          className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-action-primary focus:border-transparent text-base ${
            errors.categoryName ? 'border-red-500' : 'border-gray-300'
          }`}
        />
        {errors.categoryName && (
          <p className="mt-1 text-sm text-red-600">{errors.categoryName}</p>
        )}
      </div>

      {/* Items */}
      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-gray-900">Items</h2>
          <Button variant="secondary" onClick={addItem} size="small">
            + Add Item
          </Button>
        </div>

        <div className="space-y-4">
          {items.map((item) => (
            <div key={item.id} className="border border-gray-200 rounded-lg p-4 relative">
              {/* Delete Button */}
              {items.length > 1 && (
                <button
                  onClick={() => removeItem(item.id)}
                  className="absolute top-2 right-2 text-red-500 hover:text-red-700 font-bold text-xl w-8 h-8 flex items-center justify-center rounded hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-red-500"
                  aria-label="Delete item"
                >
                  ×
                </button>
              )}

              <div className="grid md:grid-cols-2 gap-4">
                {/* Item Name */}
                <div className="md:col-span-2">
                  <label htmlFor={`item-name-${item.id}`} className="block text-sm font-medium text-gray-700 mb-1">
                    Item Name <span className="text-red-500">*</span>
                  </label>
                  <input
                    id={`item-name-${item.id}`}
                    type="text"
                    value={item.name}
                    onChange={(e) => updateItem(item.id, 'name', e.target.value)}
                    placeholder="e.g., Chicken Soup, Caesar Salad"
                    className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-action-primary focus:border-transparent text-base ${
                      errors[`${item.id}-name`] ? 'border-red-500' : 'border-gray-300'
                    }`}
                  />
                  {errors[`${item.id}-name`] && (
                    <p className="mt-1 text-sm text-red-600">{errors[`${item.id}-name`]}</p>
                  )}
                </div>

                {/* Sizes */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Available Sizes (optional)
                  </label>
                  <div className="flex gap-4">
                    <label className="flex items-center cursor-pointer">
                      <input
                        type="checkbox"
                        checked={item.sizes.includes('Small')}
                        onChange={() => toggleSize(item.id, 'Small')}
                        className="w-4 h-4 text-action-primary border-gray-300 rounded focus:ring-2 focus:ring-action-primary"
                      />
                      <span className="ml-2 text-sm text-gray-700">Small</span>
                    </label>
                    <label className="flex items-center cursor-pointer">
                      <input
                        type="checkbox"
                        checked={item.sizes.includes('Large')}
                        onChange={() => toggleSize(item.id, 'Large')}
                        className="w-4 h-4 text-action-primary border-gray-300 rounded focus:ring-2 focus:ring-action-primary"
                      />
                      <span className="ml-2 text-sm text-gray-700">Large</span>
                    </label>
                  </div>
                  <p className="mt-1 text-xs text-gray-500">Leave unchecked if no size variants</p>
                </div>

                {/* Veg/NonVeg */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Type <span className="text-red-500">*</span>
                  </label>
                  <div className="flex gap-4">
                    <label className="flex items-center cursor-pointer">
                      <input
                        type="radio"
                        checked={item.vegFlag === 'Veg'}
                        onChange={() => updateItem(item.id, 'vegFlag', 'Veg')}
                        className="w-4 h-4 text-green-600 border-gray-300 focus:ring-2 focus:ring-green-500"
                      />
                      <span className="ml-2 text-sm text-gray-700 flex items-center">
                        <span className="inline-block w-3 h-3 bg-green-500 rounded-full mr-1"></span>
                        Veg
                      </span>
                    </label>
                    <label className="flex items-center cursor-pointer">
                      <input
                        type="radio"
                        checked={item.vegFlag === 'NonVeg'}
                        onChange={() => updateItem(item.id, 'vegFlag', 'NonVeg')}
                        className="w-4 h-4 text-red-600 border-gray-300 focus:ring-2 focus:ring-red-500"
                      />
                      <span className="ml-2 text-sm text-gray-700 flex items-center">
                        <span className="inline-block w-3 h-3 bg-red-500 rounded-full mr-1"></span>
                        Non-Veg
                      </span>
                    </label>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Actions */}
      <div className="flex gap-3 justify-end">
        <Button
          variant="secondary"
          onClick={() => navigate('/menu')}
        >
          Cancel
        </Button>
        <Button
          variant="primary"
          onClick={handleSave}
          size="large"
        >
          {editingCategoryId ? 'Update Category' : 'Save Category'}
        </Button>
      </div>
    </div>
  );
};

