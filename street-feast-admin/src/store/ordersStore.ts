import { create } from 'zustand';
import { collection, updateDoc, doc, serverTimestamp, writeBatch, onSnapshot, Unsubscribe, DocumentChange, getDocs } from 'firebase/firestore';
import { getAuth } from 'firebase/auth';
import { db, getOrdersCollectionPath, app } from '../utils/firebase';
import { toast } from '../components/Toast';

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
  chefTip: string; // Individual chef tip for this order item
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
  isSyncingFromFirestore: boolean;
  firestoreUnsubscribe: Unsubscribe | null;

  // Draft management
  setDraft: (patch: Partial<DraftOrder>) => void;
  clearDraft: () => void;
  addDraftLine: (line: OrderItem) => void;
  updateDraftLine: (id: string, patch: Partial<OrderItem>) => void;
  removeDraftLine: (id: string) => void;

  // Order operations
  placeDraft: () => Promise<{ ok: boolean; error?: string; order?: Order }>;
  updateStatus: (orderId: string, newStatus: OrderStatus) => Promise<boolean>;
  addItemsToOrder: (orderId: string, items: OrderItem[]) => Promise<boolean>;

  // Firestore sync
  syncFromFirestore: () => void;
  loadOrdersFromFirestore: () => Promise<void>;
  
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
  isSyncingFromFirestore: false,
  firestoreUnsubscribe: null,

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
  placeDraft: async () => {
    const state = get();
    
    if (state.draft.orderItems.length === 0) {
      return { ok: false, error: 'Add at least one item to the order' };
    }

    // Skip Firestore write if we're syncing from Firestore (to prevent loops)
    if (state.isSyncingFromFirestore) {
      // If syncing, just update local state - Firestore already has the data
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
    }

    const orderNumber = getNextOrderNumber();
    const auth = getAuth(app);
    const currentUser = auth.currentUser;

    try {
      const batch = writeBatch(db);
      
      // Create order document
      const ordersRef = collection(db, getOrdersCollectionPath());
      const orderDocRef = doc(ordersRef);
      
      const orderData = {
        orderNumber,
        type: state.draft.type,
        chefTip: state.draft.chefTip.trim(),
        status: 'Created' as OrderStatus,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
        createdBy: currentUser?.uid || null,
        parentOrderId: null as string | null
      };

      batch.set(orderDocRef, orderData);

      // Add orderItems to subcollection
      const orderItemsRef = collection(db, getOrdersCollectionPath(), orderDocRef.id, 'orderItems');
      for (const item of state.draft.orderItems) {
        const itemDocRef = doc(orderItemsRef);
        batch.set(itemDocRef, {
          itemId: item.itemId,
          nameSnapshot: item.nameSnapshot,
          size: item.size,
          vegFlagSnapshot: item.vegFlagSnapshot,
          qty: item.qty,
          chefTip: item.chefTip,
          createdAt: serverTimestamp()
        });
      }

      // Commit batch
      await batch.commit();

      // Update local state with Firestore document ID
      const now = Date.now();
      const order: Order = {
        id: orderDocRef.id,
        orderNumber,
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
    } catch (error: any) {
      console.error('Failed to save order to Firestore:', error);
      const errorMessage = error.code === 'permission-denied' 
        ? 'Permission denied. Check Firestore rules.'
        : error.message || 'Failed to save order';
      toast.error(errorMessage);
      return { ok: false, error: errorMessage };
    }
  },

  updateStatus: async (orderId, newStatus) => {
    const state = get();
    const order = state.orders.find(o => o.id === orderId);
    
    if (!order) return false;
    
    const allowed = ALLOWED_TRANSITIONS[order.status];
    if (!allowed.includes(newStatus)) {
      console.warn(`Transition from ${order.status} to ${newStatus} not allowed`);
      return false;
    }

    // Skip Firestore write if we're syncing from Firestore
    if (state.isSyncingFromFirestore) {
      set((state) => ({
        orders: state.orders.map(o =>
          o.id === orderId
            ? { ...o, status: newStatus, updatedAt: Date.now() }
            : o
        )
      }));
      return true;
    }

    try {
      const orderRef = doc(db, getOrdersCollectionPath(), orderId);
      await updateDoc(orderRef, {
        status: newStatus,
        updatedAt: serverTimestamp()
      });

      // Update local state
      set((state) => ({
        orders: state.orders.map(o =>
          o.id === orderId
            ? { ...o, status: newStatus, updatedAt: Date.now() }
            : o
        )
      }));

      return true;
    } catch (error: any) {
      console.error('Failed to update order status in Firestore:', error);
      const errorMessage = error.code === 'permission-denied' 
        ? 'Permission denied. Check Firestore rules.'
        : error.message || 'Failed to update order status';
      toast.error(errorMessage);
      return false;
    }
  },

  addItemsToOrder: async (orderId, items) => {
    const state = get();
    const order = state.orders.find(o => o.id === orderId);
    
    if (!order) return false;

    // Determine if this is an add-on (order already in kitchen)
    const isAddOn = order.status === 'InKitchen' || order.status === 'Prepared' || order.status === 'Delivered';

    // Skip Firestore write if we're syncing from Firestore
    if (state.isSyncingFromFirestore) {
      set((state) => ({
        orders: state.orders.map(o =>
          o.id === orderId
            ? { ...o, orderItems: [...o.orderItems, ...items], updatedAt: Date.now() }
            : o
        )
      }));
      return true;
    }

    try {
      const batch = writeBatch(db);
      const orderRef = doc(db, getOrdersCollectionPath(), orderId);

      // If add-on, create new order document with parentOrderId
      if (isAddOn) {
        const newOrderRef = doc(collection(db, getOrdersCollectionPath()));
        const auth = getAuth(app);
        const addOnOrderNumber = getNextOrderNumber();
        batch.set(newOrderRef, {
          orderNumber: addOnOrderNumber,
          type: order.type,
          chefTip: '',
          status: 'Created' as OrderStatus,
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp(),
          createdBy: auth.currentUser?.uid || null,
          parentOrderId: orderId
        });

        // Add items to new order's subcollection
        const newOrderItemsRef = collection(db, getOrdersCollectionPath(), newOrderRef.id, 'orderItems');
        for (const item of items) {
          const itemDocRef = doc(newOrderItemsRef);
          batch.set(itemDocRef, {
            itemId: item.itemId,
            nameSnapshot: item.nameSnapshot,
            size: item.size,
            vegFlagSnapshot: item.vegFlagSnapshot,
            qty: item.qty,
            chefTip: item.chefTip,
            createdAt: serverTimestamp()
          });
        }

        // Create new order locally
        const now = Date.now();
        const newOrder: Order = {
          id: newOrderRef.id,
          orderNumber: addOnOrderNumber,
          type: order.type,
          chefTip: '',
          status: 'Created',
          createdAt: now,
          updatedAt: now,
          orderItems: items.map(item => ({ ...item }))
        };

        set((state) => ({
          orders: [newOrder, ...state.orders]
        }));

        await batch.commit();
        return true;
      } else {
        // Regular add - add items to existing order
        const orderItemsRef = collection(db, getOrdersCollectionPath(), orderId, 'orderItems');
        for (const item of items) {
          const itemDocRef = doc(orderItemsRef);
          batch.set(itemDocRef, {
            itemId: item.itemId,
            nameSnapshot: item.nameSnapshot,
            size: item.size,
            vegFlagSnapshot: item.vegFlagSnapshot,
            qty: item.qty,
            chefTip: item.chefTip,
            createdAt: serverTimestamp()
          });
        }

        // Update order timestamp
        batch.update(orderRef, {
          updatedAt: serverTimestamp()
        });

        await batch.commit();

        // Update local state
        set((state) => ({
          orders: state.orders.map(o =>
            o.id === orderId
              ? { ...o, orderItems: [...o.orderItems, ...items], updatedAt: Date.now() }
              : o
          )
        }));

        return true;
      }
    } catch (error: any) {
      console.error('Failed to add items to order in Firestore:', error);
      const errorMessage = error.code === 'permission-denied' 
        ? 'Permission denied. Check Firestore rules.'
        : error.message || 'Failed to add items to order';
      toast.error(errorMessage);
      return false;
    }
  },

  // Helpers
  getAllowedTransitions: (status) => {
    return ALLOWED_TRANSITIONS[status] || [];
  },

  getFilteredOrders: (statusFilter) => {
    const state = get();
    if (statusFilter === 'All') return state.orders;
    return state.orders.filter(o => o.status === statusFilter);
  },

  // Firestore sync
  syncFromFirestore: () => {
    const state = get();
    
    // Cleanup existing listener
    if (state.firestoreUnsubscribe) {
      state.firestoreUnsubscribe();
    }

    const ordersRef = collection(db, getOrdersCollectionPath());
    const unsubscribe = onSnapshot(ordersRef, (snapshot) => {
      set({ isSyncingFromFirestore: true });

      snapshot.docChanges().forEach((change: DocumentChange) => {
        const data = change.doc.data();
        const orderId = change.doc.id;

        if (change.type === 'added' || change.type === 'modified') {
          // Check if order already exists locally
          const existingOrder = get().orders.find(o => o.id === orderId);
          
          // Convert Firestore timestamps to numbers
          const createdAt = data.createdAt?.toMillis?.() || Date.now();
          const updatedAt = data.updatedAt?.toMillis?.() || Date.now();

          // Fetch orderItems subcollection for this order
          // Note: We'll do a simplified sync - orderItems from subcollection would need a separate listener
          // For now, we'll keep the local orderItems if order exists, or empty array if new
          const orderItems = existingOrder?.orderItems || [];

          const order: Order = {
            id: orderId,
            orderNumber: data.orderNumber || 0,
            type: (data.type || 'DineIn') as OrderType,
            chefTip: data.chefTip || '',
            status: (data.status || 'Created') as OrderStatus,
            createdAt,
            updatedAt,
            orderItems
          };

          if (change.type === 'added') {
            // Add new order if not exists
            const currentOrders = get().orders;
            if (!currentOrders.find(o => o.id === orderId)) {
              set((state) => ({
                orders: [order, ...state.orders]
              }));
            }
          } else {
            // Update existing order
            set((state) => ({
              orders: state.orders.map(o =>
                o.id === orderId ? order : o
              )
            }));
          }
        } else if (change.type === 'removed') {
          // Remove order
          set((state) => ({
            orders: state.orders.filter(o => o.id !== orderId)
          }));
        }
      });

      // Reset sync flag after processing
      setTimeout(() => {
        set({ isSyncingFromFirestore: false });
      }, 100);
    }, (error) => {
      console.error('Firestore sync error:', error);
      set({ isSyncingFromFirestore: false });
    });

    set({ firestoreUnsubscribe: unsubscribe });
  },

  loadOrdersFromFirestore: async () => {
    try {
      const ordersRef = collection(db, getOrdersCollectionPath());
      const snapshot = await getDocs(ordersRef);
      
      const orders: Order[] = [];
      
      for (const docSnap of snapshot.docs) {
        const data = docSnap.data();
        const createdAt = data.createdAt?.toMillis?.() || Date.now();
        const updatedAt = data.updatedAt?.toMillis?.() || Date.now();

        // Load orderItems from subcollection
        const orderItemsRef = collection(db, getOrdersCollectionPath(), docSnap.id, 'orderItems');
        const itemsSnapshot = await getDocs(orderItemsRef);
        const orderItems: OrderItem[] = itemsSnapshot.docs.map(itemDoc => ({
          id: itemDoc.id,
          itemId: itemDoc.data().itemId || '',
          nameSnapshot: itemDoc.data().nameSnapshot || '',
          size: itemDoc.data().size || null,
          vegFlagSnapshot: (itemDoc.data().vegFlagSnapshot || 'Veg') as 'Veg' | 'NonVeg' | 'Both',
          qty: itemDoc.data().qty || 0,
          chefTip: itemDoc.data().chefTip || ''
        }));

        orders.push({
          id: docSnap.id,
          orderNumber: data.orderNumber || 0,
          type: (data.type || 'DineIn') as OrderType,
          chefTip: data.chefTip || '',
          status: (data.status || 'Created') as OrderStatus,
          createdAt,
          updatedAt,
          orderItems
        });
      }

      // Sort by createdAt descending (newest first)
      orders.sort((a, b) => b.createdAt - a.createdAt);

      set({ orders, isSyncingFromFirestore: false });
      return;
    } catch (error: any) {
      console.error('Failed to load orders from Firestore:', error);
      toast.error('Failed to load orders from server');
      set({ isSyncingFromFirestore: false });
    }
  },

  // Store management
  setOrders: (orders) => set({ orders }),

  reset: () => {
    const state = get();
    if (state.firestoreUnsubscribe) {
      state.firestoreUnsubscribe();
    }
    set({ orders: [], draft: { ...initialDraft }, firestoreUnsubscribe: null });
  }
}));

