### ROMS – Final Spec Sheet (Electron Admin + Native Android Chef/Waiter + Firebase)

## 1) Scope and Goals
- Build a minimal, smooth, easy-to-use ROMS with only the required features.
- Platforms:
  - Desktop Admin app: Electron.
  - Android apps: Chef and Waiter (Android Studio, Kotlin).
- Backend: Firebase (Auth, Firestore, Cloud Functions, FCM, Storage optional).
- Distribution: share APKs and desktop installer; no complex deployment.
- Auto-hide: Orders archived and hidden 30 minutes after Close or Cancel.

## 2) Roles and Devices
- Admin: Desktop (Electron).
- Chef: Android.
- Waiter: Android.
- Single chef for now; multi-chef later.

## 3) Order Lifecycle and Status Colors
 - Flow: (see `user-flows.md` for narratives and `architecture.md` for diagrams)
  - Admin creates order → Created
  - Chef accepts → Accepted
  - Chef marks ready → Prepared
  - Waiter delivers → Delivered
  - Admin closes or adds more items:
    - Close → Closed
    - Add more → back to Accepted
  - Admin can cancel mid-flow → Cancelled
- Status colors:
  - Created: Grey
  - Accepted: Blue
  - Prepared: Yellow
  - Delivered: Green
  - Closed: Dark Grey
  - Cancelled: Red
- Archival: Orders in Closed or Cancelled auto-hide 30 minutes after their final timestamp.

## 4) Admin App (Electron) – Features
- Authentication:
  - Email/password login.
  - Admin panel can create Chef/Waiter accounts (email, name, role, temp password), reset passwords, and deactivate users. Users can change their own passwords.
- Menu Management:
  - Always-available “Add Item” UI (name, category, price, available).
  - Excel import (CSV/XLSX) with columns: Item Name, Category, Price, Available.
  - Excel import validation: required columns enforced; invalid rows are skipped and summarized in an import report.
  - Edit/delete items; availability toggle.
  - Orders store a snapshot of item name and price at order time so past orders remain readable even if items are edited/disabled later.
- Table Management (MVP decision):
  - Free-text table numbers for fastest setup.
  - Optional quick-select list of saved tables (simple add/remove list).
  - Enforce one active order per table for dine-in until the order is Closed or Cancelled.
- Create Order:
  - Mode: Dine-in (table number), Parcel, Delivery (mark-only; details later).
  - Add items from menu; plus/minus to adjust quantities; real-time total.
- Manage Orders:
  - Real-time list of all active orders with status badges and timestamps.
  - View details: items, quantities, total, mode, table number.
  - Admin can edit items/quantities after creation (before delivery). Edits are reflected in realtime and highlighted in UI; Chef is notified if kitchen work changes. Waiter is notified only when the order is reopened after Delivery.
- Post-Delivery Actions:
  - Close Order: finalize; table freed; archival after 30 minutes.
  - Add More Items: append items; status returns to Accepted.
- Cancel Order (mid-flow):
  - Admin can cancel at any status before Closed. Visible red “Cancelled” status; order removed from Chef/Waiter views; archival after 30 minutes.
- Notifications to staff:
  - Chef receives push for new/updated orders requiring kitchen action.
  - Waiter receives push when orders become Prepared and when Admin adds items post-delivery (reopened).

## 5) Chef App (Android) – Features
- Authentication: Email/password.
- Incoming Orders:
  - List of Created orders (real-time).
  - Push notifications for new orders and for admin edits that impact kitchen.
- Actions:
  - Accept Order → status to Accepted.
  - Mark Prepared → status to Prepared.
- UI:
  - Smooth scrolling list; clear badges; updates highlighted visually.
  - Orders disappear if Cancelled or when they move beyond Chef’s responsibility.

## 6) Waiter App (Android) – Features
- Authentication: Email/password.
- Ready to Serve:
  - List of Prepared orders (real-time).
  - Push notifications when orders become Prepared and when reopened after delivery (Admin adds items).
- Actions:
  - Mark Delivered → status to Delivered.
- UI:
  - Smooth scrolling list; clear badges; updates highlighted.
  - Orders disappear when Delivered (from Waiter view) or if Cancelled.

## 7) Push Notifications (FCM)
- Who gets pushes:
  - Chef: on Created, on Admin edits that add/change kitchen work, on reopen (Add More Items).
  - Waiter: on Prepared, on reopen (Add More Items).
- Admin relies on desktop realtime UI (no push required).
- Content: brief title + order ID + table/mode; tapping opens order.
- Apps register FCM device tokens on sign-in; tokens are associated with the user. Notifications can be sent per-role using user tokens or simple role topics (chef, waiter).
- Triggers covered: Created, Prepared, Reopened/Add-More (Chef + Waiter), and impactful Admin edits affecting kitchen work (Chef).

## 8) Realtime Lists and Visibility
- Admin: all non-archived orders, filter/sort by status/time.
- Chef: Created and Accepted assigned work; hidden once Prepared or if Cancelled.
- Waiter: Prepared only; hidden once Delivered or if Cancelled.
- Archive logic: hide Closed/Cancelled after 30 minutes.

## 9) Editing and Notifications Policy
- Admin edits (pre-Delivery):
  - Items/quantities can be changed; UI highlights edited orders; Chef receives push if kitchen work increases/changes.
- After Delivery:
  - Use “Add More Items” only; order reopens to Accepted; Chef and Waiter notified.
- Cancellation:
  - Change status to Cancelled; Chef/Waiter views update immediately; red highlight for Admin.

## 10) Minimal Data Model (conceptual)
- Users: uid, name, email, role (admin/chef/waiter), active.
- Menu Items: name, category, price, available.
- Orders: mode, table number (optional), items (name, price, quantity), total, status (one of Created | Accepted | Prepared | Delivered | Closed | Cancelled), timestamps (createdAt/updatedAt/deliveredAt/closedAt/cancelledAt), archived flag.
- Status history (optional simple log) for audit readability in Admin UI.

## 11) Non-Functional and UX
See detailed design guidance in `ui-ux.md`.
- Simple, minimal UI; large buttons; clear status colors.
- Smooth scrolling and responsive performance.
- Basic loading/empty/error states.
- No complex analytics, payments, QR, or multi-restaurant.
- Distribution: share APKs and desktop installer directly.

## 12) Success Criteria
- Admin can manage menu, create/edit/cancel/close orders, add items post-delivery, and create Chef/Waiter accounts.
- Chef can accept and mark prepared; receives pushes.
- Waiter can mark delivered; receives pushes.
- Realtime updates across all apps; UI highlights status and edits clearly.
- Closed/Cancelled orders auto-hide after 30 minutes.
- Push notifications fire for each trigger (Created, Prepared, Reopened/Add-More, impactful Admin edits to kitchen).
- One active order per table (dine-in) is enforced until Closed or Cancelled.
- Experience is minimalistic, smooth, and easy to use.