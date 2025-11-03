import * as admin from 'firebase-admin';
import { onDocumentCreated, onDocumentUpdated } from 'firebase-functions/v2/firestore';
import { setGlobalOptions } from 'firebase-functions/v2';

// Ensure functions run in the same region as your Firestore (asia-south1)
setGlobalOptions({ region: 'asia-south1' });

admin.initializeApp();

const ORDERS_TOPIC = 'orders';

export const onOrderCreated = onDocumentCreated('orders/{orderId}', async (event) => {
  const data = event.data?.data();
  if (!data) return;

  const orderNumber = data.orderNumber ?? 'New';
  const message: admin.messaging.Message = {
    topic: ORDERS_TOPIC,
    notification: {
      title: `New Order #${orderNumber}`,
      body: `Type: ${data.type ?? '—'}`
    },
    data: {
      orderId: event.params.orderId,
      orderNumber: String(orderNumber),
      type: data.type || 'DineIn',
      status: data.status || 'Created',
      click_action: 'OPEN_ORDER'
    },
    android: {
      priority: 'high',
      notification: {
        clickAction: 'OPEN_ORDER',
        channelId: 'orders_updates'
      }
    }
  };

  await admin.messaging().send(message);
});

export const onOrderUpdated = onDocumentUpdated('orders/{orderId}', async (event) => {
  const before = event.data?.before.data();
  const after = event.data?.after.data();
  if (!before || !after || before.status === after.status) return;

  const orderNumber = after.orderNumber ?? 'Order';
  const message: admin.messaging.Message = {
    topic: ORDERS_TOPIC,
    notification: {
      title: `Order #${orderNumber} ${after.status}`,
      body: `Status: ${before.status} → ${after.status}`
    },
    data: {
      orderId: event.params.orderId,
      orderNumber: String(orderNumber),
      status: after.status,
      click_action: 'OPEN_ORDER'
    },
    android: {
      priority: 'high',
      notification: {
        clickAction: 'OPEN_ORDER',
        channelId: 'orders_updates'
      }
    }
  };

  await admin.messaging().send(message);
});



