import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useOrdersStore, OrderType } from '../store/ordersStore';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { toast } from './Toast';
import { TableSelector } from './TableSelector';
import { LicensePlateInput } from './LicensePlateInput';
import { getOccupiedTables, getStoreId, supabase } from '../utils/supabase';

// Helper function to format table number
const formatTableNumber = (num: number): string => {
  return `Table ${String(num).padStart(2, '0')}`;
};

export const OrderSummaryCard: React.FC = () => {
  const navigate = useNavigate();
  const { draft, setDraft, removeDraftLine, clearDraft, placeDraft } = useOrdersStore();
  const [showCancelDialog, setShowCancelDialog] = useState(false);
  const [showPlaceOrderDialog, setShowPlaceOrderDialog] = useState(false);
  const [isPlacingOrder, setIsPlacingOrder] = useState(false);
  const [occupiedTables, setOccupiedTables] = useState<number[]>([]);
  const [isLoadingTables, setIsLoadingTables] = useState(false);
  const [isRefreshingTables, setIsRefreshingTables] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  const [isTableSelectorExpanded, setIsTableSelectorExpanded] = useState(true);

  // Fetch occupied tables when order type is DineIn
  useEffect(() => {
    const fetchOccupiedTables = async () => {
      if (draft.type === 'DineIn') {
        setIsLoadingTables(true);
        try {
          const storeId = await getStoreId();
          const occupied = await getOccupiedTables(storeId);
          setOccupiedTables(occupied);
        } catch (error) {
          console.error('Error fetching occupied tables:', error);
          setOccupiedTables([]);
        } finally {
          setIsLoadingTables(false);
        }
      } else {
        setOccupiedTables([]);
      }
    };

    fetchOccupiedTables();
  }, [draft.type]);

  // Real-time subscription to refresh occupied tables when orders change
  useEffect(() => {
    if (draft.type !== 'DineIn') return;

    const channel = supabase
      .channel('table-status-updates')
      .on(
        'postgres_changes',
        {
          event: '*',
          schema: 'public',
          table: 'orders',
        },
        async (payload) => {
          console.log('[OrderSummaryCard] Order change detected, refreshing occupied tables:', payload.eventType);
          try {
            const storeId = await getStoreId();
            const occupied = await getOccupiedTables(storeId);
            setOccupiedTables(occupied);
          } catch (error) {
            console.error('[OrderSummaryCard] Error refreshing occupied tables:', error);
          }
        }
      )
      .subscribe((status) => {
        console.log('[OrderSummaryCard] Table status subscription status:', status);
      });

    return () => {
      console.log('[OrderSummaryCard] Cleaning up table status subscription');
      supabase.removeChannel(channel);
    };
  }, [draft.type]);

  // Clear table/license when order type changes
  useEffect(() => {
    if (draft.type !== 'DineIn' && draft.tableNumber !== undefined) {
      setDraft({ tableNumber: undefined });
    }
    if (draft.type !== 'EatAway' && draft.licensePlate !== undefined) {
      setDraft({ licensePlate: undefined });
    }
  }, [draft.type, setDraft]);

  // Handle manual table refresh
  const handleRefreshTables = async () => {
    if (draft.type !== 'DineIn') return;
    
    setIsRefreshingTables(true);
    try {
      const storeId = await getStoreId();
      const occupied = await getOccupiedTables(storeId);
      setOccupiedTables(occupied);
      toast.success('Table status refreshed');
    } catch (error) {
      console.error('Error refreshing tables:', error);
      toast.error('Failed to refresh table status');
    } finally {
      setIsRefreshingTables(false);
    }
  };

  // Auto-expand when table is selected, auto-collapse when table changes
  useEffect(() => {
    if (draft.type === 'DineIn' && draft.tableNumber) {
      setIsExpanded(true);
      setIsTableSelectorExpanded(false); // Collapse table selector when table is selected
    } else if (draft.type !== 'DineIn' || !draft.tableNumber) {
      setIsExpanded(false);
      setIsTableSelectorExpanded(true); // Expand table selector when no table is selected
    }
  }, [draft.type, draft.tableNumber]);

  const handleTypeChange = (type: OrderType) => {
    // Clear table/license when switching types
    setDraft({ 
      type,
      tableNumber: undefined,
      licensePlate: undefined
    });
  };

  const handlePlaceOrderClick = () => {
    setShowPlaceOrderDialog(true);
  };

  const handlePlaceOrder = async () => {
    setIsPlacingOrder(true);
    try {
      const result = await placeDraft();
    
    if (!result.ok) {
      toast.error(result.error || 'Failed to place order');
      return;
    }

    toast.success(`Order #${result.order?.orderNumber} created successfully!`);
    navigate('/dashboard');
    setShowPlaceOrderDialog(false);
    } catch (error) {
      console.error('Error placing order:', error);
      toast.error('An unexpected error occurred');
    } finally {
      setIsPlacingOrder(false);
    }
  };

  const handleCancelOrder = () => {
    clearDraft();
    setShowCancelDialog(false);
    toast.info('Order cleared');
  };

  const handleEditItem = (item: any) => {
    // Navigate to create-order page with the order item data for editing
    navigate('/create-order', { 
      state: { 
        editingOrderItem: item
      } 
    });
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

      {/* Order Items List - Collapsible when table is selected */}
      <div
        className={`flex-1 overflow-y-auto px-6 py-4 min-h-0 transition-all duration-300 ease-in-out ${
          draft.type === 'DineIn' && draft.tableNumber && !isExpanded
            ? 'max-h-0 overflow-hidden opacity-0'
            : 'max-h-full opacity-100'
        }`}
      >
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
                  
                  {/* Chef tip display */}
                  {item.chefTip && (
                    <div className="mb-2">
                      <span className="text-xs text-gray-500 italic">
                        💡 {item.chefTip.length > 30 ? `${item.chefTip.substring(0, 30)}...` : item.chefTip}
                      </span>
                    </div>
                  )}
                  
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
            <option value="EatAway">Eat Away</option>
          </select>
        </div>
      </div>

      {/* Table/License Selection Section */}
      <div className="px-6 py-3 border-b">
        {draft.type === 'DineIn' && (
          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="text-sm font-medium text-gray-700">Select Table</label>
              {draft.tableNumber ? (
                <button
                  onClick={() => setIsTableSelectorExpanded(!isTableSelectorExpanded)}
                  className="text-sm text-action-primary hover:text-action-primary-dark focus:outline-none focus:ring-2 focus:ring-action-primary rounded p-1 transition-transform"
                  aria-label={isTableSelectorExpanded ? 'Hide table selector' : 'Show table selector'}
                  title={isTableSelectorExpanded ? 'Hide table selector' : 'Show table selector'}
                >
                  <svg
                    className={`w-5 h-5 transition-transform duration-300 ${isTableSelectorExpanded ? 'rotate-180' : ''}`}
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                  </svg>
                </button>
              ) : (
              <button
                onClick={handleRefreshTables}
                disabled={isRefreshingTables || isLoadingTables}
                className="text-sm text-action-primary hover:text-action-primary-dark focus:outline-none focus:ring-2 focus:ring-action-primary rounded p-1 disabled:opacity-50 disabled:cursor-not-allowed transition-transform"
                aria-label="Refresh table status"
                title="Refresh table status"
              >
                <svg
                  className={`w-5 h-5 transition-transform duration-300 ${isRefreshingTables ? 'animate-spin' : ''}`}
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
              </button>
              )}
            </div>
            
            {/* Collapsible Table Selector */}
            <div
              className={`overflow-hidden transition-all duration-300 ease-in-out ${
                isTableSelectorExpanded ? 'max-h-[500px] opacity-100' : 'max-h-0 opacity-0'
              }`}
            >
            {isLoadingTables ? (
              <div className="text-center py-4">
                <p className="text-sm text-gray-500">Loading tables...</p>
              </div>
            ) : (
                <div className="mb-2">
                  {!draft.tableNumber && (
                    <button
                      onClick={handleRefreshTables}
                      disabled={isRefreshingTables || isLoadingTables}
                      className="text-sm text-action-primary hover:text-action-primary-dark focus:outline-none focus:ring-2 focus:ring-action-primary rounded p-1 disabled:opacity-50 disabled:cursor-not-allowed transition-transform mb-2"
                      aria-label="Refresh table status"
                      title="Refresh table status"
                    >
                      <svg
                        className={`w-5 h-5 transition-transform duration-300 ${isRefreshingTables ? 'animate-spin' : ''}`}
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                      >
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                      </svg>
                    </button>
                  )}
                <TableSelector
                  selectedTable={draft.tableNumber}
                  occupiedTables={occupiedTables}
                  onSelectTable={(tableNumber) => {
                    setDraft({ tableNumber });
                    setIsExpanded(true); // Expand when table is selected
                      setIsTableSelectorExpanded(false); // Collapse table selector when table is selected
                  }}
                />
                </div>
              )}
            </div>
            
            {/* Selected Table Display with Toggle */}
                {draft.tableNumber && (
                  <div className="flex items-center justify-between mt-2">
                <p className="text-sm text-gray-600 font-medium">
                  Selected: {formatTableNumber(draft.tableNumber)}
                    </p>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setIsTableSelectorExpanded(!isTableSelectorExpanded)}
                    className="text-sm text-action-primary hover:text-action-primary-dark focus:outline-none focus:ring-2 focus:ring-action-primary rounded p-1"
                    aria-label={isTableSelectorExpanded ? 'Hide table selector' : 'Show table selector'}
                    title={isTableSelectorExpanded ? 'Hide table selector' : 'Show table selector to change'}
                  >
                    <svg
                      className={`w-5 h-5 transition-transform duration-300 ${isTableSelectorExpanded ? 'rotate-180' : ''}`}
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                    </svg>
                  </button>
                    <button
                      onClick={() => setIsExpanded(!isExpanded)}
                      className="text-sm text-action-primary hover:text-action-primary-dark focus:outline-none focus:ring-2 focus:ring-action-primary rounded p-1"
                      aria-label={isExpanded ? 'Collapse order summary' : 'Expand order summary'}
                    title={isExpanded ? 'Collapse order summary' : 'Expand order summary'}
                    >
                      <svg
                        className={`w-5 h-5 transition-transform duration-300 ${isExpanded ? 'rotate-180' : ''}`}
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                      >
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                      </svg>
                    </button>
                  </div>
              </div>
            )}
          </div>
        )}
        
        {draft.type === 'EatAway' && (
          <div>
            <label className="text-sm font-medium text-gray-700 mb-2 block">License Plate</label>
            <LicensePlateInput
              value={draft.licensePlate}
              onChange={(value) => setDraft({ licensePlate: value })}
            />
            {draft.licensePlate && draft.licensePlate.length === 4 && (
              <p className="text-sm text-gray-600 mt-2">
                License: {draft.licensePlate}
              </p>
            )}
          </div>
        )}
      </div>

      {/* Sticky Action Buttons - Collapsible when table is selected */}
      <div
        className={`px-6 py-3 bg-white border-t sticky bottom-0 transition-all duration-300 ease-in-out ${
          draft.type === 'DineIn' && draft.tableNumber && !isExpanded
            ? 'max-h-0 overflow-hidden opacity-0 py-0'
            : 'max-h-full opacity-100'
        }`}
      >
        <Button
          variant="primary"
          onClick={handlePlaceOrderClick}
          disabled={draft.orderItems.length === 0 || isPlacingOrder}
          className="w-full"
          size="medium"
        >
          {isPlacingOrder ? 'Placing Order...' : `Place Order (${totalItems} items)`}
        </Button>
      </div>

      {/* Place Order Confirmation Dialog */}
      <Dialog
        isOpen={showPlaceOrderDialog}
        onClose={() => !isPlacingOrder && setShowPlaceOrderDialog(false)}
        title="Place Order"
        message={`Ready to place this order with ${totalItems} items?`}
        confirmText={isPlacingOrder ? "Placing..." : "Yes, Place Order"}
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

