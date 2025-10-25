import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../components/Button';
import { Dialog } from '../components/Dialog';
import { useMenuStore } from '../store/menuStore';
import { useOrdersStore, clearOrderCounter } from '../store/ordersStore';
import { clearStorage } from '../utils/storage';
import { clearOrdersStorage } from '../utils/ordersStorage';
import { logout } from '../utils/auth';
import { toast } from '../components/Toast';

export const Settings: React.FC = () => {
  const { items, frequentItemIds, setFrequentItems } = useMenuStore();
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [hasChanges, setHasChanges] = useState(false);

  // Load current frequent items on mount
  useEffect(() => {
    setSelectedIds([...frequentItemIds]);
  }, [frequentItemIds]);

  // Check if there are unsaved changes
  useEffect(() => {
    const changed = JSON.stringify(selectedIds) !== JSON.stringify(frequentItemIds);
    setHasChanges(changed);
  }, [selectedIds, frequentItemIds]);

  const handleToggleItem = (itemId: string) => {
    if (selectedIds.includes(itemId)) {
      setSelectedIds(selectedIds.filter(id => id !== itemId));
    } else {
      setSelectedIds([...selectedIds, itemId]);
    }
  };

  const moveUp = (index: number) => {
    if (index === 0) return;
    const newSelected = [...selectedIds];
    [newSelected[index - 1], newSelected[index]] = [newSelected[index], newSelected[index - 1]];
    setSelectedIds(newSelected);
  };

  const moveDown = (index: number) => {
    if (index === selectedIds.length - 1) return;
    const newSelected = [...selectedIds];
    [newSelected[index], newSelected[index + 1]] = [newSelected[index + 1], newSelected[index]];
    setSelectedIds(newSelected);
  };

  const handleSave = () => {
    setFrequentItems(selectedIds);
    toast.success('Frequent items saved successfully!');
    setHasChanges(false);
  };

  const handleReset = () => {
    setSelectedIds([...frequentItemIds]);
    setHasChanges(false);
  };

  const getItemById = (id: string) => items.find(item => item.id === id);
  const selectedItems = selectedIds.map(id => getItemById(id)).filter(item => item !== undefined);
  const availableItems = items.filter(item => !selectedIds.includes(item.id));

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Settings</h1>
        <p className="text-gray-600">Manage your restaurant settings and preferences.</p>
      </div>

      {/* Frequent Bought Section */}
      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <h2 className="text-xl font-semibold text-gray-900 mb-2">Frequent Bought Items</h2>
        <p className="text-sm text-gray-600 mb-6">
          Select items to display at the top of the order screen for quick access. Use arrows to reorder.
        </p>

        {items.length === 0 ? (
          <div className="text-center py-12 bg-gray-50 rounded-lg">
            <p className="text-gray-500 mb-2">No menu items available yet.</p>
            <p className="text-sm text-gray-400">Create your menu first to set frequent items.</p>
          </div>
        ) : (
          <>
            {/* Item Selector */}
            <div className="mb-6">
              <label className="block text-sm font-medium text-gray-700 mb-3">
                Available Items ({availableItems.length})
              </label>
              {availableItems.length === 0 ? (
                <p className="text-sm text-gray-500 italic">All items are already selected</p>
              ) : (
                <div className="grid md:grid-cols-2 gap-2 max-h-60 overflow-y-auto border border-gray-200 rounded-lg p-3">
                  {availableItems.map(item => (
                    <label
                      key={item.id}
                      className="flex items-center p-2 hover:bg-gray-50 rounded cursor-pointer"
                    >
                      <input
                        type="checkbox"
                        checked={selectedIds.includes(item.id)}
                        onChange={() => handleToggleItem(item.id)}
                        className="w-4 h-4 text-action-primary border-gray-300 rounded focus:ring-2 focus:ring-action-primary"
                      />
                      <span className="ml-2 text-sm text-gray-900">{item.name}</span>
                      <span className={`ml-auto text-xs px-2 py-0.5 rounded ${
                        item.vegFlag === 'Veg' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                      }`}>
                        {item.vegFlag}
                      </span>
                    </label>
                  ))}
                </div>
              )}
            </div>

            {/* Selected Items with Reordering */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-3">
                Selected Items ({selectedItems.length})
                {selectedItems.length > 0 && (
                  <span className="ml-2 text-gray-500 font-normal">
                    (Order matters - top items appear first)
                  </span>
                )}
              </label>
              
              {selectedItems.length === 0 ? (
                <div className="text-center py-8 bg-gray-50 rounded-lg border-2 border-dashed border-gray-300">
                  <p className="text-gray-500">No items selected yet</p>
                  <p className="text-sm text-gray-400 mt-1">Select items from the list above</p>
                </div>
              ) : (
                <div className="space-y-2">
                  {selectedItems.map((item, index) => (
                    <div
                      key={item!.id}
                      className="flex items-center gap-2 p-3 bg-gray-50 rounded-lg border border-gray-200"
                    >
                      {/* Reorder Buttons */}
                      <div className="flex flex-col gap-1">
                        <button
                          onClick={() => moveUp(index)}
                          disabled={index === 0}
                          className="w-6 h-6 flex items-center justify-center text-gray-600 hover:text-gray-900 hover:bg-gray-200 rounded disabled:opacity-30 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-gray-400"
                          aria-label="Move up"
                        >
                          ▲
                        </button>
                        <button
                          onClick={() => moveDown(index)}
                          disabled={index === selectedItems.length - 1}
                          className="w-6 h-6 flex items-center justify-center text-gray-600 hover:text-gray-900 hover:bg-gray-200 rounded disabled:opacity-30 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-gray-400"
                          aria-label="Move down"
                        >
                          ▼
                        </button>
                      </div>

                      {/* Item Info */}
                      <div className="flex-1 flex items-center justify-between">
                        <div className="flex items-center gap-3">
                          <span className="text-sm font-medium text-gray-500">#{index + 1}</span>
                          <span className="font-medium text-gray-900">{item!.name}</span>
                          {item!.sizes.length > 0 && (
                            <span className="text-xs text-gray-500">
                              ({item!.sizes.join(', ')})
                            </span>
                          )}
                        </div>
                        <span className={`text-xs px-2 py-1 rounded font-medium ${
                          item!.vegFlag === 'Veg' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                        }`}>
                          {item!.vegFlag}
                        </span>
                      </div>

                      {/* Remove Button */}
                      <button
                        onClick={() => handleToggleItem(item!.id)}
                        className="w-8 h-8 flex items-center justify-center text-red-600 hover:text-red-800 hover:bg-red-50 rounded focus:outline-none focus:ring-2 focus:ring-red-400"
                        aria-label="Remove"
                      >
                        ×
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </>
        )}
      </div>

      {/* Save Actions */}
      {items.length > 0 && (
        <div className="flex gap-3 justify-end">
          <Button
            variant="secondary"
            onClick={handleReset}
            disabled={!hasChanges}
          >
            Reset
          </Button>
          <Button
            variant="primary"
            onClick={handleSave}
            disabled={!hasChanges}
            size="large"
          >
            Save Changes
          </Button>
        </div>
      )}

      {/* Danger Zone */}
      <DangerZone />
    </div>
  );
};

// Danger Zone Component
const DangerZone: React.FC = () => {
  const navigate = useNavigate();
  const resetMenu = useMenuStore(state => state.reset);
  const resetOrders = useOrdersStore(state => state.reset);
  const [showFirstDialog, setShowFirstDialog] = useState(false);
  const [showSecondDialog, setShowSecondDialog] = useState(false);
  const [confirmText, setConfirmText] = useState('');

  const handleDeleteAll = () => {
    // Clear all stores
    resetMenu();
    resetOrders();
    
    // Clear localStorage
    clearStorage();
    clearOrdersStorage();
    clearOrderCounter();
    
    // Logout
    logout();
    
    toast.success('All data has been deleted');
    navigate('/login');
  };

  const handleFirstConfirm = () => {
    setShowFirstDialog(false);
    setShowSecondDialog(true);
  };

  const handleSecondConfirm = () => {
    if (confirmText === 'DELETE') {
      handleDeleteAll();
    }
  };

  return (
    <>
      <div className="bg-red-50 border-2 border-red-200 rounded-lg p-6 mt-6">
        <div className="mb-4">
          <h2 className="text-xl font-bold text-red-900 mb-2">DANGER ZONE</h2>
          <p className="text-sm text-red-800">
            Deleting all data will permanently remove your menu, orders, frequent items, and settings.
            This action cannot be undone and you will be logged out.
          </p>
        </div>
        <Button
          variant="danger"
          onClick={() => setShowFirstDialog(true)}
        >
          Delete All Data
        </Button>
      </div>

      {/* First Confirmation Dialog */}
      <Dialog
        isOpen={showFirstDialog}
        onClose={() => setShowFirstDialog(false)}
        title="Delete All Data?"
        message="This will delete ALL data including menu, orders, and frequent items. Are you sure you want to continue?"
        confirmText="Yes, Continue"
        cancelText="Cancel"
        onConfirm={handleFirstConfirm}
        confirmVariant="danger"
      />

      {/* Second Confirmation Dialog with Text Input */}
      {showSecondDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black bg-opacity-50" onClick={() => {
            setShowSecondDialog(false);
            setConfirmText('');
          }} />
          
          <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full p-6 animate-fade-in">
            <h2 className="text-2xl font-bold text-red-900 mb-2">Final Confirmation</h2>
            <p className="text-gray-700 mb-4">
              Type <strong>DELETE</strong> to confirm permanent deletion of all data.
            </p>
            
            <input
              type="text"
              value={confirmText}
              onChange={(e) => setConfirmText(e.target.value)}
              placeholder="Type DELETE here"
              className="w-full px-4 py-2 border-2 border-gray-300 rounded-lg focus:outline-none focus:border-red-500 mb-4"
              autoFocus
            />
            
            <div className="flex gap-3">
              <Button
                variant="secondary"
                onClick={() => {
                  setShowSecondDialog(false);
                  setConfirmText('');
                }}
                className="flex-1"
              >
                Cancel
              </Button>
              <Button
                variant="danger"
                onClick={handleSecondConfirm}
                disabled={confirmText !== 'DELETE'}
                className="flex-1"
              >
                Confirm Delete
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

