import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useOrdersStore, OrderType } from '../store/ordersStore';
import { useMenuStore } from '../store/menuStore';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { toast } from './Toast';

export const OrderSummaryCard: React.FC = () => {
  const navigate = useNavigate();
  const { draft, setDraft, updateDraftLine, removeDraftLine, clearDraft, placeDraft } = useOrdersStore();
  const { items } = useMenuStore();
  const [showCancelDialog, setShowCancelDialog] = useState(false);
  const [showPlaceOrderDialog, setShowPlaceOrderDialog] = useState(false);

  const handleTypeChange = (type: OrderType) => {
    setDraft({ type });
  };

  const handlePlaceOrderClick = () => {
    setShowPlaceOrderDialog(true);
  };

  const handlePlaceOrder = () => {
    const result = placeDraft();
    
    if (!result.ok) {
      toast.error(result.error || 'Failed to place order');
      return;
    }

    toast.success(`Order #${result.order?.orderNumber} created successfully!`);
    navigate('/dashboard');
    setShowPlaceOrderDialog(false);
  };

  const handleCancelOrder = () => {
    clearDraft();
    setShowCancelDialog(false);
    toast.info('Order cleared');
  };

  const handleEditItem = (item: any) => {
    const menuItem = items.find(i => i.id === item.itemId);
    if (menuItem) {
      // Find the category for this item
      const { categories } = useMenuStore.getState();
      const category = categories.find(c => c.id === menuItem.categoryId);
      
      if (category) {
        // Navigate to menu/create with category data
        navigate('/menu/create', { 
          state: { 
            category: category,
            editingItem: menuItem
          } 
        });
      }
    }
  };


  const totalItems = draft.orderItems.reduce((sum, item) => sum + item.qty, 0);

  return (
    <div className="w-96 bg-white shadow-lg border-l h-full flex flex-col">
      {/* Header with item count badge and delete icon */}
      <div className="px-6 py-3 border-b bg-gray-50">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-gray-900">Order Summary</h2>
          <div className="flex items-center gap-2">
            <span className="bg-action-primary text-white text-xs px-2 py-1 rounded-full">
              {totalItems}
            </span>
            {totalItems > 0 && (
              <button
                onClick={() => setShowCancelDialog(true)}
                className="text-red-600 hover:text-red-800 p-1 rounded hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-red-500"
                aria-label="Clear order"
                title="Clear all items"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Order Items List */}
      <div className="flex-1 overflow-y-auto px-6 py-4 min-h-0">
        {draft.orderItems.length === 0 ? (
          <div className="text-center py-12">
            <p className="text-gray-500 mb-2">No items added yet</p>
            <p className="text-sm text-gray-400">Select items from menu to start</p>
          </div>
        ) : (
          <div className="space-y-2">
            {draft.orderItems.slice().reverse().map(item => (
              <div key={item.id} className="border border-gray-200 rounded-lg p-3 relative">
                {/* Delete cross icon in top-right corner */}
                <button
                  onClick={() => removeDraftLine(item.id)}
                  className="absolute top-2 right-2 text-red-600 hover:text-red-800 font-bold text-lg w-6 h-6 flex items-center justify-center rounded hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-red-500"
                  aria-label="Remove item"
                >
                  ×
                </button>
                
                {/* Item info */}
                <div className="pr-8">
                  <div className="font-medium text-gray-900 text-sm mb-1">{item.nameSnapshot}</div>
                  <div className="flex items-center gap-2 mb-2">
                    {item.size && (
                      <span className="text-xs text-gray-600 bg-gray-100 px-2 py-1 rounded">{item.size}</span>
                    )}
                    <span className={`text-xs px-2 py-1 rounded font-medium ${
                      item.vegFlagSnapshot === 'Veg'
                        ? 'bg-green-100 text-green-800'
                        : item.vegFlagSnapshot === 'NonVeg'
                          ? 'bg-red-100 text-red-800'
                          : 'bg-blue-100 text-blue-800'
                    }`}>
                      {item.vegFlagSnapshot}
                    </span>
                    <span className="text-xs text-gray-600 font-medium">Qty: {item.qty}</span>
                  </div>
                  
                  {/* Edit button */}
                  <button
                    onClick={() => handleEditItem(item)}
                    className="w-full px-3 py-1 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded text-xs font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-gray-400"
                  >
                    Edit
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Order Type - Horizontal Layout */}
      <div className="px-6 py-3 border-b">
        <div className="flex items-center justify-between">
          <label className="text-sm font-medium text-gray-700">Order Type</label>
          <select
            value={draft.type}
            onChange={(e) => handleTypeChange(e.target.value as OrderType)}
            className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-action-primary text-sm"
          >
            <option value="DineIn">Dine-in</option>
            <option value="Parcel">Parcel</option>
            <option value="Delivery">Delivery</option>
          </select>
        </div>
      </div>

      {/* Sticky Action Buttons */}
      <div className="px-6 py-3 bg-white border-t sticky bottom-0">
        <Button
          variant="primary"
          onClick={handlePlaceOrderClick}
          disabled={draft.orderItems.length === 0}
          className="w-full"
          size="medium"
        >
          Place Order ({totalItems} items)
        </Button>
      </div>

      {/* Place Order Confirmation Dialog */}
      <Dialog
        isOpen={showPlaceOrderDialog}
        onClose={() => setShowPlaceOrderDialog(false)}
        title="Place Order"
        message={`Ready to place this order with ${totalItems} items?`}
        confirmText="Yes, Place Order"
        cancelText="Cancel"
        onConfirm={handlePlaceOrder}
        confirmVariant="primary"
      />

      {/* Cancel Dialog */}
      <Dialog
        isOpen={showCancelDialog}
        onClose={() => setShowCancelDialog(false)}
        title="Clear Order"
        message="Are you sure you want to clear all items from this order?"
        confirmText="Yes, Clear All"
        cancelText="No, Keep"
        onConfirm={handleCancelOrder}
        confirmVariant="danger"
      />

    </div>
  );
};

