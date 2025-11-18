import { createClient } from "https://esm.sh/@supabase/supabase-js@2.44.3";
import { serve } from "https://deno.land/std@0.203.0/http/server.ts";

interface OrderPayload {
  type: "INSERT" | "UPDATE";
  table: "orders";
  record: Record<string, unknown>;
  old_record?: Record<string, unknown> | null;
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
  InKitchen: ["chef"],
  Prepared: ["waiter"],
  Delivered: [],
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

  const body = {
    app_id: ONESIGNAL_APP_ID,
    include_subscription_ids: subscriptionIds,
    headings: { en: heading },
    contents: { en: content },
    data,
    // Temporarily commented out to test if notifications work without it
    // android_channel_id: "order_updates",
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

  console.log(`Notified OneSignal for ${subscriptionIds.length} subscriptions`);
}

function resolveNotificationCopy(status: string, orderNumber?: number) {
  const numberSuffix = orderNumber ? ` #${orderNumber}` : "";

  switch (status) {
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

serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  const payload = (await req.json()) as OrderPayload;

  if (payload.table !== "orders") {
    return new Response("Ignored", { status: 200 });
  }

  const record = payload.record ?? {};
  const oldRecord = payload.old_record ?? {};

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




