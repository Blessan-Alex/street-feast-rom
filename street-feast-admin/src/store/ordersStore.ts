import { create } from 'zustand';

// Order types
export type OrderType = 'DineIn' | 'Parcel' | 'Delivery';
export type OrderStatus = 'Created' | 'Accepted' | 'InKitchen' | 'Prepared' | 'Delivered' | 'Closed' | 'Canceled';

// Order item (snapshot of menu item at time of order)
export interface OrderItem {
  id: string;
  itemId: string;
  nameSnapshot: string;
  size: string | null; // Now supports any size format
  vegFlagSnapshot: 'Veg' | 'NonVeg' | 'Both';
  qty: number;
}

// Order structure
export interface Order {
  id: string;
  orderNumber: number;
  type: OrderType;
  chefTip: string;
  status: OrderStatus;
  createdAt: number;
  updatedAt: number;
  orderItems: OrderItem[];
}

// Draft order (for POS)
export interface DraftOrder {
  type: OrderType;
  chefTip: string;
  orderItems: OrderItem[];
}

// Allowed transitions map (SINGLE SOURCE OF TRUTH)
const ALLOWED_TRANSITIONS: Record<OrderStatus, OrderStatus[]> = {
  Created: ['Accepted', 'Canceled'],
  Accepted: ['InKitchen', 'Canceled'],
  InKitchen: ['Prepared', 'Canceled'],
  Prepared: ['Delivered'],
  Delivered: ['Closed'],
  Closed: [],
  Canceled: []
};

// Sequential order number counter
const ORDER_COUNTER_KEY = 'sf.order.counter';

const getNextOrderNumber = (): number => {
  const current = parseInt(localStorage.getItem(ORDER_COUNTER_KEY) || '1000', 10);
  const next = current + 1;
  localStorage.setItem(ORDER_COUNTER_KEY, String(next));
  return next;
};

export const clearOrderCounter = () => {
  localStorage.removeItem(ORDER_COUNTER_KEY);
};

interface OrdersStore {
  orders: Order[];
  draft: DraftOrder;

  // Draft management
  setDraft: (patch: Partial<DraftOrder>) => void;
  clearDraft: () => void;
  addDraftLine: (line: OrderItem) => void;
  updateDraftLine: (id: string, patch: Partial<OrderItem>) => void;
  removeDraftLine: (id: string) => void;

  // Order operations
  placeDraft: () => { ok: boolean; error?: string; order?: Order };
  updateStatus: (orderId: string, newStatus: OrderStatus) => boolean;
  addItemsToOrder: (orderId: string, items: OrderItem[]) => void;

  // Helpers
  getAllowedTransitions: (status: OrderStatus) => OrderStatus[];
  getFilteredOrders: (statusFilter: string) => Order[];
  
  // Store management
  setOrders: (orders: Order[]) => void;
  reset: () => void;
}

const initialDraft: DraftOrder = {
  type: 'DineIn',
  chefTip: '',
  orderItems: []
};

export const useOrdersStore = create<OrdersStore>((set, get) => ({
  orders: [],
  draft: { ...initialDraft },

  // Draft management
  setDraft: (patch) => set((state) => ({
    draft: { ...state.draft, ...patch }
  })),

  clearDraft: () => set({ draft: { ...initialDraft } }),

  addDraftLine: (line) => set((state) => ({
    draft: {
      ...state.draft,
      orderItems: [...state.draft.orderItems, line]
    }
  })),

  updateDraftLine: (id, patch) => set((state) => ({
    draft: {
      ...state.draft,
      orderItems: state.draft.orderItems.map(item =>
        item.id === id ? { ...item, ...patch } : item
      )
    }
  })),

  removeDraftLine: (id) => set((state) => ({
    draft: {
      ...state.draft,
      orderItems: state.draft.orderItems.filter(item => item.id !== id)
    }
  })),

  // Order operations
  placeDraft: () => {
    const state = get();
    
    if (state.draft.orderItems.length === 0) {
      return { ok: false, error: 'Add at least one item to the order' };
    }

    const now = Date.now();
    const order: Order = {
      id: crypto.randomUUID(),
      orderNumber: getNextOrderNumber(),
      type: state.draft.type,
      chefTip: state.draft.chefTip.trim(),
      status: 'Created',
      createdAt: now,
      updatedAt: now,
      orderItems: state.draft.orderItems.map(item => ({ ...item }))
    };

    set((state) => ({
      orders: [order, ...state.orders]
    }));

    get().clearDraft();
    return { ok: true, order };
  },

  updateStatus: (orderId, newStatus) => {
    const state = get();
    const order = state.orders.find(o => o.id === orderId);
    
    if (!order) return false;
    
    const allowed = ALLOWED_TRANSITIONS[order.status];
    if (!allowed.includes(newStatus)) {
      console.warn(`Transition from ${order.status} to ${newStatus} not allowed`);
      return false;
    }

    set((state) => ({
      orders: state.orders.map(o =>
        o.id === orderId
          ? { ...o, status: newStatus, updatedAt: Date.now() }
          : o
      )
    }));

    return true;
  },

  addItemsToOrder: (orderId, items) => set((state) => ({
    orders: state.orders.map(o =>
      o.id === orderId
        ? { ...o, orderItems: [...o.orderItems, ...items], updatedAt: Date.now() }
        : o
    )
  })),

  // Helpers
  getAllowedTransitions: (status) => {
    return ALLOWED_TRANSITIONS[status] || [];
  },

  getFilteredOrders: (statusFilter) => {
    const state = get();
    if (statusFilter === 'All') return state.orders;
    return state.orders.filter(o => o.status === statusFilter);
  },

  // Store management
  setOrders: (orders) => set({ orders }),

  reset: () => set({ orders: [], draft: { ...initialDraft } })
}));

