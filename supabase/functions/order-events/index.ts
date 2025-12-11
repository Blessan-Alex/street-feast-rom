import { createClient } from "https://esm.sh/@supabase/supabase-js@2.44.3";
import { serve } from "https://deno.land/std@0.203.0/http/server.ts";

interface OrderPayload {
  type: "INSERT" | "UPDATE";
  table: "orders";
  record: Record<string, unknown>;
  old_record?: Record<string, unknown> | null;
}

interface BulkUpdatePayload {
  type: "BULK_UPDATE";
  table: "orders";
  storeId: string;
  toStatus: string;
  count: number;
}

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const ONESIGNAL_APP_ID = Deno.env.get("ONESIGNAL_APP_ID") ?? "";
const ONESIGNAL_REST_KEY = Deno.env.get("ONESIGNAL_REST_KEY") ?? "";

if (!SUPABASE_URL || !SUPABASE_SERVICE_ROLE_KEY) {
  throw new Error("Missing Supabase environment configuration");
}

const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);

const ROLE_TARGETS: Record<string, Array<"chef" | "waiter" | "admin">> = {
  Created: ["chef"],
  Accepted: ["chef"],
  InKitchen: ["chef", "admin"],
  Prepared: ["waiter", "admin"],
  Delivered: ["admin"],
  Canceled: ["chef", "waiter"],
};

async function fetchSubscriptionIds(storeId: string, roles: string[]) {
  if (roles.length === 0) return [];

  const { data, error } = await supabase
    .from("users")
    .select("onesignal_subscription_id")
    .eq("store_id", storeId)
    .in("role", roles)
    .not("onesignal_subscription_id", "is", null);

  if (error) {
    console.error("Failed to load subscription IDs", error);
    throw error;
  }

  return (data ?? [])
    .map((row) => row.onesignal_subscription_id as string)
    .filter((id) => !!id);
}

