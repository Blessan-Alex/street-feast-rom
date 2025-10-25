import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useMenuStore } from '../store/menuStore';
import { seedMenu } from '../utils/seedData';
import { toast } from '../components/Toast';

export const MenuChooser: React.FC = () => {
  const navigate = useNavigate();
  const { categories, addCategory, addItems } = useMenuStore();

  const hasMenu = categories.length > 0;

  const handleSeedMenu = () => {
    const { categories, items } = seedMenu();
    categories.forEach(cat => addCategory(cat));
    addItems(items);
    toast.success('Demo menu loaded! 3 categories with 6 items.');
    navigate('/menu/summary');
  };

  return (
    <div className="max-w-4xl mx-auto">
      <h1 className="text-3xl font-bold text-gray-900 mb-2">Menu Management</h1>
      <p className="text-gray-600 mb-8">
        {hasMenu 
          ? 'You have an existing menu. Choose an option to continue.'
          : 'Get started by creating your restaurant menu.'}
      </p>

      {!hasMenu && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
          <p className="text-sm text-blue-800">
            <strong>Tip:</strong> You can upload a CSV file for quick setup, or create your menu manually using the interface.
          </p>
        </div>
      )}

      <div className="grid md:grid-cols-2 gap-6 mb-6">
        {/* CSV Upload Option */}
        <div 
          onClick={() => navigate('/menu/upload')}
          className="bg-white rounded-lg shadow-md hover:shadow-xl transition-shadow cursor-pointer border-2 border-transparent hover:border-action-primary p-8 text-center"
        >
          <div className="text-5xl mb-4 text-gray-400">CSV</div>
          <h2 className="text-2xl font-bold text-gray-900 mb-3">Upload from CSV</h2>
          <p className="text-gray-600 mb-4">
            Have a CSV file? Upload it and we'll validate and import your menu items automatically.
          </p>
          <div className="inline-block px-4 py-2 bg-action-primary text-white rounded-lg font-medium">
            Choose CSV File
          </div>
        </div>

        {/* Manual Creation Option */}
        <div 
          onClick={() => navigate('/menu/create')}
          className="bg-white rounded-lg shadow-md hover:shadow-xl transition-shadow cursor-pointer border-2 border-transparent hover:border-action-primary p-8 text-center"
        >
          <div className="text-5xl mb-4 text-gray-400">+</div>
          <h2 className="text-2xl font-bold text-gray-900 mb-3">Create Manually</h2>
          <p className="text-gray-600 mb-4">
            Prefer to create your menu step by step? Use our interface to add categories and items.
          </p>
          <div className="inline-block px-4 py-2 bg-gray-200 text-gray-800 rounded-lg font-medium">
            Start Creating
          </div>
        </div>
      </div>

      {!hasMenu && (
        <div className="text-center mt-6">
          <button
            onClick={handleSeedMenu}
            className="text-sm text-gray-600 hover:text-gray-900 underline focus:outline-none focus:ring-2 focus:ring-gray-400 rounded px-2 py-1"
          >
            Or load a demo menu to test quickly
          </button>
        </div>
      )}

      {hasMenu && (
        <div className="bg-white rounded-lg shadow-md p-6 text-center">
          <p className="text-gray-700 mb-4">
            Or view your existing menu:
          </p>
          <button
            onClick={() => navigate('/menu/summary')}
            className="px-6 py-3 bg-gray-200 hover:bg-gray-300 text-gray-800 rounded-lg font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-gray-400"
          >
            View Menu Summary
          </button>
        </div>
      )}
    </div>
  );
};

