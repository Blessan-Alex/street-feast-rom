export interface ValidatedRow {
  itemName: string;
  category: string;
  sizes: string[];
  vegFlag: 'Veg' | 'NonVeg';
  error: string | null;
}

export interface ValidationResult {
  valid: boolean;
  rows: ValidatedRow[];
  errors: string[];
}

const REQUIRED_HEADERS = ['Item Name', 'Category', 'Available Sizes', 'Veg/NonVeg'];
const VALID_SIZES = ['Small', 'Large'];
const VALID_VEG_FLAGS = ['Veg', 'NonVeg'];

export const validateCSV = (data: any[]): ValidationResult => {
  const errors: string[] = [];
  const rows: ValidatedRow[] = [];

  if (!data || data.length === 0) {
    return {
      valid: false,
      rows: [],
      errors: ['CSV file is empty']
    };
  }

  // Check headers
  const headers = Object.keys(data[0]);
  const missingHeaders = REQUIRED_HEADERS.filter(
    required => !headers.some(h => h.trim().toLowerCase() === required.toLowerCase())
  );

  if (missingHeaders.length > 0) {
    errors.push(`Missing required headers: ${missingHeaders.join(', ')}`);
    return { valid: false, rows: [], errors };
  }

  // Normalize header names (find case-insensitive matches)
  const getHeader = (required: string) => {
    return headers.find(h => h.trim().toLowerCase() === required.toLowerCase()) || required;
  };

  const itemNameHeader = getHeader('Item Name');
  const categoryHeader = getHeader('Category');
  const sizesHeader = getHeader('Available Sizes');
  const vegHeader = getHeader('Veg/NonVeg');

  // Validate each row
  data.forEach((row, index) => {
    const rowNum = index + 1;
    let rowError: string | null = null;

    // Get raw values
    const itemName = row[itemNameHeader]?.toString().trim() || '';
    const category = row[categoryHeader]?.toString().trim() || '';
    const sizesRaw = row[sizesHeader]?.toString().trim() || '';
    const vegFlagRaw = row[vegHeader]?.toString().trim() || '';

    // Validate Item Name
    if (!itemName) {
      rowError = `Row ${rowNum}: Item Name is required`;
    }

    // Validate Category
    if (!category && !rowError) {
      rowError = `Row ${rowNum}: Category is required`;
    }

    // Validate and parse sizes
    let sizes: string[] = [];
    if (sizesRaw) {
      const sizeParts = sizesRaw.split(',').map((s: string) => s.trim()).filter((s: string) => s);
      const invalidSizes = sizeParts.filter(
        (size: string) => !VALID_SIZES.some(valid => valid.toLowerCase() === size.toLowerCase())
      );
      
      if (invalidSizes.length > 0 && !rowError) {
        rowError = `Row ${rowNum}: Invalid sizes "${invalidSizes.join(', ')}". Must be Small, Large, or blank`;
      } else {
        // Normalize to proper case
        sizes = sizeParts.map((size: string) => {
          const validSize = VALID_SIZES.find(v => v.toLowerCase() === size.toLowerCase());
          return validSize || size;
        });
      }
    }

    // Validate Veg/NonVeg
    let vegFlag: 'Veg' | 'NonVeg' = 'Veg';
    if (!vegFlagRaw && !rowError) {
      rowError = `Row ${rowNum}: Veg/NonVeg is required`;
    } else {
      const validVegFlag = VALID_VEG_FLAGS.find(
        v => v.toLowerCase() === vegFlagRaw.toLowerCase()
      );
      if (!validVegFlag && !rowError) {
        rowError = `Row ${rowNum}: Veg/NonVeg must be "Veg" or "NonVeg"`;
      } else {
        vegFlag = (validVegFlag as 'Veg' | 'NonVeg') || 'Veg';
      }
    }

    rows.push({
      itemName,
      category,
      sizes,
      vegFlag,
      error: rowError
    });
  });

  const hasErrors = rows.some(row => row.error !== null) || errors.length > 0;

  return {
    valid: !hasErrors,
    rows,
    errors
  };
};

