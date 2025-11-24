import React, { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useOrdersStore } from '../store/ordersStore';
import { Button } from '../components/Button';

export const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  const orders = useOrdersStore(state => state.orders);

  // Compute KPIs
  const kpis = useMemo(() => {
    // Debug: Log order status breakdown
    const statusBreakdown = orders.reduce((acc, order) => {
      acc[order.status] = (acc[order.status] || 0) + 1;
      return acc;
    }, {} as Record<string, number>);
    console.log('[Dashboard] Order status breakdown:', statusBreakdown);
    console.log('[Dashboard] Total orders:', orders.length);
    
    // Total Active should exclude Delivered, Closed, and Canceled
    // Only count orders that are actually in progress: Created, Accepted, InKitchen, Prepared
    const totalActive = orders.filter(o => 
      !['Delivered', 'Closed', 'Canceled'].includes(o.status)
    ).length;
    
    const newCount = orders.filter(o => o.status === 'Created').length;
    const inKitchen = orders.filter(o => o.status === 'InKitchen').length;
    const ready = orders.filter(o => o.status === 'Prepared').length;
    
    console.log('[Dashboard] KPIs:', { totalActive, newCount, inKitchen, ready });

    return { totalActive, newCount, inKitchen, ready };
  }, [orders]);

  // Recent orders - sorted by updatedAt (latest first)
  const newOrders = useMemo(() =>
    orders
      .filter(o => o.status === 'Created')
      .sort((a, b) => b.updatedAt - a.updatedAt), // Latest first
    [orders]
  );

  const readyOrders = useMemo(() =>
    orders
      .filter(o => o.status === 'Prepared')
      .sort((a, b) => b.updatedAt - a.updatedAt), // Latest first
    [orders]
  );

  const formatTimeAgo = (timestamp: number) => {
    const minutes = Math.floor((Date.now() - timestamp) / 60000);
    if (minutes < 1) return 'Just now';
    if (minutes === 1) return '1 min ago';
    if (minutes < 60) return `${minutes} mins ago`;
    const hours = Math.floor(minutes / 60);
    if (hours === 1) return '1 hour ago';
    return `${hours} hours ago`;
  };

  return (
    <div className="max-w-7xl mx-auto">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Dashboard</h1>
        <p className="text-gray-600">Real-time overview of your restaurant operations</p>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {/* Total Active */}
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-sm font-medium text-gray-600">Total Active</h3>
            <div className="w-3 h-3 bg-blue-500 rounded-full"></div>
          </div>
          <p className="text-4xl font-bold text-gray-900">{kpis.totalActive}</p>
          <p className="text-xs text-gray-500 mt-1">Orders in progress</p>
        </div>

        {/* New Orders */}
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-sm font-medium text-gray-600">New</h3>
            <div className="w-3 h-3 bg-status-accepted rounded-full"></div>
          </div>
          <p className="text-4xl font-bold text-gray-900">{kpis.newCount}</p>
          <p className="text-xs text-gray-500 mt-1">Awaiting acceptance</p>
        </div>

        {/* In Kitchen */}
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-sm font-medium text-gray-600">In Kitchen</h3>
            <div className="w-3 h-3 bg-status-inkitchen rounded-full"></div>
          </div>
          <p className="text-4xl font-bold text-gray-900">{kpis.inKitchen}</p>
          <p className="text-xs text-gray-500 mt-1">Being prepared</p>
        </div>

        {/* Ready */}
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-sm font-medium text-gray-600">Ready</h3>
            <div className="w-3 h-3 bg-status-prepared rounded-full"></div>
          </div>
          <p className="text-4xl font-bold text-gray-900">{kpis.ready}</p>
          <p className="text-xs text-gray-500 mt-1">Ready to serve</p>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        {/* New Orders Section */}
        <div className="bg-white rounded-lg shadow">
          <div className="px-6 py-4 border-b flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-900">New Orders</h2>
            {newOrders.length > 0 && (
              <span className="px-2 py-1 bg-status-accepted text-white text-xs font-medium rounded">
                {newOrders.length}
              </span>
            )}
          </div>
          <div className="p-6 max-h-96 overflow-y-auto">
            {newOrders.length === 0 ? (
              <div className="text-center py-8">
                <p className="text-gray-500">No new orders at the moment</p>
                <p className="text-sm text-gray-400 mt-1">New orders will appear here</p>
              </div>
            ) : (
              <div className="space-y-3">
                {newOrders.map(order => (
                  <div key={order.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-gray-900">Order #{order.orderNumber}</span>
                        <span className="text-xs px-2 py-0.5 bg-blue-100 text-blue-800 rounded">
                          {order.type}
                        </span>
                      </div>
                      <div className="flex items-center gap-3 mt-1 text-sm text-gray-600">
                        <span>{order.orderItems.length} items</span>
                        <span>•</span>
                        <span>{formatTimeAgo(order.updatedAt)}</span>
                      </div>
                    </div>
                    <Button
                      variant="secondary"
                      size="small"
                      onClick={() => navigate('/manage-orders')}
                    >
                      View
                    </Button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Ready Orders Section */}
        <div className="bg-white rounded-lg shadow">
          <div className="px-6 py-4 border-b flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-900">Ready to Serve</h2>
            {readyOrders.length > 0 && (
              <span className="px-2 py-1 bg-status-prepared text-white text-xs font-medium rounded">
                {readyOrders.length}
              </span>
            )}
          </div>
          <div className="p-6 max-h-96 overflow-y-auto">
            {readyOrders.length === 0 ? (
              <div className="text-center py-8">
                <p className="text-gray-500">No orders ready yet</p>
                <p className="text-sm text-gray-400 mt-1">Ready orders will appear here</p>
              </div>
            ) : (
              <div className="space-y-3">
                {readyOrders.map(order => (
                  <div key={order.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-gray-900">Order #{order.orderNumber}</span>
                        <span className="text-xs px-2 py-0.5 bg-green-100 text-green-800 rounded">
                          {order.type}
                        </span>
                      </div>
                      <div className="flex items-center gap-3 mt-1 text-sm text-gray-600">
                        <span>{order.orderItems.length} items</span>
                        <span>•</span>
                        <span>{formatTimeAgo(order.updatedAt)}</span>
                      </div>
                    </div>
                    <Button
                      variant="secondary"
                      size="small"
                      onClick={() => navigate('/manage-orders')}
                    >
                      View
                    </Button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Quick Action Buttons */}
      <div className="flex justify-center gap-4">
        <Button
          variant="secondary"
          size="medium"
          onClick={() => {
            console.log('[Test] Testing notification...');
            console.log('[Test] Electron API available:', !!window.electronAPI);
            console.log('[Test] Browser Notification available:', 'Notification' in window);
            if ('Notification' in window) {
              console.log('[Test] Browser permission:', Notification.permission);
            }
            
            if (window.electronAPI) {
              console.log('[Test] Sending Electron notification...');
              window.electronAPI.notify({ 
                title: 'Test Notification', 
                body: 'This is a test notification from the admin app' 
              });
            } else if ('Notification' in window) {
              if (Notification.permission === 'granted') {
                console.log('[Test] Sending browser notification...');
                new Notification('Test Notification', { 
                  body: 'This is a test notification from the admin app' 
                });
              } else {
                console.log('[Test] Requesting permission...');
                Notification.requestPermission().then((permission) => {
                  if (permission === 'granted') {
                    new Notification('Test Notification', { 
                      body: 'This is a test notification from the admin app' 
                    });
                  }
                });
              }
            }
          }}
        >
          Test Notification
        </Button>
        <Button
          variant="primary"
          size="large"
          onClick={() => navigate('/create-order')}
          className="px-8"
        >
          Create New Order
        </Button>
      </div>
    </div>
  );
};
