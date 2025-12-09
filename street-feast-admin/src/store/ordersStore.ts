import { create } from 'zustand';
import { supabase, getStoreId } from '../utils/supabase';
import { toast } from '../components/Toast';

// Order types
export type OrderType = 'DineIn' | 'Parcel' | 'EatAway';
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
  tableNumber?: number;
  licensePlate?: string;
  orderItems: OrderItem[];
}

// Draft order (for POS)
export interface DraftOrder {
  type: OrderType;
  chefTip: string;
  tableNumber?: number;
  licensePlate?: string;
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

// Order numbers are now assigned by database trigger (orders_assign_number)
// No need for localStorage counter - both admin and waiter use the same sequential numbering

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
  fetchOrders: () => Promise<void>;
  
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
      // Note: number is set to null - database trigger will assign sequential number
      const orderJson: any = {
        number: null, // Let database trigger assign the number
        type: state.draft.type,
        chef_tip: state.draft.chefTip.trim(),
        status: 'Created',
        created_by: user?.id || null,
        parent_order_id: null,
        created_at: now,
        updated_at: now
      };
      
      // Add table_number or license_plate based on order type
      if (state.draft.type === 'DineIn' && state.draft.tableNumber) {
        orderJson.table_number = state.draft.tableNumber;
      } else if (state.draft.type === 'EatAway' && state.draft.licensePlate) {
        orderJson.license_plate = state.draft.licensePlate;
      }

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

      // Fetch the created order to get the assigned order number
      const { data: createdOrder, error: fetchError } = await supabase
        .from('orders')
        .select('number')
        .eq('id', orderId)
        .single();

      if (fetchError || !createdOrder) {
        console.error('Failed to fetch created order number:', fetchError);
        // Continue with order creation even if fetch fails
      }

