# Streat Feast — Final 4-Day Build Roadmap (25–28 Oct)

**Scope rules:** Admin-only desktop (Electron). Android app shared (Chef/Waiter). No prices/payments/printing. Sizes: Small/Large. Veg/NonVeg. One order-level Chef Tip. Single store. 12-hour times. Firebase (Auth, Firestore, FCM, Functions). Beginner-friendly UX.

**Assumptions:** Solo or very small team. Minimal dependencies. Asia/Kolkata time.

**Repo layout:**
```
streat-feast/
  apps/
    desktop/            # Electron (React)
    android/            # Android Studio (XML)
  functions/            # Firebase Cloud Functions
  docs/                 # spec-sheet.md, architecture_diagram.md, acceptance_checklist.md
```
---

## Day 1 — Sat, 25 Oct
**Focus:** Electron scaffolding, navigation, menu flows UI, CSV preview/validation, and mock data; polish core UX patterns.

### Setup & UI foundation
- Scaffold Electron + React (Vite) with a simple router.
- Global theme: Roboto, color tokens (green/red/yellow/gray), spacing, typography scale.
- Shared components: Button, Toast, Dialog/Confirm, EmptyState, Badge/Chip.
**DoD:** App boots; nav shell with pages stubbed; toasts/dialogs work.

### Auth (frontend-only today)
- Login screen (email/password).
- Mock auth toggles `isAuthenticated`; guard routes to Dashboard on success.
**DoD:** Any non-empty credentials simulate login; logout works.

### Menu flows — Upload + Manual + Summary
- Upload: parse CSV (Papaparse/XLSX), validate headers/values, preview first 20 rows, red line errors + summary.
- Create Categories (empty) → **+** → Category Editor (Item Name, Sizes Small/Large, Veg/NonVeg; Add More; Complete).
- Menu Summary: per-category cards (Name, #Items, View/Edit, Add More, Delete), Complete.
**DoD:** Valid CSV previews; invalid CSV shows clear errors; manual add/edit works; counts update (mock store).

### Frequent Bought (UI only)
- Multi-select of items + drag to reorder (stored in mock store).
**DoD:** List renders and reorders visually.

### UX polish pass
- Large touch targets, readable labels, focus states, consistent chips.
**Deliverables:** Electron app: Login → Menu Upload/Manual → Summary → Frequent Bought (UI over mock).  
**Checklist:** M1–M5.

---

## Day 2 — Sun, 26 Oct
**Focus:** Electron POS + Dashboard + Manage Orders (UI over mock); order summary with Chef Tip + Order Type; enforce UI status rules.

### Create Order (POS)
- Frequent Bought quick-add; Category→Items modal; Search (debounced).
- Order Summary card: line items `Name — Size × Qty`, **Chef Tip** (order-level), **Order Type** (Dine-in/Parcel/Delivery), **Cancel** (red), **Place Order** (green).
**DoD:** Assemble order in UI; cancel resets; placing creates draft order in mock store.

### Manage Orders
- List with filters (All, Created, Accepted, InKitchen, Prepared, Delivered, Closed, Canceled).
- Order detail drawer: metadata, items, Chef Tip, timeline.
**DoD:** Open order, move through allowed transitions (UI only).

### Dashboard
- KPI chips: Total Active, New, In Kitchen, Ready (client computed).
**DoD:** Counts update when orders change.

### Status rules & Add-On behavior
- Editable while {Created, Accepted}; Cancel from {Created, Accepted, InKitchen}; Delivered→Closed.
- Add items after InKitchen → create sub-ticket.
**DoD:** Illegal moves blocked; add-on creates distinct sub-ticket.

### Settings
- Download CSV template; Delete All Data (double confirm).
**Deliverables:** Admin lifecycle UI complete (POS + Manage + Dashboard + Settings) with transitions enforced (mock).  
**Checklist:** O1–O6, D1–D5, N1–N4 (UI).

---

## Day 3 — Mon, 27 Oct
**Focus:** Android front-end (XML) with mock data + local notifications + offline cache; Chef/Waiter flows match spec.

### Android project + role UI
- Single app with tabs: **Chef** (New, Preparing), **Waiter** (Ready).
- Login (mock) → role gating.
**DoD:** Skeleton navigates; mock session gates tabs.

### Lists & Actions
- Chef/New: Order #, Type, items summary; **Accept** (Created→Accepted→auto InKitchen).
- Chef/Preparing: shows items + **Chef Tip**; **Mark Prepared**.
- Waiter/Ready: items; **Mark Delivered**.
**DoD:** Flows run end-to-end in mock; illegal transitions blocked with toast.

### Notifications (local mock)
- Local notifications + sounds: Ping (New/Prepared), Click (Delivered), Buzzer (Canceled). Mute toggle.
**DoD:** Status changes trigger local notifications with correct text.

### Offline cache
- JSON file or Room mini-schema; load on start; actions update cache.
**DoD:** Lists render offline with cached data.

**Deliverables:** APK with Chef/Waiter flows over mock; local notifications; offline viewing.  
**Checklist:** C1–C5, W1–W4, N1–N4 (local).

---

## Day 4 — Tue, 28 Oct
**Focus:** Backend wiring (Firebase Auth, Firestore, Functions, FCM), security rules, end-to-end acceptance, packaging.

### Firebase project + configs
- Enable Auth (Email/Password), Firestore, FCM; add configs to both apps.
**DoD:** Apps initialize Firebase without errors.

### Auth integration
- Replace mock login with Firebase Auth; seed users (admin/chef/waiter).
**DoD:** Login works; role UI gates properly.

### Firestore repositories
- Implement reads/writes as per schema; Android enables offline persistence.
**DoD:** Both apps run on Firestore; realtime updates visible.

### Security rules
- Apply starter rules (admin full; chef → Prepared; waiter → Delivered; orderItems editable only while {Created, Accepted}).
**DoD:** Rules deployed; negative tests pass.

### Cloud Functions
- `createOrderNumber`, `onOrderStatusWrite` (auto Accepted→InKitchen + FCM), `dangerDeleteAll`.
**DoD:** Functions deploy; triggers fire; counter increments.

### FCM topics + notifications
- Admin subscribe to `admin` (or rely on UI toasts), Chef → `chef`, Waiter → `waiter`.
**DoD:** Devices receive pushes with correct title/body.

### Final QA, builds & handoff
- Run full acceptance checklist; build Electron package + Android APK; prepare demo script.
**Deliverables:** Fully wired prototype + build artifacts + demo script.  
**Checklist:** Remaining B1–B5, U1–U4; full pass.