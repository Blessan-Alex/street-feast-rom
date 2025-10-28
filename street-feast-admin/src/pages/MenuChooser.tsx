import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMenuStore } from '../store/menuStore';
import { seedMenu } from '../utils/seedData';
import { toast } from '../components/Toast';

export const MenuChooser: React.FC = () => {
  const navigate = useNavigate();
  const { categories, addCategory, addItems } = useMenuStore();
  const [showUploadModal, setShowUploadModal] = useState(false);

  const hasMenu = categories.length > 0;

  const handleSeedMenu = () => {
    const { categories, items } = seedMenu();
    categories.forEach(cat => addCategory(cat));
    addItems(items);
    toast.success('Demo menu loaded! 3 categories with 6 items.');
    navigate('/menu/summary');
  };

  const handleUploadClick = () => {
    if (!hasMenu) {
      setShowUploadModal(true);
    } else {
      navigate('/menu/upload');
    }
  };

  const handleManualClick = () => {
    if (!hasMenu) {
      setShowUploadModal(true);
    } else {
      navigate('/menu/create');
    }
  };

  // First-time user experience - show only Upload Menu card
  if (!hasMenu) {
    return (
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Welcome to Street Feast</h1>
        <p className="text-gray-600 mb-8">
          Let's get started by setting up your restaurant menu.
        </p>

        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
          <p className="text-sm text-blue-800">
            <strong>First Time Setup:</strong> You need to upload your menu before you can start taking orders.
          </p>
        </div>

        {/* Single Upload Menu Card */}
        <div className="flex justify-center">
          <div 
            onClick={handleUploadClick}
            className="bg-white rounded-lg shadow-md hover:shadow-xl transition-shadow cursor-pointer border-2 border-transparent hover:border-action-primary p-12 text-center max-w-md"
          >
            <div className="text-6xl mb-6 text-action-primary">MENU</div>
            <h2 className="text-3xl font-bold text-gray-900 mb-4">Upload Menu</h2>
            <p className="text-gray-600 mb-6">
              Click here to get started with your menu setup
            </p>
            <div className="inline-block px-6 py-3 bg-action-primary text-white rounded-lg font-medium text-lg">
              Get Started
            </div>
          </div>
        </div>

        {/* Floating Modal for Upload Options */}
        {showUploadModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center">
            <div 
              className="absolute inset-0 bg-black bg-opacity-50"
              onClick={() => setShowUploadModal(false)}
            />
            <div className="relative bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 p-8">
              <h2 className="text-2xl font-bold text-gray-900 mb-4">Choose Menu Setup Method</h2>
              <p className="text-gray-600 mb-6">
                How would you like to create your menu?
              </p>
              
              <div className="grid md:grid-cols-2 gap-4 mb-6">
                <div 
                  onClick={() => {
                    setShowUploadModal(false);
                    navigate('/menu/upload');
                  }}
                  className="bg-gray-50 rounded-lg p-6 cursor-pointer hover:bg-gray-100 transition-colors border-2 border-transparent hover:border-action-primary"
                >
                  <div className="text-4xl mb-3 text-gray-600">CSV</div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-2">Upload CSV</h3>
                  <p className="text-sm text-gray-600">
                    Import your menu from a CSV file
                  </p>
                </div>
                
                <div 
                  onClick={() => {
                    setShowUploadModal(false);
                    navigate('/menu/create');
                  }}
                  className="bg-gray-50 rounded-lg p-6 cursor-pointer hover:bg-gray-100 transition-colors border-2 border-transparent hover:border-action-primary"
                >
                  <div className="text-4xl mb-3 text-gray-600">EDIT</div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-2">Create Manually</h3>
                  <p className="text-sm text-gray-600">
                    Build your menu step by step
                  </p>
                </div>
              </div>

              <div className="text-center">
                <button
                  onClick={() => setShowUploadModal(false)}
                  className="px-4 py-2 text-gray-600 hover:text-gray-900 underline focus:outline-none focus:ring-2 focus:ring-gray-400 rounded"
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Demo Option */}
        <div className="text-center mt-8">
          <button
            onClick={handleSeedMenu}
            className="text-sm text-gray-600 hover:text-gray-900 underline focus:outline-none focus:ring-2 focus:ring-gray-400 rounded px-2 py-1"
          >
            Or load a demo menu to test quickly
          </button>
        </div>
      </div>
    );
  }

  // Existing user experience - show full options
  return (
    <div className="max-w-4xl mx-auto">
      <h1 className="text-3xl font-bold text-gray-900 mb-2">Menu Management</h1>
      <p className="text-gray-600 mb-8">
        You have an existing menu. Choose an option to continue.
      </p>

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
    </div>
  );
};

