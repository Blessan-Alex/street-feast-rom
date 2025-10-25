Streat Feast — Final Product Plan
Roles & Apps
Admin (Desktop / Electron): logs in, manages menu, creates & edits orders, sees dashboards, sets “frequently bought,” deletes all data.


Chef (Android): sees new/accepted/in-kitchen orders, marks Prepared, reads Chef Tip.


Waiter (Android): sees Prepared orders, marks Delivered/Served. Gets notified when Chef marks Prepared.


No prices anywhere. Quantities are counts only. Sizes are Small / Large (optional). Veg / Non-Veg. One order-level Chef Tip note. Single location. 12-hour times. No payments. No printing.

A) Screen-by-Screen Descriptions
DESKTOP (Admin / Electron)
1) Login
Purpose: Secure access for the admin.
 Elements: Username/email, password, “Login”, “Forgot Password?”.
 Actions: Submit → if success, land on Order Dashboard. Errors are large, simple, and red.

2) Order Dashboard
Purpose: Real-time view of all orders + quick actions.
 Elements:
KPIs: Total Active, New, In Kitchen, Ready (Prepared).


Pills to filter: All, Created, Accepted, In Kitchen, Prepared, Delivered, Closed, Canceled.


Table: Order ID, Type (Dine-in / Parcel / Delivery), Customer/Label (if provided), Time, Status, Items (count), Actions (View 👁, Edit ✎, Cancel ⛔).


“Sort by: Newest/Oldest”.
 Actions:


Open an order detail.


Change status (e.g., Cancel), if allowed.


Live updates via realtime stream.



3) Menu — Add Menu (chooser)
Purpose: First-time setup or maintenance.
 Elements: Two big options:
Upload from Excel/CSV


Upload by Interface (Manual)
 Actions: Choose a method.



4) Menu — Upload (Excel/CSV)
Purpose: Bulk menu creation/update.
 Elements: “Choose your file”, a short template reminder.
 Template (CSV/XLSX) columns:
 Item Name | Category | Available Sizes | Veg/NonVeg
Available Sizes: comma separated values from {Small, Large}; blank means “one size”.


Veg/NonVeg: Veg or NonVeg (case-insensitive).
 Actions: Select file → Validate → Show preview (first 20 rows + errors) → Apply → redirect to Menu Summary.



5) Menu — Create Categories (empty)
Purpose: Start manual menu.
 Elements: “Create Categories” + big +.
 Actions: Tap + → Category editor.

6) Menu — Category Editor
Purpose: Create a category and its items.
 Elements:
Category Name (e.g., Chinese, Indian).


Grid of Item cards:


Item Name


Sizes (checkboxes: Small, Large).


Veg/Non-Veg selector.


Add More (append item cards).


Complete (save) / Edit (toggle fields).
 Actions: Define items and sizes; set veg/non-veg; save.



7) Menu — Summary
Purpose: Overview of the menu.
 Elements: Cards per Category showing Category Name, No. of Items, View/Edit, Add More, Delete Categories, Complete (finish setup).
 Actions: Edit any category, add more, bulk delete, finish → you’re ready to take orders.

8) Create Order (POS)
Purpose: Build orders quickly and clearly.
 Layout:
Left Nav: Menu / Create Order / Manage Orders.


Top: Frequent Bought (admin-curated list) with quick-add “+”.


Browse: Category tiles → (tap) → items list modal (shows Item Name, Sizes toggle if available, Veg/Non-Veg badge).


Search Product (right/top) — debounced.


Right Sidebar — Order Summary Card:


List of lines (e.g., Chicken Soup — Large × 2).


Chef Tip (optional) — one text field for the whole order.


Order Type: Dine-in / Parcel / Delivery (radio buttons).


Actions: Cancel Order (red), Place Order (green “Give Order”).
 Rules:


Editing allowed until Chef starts preparing (status changes beyond Accepted).


You can add items to an existing order any time; if Chef already started, added items create a new sub-ticket flagged as “Add-on”.



9) Manage Orders (List/Details)
Purpose: Admin oversight on all tickets.
 Elements: Filter by status; open detail; change allowed statuses; cancel.
 Detail View: Order metadata, type, items, sizes, counts, Chef Tip, status timeline.

10) Admin Settings (Simple)
Purpose: Curate Frequent Bought, download/upload CSV template, Delete All Data (danger).
 Elements:
Frequent Bought: pick items to pin (multi-select); order by drag-handle.


Data Controls: Download template, Upload menu, Delete All Data (double confirmation).



ANDROID (Chef & Waiter)
A) Login
Purpose: Staff sign-in.
 Elements: Username, password, “Login”, “Forgot Password?”.
 Action: Login → lands on role-combined home.
We’ll use one APK with two tabs; both Chef and Waiter can use it, but Waiter primarily uses “Prepared → Delivered”.

