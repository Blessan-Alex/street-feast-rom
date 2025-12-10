import React from 'react';

interface TableSelectorProps {
  selectedTable?: number;
  occupiedTables: number[];
  onSelectTable: (tableNumber: number) => void;
  className?: string;
}

export const TableSelector: React.FC<TableSelectorProps> = ({
  selectedTable,
  occupiedTables,
  onSelectTable,
  className = ''
}) => {
  const tables = [1, 2, 3, 4, 5, 6, 7];

  const getTableState = (tableNumber: number): 'available' | 'occupied' | 'selected' => {
    if (selectedTable === tableNumber) return 'selected';
    if (occupiedTables.includes(tableNumber)) return 'occupied';
    return 'available';
  };

  const getTableClasses = (tableNumber: number): string => {
    const state = getTableState(tableNumber);
    const baseClasses = 'relative w-full min-h-[100px] rounded-lg font-bold text-xl transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2';
    
    switch (state) {
      case 'selected':
        return `${baseClasses} bg-action-primary text-white shadow-md focus:ring-action-primary`;
      case 'occupied':
        return `${baseClasses} bg-gray-300 text-gray-500 cursor-not-allowed opacity-60`;
      case 'available':
        return `${baseClasses} bg-white border-2 border-gray-200 text-gray-900 hover:border-action-primary hover:shadow-md cursor-pointer`;
      default:
        return baseClasses;
    }
  };

  return (
    <div className={`grid grid-cols-3 gap-4 ${className}`}>
      {tables.map((tableNumber) => {
        const state = getTableState(tableNumber);
        const isOccupied = state === 'occupied';
        
        return (
          <button
            key={tableNumber}
            type="button"
            onClick={() => !isOccupied && onSelectTable(tableNumber)}
            disabled={isOccupied}
            className={getTableClasses(tableNumber)}
          >
            {tableNumber}
            {isOccupied && (
              <span className="absolute top-1 right-1 text-xs font-normal bg-red-500 text-white px-1.5 py-0.5 rounded">
                Occupied
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
};


