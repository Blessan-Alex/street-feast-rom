import { create } from 'zustand';
import { supabase, getStoreId } from '../utils/supabase';
import { saveToStorage } from '../utils/storage';

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
  sizes: string[]; // Now supports any size format
  vegFlag: 'Veg' | 'NonVeg' | 'Both';
  flavors?: string; // Optional flavors/toppings
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
  
  // Backend sync functions
  fetchMenuFromBackend: () => Promise<{ ok: boolean; error?: string }>;
  saveMenuToBackend: () => Promise<{ ok: boolean; error?: string }>;
  updateFrequentItemsInBackend: (itemIds: string[]) => Promise<{ ok: boolean; error?: string }>;
  
  reset: () => void;
}

export const useMenuStore = create<MenuStore>((set, get) => ({
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
  
  // Backend sync functions
  fetchMenuFromBackend: async () => {
    try {
      const storeId = await getStoreId();
      const { data, error } = await supabase.rpc('get_menu', {
        p_store_id: storeId
      });

      if (error) {
        console.error('Failed to fetch menu from backend:', error);
        return { ok: false, error: error.message };
      }

      if (!data) {
        return { ok: false, error: 'No menu data returned' };
      }

      // Map backend response to store format
      const categories: Category[] = (data.categories || []).map((cat: any) => ({
        id: cat.id,
        name: cat.name,
        isActive: cat.is_active,
        createdAt: new Date(cat.created_at).getTime(),
        updatedAt: new Date(cat.updated_at).getTime()
      }));

      const items: Item[] = (data.items || []).map((item: any) => ({
        id: item.id,
        categoryId: item.category_id,
        name: item.name,
        sizes: item.sizes || [],
        vegFlag: (item.veg_flag || 'Veg') as 'Veg' | 'NonVeg' | 'Both',
        flavors: item.flavors ? (Array.isArray(item.flavors) ? item.flavors.join(', ') : item.flavors) : undefined,
        isActive: item.is_active,
        createdAt: new Date(item.created_at).getTime(),
        updatedAt: new Date(item.updated_at).getTime()
      }));

      const frequentItemIds: string[] = (data.frequent_items || [])
        .sort((a: any, b: any) => a.order_index - b.order_index)
        .map((fi: any) => fi.item_id);

      // Update store
      set({ categories, items, frequentItemIds });
      
      // Save to localStorage as fallback
      saveToStorage(categories, items, frequentItemIds);

      return { ok: true };
    } catch (error: any) {
      console.error('Error fetching menu from backend:', error);
      return { ok: false, error: error.message || 'Failed to fetch menu' };
    }
  },

  saveMenuToBackend: async (): Promise<{ ok: boolean; error?: string }> => {
    try {
      const state: MenuStore = get();
      const storeId = await getStoreId();

      // Prepare categories JSONB
      const categoriesJson = state.categories.map((cat: Category) => ({
        id: cat.id,
        name: cat.name,
        isActive: cat.isActive,
        createdAt: new Date(cat.createdAt).toISOString(),
        updatedAt: new Date(cat.updatedAt).toISOString()
      }));

      // Prepare items JSONB
      const itemsJson = state.items.map((item: Item) => ({
        id: item.id,
        categoryId: item.categoryId,
        name: item.name,
        sizes: item.sizes,
        vegFlag: item.vegFlag,
        flavors: item.flavors ? (item.flavors.includes(',') ? item.flavors.split(', ').map((f: string) => f.trim()) : [item.flavors]) : null,
        isActive: item.isActive,
        createdAt: new Date(item.createdAt).toISOString(),
        updatedAt: new Date(item.updatedAt).toISOString()
      }));

      // Prepare frequent_items JSONB
      const frequentItemsJson = state.frequentItemIds.map((itemId: string, index: number) => ({
        itemId: itemId,
        orderIndex: index
      }));

      const { error }: { error: any } = await supabase.rpc('menu_upsert', {
        p_store_id: storeId,
        p_categories: categoriesJson,
        p_items: itemsJson,
        p_frequent_items: frequentItemsJson
      });

      if (error) {
        console.error('Failed to save menu to backend:', error);
        return { ok: false, error: error.message };
      }

      return { ok: true };
    } catch (error: any) {
      console.error('Error saving menu to backend:', error);
      return { ok: false, error: error.message || 'Failed to save menu' };
    }
  },

  updateFrequentItemsInBackend: async (itemIds: string[]) => {
    try {
      const storeId = await getStoreId();
      
      // Convert string UUIDs to UUID array
      const itemUuids = itemIds.map(id => id);

      const { error } = await supabase.rpc('update_frequent_items', {
        p_store_id: storeId,
        p_item_ids: itemUuids
      });

      if (error) {
        console.error('Failed to update frequent items in backend:', error);
        return { ok: false, error: error.message };
      }

      // Update local store
      set({ frequentItemIds: itemIds });
      
      // Save to localStorage
      const state = get();
      saveToStorage(state.categories, state.items, itemIds);

      return { ok: true };
    } catch (error: any) {
      console.error('Error updating frequent items in backend:', error);
      return { ok: false, error: error.message || 'Failed to update frequent items' };
    }
  },
  
  reset: () => set({ 
    categories: [], 
    items: [], 
    frequentItemIds: [] 
  }),
}));

