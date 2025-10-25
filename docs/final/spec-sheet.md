# Streat Feast — Final Specification Sheet (Improved)

## Overview
A minimalistic, beginner-friendly restaurant management system built using **Firebase + Firestore**, **Electron** (Admin), and **Android Studio** (XML layouts) for Chef and Waiter. Focused on speed, clarity, and simplicity with color-coded UX and minimal overhead.

---

## Roles & Apps
- **Admin (Desktop / Electron):** manages menu, creates & edits orders, monitors dashboard, sets “frequently bought”, deletes data.
- **Chef (Android):** views new/accepted/in-kitchen orders, marks orders as Prepared, reads Chef Tip.
- **Waiter (Android):** views Prepared orders, marks as Delivered/Served, receives notifications when orders are Prepared.

> No prices, only item counts. Optional sizes (Small / Large). Veg / Non-Veg indicator. Single store, 12-hour clock. No payments, no printing.

---

## A) Screen-by-Screen Summary

### Desktop (Admin)
1. **Login:** Secure access; simple username/password form.
2. **Order Dashboard:** Live KPIs, filters, order list (ID, Type, Time, Status, Items, Actions). Realtime updates.
3. **Menu Add (Chooser):** Upload CSV or create manually.
4. **Menu Upload:** CSV/XLSX import with preview and validation.
5. **Create Categories:** Start manual category creation.
6. **Category Editor:** Add/edit category, items, sizes, veg/non-veg.
7. **Menu Summary:** Overview of all categories and items; bulk edit/delete.
8. **Create Order (POS):** Build orders; select items, sizes, quantities; add Chef Tip; select Dine-in/Parcel/Delivery.
9. **Manage Orders:** Track and modify active orders.
10. **Admin Settings:** Manage frequent items, import/export CSV, delete all data.

### Android (Chef/Waiter)
1. **Login:** Staff authentication (shared app with role-based UI).
2. **New Orders (Chef):** Accept new orders (Created → Accepted → In Kitchen).
3. **Preparing Orders (Chef):** Work queue; mark as Prepared.
4. **Prepared Orders (Waiter):** View ready orders; mark Delivered/Served.

---

## B) Functional Overview
| Screen | Core Functions | Firebase Work |
|--------|----------------|----------------|
| Login | Email/password auth | Firebase Auth |
| Dashboard | Live filters, status updates | Firestore listeners (realtime) |
| Menu Upload | Validate CSV, batch insert | Firestore batch writes |
| Category Editor | CRUD categories/items | `categories` + `items` collections |
| POS | Create/edit orders | `orders` + `orderItems` subcollection |
| Manage Orders | Update/cancel | Firestore patch |
| Android (Chef) | Accept/prepare | Update `status` fields |
| Android (Waiter) | Deliver/serve | Update `status` fields |
| Settings | Frequent items, delete data | `frequentItems` + callable functions |

---

## C) Order Lifecycle
`Created → Accepted → InKitchen → Prepared → Delivered → Closed`  
↳ Cancel from {Created, Accepted, InKitchen}

- **Editable** while status ∈ {Created, Accepted}.
- **Add-ons** allowed post-InKitchen (sub-ticket).
- **Chef Tip:** single order-level note.

---

## D) UX Guidelines
- **Green** = Confirm / Done, **Red** = Cancel / Error, **Yellow** = Attention, **Gray** = Neutral.
- Large buttons, single main action per screen.
- Font: Roboto (default Google font). High contrast, 14–16pt text.
- Empty states and clear modals (“No orders yet”, “Are you sure to cancel?”).
- Consistent layout and color-coded statuses.
- **Accessibility:** Avoid dense text, use color + text together.
- **Offline Mode:** Android apps enable offline persistence via `FirebaseFirestoreSettings.setPersistenceEnabled(true)`.

---

## E) Notifications (FCM)
| Event | Recipient | Message |
|--------|------------|----------|
| New Order | Chef, Admin | “Order #123 ready to accept.” |
| In Kitchen | Admin | “Order #123 is now being prepared.” |
| Prepared | Waiter, Admin | “Order #123 ready to serve/deliver.” |
| Delivered | Admin | “Order #123 marked delivered.” |
| Canceled | Chef, Admin | “Order #123 was canceled.” |
| Add-on | Chef | “Extra items added to Order #123.” |

