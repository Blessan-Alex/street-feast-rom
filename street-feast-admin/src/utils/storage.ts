import { Category, Item } from '../store/menuStore';

export const STORAGE_KEYS = {
  CATEGORIES: 'streat-feast-categories',
  ITEMS: 'streat-feast-items',
  FREQUENT: 'streat-feast-frequent',
  USER: 'streat-feast-user',
};

export const loadFromStorage = () => {
  try {
    return {
      categories: JSON.parse(localStorage.getItem(STORAGE_KEYS.CATEGORIES) || '[]') as Category[],
      items: JSON.parse(localStorage.getItem(STORAGE_KEYS.ITEMS) || '[]') as Item[],
      frequentItemIds: JSON.parse(localStorage.getItem(STORAGE_KEYS.FREQUENT) || '[]') as string[],
    };
  } catch (e) {
    console.error('Failed to load from storage:', e);
    return { categories: [], items: [], frequentItemIds: [] };
  }
};

export const saveToStorage = (categories: Category[], items: Item[], frequentItemIds: string[]) => {
  localStorage.setItem(STORAGE_KEYS.CATEGORIES, JSON.stringify(categories));
  localStorage.setItem(STORAGE_KEYS.ITEMS, JSON.stringify(items));
  localStorage.setItem(STORAGE_KEYS.FREQUENT, JSON.stringify(frequentItemIds));
};

export const clearStorage = () => {
  Object.values(STORAGE_KEYS).forEach(key => localStorage.removeItem(key));
};

