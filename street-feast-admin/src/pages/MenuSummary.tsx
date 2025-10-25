import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../components/Button';
import { Dialog } from '../components/Dialog';
import { useMenuStore, Category } from '../store/menuStore';
import { toast } from '../components/Toast';

export const MenuSummary: React.FC = () => {
  const navigate = useNavigate();
  const { categories, items, deleteCategory } = useMenuStore();
  
  const [viewingCategory, setViewingCategory] = useState<Category | null>(null);
  const [deletingCategory, setDeletingCategory] = useState<Category | null>(null);

  const getCategoryItemCount = (categoryId: string) => {
    return items.filter(item => item.categoryId === categoryId).length;
  };

  const getCategoryItems = (categoryId: string) => {
    return items.filter(item => item.categoryId === categoryId);
  };

  const handleEdit = (category: Category) => {
    navigate('/menu/create', { state: { category } });
  };

  const handleDelete = (category: Category) => {
    setDeletingCategory(category);
  };

  const confirmDelete = () => {
    if (deletingCategory) {
      deleteCategory(deletingCategory.id);
      toast.success(`Category "${deletingCategory.name}" deleted successfully`);
      setDeletingCategory(null);
    }
  };

  // Empty state
  if (categories.length === 0) {
    return (
      <div className="max-w-2xl mx-auto text-center py-12">
        <div className="text-6xl mb-4">📋</div>
        <h2 className="text-2xl font-bold text-gray-900 mb-2">No categories yet</h2>
        <p className="text-gray-600 mb-6">
          Create your first category to get started with your menu.
        </p>
        <div className="flex gap-3 justify-center">
          <Button variant="primary" size="large" onClick={() => navigate('/menu/upload')}>
            Upload CSV
          </Button>
          <Button variant="secondary" size="large" onClick={() => navigate('/menu/create')}>
            Create Manually
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Menu Summary</h1>
          <p className="text-gray-600">
            {categories.length} {categories.length === 1 ? 'category' : 'categories'} • {items.length} {items.length === 1 ? 'item' : 'items'}
          </p>
        </div>
        <Button variant="primary" onClick={() => navigate('/menu')}>
          + Add More
        </Button>
      </div>

      {/* Categories Grid */}
      <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4 mb-6">
        {categories.map(category => {
          const itemCount = getCategoryItemCount(category.id);
          return (
            <div key={category.id} className="bg-white rounded-lg shadow hover:shadow-lg transition-shadow border border-gray-200">
              <div className="p-5">
                <h3 className="text-xl font-bold text-gray-900 mb-1">{category.name}</h3>
                <p className="text-sm text-gray-600 mb-4">
                  {itemCount} {itemCount === 1 ? 'item' : 'items'}
                </p>
                
                <div className="flex gap-2">
                  <button
                    onClick={() => setViewingCategory(category)}
                    className="flex-1 px-3 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-gray-400"
                  >
                    View Items
                  </button>
                  <button
                    onClick={() => handleEdit(category)}
                    className="px-3 py-2 text-sm font-medium text-blue-700 bg-blue-50 hover:bg-blue-100 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-blue-400"
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => handleDelete(category)}
                    className="px-3 py-2 text-sm font-medium text-red-700 bg-red-50 hover:bg-red-100 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-red-400"
                  >
                    Delete
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Complete Setup Button */}
      <div className="bg-white rounded-lg shadow p-6 text-center">
        <p className="text-gray-700 mb-4">Ready to start taking orders?</p>
        <Button variant="primary" size="large" onClick={() => navigate('/menu')}>
          Complete Setup
        </Button>
      </div>

      {/* View Items Modal */}
      {viewingCategory && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div 
            className="absolute inset-0 bg-black bg-opacity-50"
            onClick={() => setViewingCategory(null)}
          />
          <div className="relative bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[80vh] overflow-hidden flex flex-col">
            <div className="px-6 py-4 border-b bg-gray-50 flex items-center justify-between">
              <h2 className="text-2xl font-bold text-gray-900">{viewingCategory.name}</h2>
              <button
                onClick={() => setViewingCategory(null)}
                className="text-gray-500 hover:text-gray-700 text-2xl font-bold w-8 h-8 flex items-center justify-center rounded hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-gray-400"
              >
                ×
              </button>
            </div>
            
            <div className="p-6 overflow-y-auto">
              {getCategoryItems(viewingCategory.id).length === 0 ? (
                <p className="text-center text-gray-500 py-8">No items in this category yet.</p>
              ) : (
                <div className="space-y-3">
                  {getCategoryItems(viewingCategory.id).map(item => (
                    <div key={item.id} className="border border-gray-200 rounded-lg p-4">
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <h4 className="font-semibold text-gray-900">{item.name}</h4>
                          <div className="flex items-center gap-3 mt-2 text-sm text-gray-600">
                            {item.sizes.length > 0 && (
                              <span className="inline-flex items-center">
                                <span className="font-medium">Sizes:</span>
                                <span className="ml-1">{item.sizes.join(', ')}</span>
                              </span>
                            )}
                            {item.sizes.length === 0 && (
                              <span className="text-gray-400">No sizes</span>
                            )}
                          </div>
                        </div>
                        <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${
                          item.vegFlag === 'Veg' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                        }`}>
                          {item.vegFlag === 'Veg' ? '🟢' : '🔴'} {item.vegFlag}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="px-6 py-4 border-t bg-gray-50">
              <Button variant="secondary" onClick={() => setViewingCategory(null)} className="w-full">
                Close
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Dialog */}
      <Dialog
        isOpen={!!deletingCategory}
        onClose={() => setDeletingCategory(null)}
        title="Delete Category"
        message={`Are you sure you want to delete "${deletingCategory?.name}"? This will delete the category and all its items. This action cannot be undone.`}
        confirmText="Delete"
        cancelText="Cancel"
        onConfirm={confirmDelete}
        confirmVariant="danger"
      />
    </div>
  );
};

