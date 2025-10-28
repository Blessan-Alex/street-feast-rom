import React, { useState, useMemo } from 'react';
import { useOrdersStore, Order, OrderStatus } from '../store/ordersStore';
import { Button } from '../components/Button';
import { Dialog } from '../components/Dialog';
import { toast } from '../components/Toast';

const STATUS_COLORS: Record<OrderStatus, string> = {
  Created: 'bg-status-created text-white',
  Accepted: 'bg-status-accepted text-white',
  InKitchen: 'bg-status-inkitchen text-white',
  Prepared: 'bg-status-prepared text-white',
  Delivered: 'bg-status-delivered text-white',
  Closed: 'bg-status-closed text-white',
  Canceled: 'bg-status-canceled text-white'
};

export const ManageOrders: React.FC = () => {
  const { orders, updateStatus, getAllowedTransitions } = useOrdersStore();
  const [statusFilter, setStatusFilter] = useState<string>('All');
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [confirmDialog, setConfirmDialog] = useState<{
    isOpen: boolean;
    orderId: string;
    newStatus: OrderStatus;
  } | null>(null);

  const statuses = ['All', 'Created', 'Accepted', 'InKitchen', 'Prepared', 'Delivered', 'Closed', 'Canceled'];

  const filteredOrders = useMemo(() => {
    if (statusFilter === 'All') return orders;
    return orders.filter(o => o.status === statusFilter);
  }, [orders, statusFilter]);

  const formatTime = (timestamp: number) => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: true
    });
  };

  const formatTimeAgo = (timestamp: number) => {
    const minutes = Math.floor((Date.now() - timestamp) / 60000);
    if (minutes < 1) return 'Just now';
    if (minutes === 1) return '1 min ago';
    if (minutes < 60) return `${minutes} mins ago`;
    const hours = Math.floor(minutes / 60);
    if (hours === 1) return '1 hour ago';
    return `${hours} hours ago`;
  };

  const handleStatusChange = (orderId: string, newStatus: OrderStatus) => {
    if (newStatus === 'Canceled') {
      setConfirmDialog({ isOpen: true, orderId, newStatus });
    } else {
      applyStatusChange(orderId, newStatus);
    }
  };

  const applyStatusChange = (orderId: string, newStatus: OrderStatus) => {
    const success = updateStatus(orderId, newStatus);
    if (success) {
      toast.success(`Order status updated to ${newStatus}`);
      setConfirmDialog(null);
    } else {
      toast.error('Failed to update order status');
    }
  };

  return (
    <div className="max-w-7xl mx-auto">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Manage Orders</h1>
        <p className="text-gray-600">View and manage all orders</p>
      </div>

      {/* Status Filter Pills */}
      <div className="mb-6 flex flex-wrap gap-2">
        {statuses.map(status => {
          const count = status === 'All'
            ? orders.length
            : orders.filter(o => o.status === status).length;
          
          return (
            <button
              key={status}
              onClick={() => setStatusFilter(status)}
              className={`px-4 py-2 rounded-lg font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-action-primary ${
                statusFilter === status
                  ? 'bg-action-primary text-white'
                  : 'bg-white text-gray-700 hover:bg-gray-100'
              }`}
            >
              {status} ({count})
            </button>
          );
        })}
      </div>

      {/* Orders List */}
      {filteredOrders.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-12 text-center">
          <p className="text-gray-500">No orders found</p>
          <p className="text-sm text-gray-400 mt-1">
            {statusFilter !== 'All' ? `No orders with status "${statusFilter}"` : 'Create your first order to get started'}
          </p>
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="w-full">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Order #</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Items</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Chef Tip</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Time</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filteredOrders.map(order => (
                <tr key={order.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">#{order.orderNumber}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className="text-sm text-gray-900">
                      {order.type === 'DineIn' ? 'Dine-in' : order.type}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className="text-sm text-gray-900">{order.orderItems.length}</span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {order.chefTip ? (
                      <span className="text-sm text-gray-600" title={order.chefTip}>✓</span>
                    ) : (
                      <span className="text-sm text-gray-400">-</span>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${STATUS_COLORS[order.status]}`}>
                      {order.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <div>{formatTime(order.createdAt)}</div>
                    <div className="text-xs text-gray-400">{formatTimeAgo(order.createdAt)}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    <Button
                      variant="secondary"
                      size="small"
                      onClick={() => setSelectedOrder(order)}
                    >
                      View
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Order Detail Modal */}
      {selectedOrder && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black bg-opacity-50" onClick={() => setSelectedOrder(null)} />
          
          <div className="relative bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto animate-fade-in">
            {/* Header */}
            <div className="px-6 py-4 border-b sticky top-0 bg-white flex items-center justify-between">
              <div>
                <h2 className="text-2xl font-bold text-gray-900">Order #{selectedOrder.orderNumber}</h2>
                <p className="text-sm text-gray-600">
                  Created: {new Date(selectedOrder.createdAt).toLocaleString('en-US', {
                    month: 'short',
                    day: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit',
                    hour12: true
                  })}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <span className={`px-3 py-1 text-sm font-semibold rounded ${STATUS_COLORS[selectedOrder.status]}`}>
                  {selectedOrder.status}
                </span>
                <button
                  onClick={() => setSelectedOrder(null)}
                  className="text-gray-500 hover:text-gray-700 text-2xl font-bold w-8 h-8 flex items-center justify-center rounded hover:bg-gray-100"
                >
                  ×
                </button>
              </div>
            </div>

            {/* Content */}
            <div className="px-6 py-4">
              {/* Type */}
              <div className="mb-4">
                <span className="px-3 py-1 bg-gray-100 text-gray-800 rounded text-sm font-medium">
                  {selectedOrder.type === 'DineIn' ? 'Dine-in' : selectedOrder.type}
                </span>
              </div>

              {/* Chef Tip */}
              {selectedOrder.chefTip && (
                <div className="mb-6 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
                  <h3 className="text-sm font-semibold text-yellow-900 mb-1">Chef Tip:</h3>
                  <p className="text-sm text-yellow-800">{selectedOrder.chefTip}</p>
                </div>
              )}

              {/* Order Items */}
              <div className="mb-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-3">Items</h3>
                <div className="space-y-2">
                  {selectedOrder.orderItems.map(item => (
                    <div key={item.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                      <div className="flex-1">
                        <div className="font-medium text-gray-900">
                          {item.nameSnapshot}
                          {item.size && <span className="text-gray-600"> — {item.size}</span>}
                        </div>
                        <span className={`text-xs px-2 py-0.5 rounded mt-1 inline-block ${
                          item.vegFlagSnapshot === 'Veg'
                            ? 'bg-green-100 text-green-800'
                            : 'bg-red-100 text-red-800'
                        }`}>
                          {item.vegFlagSnapshot}
                        </span>
                        {/* Add chef tip display for individual items */}
                        {item.chefTip && (
                          <div className="mt-1">
                            <span className="text-xs text-gray-600 italic">
                              💡 {item.chefTip}
                            </span>
                          </div>
                        )}
                      </div>
                      <div className="text-lg font-semibold text-gray-900">× {item.qty}</div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Status Transitions */}
              <div>
                <h3 className="text-lg font-semibold text-gray-900 mb-3">Actions</h3>
                <div className="flex flex-wrap gap-2">
                  {getAllowedTransitions(selectedOrder.status).map(nextStatus => (
                    <Button
                      key={nextStatus}
                      variant={nextStatus === 'Canceled' ? 'danger' : 'primary'}
                      onClick={() => handleStatusChange(selectedOrder.id, nextStatus)}
                    >
                      {nextStatus === 'Canceled' ? 'Cancel Order' : `Mark as ${nextStatus}`}
                    </Button>
                  ))}
                  {getAllowedTransitions(selectedOrder.status).length === 0 && (
                    <p className="text-sm text-gray-500">No actions available for this order</p>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Confirm Cancel Dialog */}
      {confirmDialog && (
        <Dialog
          isOpen={confirmDialog.isOpen}
          onClose={() => setConfirmDialog(null)}
          title="Cancel Order"
          message={`Are you sure you want to cancel this order? This action cannot be undone.`}
          confirmText="Yes, Cancel Order"
          cancelText="No, Keep Order"
          onConfirm={() => applyStatusChange(confirmDialog.orderId, confirmDialog.newStatus)}
          confirmVariant="danger"
        />
      )}
    </div>
  );
};

