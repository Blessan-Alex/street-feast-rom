import React, { useState, useEffect } from 'react';
import { Button } from '../components/Button';
import { useMenuStore } from '../store/menuStore';
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

      {/* Other Settings Placeholder */}
      <div className="bg-white rounded-lg shadow p-6 mt-6">
        <h2 className="text-xl font-semibold text-gray-900 mb-2">Data Controls</h2>
        <p className="text-sm text-gray-600 mb-4">
          Additional settings will be available here soon.
        </p>
        <div className="space-y-2">
          <Button variant="secondary" disabled>
            Download Template
          </Button>
          <Button variant="danger" disabled>
            Delete All Data
          </Button>
        </div>
      </div>
    </div>
  );
};

