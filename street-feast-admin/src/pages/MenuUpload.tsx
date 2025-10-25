import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Papa from 'papaparse';
import { Button } from '../components/Button';
import { validateCSV, ValidatedRow, ValidationResult } from '../utils/csvValidator';
import { useMenuStore } from '../store/menuStore';
import { toast } from '../components/Toast';

export const MenuUpload: React.FC = () => {
  const navigate = useNavigate();
  const { addCategory, addItems } = useMenuStore();
  const [file, setFile] = useState<File | null>(null);
  const [validation, setValidation] = useState<ValidationResult | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0];
    if (!selectedFile) return;

    setFile(selectedFile);
    setIsProcessing(true);

    // Parse CSV
    Papa.parse(selectedFile, {
      complete: (results: Papa.ParseResult<any>) => {
        const validationResult = validateCSV(results.data);
        setValidation(validationResult);
        setIsProcessing(false);
      },
      header: true,
      skipEmptyLines: true,
      error: (error: Error) => {
        toast.error(`Failed to parse CSV: ${error.message}`);
        setIsProcessing(false);
      }
    });
  };

  const downloadTemplate = () => {
    const template = `Item Name,Category,Available Sizes,Veg/NonVeg
Chicken Soup,Chinese,"Small, Large",NonVeg
Caesar Salad,American,,Veg
Paneer Tikka,Indian,Small,Veg
Chocolate Cake,Desserts,,Veg
Butter Chicken,Indian,"Small, Large",NonVeg`;

    const blob = new Blob([template], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'menu-template.csv';
    a.click();
    window.URL.revokeObjectURL(url);
    toast.success('Template downloaded successfully');
  };

  const handleApply = () => {
    if (!validation || !validation.valid) {
      toast.error('Please fix validation errors before applying');
      return;
    }

    setIsProcessing(true);

    try {
      // Group items by category
      const categoriesMap = new Map<string, ValidatedRow[]>();
      validation.rows.forEach(row => {
        if (!categoriesMap.has(row.category)) {
          categoriesMap.set(row.category, []);
        }
        categoriesMap.get(row.category)!.push(row);
      });

      // Create categories and items
      categoriesMap.forEach((items, categoryName) => {
        const categoryId = `cat-${Date.now()}-${Math.random().toString(36).substring(7)}`;
        const now = Date.now();

        // Add category
        addCategory({
          id: categoryId,
          name: categoryName,
          isActive: true,
          createdAt: now,
          updatedAt: now
        });

        // Add items for this category
        const itemsToAdd = items.map((item, index) => ({
          id: `item-${Date.now()}-${index}-${Math.random().toString(36).substring(7)}`,
          categoryId,
          name: item.itemName,
          sizes: item.sizes,
          vegFlag: item.vegFlag,
          isActive: true,
          createdAt: now,
          updatedAt: now
        }));

        addItems(itemsToAdd);
      });

      toast.success(`Successfully imported ${validation.rows.length} items in ${categoriesMap.size} categories`);
      navigate('/menu/summary');
    } catch (error) {
      toast.error('Failed to import menu data');
      setIsProcessing(false);
    }
  };

  const previewRows = validation?.rows.slice(0, 20) || [];

  return (
    <div className="max-w-6xl mx-auto">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Upload Menu from CSV</h1>
        <p className="text-gray-600">
          Upload your menu CSV file and we'll validate it before importing.
        </p>
      </div>

      {/* Template Info */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
        <div className="flex items-start justify-between">
          <div className="flex-1">
            <h3 className="font-semibold text-blue-900 mb-2">CSV Template Requirements:</h3>
            <ul className="text-sm text-blue-800 space-y-1">
              <li>• <strong>Required headers:</strong> Item Name, Category, Available Sizes, Veg/NonVeg</li>
              <li>• <strong>Available Sizes:</strong> Small, Large (comma-separated) or leave blank for no sizes</li>
              <li>• <strong>Veg/NonVeg:</strong> Must be either "Veg" or "NonVeg"</li>
            </ul>
          </div>
          <button
            onClick={downloadTemplate}
            className="ml-4 px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors"
          >
            Download Template
          </button>
        </div>
      </div>

      {/* File Input */}
      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <label htmlFor="csv-file" className="block text-lg font-semibold text-gray-900 mb-3">
          Choose CSV File
        </label>
        <input
          id="csv-file"
          type="file"
          accept=".csv"
          onChange={handleFileSelect}
          disabled={isProcessing}
          className="w-full px-4 py-3 border-2 border-dashed border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-action-primary focus:border-transparent text-base cursor-pointer hover:border-action-primary transition-colors"
        />
        {file && (
          <p className="mt-2 text-sm text-gray-600">
            Selected: <strong>{file.name}</strong>
          </p>
        )}
      </div>

      {/* Validation Errors */}
      {validation && validation.errors.length > 0 && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
          <h3 className="font-semibold text-red-900 mb-2">Validation Errors:</h3>
          <ul className="text-sm text-red-800 space-y-1">
            {validation.errors.map((error, index) => (
              <li key={index}>• {error}</li>
            ))}
          </ul>
        </div>
      )}

      {/* Preview Table */}
      {validation && validation.rows.length > 0 && (
        <div className="bg-white rounded-lg shadow overflow-hidden mb-6">
          <div className="px-6 py-4 border-b bg-gray-50">
            <h3 className="font-semibold text-gray-900">
              Preview (showing first 20 of {validation.rows.length} rows)
            </h3>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-100 border-b">
                <tr>
                  <th className="px-4 py-3 text-left text-sm font-semibold text-gray-700">#</th>
                  <th className="px-4 py-3 text-left text-sm font-semibold text-gray-700">Item Name</th>
                  <th className="px-4 py-3 text-left text-sm font-semibold text-gray-700">Category</th>
                  <th className="px-4 py-3 text-left text-sm font-semibold text-gray-700">Sizes</th>
                  <th className="px-4 py-3 text-left text-sm font-semibold text-gray-700">Veg/NonVeg</th>
                  <th className="px-4 py-3 text-left text-sm font-semibold text-gray-700">Status</th>
                </tr>
              </thead>
              <tbody>
                {previewRows.map((row, index) => (
                  <tr 
                    key={index} 
                    className={row.error ? 'bg-red-50' : 'hover:bg-gray-50'}
                  >
                    <td className="px-4 py-3 text-sm text-gray-600">{index + 1}</td>
                    <td className="px-4 py-3 text-sm text-gray-900">{row.itemName || <span className="text-gray-400">-</span>}</td>
                    <td className="px-4 py-3 text-sm text-gray-900">{row.category || <span className="text-gray-400">-</span>}</td>
                    <td className="px-4 py-3 text-sm text-gray-900">
                      {row.sizes.length > 0 ? row.sizes.join(', ') : <span className="text-gray-400">No sizes</span>}
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${
                        row.vegFlag === 'Veg' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                      }`}>
                        {row.vegFlag}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm">
                      {row.error ? (
                        <span className="text-red-700 text-xs">{row.error}</span>
                      ) : (
                        <span className="text-green-700 text-lg">✓</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Actions */}
      {validation && (
        <div className="flex gap-3 justify-end">
          <Button
            variant="secondary"
            onClick={() => navigate('/menu')}
            disabled={isProcessing}
          >
            Cancel
          </Button>
          <Button
            variant="primary"
            onClick={handleApply}
            disabled={!validation.valid || isProcessing}
            size="large"
          >
            {isProcessing ? 'Processing...' : 'Apply & Import'}
          </Button>
        </div>
      )}
    </div>
  );
};