async function notifyOneSignal(subscriptionIds: string[], heading: string, content: string, data: Record<string, unknown>) {
  if (!ONESIGNAL_APP_ID || !ONESIGNAL_REST_KEY) {
    console.warn("OneSignal credentials not configured; skipping push");
    return;
  }

  if (subscriptionIds.length === 0) {
    console.log("No subscription IDs to notify");
    return;
  }

  // Determine sound based on status
  const status = data.status as string;
  const androidSound = status === "Canceled" ? "buzzer" : "ping";
  // Note: android_channel_id removed - OneSignal will use app's default channel
  // The Android app already creates "order_updates" channel locally for local notifications

  const body = {
    app_id: ONESIGNAL_APP_ID,
    include_subscription_ids: subscriptionIds,
    headings: { en: heading },
    contents: { en: content },
    data,
    android_sound: androidSound,
  };

  const response = await fetch("https://onesignal.com/api/v1/notifications", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Basic ${ONESIGNAL_REST_KEY}`,
    },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    const errText = await response.text();
    console.error("OneSignal push failed", response.status, errText);
    throw new Error(`OneSignal request failed: ${response.status}`);
  }

  console.log(`Notified OneSignal for ${subscriptionIds.length} subscriptions with sound: ${androidSound}`);
}

function resolveNotificationCopy(status: string, orderNumber?: number) {
  const numberSuffix = orderNumber ? ` #${orderNumber}` : "";

  switch (status) {
    case "InKitchen":
      return {
        heading: `Order${numberSuffix} accepted by chef`,
        content: "Order is now being prepared.",
      };
    case "Prepared":
      return {
        heading: `Order${numberSuffix} ready`,
        content: "Please deliver to the customer.",
      };
    case "Canceled":
      return {
        heading: `Order${numberSuffix} canceled`,
        content: "Review details and update the queue.",
      };
    default:
      return {
        heading: `New order${numberSuffix}`,
        content: "Review items and start preparation.",
      };
  }
}

interface ItemPreparedPayload {
  type: "ITEM_PREPARED";
  orderId: string;
  orderNumber: number;
  itemId: string;
  itemName: string;
  storeId: string;
  tableNumber?: number | null;
  licensePlate?: string | null;
  orderType: string;
}

interface OrderAlteredPayload {
  type: "ORDER_ALTERED";
  orderId: string;
  orderNumber: number;
  storeId: string;
  status: string;
  tableNumber?: number | null;
  licensePlate?: string | null;
  orderType: string;
}

interface OrderAddItemsPayload {
  type: "ORDER_ADD_ITEMS";
  orderId: string;
  orderNumber: number;
  storeId: string;
  itemCount: number;
  tableNumber?: number | null;
  licensePlate?: string | null;
  orderType: string;
}

serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  const payload = (await req.json()) as
    | OrderPayload
    | BulkUpdatePayload
    | ItemPreparedPayload
    | OrderAlteredPayload
    | OrderAddItemsPayload;

  // Handle order altered notifications (from alter_order_v2 RPC)
  if (payload.type === "ORDER_ALTERED") {
    const alteredPayload = payload as OrderAlteredPayload;
    const { storeId, orderNumber, orderId, tableNumber, licensePlate, orderType } = alteredPayload;

    if (!storeId || !orderNumber || !orderId) {
      return new Response("Bad payload", { status: 202 });
    }

    try {
      const subscriptionIds = await fetchSubscriptionIds(storeId, ["chef", "waiter", "admin"]);
      
      const heading = `Order #${orderNumber} edited`;
      const content = `Order #${orderNumber} was replaced/updated. Review changes.`;
      
      await notifyOneSignal(subscriptionIds, heading, content, {
        status: "OrderAltered",
        orderId,
        orderNumber,
        tableNumber: tableNumber ?? null,
        licensePlate: licensePlate ?? null,
        orderType,
        storeId,
      });
    } catch (error) {
      console.error("Order altered notification failed", error);
      return new Response("Error", { status: 500 });
    }

    return new Response("OK", { status: 200 });
  }

  // Handle add-items notifications (child or merged orders)
  if (payload.type === "ORDER_ADD_ITEMS") {
    const addPayload = payload as OrderAddItemsPayload;
    const { storeId, orderNumber, orderId, itemCount, tableNumber, licensePlate, orderType } = addPayload;

    if (!storeId || !orderNumber || !orderId || !itemCount) {
      return new Response("Bad payload", { status: 202 });
    }

    try {
      const subscriptionIds = await fetchSubscriptionIds(storeId, ["chef", "waiter", "admin"]);

      const heading = `+${itemCount} item${itemCount > 1 ? "s" : ""} added to order #${orderNumber}`;
      const content = `Order #${orderNumber} received additional items.`;

      await notifyOneSignal(subscriptionIds, heading, content, {
        status: "OrderAddItems",
        orderId,
        orderNumber,
        itemCount,
        tableNumber: tableNumber ?? null,
        licensePlate: licensePlate ?? null,
        orderType,
        storeId,
      });
    } catch (error) {
      console.error("Order add-items notification failed", error);
      return new Response("Error", { status: 500 });
    }

    return new Response("OK", { status: 200 });
  }

  // Handle item prepared notifications (from mark_item_prepared RPC)
  if (payload.type === "ITEM_PREPARED") {
    const itemPayload = payload as ItemPreparedPayload;
    const { storeId, orderNumber, itemName, orderId, itemId, tableNumber, licensePlate, orderType } = itemPayload;

    if (!storeId || !orderNumber || !itemName) {
      return new Response("Bad payload", { status: 202 });
    }

    try {
      const subscriptionIds = await fetchSubscriptionIds(storeId, ["waiter", "admin"]);
      
      const heading = `Item ready for order #${orderNumber}`;
      const content = `${itemName} is prepared`;
      
      await notifyOneSignal(subscriptionIds, heading, content, {
        status: "ItemPrepared",
        orderId,
        orderNumber,
        itemId,
        tableNumber: tableNumber ?? null,
        licensePlate: licensePlate ?? null,
        orderType,
        storeId,
      });
    } catch (error) {
      console.error("Item prepared notification failed", error);
      return new Response("Error", { status: 500 });
    }

    return new Response("OK", { status: 200 });
  }

  // Handle bulk update notifications
  if (payload.type === "BULK_UPDATE") {
    const bulkPayload = payload as BulkUpdatePayload;
    const { storeId, toStatus, count } = bulkPayload;

    if (!storeId || !toStatus || count === 0) {
      return new Response("Bad payload", { status: 202 });
    }

    const roles = ROLE_TARGETS[toStatus] ?? [];
    if (roles.length === 0) {
      return new Response("No targets for status", { status: 200 });
    }

    try {
      const subscriptionIds = await fetchSubscriptionIds(storeId, roles);
      
      let heading: string;
      let content: string;
      
      if (toStatus === "InKitchen") {
        heading = "All orders accepted";
        content = `${count} order${count > 1 ? 's' : ''} have been accepted and are now being prepared.`;
      } else if (toStatus === "Prepared") {
        heading = "All orders ready";
        content = `${count} order${count > 1 ? 's' : ''} have been marked as prepared and are ready to serve.`;
      } else if (toStatus === "Delivered") {
        heading = "All orders delivered";
        content = `${count} order${count > 1 ? 's' : ''} have been marked as delivered.`;
      } else {
        heading = "Orders updated";
        content = `${count} order${count > 1 ? 's' : ''} have been updated.`;
      }
      
      await notifyOneSignal(subscriptionIds, heading, content, {
        bulkUpdate: true,
        status: toStatus,
        storeId,
        count,
      });
    } catch (error) {
      console.error("Bulk notification failed", error);
      return new Response("Error", { status: 500 });
    }

    return new Response("OK", { status: 200 });
  }

  // Handle regular order updates (from database triggers)
  const orderPayload = payload as OrderPayload;

  if (orderPayload.table !== "orders") {
    return new Response("Ignored", { status: 200 });
  }

  const record = orderPayload.record ?? {};
  const oldRecord = orderPayload.old_record ?? {};

  const storeId = record.store_id as string | undefined;
  const status = record.status as string | undefined;
  const oldStatus = (oldRecord?.status ?? undefined) as string | undefined;
  const orderId = record.id as string | undefined;
  const orderNumber = record.number as number | undefined;

  if (!storeId || !status || !orderId) {
    console.warn("Missing required order fields", record);
    return new Response("Bad payload", { status: 202 });
  }

  if (payload.type === "UPDATE" && status === oldStatus) {
    return new Response("No status change", { status: 200 });
  }

  const roles = ROLE_TARGETS[status] ?? [];
  if (roles.length === 0) {
    return new Response("No targets for status", { status: 200 });
  }

  try {
    const subscriptionIds = await fetchSubscriptionIds(storeId, roles);
    const { heading, content } = resolveNotificationCopy(status, orderNumber);
    await notifyOneSignal(subscriptionIds, heading, content, {
      orderId,
      status,
      storeId,
      number: orderNumber ?? null,
    });
  } catch (error) {
    console.error("Notification pipeline failed", error);
    return new Response("Error", { status: 500 });
  }

  return new Response("OK", { status: 200 });
});




