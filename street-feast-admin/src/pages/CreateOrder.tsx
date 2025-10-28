import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useMenuStore, Item } from '../store/menuStore';
import { useOrdersStore } from '../store/ordersStore';
import { OrderSummaryCard } from '../components/OrderSummaryCard';
import { ItemAdditionModal } from '../components/ItemAdditionModal';

export const CreateOrder: React.FC = () => {
  const { categories, items, frequentItemIds } = useMenuStore();
  const { addDraftLine, updateDraftLine } = useOrdersStore();
  const navigate = useNavigate();
  const location = useLocation();

  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [itemToAdd, setItemToAdd] = useState<Item | null>(null);
  const [editingOrderItem, setEditingOrderItem] = useState<any>(null);

  // Frequent items
  const frequentItems = useMemo(() => {
    return frequentItemIds
      .map(id => items.find(item => item.id === id))
      .filter(item => item !== undefined) as Item[];
  }, [frequentItemIds, items]);

  // Filtered items based on search
  const filteredItems = useMemo(() => {
    if (!searchTerm) return items;
    
    const term = searchTerm.toLowerCase();
    return items.filter(item => {
      const category = categories.find(c => c.id === item.categoryId);
      return (
        item.name.toLowerCase().includes(term) ||
        category?.name.toLowerCase().includes(term)
      );
    });
  }, [searchTerm, items, categories]);

  // Items for selected category
  const categoryItems = useMemo(() => {
    if (!selectedCategory) return [];
    return items.filter(item => item.categoryId === selectedCategory);
  }, [selectedCategory, items]);

  // Debounce search
  useEffect(() => {
    const timer = setTimeout(() => {
      // Search is already handled by filteredItems memo
    }, 250);
    return () => clearTimeout(timer);
  }, [searchTerm]);

  // Handle editing order items from navigation state
  useEffect(() => {
    const editingItem = location.state?.editingOrderItem;
    if (editingItem) {
      setEditingOrderItem(editingItem);
      // Find the menu item and set it for editing
      const menuItem = items.find(i => i.id === editingItem.itemId);
      if (menuItem) {
        setItemToAdd(menuItem);
      }
    }
  }, [location.state, items]);

  const handleItemClick = (item: Item) => {
    setItemToAdd(item);
  };

  const handleAddItem = (data: { size: string | null; chefTip: string; quantity: number }) => {
    if (!itemToAdd) return;
    
    if (editingOrderItem) {
      // Update existing order item
      updateDraftLine(editingOrderItem.id, {
        size: data.size,
        qty: data.quantity,
        chefTip: data.chefTip
      });
      setEditingOrderItem(null);
    } else {
      // Add new order item
      addDraftLine({
        id: crypto.randomUUID(),
        itemId: itemToAdd.id,
        nameSnapshot: itemToAdd.name,
        size: data.size,
        vegFlagSnapshot: itemToAdd.vegFlag,
        qty: data.quantity,
        chefTip: data.chefTip
      });
    }
    
    setItemToAdd(null);
  };

  const getCategoryById = (id: string) => {
    return categories.find(c => c.id === id);
  };

  const handleSelectCategory = (categoryId: string) => {
    setSelectedCategory(categoryId);
    const category = getCategoryById(categoryId);
    if (category) {
      navigate('/create-order', { state: { categoryName: category.name } });
    }
  };

  const handleBackToCategories = () => {
    setSelectedCategory(null);
    // Clear breadcrumb state
    navigate('/create-order', { replace: true });
  };

  return (
    <div className="flex h-full">
      {/* Main Content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Search Bar */}
        <div className="px-6 py-4 bg-white border-b">
          <div className="max-w-2xl relative">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <svg className="h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search items or categories..."
              className="w-full pl-10 pr-10 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-action-primary text-base"
            />
            {searchTerm && (
              <button
                onClick={() => setSearchTerm('')}
                className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
              >
                ×
              </button>
            )}
          </div>
        </div>

        {/* Frequent Bought Strip */}
        {frequentItems.length > 0 && !searchTerm && (
          <div className="px-6 py-4 bg-gray-50 border-b">
            <h3 className="text-sm font-semibold text-gray-700 mb-3">Frequent Items</h3>
            <div className="flex gap-2 overflow-x-auto pb-2">
              {frequentItems.map(item => (
                <div
                  key={item.id}
                  className="flex-shrink-0 w-48 h-16 p-2 bg-white border-2 border-gray-200 rounded-lg hover:border-action-primary hover:shadow-md transition-all flex items-center justify-between"
                >
                  <div className="flex-1 min-w-0">
                    <div className="text-sm font-medium text-gray-900 truncate">{item.name}</div>
                    <span className={`text-xs px-1.5 py-0.5 rounded ${
                      item.vegFlag === 'Veg' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                    }`}>
                      {item.vegFlag}
                    </span>
                  </div>
                  <button
                    onClick={() => handleItemClick(item)}
                    className="ml-2 w-8 h-8 flex items-center justify-center bg-action-primary text-white rounded-full hover:bg-action-primary/90 transition-colors focus:outline-none focus:ring-2 focus:ring-action-primary"
                    aria-label="Add item"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                    </svg>
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Content Area */}
        <div className="flex-1 overflow-y-auto p-6">
          {searchTerm ? (
            /* Search Results */
            <div>
              <h2 className="text-xl font-bold text-gray-900 mb-4">
                Search Results ({filteredItems.length})
              </h2>
              {filteredItems.length === 0 ? (
                <div className="text-center py-12">
                  <p className="text-gray-500">No items found matching "{searchTerm}"</p>
                </div>
              ) : (
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                  {filteredItems.map(item => {
                    const category = getCategoryById(item.categoryId);
                    return (
                      <div
                        key={item.id}
                        className="relative p-4 bg-white border-2 border-gray-200 rounded-lg hover:border-action-primary hover:shadow-lg transition-all"
                      >
                        <div className="font-semibold text-gray-900 mb-2">{item.name}</div>
                        <div className="text-xs text-gray-500 mb-2">{category?.name}</div>
                        <div className="flex items-center gap-2 mb-3">
                          <span className={`text-xs px-2 py-0.5 rounded ${
                            item.vegFlag === 'Veg' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                          }`}>
                            {item.vegFlag}
                          </span>
                          {item.sizes.length > 0 && (
                            <span className="text-xs text-gray-500">{item.sizes.join(', ')}</span>
                          )}
                        </div>
                        <button
                          onClick={() => handleItemClick(item)}
                          className="absolute bottom-3 right-3 w-8 h-8 flex items-center justify-center bg-action-primary text-white rounded-full hover:bg-action-primary/90 transition-colors focus:outline-none focus:ring-2 focus:ring-action-primary shadow-md"
                          aria-label="Add item"
                        >
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                          </svg>
                        </button>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          ) : selectedCategory ? (
            /* Category Items View */
            <div>
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-xl font-bold text-gray-900">
                  {getCategoryById(selectedCategory)?.name}
                </h2>
                <button
                  onClick={handleBackToCategories}
                  className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg focus:outline-none focus:ring-2 focus:ring-gray-400"
                >
                  ← Back to Categories
                </button>
              </div>
              <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                {categoryItems.map(item => (
                  <div
                    key={item.id}
                    className="relative p-4 bg-white border-2 border-gray-200 rounded-lg hover:border-action-primary hover:shadow-lg transition-all"
                  >
                    <div className="font-semibold text-gray-900 mb-2">{item.name}</div>
                    <div className="flex items-center gap-2 mb-3">
                      <span className={`text-xs px-2 py-0.5 rounded ${
                        item.vegFlag === 'Veg' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                      }`}>
                        {item.vegFlag}
                      </span>
                      {item.sizes.length > 0 && (
                        <span className="text-xs text-gray-500">{item.sizes.join(', ')}</span>
                      )}
                    </div>
                    <button
                      onClick={() => handleItemClick(item)}
                      className="absolute bottom-3 right-3 w-8 h-8 flex items-center justify-center bg-action-primary text-white rounded-full hover:bg-action-primary/90 transition-colors focus:outline-none focus:ring-2 focus:ring-action-primary shadow-md"
                      aria-label="Add item"
                    >
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                      </svg>
                    </button>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            /* Categories Grid */
            <div>
              <h2 className="text-xl font-bold text-gray-900 mb-4">Categories</h2>
              {categories.length === 0 ? (
                <div className="text-center py-12">
                  <p className="text-gray-500">No categories available</p>
                  <p className="text-sm text-gray-400 mt-1">Create your menu first</p>
                </div>
              ) : (
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                  {categories.map(category => {
                    const itemCount = items.filter(i => i.categoryId === category.id).length;
                    return (
                      <button
                        key={category.id}
                        onClick={() => handleSelectCategory(category.id)}
                        className="relative p-5 bg-white border-2 border-gray-200 rounded-lg hover:border-action-primary hover:shadow-md transition-all text-left focus:outline-none focus:ring-2 focus:ring-action-primary flex flex-col justify-between min-h-[100px]"
                      >
                        <div className="absolute top-3 right-3 w-6 h-6 bg-blue-200 text-blue-800 text-xs font-bold rounded-full flex items-center justify-center">
                          {itemCount}
                        </div>
                        <div className="flex-1 min-w-0 pr-8">
                          <div className="text-base font-medium text-gray-900 leading-tight">{category.name}</div>
                        </div>
                        <div className="flex justify-end mt-3">
                          <svg className="w-5 h-5 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                          </svg>
                        </div>
                      </button>
                    );
                  })}
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Right Sidebar - Order Summary */}
      <OrderSummaryCard />

      {/* Item Addition Modal */}
      {itemToAdd && (
        <ItemAdditionModal
          item={itemToAdd}
          isOpen={!!itemToAdd}
          onClose={() => {
            setItemToAdd(null);
            setEditingOrderItem(null);
          }}
          onAdd={handleAddItem}
          editData={editingOrderItem ? {
            size: editingOrderItem.size,
            chefTip: editingOrderItem.chefTip || '',
            quantity: editingOrderItem.qty
          } : undefined}
        />
      )}
    </div>
  );
};

