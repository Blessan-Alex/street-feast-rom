import { create } from 'zustand';
import { supabase, getStoreId } from '../utils/supabase';
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
  
  // Helpers
  getAllowedTransitions: (status: OrderStatus) => OrderStatus[];
  getFilteredOrders: (statusFilter: string) => Order[];
  
  // Store management
  setOrders: (orders: Order[]) => void;
  reset: () => void;
  
  // Realtime methods
  upsertOrder: (supabaseOrder: any) => Promise<void>;
  removeOrder: (orderId: string) => void;
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
  placeDraft: async () => {
    const state = get();
    
    if (state.draft.orderItems.length === 0) {
      return { ok: false, error: 'Add at least one item to the order' };
    }

    const orderNumber = getNextOrderNumber();
    const now = new Date().toISOString();

    try {
      // Get store ID
      const storeId = await getStoreId();

      // Get current user from Supabase auth
      const { data: { user } } = await supabase.auth.getUser();

      // Map order items to Supabase format
      const orderItemsJson = state.draft.orderItems.map(item => ({
        id: item.id,
        sku: item.itemId,
        name: item.nameSnapshot,
        size: item.size,
        veg_flag: item.vegFlagSnapshot,
        quantity: item.qty,
        modifiers: {
          chefTip: item.chefTip
        }
      }));

      // Prepare order JSONB for RPC
      const orderJson = {
        number: orderNumber,
        type: state.draft.type,
        chef_tip: state.draft.chefTip.trim(),
        status: 'Created',
        created_by: user?.id || null,
        parent_order_id: null,
        created_at: now,
        updated_at: now
      };

      // Call orders_upsert RPC
      const { data: orderId, error } = await supabase.rpc('orders_upsert', {
        p_store_id: storeId,
        p_order: orderJson,
        p_items: orderItemsJson,
        p_actor_id: user?.id || null
      });

      if (error) {
        console.error('Failed to save order to Supabase:', error);
        const errorMessage = error.message || 'Failed to save order';
        toast.error(errorMessage);
        return { ok: false, error: errorMessage };
      }

      // Update local state
      const order: Order = {
        id: orderId as string,
        orderNumber,
        type: state.draft.type,
        chefTip: state.draft.chefTip.trim(),
        status: 'Created',
        createdAt: Date.now(),
        updatedAt: Date.now(),
        orderItems: state.draft.orderItems.map(item => ({ ...item }))
      };

      set((state) => ({
        orders: [order, ...state.orders]
      }));

      get().clearDraft();
      return { ok: true, order };
    } catch (error: any) {
      console.error('Failed to save order:', error);
      const errorMessage = error.message || 'Failed to save order';
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

    try {
      const { error } = await supabase
        .from('orders')
        .update({
          status: newStatus,
          updated_at: new Date().toISOString()
        })
        .eq('id', orderId);

      if (error) {
        console.error('Failed to update order status in Supabase:', error);
        const errorMessage = error.message || 'Failed to update order status';
        toast.error(errorMessage);
        return false;
      }

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
      console.error('Failed to update order status:', error);
      const errorMessage = error.message || 'Failed to update order status';
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

    try {
      if (isAddOn) {
        // Create new order with parent_order_id
        const addOnOrderNumber = getNextOrderNumber();
        const now = new Date().toISOString();
        const storeId = await getStoreId();

        // Get current user
        const { data: { user } } = await supabase.auth.getUser();

        // Map order items
        const orderItemsJson = items.map(item => ({
          id: item.id,
          sku: item.itemId,
          name: item.nameSnapshot,
          size: item.size,
          veg_flag: item.vegFlagSnapshot,
          quantity: item.qty,
          modifiers: {
            chefTip: item.chefTip
          }
        }));

        const orderJson = {
          number: addOnOrderNumber,
          type: order.type,
          chef_tip: '',
          status: 'Created',
          created_by: user?.id || null,
          parent_order_id: orderId,
          created_at: now,
          updated_at: now
        };

        // Create new order via RPC
        const { data: newOrderId, error } = await supabase.rpc('orders_upsert', {
          p_store_id: storeId,
          p_order: orderJson,
          p_items: orderItemsJson,
          p_actor_id: user?.id || null
        });

        if (error) {
          console.error('Failed to add items to order:', error);
          toast.error(error.message || 'Failed to add items to order');
          return false;
        }

        // Update local state
        const newOrder: Order = {
          id: newOrderId as string,
          orderNumber: addOnOrderNumber,
          type: order.type,
          chefTip: '',
          status: 'Created',
          createdAt: Date.now(),
          updatedAt: Date.now(),
          orderItems: items.map(item => ({ ...item }))
        };

        set((state) => ({
          orders: [newOrder, ...state.orders]
        }));

        return true;
      } else {
        // Add items to existing order - need to fetch current items and update
        const storeId = await getStoreId();
        const { data: { user } } = await supabase.auth.getUser();

        // Fetch current order from Supabase
        const { data: currentOrder, error: fetchError } = await supabase
          .from('orders')
          .select('*')
          .eq('id', orderId)
          .single();

        if (fetchError || !currentOrder) {
          console.error('Failed to fetch order:', fetchError);
          toast.error('Failed to fetch order');
          return false;
        }

        // Fetch current items
        const { data: currentItems, error: itemsError } = await supabase
          .from('order_items')
          .select('*')
          .eq('order_id', orderId);

        if (itemsError) {
          console.error('Failed to fetch order items:', itemsError);
          toast.error('Failed to fetch order items');
          return false;
        }

        // Combine existing and new items
        const existingItemsJson = (currentItems || []).map(item => ({
          id: item.id,
          sku: item.sku,
          name: item.name,
          size: item.size,
          veg_flag: item.veg_flag,
          quantity: item.quantity,
          modifiers: item.modifiers || {}
        }));

        const newItemsJson = items.map(item => ({
          id: item.id,
          sku: item.itemId,
          name: item.nameSnapshot,
          size: item.size,
          veg_flag: item.vegFlagSnapshot,
          quantity: item.qty,
          modifiers: {
            chefTip: item.chefTip
          }
        }));

        const allItemsJson = [...existingItemsJson, ...newItemsJson];

        // Update order via RPC
        const orderJson = {
          id: orderId,
          number: currentOrder.number,
          type: currentOrder.type,
          chef_tip: currentOrder.chef_tip || '',
          status: currentOrder.status,
          created_by: currentOrder.created_by,
          parent_order_id: currentOrder.parent_order_id,
          created_at: currentOrder.created_at,
          updated_at: new Date().toISOString()
        };

        const { error: updateError } = await supabase.rpc('orders_upsert', {
          p_store_id: storeId,
          p_order: orderJson,
          p_items: allItemsJson,
          p_actor_id: user?.id || null
        });

        if (updateError) {
          console.error('Failed to add items to order:', updateError);
          toast.error(updateError.message || 'Failed to add items to order');
          return false;
        }

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
      console.error('Failed to add items to order:', error);
      toast.error(error.message || 'Failed to add items to order');
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

  // Store management
  setOrders: (orders) => set({ orders }),

  reset: () => {
    set({ orders: [], draft: { ...initialDraft } });
  },

  // Realtime methods
  upsertOrder: async (supabaseOrder: any) => {
    try {
      // Fetch order items for this order
      const { data: orderItems, error: itemsError } = await supabase
        .from('order_items')
        .select('*')
        .eq('order_id', supabaseOrder.id);

      if (itemsError) {
        console.error('Failed to fetch order items for realtime update:', itemsError);
        return;
      }

      // Convert Supabase order to local Order format
      const order: Order = {
        id: supabaseOrder.id,
        orderNumber: supabaseOrder.number || 0,
        type: (supabaseOrder.type || 'DineIn') as OrderType,
        chefTip: supabaseOrder.chef_tip || '',
        status: (supabaseOrder.status || 'Created') as OrderStatus,
        createdAt: supabaseOrder.created_at ? new Date(supabaseOrder.created_at).getTime() : Date.now(),
        updatedAt: supabaseOrder.updated_at ? new Date(supabaseOrder.updated_at).getTime() : Date.now(),
        orderItems: (orderItems || []).map((item: any) => ({
          id: item.id,
          itemId: item.sku || '',
          nameSnapshot: item.name || '',
          size: item.size || null,
          vegFlagSnapshot: (item.veg_flag || 'Both') as 'Veg' | 'NonVeg' | 'Both',
          qty: item.quantity || 1,
          chefTip: (item.modifiers?.chefTip || '') as string
        }))
      };

      // Upsert into store
      set((state) => {
        const existingIndex = state.orders.findIndex(o => o.id === order.id);
        if (existingIndex >= 0) {
          // Update existing order
          const updatedOrders = [...state.orders];
          updatedOrders[existingIndex] = order;
          return { orders: updatedOrders };
        } else {
          // Add new order at the beginning
          return { orders: [order, ...state.orders] };
        }
      });
    } catch (error: any) {
      console.error('Failed to upsert order from realtime:', error);
    }
  },

  removeOrder: (orderId: string) => {
    set((state) => ({
      orders: state.orders.filter(o => o.id !== orderId)
    }));
  }
}));

// Realtime subscription function
export function initOrdersRealtime(storeId: string): () => void {
  console.log('[initOrdersRealtime] Initializing realtime subscription for store:', storeId);
  
  // Check if user is authenticated
  supabase.auth.getSession().then(({ data: { session }, error }) => {
    if (error) {
      console.error('[initOrdersRealtime] Auth error:', error);
    } else {
      console.log('[initOrdersRealtime] User session:', session ? 'Authenticated' : 'Not authenticated');
      if (!session) {
        console.warn('[initOrdersRealtime] No user session - realtime may fail due to RLS');
      }
    }
  });
  
  const channel = supabase
    .channel(`realtime:admin-orders:${storeId}`, {
      config: {
        broadcast: { self: true },
        presence: { key: '' }
      }
    })
    .on(
      'postgres_changes',
      {
        event: '*', // INSERT | UPDATE | DELETE
        schema: 'public',
        table: 'orders',
        filter: `store_id=eq.${storeId}`,
      },
      async (payload) => {
        console.log('[Realtime] Event received:', {
          eventType: payload.eventType,
          new: payload.new,
          old: payload.old,
          timestamp: new Date().toISOString()
        });

        const { eventType, new: newRow, old: oldRow } = payload;

        switch (eventType) {
          case 'INSERT':
            console.log('[Realtime] INSERT event - new order:', newRow);
            if (newRow) {
              await useOrdersStore.getState().upsertOrder(newRow);
            }
            break;
            
          case 'UPDATE':
            console.log('[Realtime] UPDATE event:', {
              oldStatus: oldRow?.status,
              newStatus: newRow?.status,
              orderId: newRow?.id
            });
            if (newRow) {
              await useOrdersStore.getState().upsertOrder(newRow);
              
              // Optional: Show browser notification for status changes
              if (newRow.status) {
                const status = newRow.status as string;
                if (status === 'InKitchen' || status === 'Prepared') {
                  console.log('[Realtime] Showing browser notification for status:', status);
                  showBrowserNotification(newRow);
                }
              }
            }
            break;

          case 'DELETE':
            console.log('[Realtime] DELETE event - removing order:', oldRow?.id);
            if (oldRow) {
              useOrdersStore.getState().removeOrder(oldRow.id as string);
            }
            break;
            
          default:
            console.log('[Realtime] Unknown event type:', eventType);
        }
      }
    )
    .subscribe((status, err) => {
      console.log('[Realtime] Subscription status changed:', status);
      if (err) {
        console.error('[Realtime] Subscription error details:', {
          error: err,
          message: err?.message,
          status: err?.status,
          details: err
        });
      }
      if (status === 'SUBSCRIBED') {
        console.log('[Realtime] Successfully subscribed to orders changes');
      } else if (status === 'CHANNEL_ERROR') {
        console.error('[Realtime] Channel error occurred - check RLS policies and authentication');
        // Try to get more details
        channel.on('error', (error) => {
          console.error('[Realtime] Channel error event:', error);
        });
      } else if (status === 'TIMED_OUT') {
        console.error('[Realtime] Subscription timed out');
      } else if (status === 'CLOSED') {
        console.warn('[Realtime] Channel closed');
      }
    });

  // Return cleanup function
  return () => {
    console.log('[initOrdersRealtime] Cleaning up realtime subscription');
    supabase.removeChannel(channel);
  };
}

// Optional: Browser notification helper
function showBrowserNotification(order: any): void {
  if (!('Notification' in window)) {
    return;
  }

  if (Notification.permission === 'granted') {
    const orderNumber = order.number || 'N/A';
    const status = order.status;
    let title = '';
    let body = '';

    if (status === 'InKitchen') {
      title = `Order #${orderNumber} accepted`;
      body = 'Order is now being prepared.';
    } else if (status === 'Prepared') {
      title = `Order #${orderNumber} ready`;
      body = 'Order is ready to serve.';
    }

    if (title && body) {
      new Notification(title, { body });
    }
  } else if (Notification.permission !== 'denied') {
    // Request permission
    Notification.requestPermission().then((permission) => {
      if (permission === 'granted') {
        showBrowserNotification(order);
      }
    });
  }
}
