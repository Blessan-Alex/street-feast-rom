import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../components/Button';
import { Dialog } from '../components/Dialog';
import { MenuActionModal } from '../components/MenuActionModal';
import { useMenuStore, Category } from '../store/menuStore';
import { toast } from '../components/Toast';

export const MenuSummary: React.FC = () => {
  const navigate = useNavigate();
  const { categories, items, deleteCategory } = useMenuStore();
  
  const [viewingCategory, setViewingCategory] = useState<Category | null>(null);
  const [deletingCategory, setDeletingCategory] = useState<Category | null>(null);
  const [selectedCategories, setSelectedCategories] = useState<string[]>([]);
  const [showAddModal, setShowAddModal] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showUploadOptions, setShowUploadOptions] = useState(false);

  const getCategoryItemCount = (categoryId: string) => {
    return items.filter(item => item.categoryId === categoryId).length;
  };

  const getCategoryItems = (categoryId: string) => {
    return items.filter(item => item.categoryId === categoryId);
  };

  const handleEdit = (category: Category) => {
    navigate('/menu/create', { 
      state: { 
        category: category 
      } 
    });
  };

  const handleDelete = (category: Category) => {
    setDeletingCategory(category);
  };

  const handleAddCategory = () => {
    setShowAddModal(true);
  };

  const handleUploadMenu = () => {
    setShowUploadOptions(true);
  };

  const handleBulkDelete = () => {
    if (selectedCategories.length === 0) {
      toast.error('Please select categories to delete');
      return;
    }
    setShowDeleteModal(true);
  };

  const confirmAddCategory = () => {
    setShowAddModal(false);
    toast.success('Category and item created successfully!');
  };


  const confirmDeleteCategory = () => {
    if (deletingCategory) {
      deleteCategory(deletingCategory.id);
      toast.success(`Category "${deletingCategory.name}" deleted successfully`);
      setDeletingCategory(null);
      setSelectedCategories(selectedCategories.filter(id => id !== deletingCategory.id));
    }
  };

  const confirmBulkDelete = () => {
    if (selectedCategories.length === 0) {
      setShowDeleteModal(false);
      return;
    }
    const ids = [...selectedCategories];
    ids.forEach(id => deleteCategory(id));
    toast.success(`Deleted ${ids.length} ${ids.length === 1 ? 'category' : 'categories'}`);
    setSelectedCategories([]);
    setShowDeleteModal(false);
  };

  const toggleCategorySelection = (categoryId: string) => {
    setSelectedCategories(prev => 
      prev.includes(categoryId) 
        ? prev.filter(id => id !== categoryId)
        : [...prev, categoryId]
    );
  };

  const handleSaveChanges = () => {
    // In a real app, this would save changes to the server
    toast.success('Changes saved successfully!');
  };

  // Empty state
  if (categories.length === 0) {
    return (
      <div className="max-w-2xl mx-auto text-center py-12">
        <div className="text-6xl mb-4 text-gray-300">MENU</div>
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
    <div className="flex h-full">
      {/* Main Content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Header with buttons */}
        <div className="px-6 py-4 bg-white border-b">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900 mb-1">Menu Summary</h1>
              <p className="text-gray-600">
                {categories.length} {categories.length === 1 ? 'category' : 'categories'} • {items.length} {items.length === 1 ? 'item' : 'items'}
              </p>
            </div>
            <div className="flex gap-3">
              <Button variant="secondary" onClick={handleUploadMenu}>
                Upload Menu
              </Button>
              <Button variant="primary" onClick={handleAddCategory}>
                + Add More
              </Button>
            </div>
          </div>
        </div>

        {/* Content Area */}
        <div className="flex-1 overflow-y-auto p-6">
          {/* Categories Grid */}
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {categories.map(category => {
              const itemCount = getCategoryItemCount(category.id);
              const isSelected = selectedCategories.includes(category.id);
              return (
                <div 
                  key={category.id} 
                  className={`p-4 bg-white border-2 rounded-lg hover:border-action-primary hover:shadow-lg transition-all cursor-pointer ${
                    isSelected ? 'border-action-primary bg-action-primary/5' : 'border-gray-200'
                  }`}
                  onClick={() => toggleCategorySelection(category.id)}
                >
                  <div className="text-lg font-bold text-gray-900 mb-1">{category.name}</div>
                  <div className="text-sm text-gray-500 mb-3">{itemCount} items</div>
                  
                  <div className="flex gap-2">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setViewingCategory(category);
                      }}
                      className="flex-1 px-2 py-1 text-xs font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded transition-colors focus:outline-none focus:ring-2 focus:ring-gray-400"
                    >
                      View
                    </button>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleEdit(category);
                      }}
                      className="px-2 py-1 text-xs font-medium text-blue-700 bg-blue-50 hover:bg-blue-100 rounded transition-colors focus:outline-none focus:ring-2 focus:ring-blue-400"
                    >
                      Edit
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Bottom Action Bar */}
        <div className="px-6 py-4 bg-white border-t flex items-center justify-between">
          <Button 
            variant="danger" 
            onClick={handleBulkDelete}
            disabled={selectedCategories.length === 0}
            size="small"
          >
            Delete Categories ({selectedCategories.length})
          </Button>
          
          <Button 
            variant="primary" 
            onClick={handleSaveChanges}
            size="small"
          >
            Save Changes
          </Button>
        </div>
      </div>

      {/* Upload Options Modal */}
      {showUploadOptions && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div 
            className="absolute inset-0 bg-black bg-opacity-50 backdrop-blur-sm"
            onClick={() => setShowUploadOptions(false)}
          />
          <div className="relative bg-white rounded-lg shadow-xl max-w-4xl w-full mx-4 p-8">
            <div className="text-center mb-8">
              <h2 className="text-3xl font-bold text-gray-900 mb-2">Add Menu Items</h2>
              <p className="text-gray-600">
                Choose how you'd like to add items to your menu
              </p>
            </div>
            
            <div className="grid md:grid-cols-2 gap-8">
              {/* Import File Card */}
              <div 
                onClick={() => {
                  setShowUploadOptions(false);
                  navigate('/menu/upload');
                }}
                className="bg-gradient-to-br from-blue-50 to-blue-100 rounded-xl p-8 cursor-pointer hover:shadow-lg transition-all border-2 border-transparent hover:border-blue-300 group"
              >
                <div className="text-center">
                  <div className="w-16 h-16 bg-blue-500 rounded-full flex items-center justify-center mx-auto mb-4 group-hover:scale-110 transition-transform">
                    <svg className="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                    </svg>
                  </div>
                  <h3 className="text-xl font-bold text-gray-900 mb-3">Import File</h3>
                  <p className="text-gray-600 mb-6">
                    Upload a CSV file with your menu items. Perfect for bulk imports or migrating from other systems.
                  </p>
                  <div className="inline-flex items-center px-6 py-3 bg-blue-500 text-white rounded-lg font-medium group-hover:bg-blue-600 transition-colors">
                    <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                    </svg>
                    Upload CSV
                  </div>
                </div>
              </div>

              {/* Manual Creation Card */}
              <div 
                onClick={() => {
                  setShowUploadOptions(false);
                  navigate('/menu/create');
                }}
                className="bg-gradient-to-br from-green-50 to-green-100 rounded-xl p-8 cursor-pointer hover:shadow-lg transition-all border-2 border-transparent hover:border-green-300 group"
              >
                <div className="text-center">
                  <div className="w-16 h-16 bg-green-500 rounded-full flex items-center justify-center mx-auto mb-4 group-hover:scale-110 transition-transform">
                    <svg className="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                    </svg>
                  </div>
                  <h3 className="text-xl font-bold text-gray-900 mb-3">Create Manually</h3>
                  <p className="text-gray-600 mb-6">
                    Use our interactive wizard to create categories and items step by step. Great for detailed customization.
                  </p>
                  <div className="inline-flex items-center px-6 py-3 bg-green-500 text-white rounded-lg font-medium group-hover:bg-green-600 transition-colors">
                    <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                    </svg>
                    Start Creating
                  </div>
                </div>
              </div>
            </div>

            <div className="text-center mt-8">
              <button
                onClick={() => setShowUploadOptions(false)}
                className="px-6 py-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-gray-400"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Floating Modals */}
      <MenuActionModal
        isOpen={showAddModal}
        onClose={() => setShowAddModal(false)}
        action="add"
        onConfirm={confirmAddCategory}
        title="Add New Category"
        message="Create a new category to organize your menu items."
      />


      <MenuActionModal
        isOpen={showDeleteModal}
        onClose={() => setShowDeleteModal(false)}
        action="delete"
        onConfirm={confirmBulkDelete}
        title="Delete Categories"
        message={`Are you sure you want to delete ${selectedCategories.length} selected category(ies)? This will also delete all items in those categories.`}
      />

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
                            {item.flavors && (
                              <span className="inline-flex items-center">
                                <span className="font-medium">Flavors:</span>
                                <span className="ml-1">{item.flavors}</span>
                              </span>
                            )}
                          </div>
                        </div>
                        <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${
                          item.vegFlag === 'Veg' ? 'bg-green-100 text-green-800' : 
                          item.vegFlag === 'NonVeg' ? 'bg-red-100 text-red-800' : 
                          'bg-blue-100 text-blue-800'
                        }`}>
                          {item.vegFlag}
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
        onConfirm={confirmDeleteCategory}
        confirmVariant="danger"
      />
    </div>
  );
};

