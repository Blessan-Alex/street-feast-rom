import React, { useState, useEffect, useMemo } from 'react';
import { useMenuStore, Item } from '../store/menuStore';
import { useOrdersStore } from '../store/ordersStore';
import { OrderSummaryCard } from '../components/OrderSummaryCard';
import { SizeSelector } from '../components/SizeSelector';

export const CreateOrder: React.FC = () => {
  const { categories, items, frequentItemIds } = useMenuStore();
  const { addDraftLine } = useOrdersStore();

  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [itemToAdd, setItemToAdd] = useState<Item | null>(null);

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

  const handleItemClick = (item: Item) => {
    if (item.sizes.length > 0) {
      // Show size selector
      setItemToAdd(item);
    } else {
      // Add directly
      addItemToOrder(item, null);
    }
  };

  const addItemToOrder = (item: Item, size: 'Small' | 'Large' | null) => {
    addDraftLine({
      id: crypto.randomUUID(),
      itemId: item.id,
      nameSnapshot: item.name,
      size,
      vegFlagSnapshot: item.vegFlag,
      qty: 1
    });
  };

  const getCategoryById = (id: string) => {
    return categories.find(c => c.id === id);
  };

  return (
    <div className="flex h-full">
      {/* Main Content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Search Bar */}
        <div className="px-6 py-4 bg-white border-b">
          <div className="max-w-2xl">
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search items or categories..."
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-action-primary text-base"
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
            <div className="flex gap-3 overflow-x-auto pb-2">
              {frequentItems.map(item => (
                <button
                  key={item.id}
                  onClick={() => handleItemClick(item)}
                  className="flex-shrink-0 w-32 p-3 bg-white border-2 border-gray-200 rounded-lg hover:border-action-primary hover:shadow-md transition-all focus:outline-none focus:ring-2 focus:ring-action-primary"
                >
                  <div className="text-sm font-medium text-gray-900 truncate">{item.name}</div>
                  <div className="mt-1">
                    <span className={`text-xs px-2 py-0.5 rounded ${
                      item.vegFlag === 'Veg' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                    }`}>
                      {item.vegFlag}
                    </span>
                  </div>
                  <div className="mt-2 text-action-primary text-xl font-bold">+</div>
                </button>
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
                      <button
                        key={item.id}
                        onClick={() => handleItemClick(item)}
                        className="p-4 bg-white border-2 border-gray-200 rounded-lg hover:border-action-primary hover:shadow-lg transition-all text-left focus:outline-none focus:ring-2 focus:ring-action-primary"
                      >
                        <div className="font-semibold text-gray-900">{item.name}</div>
                        <div className="text-xs text-gray-500 mt-1">{category?.name}</div>
                        <div className="flex items-center gap-2 mt-2">
                          <span className={`text-xs px-2 py-0.5 rounded ${
                            item.vegFlag === 'Veg' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                          }`}>
                            {item.vegFlag}
                          </span>
                          {item.sizes.length > 0 && (
                            <span className="text-xs text-gray-500">{item.sizes.join(', ')}</span>
                          )}
                        </div>
                      </button>
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
                  onClick={() => setSelectedCategory(null)}
                  className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg focus:outline-none focus:ring-2 focus:ring-gray-400"
                >
                  ← Back to Categories
                </button>
              </div>
              <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                {categoryItems.map(item => (
                  <button
                    key={item.id}
                    onClick={() => handleItemClick(item)}
                    className="p-4 bg-white border-2 border-gray-200 rounded-lg hover:border-action-primary hover:shadow-lg transition-all text-left focus:outline-none focus:ring-2 focus:ring-action-primary"
                  >
                    <div className="font-semibold text-gray-900">{item.name}</div>
                    <div className="flex items-center gap-2 mt-2">
                      <span className={`text-xs px-2 py-0.5 rounded ${
                        item.vegFlag === 'Veg' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                      }`}>
                        {item.vegFlag}
                      </span>
                      {item.sizes.length > 0 && (
                        <span className="text-xs text-gray-500">{item.sizes.join(', ')}</span>
                      )}
                    </div>
                  </button>
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
                        onClick={() => setSelectedCategory(category.id)}
                        className="p-6 bg-white border-2 border-gray-200 rounded-lg hover:border-action-primary hover:shadow-lg transition-all text-left focus:outline-none focus:ring-2 focus:ring-action-primary"
                      >
                        <div className="text-lg font-bold text-gray-900">{category.name}</div>
                        <div className="text-sm text-gray-500 mt-1">{itemCount} items</div>
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

      {/* Size Selector Modal */}
      {itemToAdd && (
        <SizeSelector
          item={itemToAdd}
          isOpen={!!itemToAdd}
          onClose={() => setItemToAdd(null)}
          onSelect={(size) => {
            addItemToOrder(itemToAdd, size);
            setItemToAdd(null);
          }}
        />
      )}
    </div>
  );
};

