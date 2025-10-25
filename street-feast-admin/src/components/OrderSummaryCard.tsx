import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useOrdersStore, OrderType } from '../store/ordersStore';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { toast } from './Toast';

export const OrderSummaryCard: React.FC = () => {
  const navigate = useNavigate();
  const { draft, setDraft, updateDraftLine, removeDraftLine, clearDraft, placeDraft } = useOrdersStore();
  const [showCancelDialog, setShowCancelDialog] = useState(false);

  const handleQtyChange = (id: string, delta: number) => {
    const line = draft.orderItems.find(l => l.id === id);
    if (!line) return;
    
    const newQty = Math.max(1, line.qty + delta);
    updateDraftLine(id, { qty: newQty });
  };

  const handleTypeChange = (type: OrderType) => {
    setDraft({ type });
  };

  const handlePlaceOrder = () => {
    const result = placeDraft();
    
    if (!result.ok) {
      toast.error(result.error || 'Failed to place order');
      return;
    }

    toast.success(`Order #${result.order?.orderNumber} created successfully!`);
    navigate('/dashboard');
  };

  const handleCancelOrder = () => {
    clearDraft();
    setShowCancelDialog(false);
    toast.info('Order cancelled');
  };

  const totalItems = draft.orderItems.reduce((sum, item) => sum + item.qty, 0);

  return (
    <div className="w-96 bg-white shadow-lg border-l h-full flex flex-col">
      {/* Header */}
      <div className="px-6 py-4 border-b">
        <h2 className="text-xl font-bold text-gray-900">Order Summary</h2>
        <p className="text-sm text-gray-600">{totalItems} {totalItems === 1 ? 'item' : 'items'}</p>
      </div>

      {/* Order Items List */}
      <div className="flex-1 overflow-y-auto px-6 py-4">
        {draft.orderItems.length === 0 ? (
          <div className="text-center py-12">
            <p className="text-gray-500 mb-2">No items added yet</p>
            <p className="text-sm text-gray-400">Select items from menu to start</p>
          </div>
        ) : (
          <div className="space-y-3">
            {draft.orderItems.map(item => (
              <div key={item.id} className="border border-gray-200 rounded-lg p-3 relative">
                {/* Remove button */}
                <button
                  onClick={() => removeDraftLine(item.id)}
                  className="absolute top-2 right-2 text-red-600 hover:text-red-800 font-bold text-lg w-6 h-6 flex items-center justify-center rounded hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-red-500"
                  aria-label="Remove item"
                >
                  ×
                </button>

                {/* Item info */}
                <div className="pr-8">
                  <div className="font-medium text-gray-900">{item.nameSnapshot}</div>
                  <div className="flex items-center gap-2 mt-1">
                    {item.size && (
                      <span className="text-xs text-gray-600">{item.size}</span>
                    )}
                    <span className={`text-xs px-2 py-0.5 rounded font-medium ${
                      item.vegFlagSnapshot === 'Veg'
                        ? 'bg-green-100 text-green-800'
                        : 'bg-red-100 text-red-800'
                    }`}>
                      {item.vegFlagSnapshot}
                    </span>
                  </div>
                </div>

                {/* Qty controls */}
                <div className="flex items-center gap-2 mt-3">
                  <button
                    onClick={() => handleQtyChange(item.id, -1)}
                    disabled={item.qty <= 1}
                    className="w-8 h-8 flex items-center justify-center border border-gray-300 rounded hover:bg-gray-100 disabled:opacity-30 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-action-primary"
                  >
                    −
                  </button>
                  <span className="w-12 text-center font-semibold text-gray-900">{item.qty}</span>
                  <button
                    onClick={() => handleQtyChange(item.id, 1)}
                    className="w-8 h-8 flex items-center justify-center border border-gray-300 rounded hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-action-primary"
                  >
                    +
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Chef Tip */}
      <div className="px-6 py-4 border-t border-b">
        <label htmlFor="chefTip" className="block text-sm font-medium text-gray-700 mb-2">
          Chef Tip (Optional)
        </label>
        <textarea
          id="chefTip"
          value={draft.chefTip}
          onChange={(e) => setDraft({ chefTip: e.target.value })}
          placeholder="Special instructions for the chef..."
          rows={3}
          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-action-primary text-sm"
        />
      </div>

      {/* Order Type */}
      <div className="px-6 py-4 border-b">
        <label className="block text-sm font-medium text-gray-700 mb-2">Order Type</label>
        <div className="space-y-2">
          {(['DineIn', 'Parcel', 'Delivery'] as OrderType[]).map(type => (
            <label key={type} className="flex items-center cursor-pointer">
              <input
                type="radio"
                checked={draft.type === type}
                onChange={() => handleTypeChange(type)}
                className="w-4 h-4 text-action-primary border-gray-300 focus:ring-2 focus:ring-action-primary"
              />
              <span className="ml-2 text-sm text-gray-900">
                {type === 'DineIn' ? 'Dine-in' : type}
              </span>
            </label>
          ))}
        </div>
      </div>

      {/* Action Buttons */}
      <div className="px-6 py-4 space-y-2">
        <Button
          variant="primary"
          onClick={handlePlaceOrder}
          disabled={draft.orderItems.length === 0}
          className="w-full"
          size="large"
        >
          Place Order
        </Button>
        <Button
          variant="danger"
          onClick={() => setShowCancelDialog(true)}
          className="w-full"
        >
          Cancel Order
        </Button>
      </div>

      {/* Cancel Dialog */}
      <Dialog
        isOpen={showCancelDialog}
        onClose={() => setShowCancelDialog(false)}
        title="Cancel Order"
        message="Are you sure you want to cancel this order? All items will be removed."
        confirmText="Yes, Cancel"
        cancelText="No, Keep"
        onConfirm={handleCancelOrder}
        confirmVariant="danger"
      />
    </div>
  );
};

