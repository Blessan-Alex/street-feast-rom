import { Order, DraftOrder } from '../store/ordersStore';

const STORAGE_KEYS = {
  ORDERS: 'sf.orders',
  DRAFT: 'sf.draft'
};

export const loadOrdersFromStorage = () => {
  try {
    return {
      orders: JSON.parse(localStorage.getItem(STORAGE_KEYS.ORDERS) || '[]') as Order[],
      draft: JSON.parse(localStorage.getItem(STORAGE_KEYS.DRAFT) || JSON.stringify({
        type: 'DineIn',
        chefTip: '',
        orderItems: []
      })) as DraftOrder
    };
  } catch (e) {
    console.error('Failed to load orders from storage:', e);
    return {
      orders: [],
      draft: { type: 'DineIn' as const, chefTip: '', orderItems: [] }
    };
  }
};

export const saveOrdersToStorage = (orders: Order[], draft: DraftOrder) => {
  localStorage.setItem(STORAGE_KEYS.ORDERS, JSON.stringify(orders));
  localStorage.setItem(STORAGE_KEYS.DRAFT, JSON.stringify(draft));
};

export const clearOrdersStorage = () => {
  localStorage.removeItem(STORAGE_KEYS.ORDERS);
  localStorage.removeItem(STORAGE_KEYS.DRAFT);
};

