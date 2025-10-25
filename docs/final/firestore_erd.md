# Streat Feast — Firestore Entity Relationship Diagram (ERD)

## Overview
Firestore is used as the primary realtime database for all modules — Admin, Chef, and Waiter. The data is structured for simplicity, clarity, and scalability, optimized for small restaurants with a single location (expandable to multiple stores later).

---

## ERD Structure

### **/stores/{storeId}**
Top-level collection representing a single restaurant or branch.

#### Fields
- **storeId:** string — unique identifier
- **name:** string — store name (optional)
- **createdAt:** timestamp
- **updatedAt:** timestamp

---

### **/categories/{categoryId}**
Represents a cuisine or menu group.

#### Fields
- **name:** string — e.g., “Chinese”, “Indian”
- **isActive:** boolean
- **createdAt:** timestamp
- **updatedAt:** timestamp
- **storeId:** reference (backlink)

#### Relationships
- One-to-many with **items** collection.

---

### **/items/{itemId}**
Represents a food item within a category.

#### Fields
- **categoryId:** string — reference to category
- **name:** string
- **sizes:** array<string> — [“Small”, “Large”] or []
- **vegFlag:** string — “Veg” or “NonVeg”
- **isActive:** boolean
- **createdAt:** timestamp
- **updatedAt:** timestamp

#### Relationships
- Belongs to one **category**.
- Can appear in multiple orders.

---

### **/frequentItems/{itemId}**
Represents admin-curated frequently ordered items.

#### Fields
- **itemId:** string (reference to `/items`)
- **order:** number (sorting order)
- **createdAt:** timestamp

#### Relationships
- Mirrors selected items from `/items`.

---

### **/orders/{orderId}**
Represents an active or past order created by admin.

#### Fields
- **orderNumber:** number — sequential auto-generated via Cloud Function.
- **type:** string — “DineIn” | “Parcel” | “Delivery”
- **chefTip:** string — optional note to chef.
- **status:** string — one of: Created / Accepted / InKitchen / Prepared / Delivered / Closed / Canceled.
- **createdBy:** string — userId reference.
- **createdAt:** timestamp
- **updatedAt:** timestamp
- **canEdit:** boolean (derived client-side)
- **parentOrderId:** string | null — for add-ons.

#### Relationships
- One-to-many with **orderItems**.
- References **user** (creator).

---

### **/orders/{orderId}/orderItems/{orderItemId}**
Stores individual items within an order.

#### Fields
- **itemId:** string (snapshot reference)
- **nameSnapshot:** string
- **size:** string — “Small”, “Large”, or null
- **vegFlagSnapshot:** string — “Veg” | “NonVeg”
- **qty:** number — quantity (≥ 1)
- **createdAt:** timestamp

#### Relationships
- Belongs to one **order**.

---

### **/users/{userId}**
Represents staff accounts across devices.

#### Fields
- **displayName:** string
- **email:** string
- **role:** string — “admin” | “chef” | “waiter”
- **deviceTokens:** array<string> — FCM tokens
- **createdAt:** timestamp

#### Relationships
- Linked to orders via `createdBy`.

---

## Relationship Summary
| Entity | Relationship | Target | Cardinality |
|---------|--------------|---------|--------------|
| store | has | categories | 1 → * |
| category | has | items | 1 → * |
| item | appears in | orderItems | * → * |
| order | has | orderItems | 1 → * |
| order | created by | user | * → 1 |
| user | receives notifications via | deviceTokens | 1 → * |

---

## Notes
- All timestamps use `FieldValue.serverTimestamp()`.
- Optimized for Firestore’s subcollection querying and real-time streams.
- Designed for easy future scaling: multi-store can be enabled by keeping `storeId` context.
- No pricing, payments, or images stored — lightweight and low-cost.

---

**Purpose:** Provide a simple, clean Firestore data schema that supports realtime, role-based restaurant order management with minimal overhead.