B) New Orders (Chef)
Purpose: Show newly Created orders awaiting Accept.
 Elements: Cards: Order #, Type, concise items list (e.g., “2× Burger, 1× Caesar Salad”), time since, button Accept.
 Actions: Accept → moves order to In Kitchen (preparing).

C) Preparing Orders (Chef)
Purpose: Work queue.
 Elements: Cards grouped by type (Dine-in / Parcel / Delivery):
Items list with sizes/counts.


Chef Tip (read-only) prominently.


Big Mark Prepared (green).
 Actions: Mark Prepared → notifies Waiter & Admin.



D) Prepared / Ready (Waiter)
Purpose: Pickup/delivery/serve queue for the Waiter.
 Elements: Cards: Order #, Type, items summary, time since prepared, Mark Delivered/Served (green).
 Actions: Mark Delivered/Served → order moves to Delivered; Admin can later set Closed.

B) Functional Spec (What Each Screen Does)
Screen
Key Functions
Firebase Work
Login (all)
Email/password auth
Firebase Auth (Email/Password)
Order Dashboard
Live counts, filters, actions
Firestore orders live query with indexes; status aggregates computed client-side
Add Menu (Chooser)
Route choice
n/a
Upload Menu
Validate + parse CSV/XLSX; preview; apply
Local parse → write to Firestore in batch; Cloud Function optional for heavy validation
Create Categories / Editor
CRUD categories & items; set sizes and veg/non-veg
Collections categories, items; item fields: sizes: ['Small','Large'], `veg: 'Veg'
Menu Summary
List & counts; bulk delete
Count via items query per category; delete batch
Create Order (POS)
Add items, set sizes, set counts, Chef Tip, pick Type, place order; edit until preparing
Create orders doc + orderItems subcollection; status: 'Created'; updates allowed until status > Accepted
Manage Orders
Browse + update status
Patch status with allowed transitions
Android — New Orders
Accept order
Update status: 'Accepted' then immediately status: 'InKitchen'
Android — Preparing
Mark prepared
Update status: 'Prepared'
Android — Ready
Mark delivered
Update status: 'Delivered' (admin can later set Closed)
Settings
Set Frequent Bought; Delete All Data
frequentItems collection; callable function dangerDeleteAll


C) Order Lifecycle & Rules
Created  →  Accepted  →  InKitchen  →  Prepared  →  Delivered/Served  →  Closed
   ↑                 (auto on Accept)                                  ↘
   └────────────── Cancel (from Created/Accepted/InKitchen only)        (optional archival)

Editing: Allowed while status ∈ {Created, Accepted}.


Add more items: Always allowed; if InKitchen or beyond, create a new sub-ticket attached to the same order (parentOrderId).


Type selection: Set in Order Summary before Place Order; editable until InKitchen.


Chef Tip: One order-level text field, visible on Chef screen.



D) Simple, Beginner-Friendly UX Rules
Colors: Green = primary/positive action (Place Order, Mark Prepared, Delivered). Red = destructive (Cancel, Delete All). Yellow/Orange = attention (Accepted/InKitchen chips). Grey = neutral.


Buttons: One big primary per screen; secondary actions are smaller and right-aligned.


Text: Large headings, short labels (“Accept”, “Prepared”).


Touch targets: ≥ 44×44dp on Android; roomy click targets on desktop.


Empty states: clear icon + single sentence (“No orders to prepare right now.”).


Progressive disclosure: item details open in a simple modal; the main POS stays clean.


Accessibility: high contrast, 14–16pt min font, never rely on color alone (use labels/chips).



E) Notification & Sound Matrix (FCM + local sound)
All are short, affirmative, color-coded, with a single sound cue on each app.
Event
Who Receives
Text (Title – Body)
New Order Created
Chef (Android), Admin (desktop toast)
New Order – “Order #{{id}} is ready to accept.”
Order Accepted / In Kitchen
Admin (desktop toast)
Order In Kitchen – “Order #{{id}} is now being prepared.”
Order Prepared
Waiter (Android), Admin (desktop toast)
Order Ready – “Order #{{id}} is ready to serve/deliver.”
Order Delivered/Served
Admin (desktop toast)
Order Delivered – “Order #{{id}} has been marked delivered.”
Order Canceled
Chef & Admin
Order Canceled – “Order #{{id}} was canceled.”
Item Added to Existing Order
Chef (Android)
Update – “Extra items added to Order #{{id}}.”

Sounds: a short “ping” for New/Prepared; softer click for Delivered; buzzer for Canceled. Toggleable in Settings.

F) Data Model (Firestore)
/stores/{storeId}                   // single store now; keep for future multi-store
  /categories/{categoryId}
    name: string                    // "Chinese"
    isActive: boolean
    createdAt, updatedAt

  /items/{itemId}
    categoryId: string
    name: string
    sizes: array<string>            // [], ["Small"], ["Small","Large"]
    vegFlag: "Veg" | "NonVeg"
    isActive: boolean
    createdAt, updatedAt

  /frequentItems/{itemId}           // subset mirror by reference
    order: number                   // sort position

  /orders/{orderId}
    orderNumber: number             // sequential
    type: "DineIn" | "Parcel" | "Delivery"
    chefTip: string                 // optional
    status: "Created" | "Accepted" | "InKitchen" | "Prepared" | "Delivered" | "Closed" | "Canceled"
    createdBy: userId
    createdAt: timestamp
    updatedAt: timestamp
    canEdit: boolean                // derived convenience (true until > Accepted)
    parentOrderId: string | null    // for add-on sub-tickets (optional)

    /orderItems/{orderItemId}
      itemId: string
      nameSnapshot: string          // copy for safety
      size: "Small" | "Large" | null
      vegFlagSnapshot: "Veg" | "NonVeg"
      qty: number                   // integer >= 1
      createdAt

/users/{userId}
  role: "admin" | "chef" | "waiter" // used only on Android for gating buttons
  displayName, email
  deviceTokens: string[]            // FCM tokens

Counters: counts per status are computed client-side from live query; if you need super fast KPIs later, add Cloud Functions to maintain /stores/{id}/stats.

G) Security & Rules (high-level)
Auth: Firebase Email/Password. One admin account. Chef/Waiter accounts for Android.


Firestore Rules (idea):


Admin: full read/write on stores/*.


Chef: read items/categories/orders; write orders.status → Prepared only; cannot edit items or delete.


Waiter: read orders; write orders.status → Delivered only.


Order edits guarded by server (Cloud Function) or security rules that deny writes when status ∉ {Created,Accepted} except adding add-on sub-tickets.


Delete All Data: Protected by admin custom claim, requires re-auth + double confirm; implemented as Callable Cloud Function that deletes collections in batches.



H) Cloud Functions (minimal, free-tier friendly)
onOrderStatusWrite


Triggers FCM notifications based on new status.


When status becomes Accepted, immediately set InKitchen (no extra tap).


dangerDeleteAll(storeId) (callable)


Batches delete orders, orderItems, categories, items, frequentItems.


createOrderNumber


Transaction to increment a /stores/{id}/counters/orderNumber doc to produce sequential orderNumber.


All optional but recommended for integrity + notifications.

I) CSV Import Validation (Desktop)
Accept .csv or .xlsx (convert to rows).


Validate headers: Item Name, Category, Available Sizes, Veg/NonVeg.


Available Sizes must be subset of {Small, Large}, case-insensitive, comma-separated.


Veg/NonVeg must be Veg or NonVeg.


Show preview table with row-level errors in red.


On Apply, batch write items & categories (create if missing).


No images, no prices — keeps it light and fast.



J) Allowed Status Transitions
Admin:


Created ↔ Accepted (edit), Accepted → Canceled, Delivered → Closed, Add items (anytime → creates add-on if ≥ InKitchen).


Chef (Android):


Created → Accepted → InKitchen (auto) → Prepared.


Waiter (Android):


Prepared → Delivered.


Any illegal transition is blocked; show a simple message (“This order can’t be edited now because the kitchen has started.”).

K) Empty/Loading/Error States
Empty lists: friendly gray icon + “Nothing here yet.”


Loading: skeleton rows.


Validation errors: one sentence, high-contrast red, actionable text (“Available Sizes must be Small, Large, or blank”).


Cancel confirmations: “Are you sure? This will cancel Order #1024.” (Red confirm).



L) Sounds & Color Guide
Ping (new/prepared), Soft click (delivered), Buzzer (canceled).


Buttons: Green (Place Order, Prepared, Delivered), Red (Cancel, Delete), Gray (secondary).


Status chips (consistent):


Created (blue), Accepted (orange), In Kitchen (yellow), Prepared (green), Delivered (purple), Closed (gray), Canceled (red).



M) Tech Notes (Firebase + Electron + Android)
Electron (React or vanilla): use Firestore Web SDK; local CSV parsing with Papaparse/XLSX.


Android (Kotlin/Jetpack or simple XML): Firestore SDK + FCM; foreground service optional for consistent notifications; enable offline persistence.


Realtime: Firestore listeners for dashboard, POS, and Android lists.


Fonts/Colors: system defaults & Google Fonts default (Roboto); keep high contrast and large touch areas.



N) Acceptance Checklist (MVP)
Admin can upload CSV and see items in categories.


Admin can create order with items (sizes optional), set Dine-in/Parcel/Delivery, enter Chef Tip, place order.


Orders stream into Android “New Orders”. Chef can Accept → goes to In Kitchen automatically.


Chef can Mark Prepared; Waiter receives FCM + sees it under “Ready”.


Waiter can Mark Delivered; Admin dashboard updates instantly.


Admin can edit orders while Created/Accepted; blocked after InKitchen.


Admin can add items anytime; creates add-on ticket if order is already in kitchen.


Admin can curate Frequent Bought.


Admin can Delete All Data (double confirm).


All lists show helpful empty/loading/error states.


All times display in 12-hour format.