**Sounds:** Ping (New/Prepared), Click (Delivered), Buzzer (Canceled).  
**FCM Topics:** Chef app → `chef`, Waiter app → `waiter`, Admin app → `admin`.

---

## F) Firestore Data Structure
```
/stores/{storeId}
  /categories/{categoryId}
    name: string
    isActive: boolean
    createdAt, updatedAt: serverTimestamp()

  /items/{itemId}
    categoryId: string
    name: string
    sizes: array<string>
    vegFlag: "Veg" | "NonVeg"
    isActive: boolean
    createdAt, updatedAt: serverTimestamp()

  /frequentItems/{itemId}
    order: number

  /orders/{orderId}
    orderNumber: number
    type: "DineIn" | "Parcel" | "Delivery"
    chefTip: string
    status: "Created" | "Accepted" | "InKitchen" | "Prepared" | "Delivered" | "Closed" | "Canceled"
    createdBy: userId
    createdAt, updatedAt: serverTimestamp()
    parentOrderId: string | null

    /orderItems/{orderItemId}
      itemId: string
      nameSnapshot: string
      size: "Small" | "Large" | null
      vegFlagSnapshot: "Veg" | "NonVeg"
      qty: number
      createdAt: serverTimestamp()

/users/{userId}
  role: "admin" | "chef" | "waiter"
  displayName, email, deviceTokens: []
```
> **Future scalability:** Structure supports future multi-store setup by expanding storeId context.

---

## G) Security & Access
- **Auth:** Firebase Email/Password.
- **Roles:**  
  - Admin: full read/write.  
  - Chef: update `status → Prepared`.  
  - Waiter: update `status → Delivered`.  
- **Data Deletion:** Admin-only callable function with re-auth & double confirm.
- **Timestamps:** Use `FieldValue.serverTimestamp()` for createdAt/updatedAt consistency.

---

## H) Cloud Functions
- **onOrderStatusWrite:** Sends FCM notifications; auto-sets InKitchen after Accepted.
- **createOrderNumber:** Sequential ID generator.
- **dangerDeleteAll:** Wipes all data (admin only).

---

## I) CSV Import Validation
- Required headers: `Item Name`, `Category`, `Available Sizes`, `Veg/NonVeg`.
- Sizes: only `Small`, `Large` (comma-separated).
- Veg/NonVeg: must be `Veg` or `NonVeg`.
- Show preview & validation before upload.
- Example error messages:
  - Missing header: *“Column ‘Item Name’ not found.”*
  - Invalid size: *“Available Sizes must be ‘Small’, ‘Large’, or blank.”*

---

## J) Transitions Allowed
| Role | Allowed Transitions |
|-------|----------------------|
| Admin | Created↔Accepted, Delivered→Closed, Cancel any active |
| Chef | Created→Accepted→InKitchen→Prepared |
| Waiter | Prepared→Delivered |

---

## K) Color & Sound Reference
| Status | Color | Sound |
|--------|--------|--------|
| Created | Blue | Ping |
| Accepted | Orange | - |
| In Kitchen | Yellow | - |
| Prepared | Green | Ping |
| Delivered | Purple | Click |
| Closed | Gray | - |
| Canceled | Red | Buzzer |

---

## L) Tech Stack Summary
| Layer | Tech | Notes |
|--------|------|--------|
| Desktop | Electron JS | Firestore SDK + Papaparse |
| Mobile | Android (XML + Firestore SDK) | Simple layouts, offline persistence enabled |
| Backend | Firebase Cloud Functions | Notifications + automation |
| Database | Firestore | Realtime updates + offline sync |
| Auth | Firebase Auth | Email/password |
| Notifications | Firebase Cloud Messaging | Topic-based notifications |

---

## M) MVP Checklist
- [x] Admin can upload/manage menu (CSV/manual).
- [x] Orders sync live between desktop and Android.
- [x] Chef can mark orders Prepared.
- [x] Waiter notified → can mark Delivered.
- [x] Admin dashboard updates realtime.
- [x] Edit allowed while Created/Accepted.
- [x] Frequent items curated manually.
- [x] Delete All Data (double confirm).
- [x] 12-hour time display.
- [x] UX optimized for simplicity.
- [x] Offline mode confirmed for Android.

---

**Goal:** Maintain simplicity, reliability, and clarity while minimizing cloud and code overhead. Built for low-training environments with intuitive color and action cues.