      // Update local state
      const order: Order = {
        id: orderId as string,
        orderNumber: createdOrder?.number || 0, // Use number assigned by database trigger
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

        // Note: number is set to null - database trigger will assign sequential number
        const orderJson = {
          number: null, // Let database trigger assign the number
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

        // Fetch the created order to get the assigned order number
        const { data: createdOrder, error: fetchError } = await supabase
          .from('orders')
          .select('number')
          .eq('id', newOrderId)
          .single();

        if (fetchError) {
          console.error('Failed to fetch created order number:', fetchError);
        }

        // Update local state
        const newOrder: Order = {
          id: newOrderId as string,
          orderNumber: createdOrder?.number || 0, // Use number assigned by database trigger
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

  fetchOrders: async () => {
    try {
      const storeId = await getStoreId();
      console.log('[fetchOrders] Fetching orders for store:', storeId);
      
      const { data: orders, error } = await supabase
        .from('orders')
        .select('*')
        .eq('store_id', storeId)
        .order('created_at', { ascending: false });
      
      if (error) {
        console.error('[fetchOrders] Error fetching orders:', error);
        return;
      }
      
      if (!orders || orders.length === 0) {
        console.log('[fetchOrders] No orders found for store');
        set({ orders: [] });
        return;
      }
      
      // Debug: Log status breakdown
      const statusBreakdown = orders.reduce((acc: any, order: any) => {
        acc[order.status] = (acc[order.status] || 0) + 1;
        return acc;
      }, {});
      console.log('[fetchOrders] Status breakdown:', statusBreakdown);
      console.log('[fetchOrders] Total orders fetched:', orders.length);
      console.log('[fetchOrders] Store ID:', storeId);
      
      // Fetch order items for all orders
      const orderIds = orders.map(o => o.id);
      const { data: items, error: itemsError } = await supabase
        .from('order_items')
        .select('*')
        .in('order_id', orderIds);
      
      if (itemsError) {
        console.error('[fetchOrders] Error fetching order items:', itemsError);
        // Continue with orders even if items fetch fails
      }
      
      // Convert to local Order format
      const localOrders: Order[] = orders.map(order => {
        const orderItems = (items || []).filter(item => item.order_id === order.id);
        return {
          id: order.id,
          orderNumber: order.number || 0,
          type: (order.type || 'DineIn') as OrderType,
          chefTip: order.chef_tip || '',
          status: (order.status || 'Created') as OrderStatus,
          createdAt: order.created_at ? new Date(order.created_at).getTime() : Date.now(),
          updatedAt: order.updated_at ? new Date(order.updated_at).getTime() : Date.now(),
          tableNumber: order.table_number || undefined,
          licensePlate: order.license_plate || undefined,
          orderItems: orderItems.map((item: any) => ({
            id: item.id,
            itemId: item.sku || '',
            nameSnapshot: item.name || '',
            size: item.size || null,
            vegFlagSnapshot: (item.veg_flag || 'Both') as 'Veg' | 'NonVeg' | 'Both',
            qty: item.quantity || 1,
            chefTip: (item.modifiers?.chefTip || '') as string
          }))
        };
      });
      
      set({ orders: localOrders });
      console.log('[fetchOrders] Loaded', localOrders.length, 'orders');
    } catch (error: any) {
      console.error('[fetchOrders] Exception:', error);
    }
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
        tableNumber: supabaseOrder.table_number || undefined,
        licensePlate: supabaseOrder.license_plate || undefined,
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
        const newRow = payload.new as any;
        const oldRow = payload.old as any;
        
        console.log('[Realtime] 📨 Event received:', {
          eventType: payload.eventType,
          orderId: newRow?.id || oldRow?.id,
          orderNumber: newRow?.number || oldRow?.number,
          status: newRow?.status || oldRow?.status,
          timestamp: new Date().toISOString()
        });

        const { eventType } = payload;

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
              orderId: newRow?.id,
              orderNumber: newRow?.number
            });
            if (newRow) {
              await useOrdersStore.getState().upsertOrder(newRow);
              
              // Show in-app toast notification for status changes
              const oldStatus = oldRow?.status as string;
              const newStatus = newRow?.status as string;
              const orderNumber = newRow?.number || 'N/A';
              
              console.log('[Realtime] Status check:', {
                oldStatus,
                newStatus,
                statusChanged: oldStatus && oldStatus !== newStatus,
                shouldNotify: newStatus === 'InKitchen' || newStatus === 'Prepared'
              });
              
              // Show toast notifications for InKitchen and Prepared regardless of oldStatus
              // (prioritize toasts since they work reliably)
              if (newStatus === 'InKitchen') {
                toast.info(`Chef has accepted Order #${orderNumber}`);
              } else if (newStatus === 'Prepared') {
                toast.success(`Chef has prepared Order #${orderNumber}`);
              } else if (newStatus === 'Delivered') {
                // Show toast for delivered orders (check oldStatus to avoid duplicates on initial load)
                if (oldStatus && oldStatus !== newStatus) {
                  console.log('[Realtime] Showing Delivered toast for Order #' + orderNumber);
                  toast.info(`Waiter has delivered Order #${orderNumber}`);
                } else if (!oldStatus) {
                  // If no oldStatus, it's likely a new subscription - still show toast
                  console.log('[Realtime] Showing Delivered toast (no oldStatus) for Order #' + orderNumber);
                  toast.info(`Waiter has delivered Order #${orderNumber}`);
                }
              }
              
              // Show notification for status changes (works in both Electron and browser)
              if (newRow.status) {
                const status = newRow.status as string;
                console.log('[Realtime] Checking notification for status:', status);
                if (status === 'InKitchen' || status === 'Prepared') {
                  console.log('[Realtime] ✅ Showing notification for status:', status, 'Order #', orderNumber);
                  showBrowserNotification(newRow);
                } else {
                  console.log('[Realtime] ⏭️ Skipping notification - status not InKitchen or Prepared:', status);
                }
              } else {
                console.warn('[Realtime] ⚠️ No status found in newRow:', newRow);
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
      console.log('[Realtime] Subscription status changed:', status, 'at', new Date().toISOString());
      if (err) {
        console.error('[Realtime] Subscription error details:', {
          error: err,
          message: err?.message,
          details: err,
          timestamp: new Date().toISOString()
        });
      }
      if (status === 'SUBSCRIBED') {
        console.log('[Realtime] ✅ Successfully subscribed to orders changes for store:', storeId);
        console.log('[Realtime] Listening for INSERT, UPDATE, DELETE events on orders table');
      } else if (status === 'CHANNEL_ERROR') {
        console.error('[Realtime] ❌ Channel error occurred - check RLS policies and authentication');
        console.error('[Realtime] Verify user has SELECT permission on orders table for store:', storeId);
      } else if (status === 'TIMED_OUT') {
        console.error('[Realtime] ❌ Subscription timed out - network or server issue');
      } else if (status === 'CLOSED') {
        console.warn('[Realtime] ⚠️ Channel closed');
      } else {
        console.log('[Realtime] Subscription status:', status);
      }
    });

  // Return cleanup function
  return () => {
    console.log('[initOrdersRealtime] Cleaning up realtime subscription');
    supabase.removeChannel(channel);
  };
}

// Notification helper that works in both Electron and browser
function showBrowserNotification(order: any): void {
  console.log('[Notification] Function called with order:', {
    id: order?.id,
    number: order?.number,
    status: order?.status
  });
  
  const orderNumber = order.number || 'N/A';
  const status = order.status;
  let title = '';
  let body = '';

  if (status === 'InKitchen') {
    title = `Order #${orderNumber} accepted`;
    body = 'Chef has accepted Order #' + orderNumber;
  } else if (status === 'Prepared') {
    title = `Order #${orderNumber} ready`;
    body = 'Chef has prepared Order #' + orderNumber;
  }

  if (!title || !body) {
    console.warn('[Notification] No title/body generated, status was:', status);
    return;
  }

  console.log('[Notification] Attempting to show notification:', { 
    title, 
    body, 
    hasElectronAPI: !!window.electronAPI, 
    hasNotificationAPI: 'Notification' in window,
    browserPermission: 'Notification' in window ? Notification.permission : 'N/A'
  });

  // Priority 1: Use Electron notifications if available (Electron app)
  if (window.electronAPI) {
    try {
      console.log('[Notification] Sending Electron notification:', { title, body });
      window.electronAPI.notify({ title, body });
      return;
    } catch (error) {
      console.error('[Notification] Electron notification failed:', error);
      // Fall through to browser notification
    }
  }

  // Priority 2: Use browser Notification API (web browser or Electron fallback)
  if (!('Notification' in window)) {
    console.warn('[Notification] Notifications not supported in this environment');
    return;
  }

  console.log('[Notification] Browser permission status:', Notification.permission);

  // Check if permission is already granted
  if (Notification.permission === 'granted') {
    try {
      console.log('[Notification] Showing browser notification:', { title, body });
      const notification = new Notification(title, { 
        body,
        icon: '/assets/logo/logo.png', // Use existing logo
        badge: '/assets/logo/logo.png',
        tag: `order-${orderNumber}`, // Prevent duplicate notifications
        requireInteraction: false,
        silent: false
      });
      
      // Store reference to prevent garbage collection
      console.log('[Notification] Notification created successfully:', notification);
      
      // Handle notification click
      notification.onclick = () => {
        console.log('[Notification] Notification clicked');
        window.focus();
        notification.close();
      };
      
      // Handle notification close
      notification.onclose = () => {
        console.log('[Notification] Notification closed');
      };
      
      // Handle notification error
      notification.onerror = (error) => {
        console.error('[Notification] Notification error:', error);
      };
      
      // Auto-close after 5 seconds
      setTimeout(() => {
        notification.close();
      }, 5000);
      
    } catch (error) {
      console.error('[Notification] Browser notification failed:', error);
    }
  } 
  // Request permission if not denied
  else if (Notification.permission !== 'denied') {
    console.log('[Notification] Requesting notification permission...');
    Notification.requestPermission().then((permission) => {
      console.log('[Notification] Permission result:', permission);
      if (permission === 'granted') {
        try {
          console.log('[Notification] Permission granted, showing notification:', { title, body });
          const notification = new Notification(title, { 
            body,
            icon: '/assets/logo/logo.png', // Use existing logo
            badge: '/assets/logo/logo.png',
            tag: `order-${orderNumber}`,
            requireInteraction: false,
            silent: false
          });
          
          console.log('[Notification] Notification created after permission grant:', notification);
          
          // Handle notification click
          notification.onclick = () => {
            console.log('[Notification] Notification clicked');
            window.focus();
            notification.close();
          };
          
          // Handle notification close
          notification.onclose = () => {
            console.log('[Notification] Notification closed');
          };
          
          // Handle notification error
          notification.onerror = (error) => {
            console.error('[Notification] Notification error:', error);
          };
          
          // Auto-close after 5 seconds
          setTimeout(() => {
            notification.close();
          }, 5000);
          
        } catch (error) {
          console.error('[Notification] Browser notification failed after permission grant:', error);
        }
      } else {
        console.warn('[Notification] Notification permission denied');
      }
    }).catch((error) => {
      console.error('[Notification] Error requesting notification permission:', error);
    });
  } else {
    console.warn('[Notification] Notification permission was previously denied');
  }
}
