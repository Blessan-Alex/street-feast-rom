export interface ValidatedRow {
  itemName: string;
  category: string;
  sizes: string[];
  vegFlag: 'Veg' | 'NonVeg' | 'Both';
  flavors?: string;
  error: string | null;
}

export interface ValidationResult {
  valid: boolean;
  rows: ValidatedRow[];
  errors: string[];
}

const REQUIRED_HEADERS = ['Category', 'Item Name', 'Veg / Non-Veg', 'Portions (Half / Full)', 'Flavours / Toppings'];

// Inference logic for Veg/NonVeg when not specified
const inferVegFlag = (itemName: string, category: string): 'Veg' | 'NonVeg' => {
  const text = `${itemName} ${category}`.toLowerCase();
  
  const nonVegKeywords = ['chicken', 'egg', 'meat', 'fish', 'prawn', 'mutton', 'beef', 'pork'];
  const vegKeywords = ['paneer', 'veg', 'mushroom', 'chaap', 'dal', 'rice', 'noodles', 'pizza', 'burger', 'wrap', 'bowl', 'fries', 'drink', 'coffee', 'tea', 'soda', 'water', 'salad', 'naan', 'roti'];
  
  // Check for non-veg keywords first
  for (const keyword of nonVegKeywords) {
    if (text.includes(keyword)) {
      return 'NonVeg';
    }
  }
  
  // Check for veg keywords
  for (const keyword of vegKeywords) {
    if (text.includes(keyword)) {
      return 'Veg';
    }
  }
  
  // Default to Veg if no keywords found
  return 'Veg';
};

// Parse sizes from various formats
const parseSizes = (sizesRaw: string): string[] => {
  if (!sizesRaw || sizesRaw.trim() === '') return [];
  
  // Split by common separators and clean up
  const sizes = sizesRaw
    .split(/[,/]/)
    .map(s => s.trim())
    .filter(s => s.length > 0)
    .map(s => s.replace(/['"]/g, '')); // Remove quotes
  
  return sizes;
};

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
  const headers = Object.keys(data[0]).map(h => h.trim().replace(/\r/g, ''));
  const missingHeaders = REQUIRED_HEADERS.filter(
    required => !headers.some(h => h.toLowerCase() === required.toLowerCase())
  );

  if (missingHeaders.length > 0) {
    errors.push(`Missing required headers: ${missingHeaders.join(', ')}`);
    return { valid: false, rows: [], errors };
  }

  // Normalize header names (find case-insensitive matches)
  const getHeader = (required: string) => {
    return headers.find(h => h.toLowerCase() === required.toLowerCase()) || required;
  };

  const categoryHeader = getHeader('Category');
  const itemNameHeader = getHeader('Item Name');
  const vegHeader = getHeader('Veg / Non-Veg');
  const sizesHeader = getHeader('Portions (Half / Full)');
  const flavorsHeader = getHeader('Flavours / Toppings');

  // Validate each row
  data.forEach((row, index) => {
    const rowNum = index + 1;
    let rowError: string | null = null;

    // Get raw values
    const category = row[categoryHeader]?.toString().trim() || '';
    const itemName = row[itemNameHeader]?.toString().trim() || '';
    const vegFlagRaw = row[vegHeader]?.toString().trim() || '';
    const sizesRaw = row[sizesHeader]?.toString().trim() || '';
    const flavorsRaw = row[flavorsHeader]?.toString().trim() || '';

    // Validate Category
    if (!category) {
      rowError = `Row ${rowNum}: Category is required`;
    }

    // Validate Item Name
    if (!itemName && !rowError) {
      rowError = `Row ${rowNum}: Item Name is required`;
    }

    // Parse and validate Veg/NonVeg
    let vegFlag: 'Veg' | 'NonVeg' | 'Both' = 'Veg';
    if (vegFlagRaw) {
      const vegLower = vegFlagRaw.toLowerCase();
      if (vegLower.includes('veg') && vegLower.includes('non')) {
        vegFlag = 'Both';
      } else if (vegLower.includes('non') || vegLower.includes('non veg')) {
        vegFlag = 'NonVeg';
      } else if (vegLower.includes('veg')) {
        vegFlag = 'Veg';
      } else {
        // Try to infer from item name and category
        vegFlag = inferVegFlag(itemName, category);
      }
    } else {
      // No veg flag specified, infer from context
      vegFlag = inferVegFlag(itemName, category);
    }

    // Parse sizes
    const sizes = parseSizes(sizesRaw);

    rows.push({
      itemName,
      category,
      sizes,
      vegFlag,
      flavors: flavorsRaw || undefined,
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

