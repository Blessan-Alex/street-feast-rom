import { create } from 'zustand';

export interface Category {
  id: string;
  name: string;
  isActive: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface Item {
  id: string;
  categoryId: string;
  name: string;
  sizes: string[];
  vegFlag: 'Veg' | 'NonVeg';
  isActive: boolean;
  createdAt: number;
  updatedAt: number;
}

interface MenuStore {
  categories: Category[];
  items: Item[];
  frequentItemIds: string[];
  
  setCategories: (categories: Category[]) => void;
  setItems: (items: Item[]) => void;
  setFrequentItems: (ids: string[]) => void;
  setAll: (categories: Category[], items: Item[]) => void;
  
  addCategory: (category: Category) => void;
  updateCategory: (id: string, updates: Partial<Category>) => void;
  deleteCategory: (id: string) => void;
  
  addItems: (newItems: Item[]) => void;
  updateItem: (id: string, updates: Partial<Item>) => void;
  
  reset: () => void;
}

export const useMenuStore = create<MenuStore>((set) => ({
  categories: [],
  items: [],
  frequentItemIds: [],
  
  setCategories: (categories) => set({ categories }),
  setItems: (items) => set({ items }),
  setFrequentItems: (ids) => set({ frequentItemIds: ids }),
  setAll: (categories, items) => set({ categories, items }),
  
  addCategory: (category) => set((state) => ({
    categories: [...state.categories, category]
  })),
  
  updateCategory: (id, updates) => set((state) => ({
    categories: state.categories.map(cat => 
      cat.id === id ? { ...cat, ...updates, updatedAt: Date.now() } : cat
    )
  })),
  
  deleteCategory: (id) => set((state) => ({
    categories: state.categories.filter(cat => cat.id !== id),
    items: state.items.filter(item => item.categoryId !== id)
  })),
  
  addItems: (newItems) => set((state) => ({
    items: [...state.items, ...newItems]
  })),
  
  updateItem: (id, updates) => set((state) => ({
    items: state.items.map(item => 
      item.id === id ? { ...item, ...updates, updatedAt: Date.now() } : item
    )
  })),
  
  reset: () => set({ 
    categories: [], 
    items: [], 
    frequentItemIds: [] 
  }),
}));

