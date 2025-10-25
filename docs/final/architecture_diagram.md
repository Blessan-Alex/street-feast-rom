# Streat Feast — Simple Architecture Overview

## System Overview
Streat Feast is built with a **three-role architecture** — Admin (desktop via Electron), Chef (Android), and Waiter (Android). The core infrastructure runs on **Firebase**, ensuring real-time synchronization, secure authentication, and push notifications.

---

## Architecture Summary
```
+-------------------+
|  Admin (Electron) |
|-------------------|
| • Menu Management |
| • Create Orders   |
| • Dashboard View  |
| • Data Controls   |
+-------------------+
          |
          | Firestore Realtime Sync (Web SDK)
          v
+-------------------+
|     Firebase      |
|-------------------|
| • Firestore DB    |
| • Firebase Auth   |
| • Cloud Functions |
| • FCM Messaging   |
+-------------------+
          ^
          | Android SDK (Realtime Listeners)
          |
+-------------------+
|  Chef App (Android)  |
|----------------------|
| • Accept Orders       |
| • Mark Prepared       |
| • Receive Chef Tip    |
+----------------------+
          ^
          |
+-------------------+
| Waiter App (Android) |
|----------------------|
| • See Prepared Orders|
| • Mark Delivered      |
| • Receive Alerts      |
+----------------------+
```

---

## Components Breakdown
### 1. **Client Layer**
- **Admin (Electron JS)**
  - Uses Firebase Web SDK.
  - Functions: menu management, create/edit orders, monitor statuses.
- **Chef & Waiter (Android XML)**
  - Built with Android Studio.
  - Uses Firebase Firestore SDK + Firebase Cloud Messaging (FCM).
  - Shared app with tabbed roles (Chef / Waiter).

### 2. **Backend Layer (Firebase)**
- **Firestore** — main data store.
- **Auth** — role-based user authentication.
- **Cloud Functions** — handles:
  - Auto status updates (Accepted → InKitchen).
  - Notifications via FCM.
  - Data deletion tasks.
- **Firebase Cloud Messaging (FCM)** — push notifications for each role.

### 3. **Data Flow Example**
1. Admin creates a new order in Electron.
2. Firestore stores order under `/orders` collection.
3. Chef app receives real-time update → shows in “New Orders”.
4. Chef marks as “Prepared” → Firestore updates.
5. Waiter app receives FCM push → order appears in “Ready”.
6. Waiter marks “Delivered” → Admin sees update instantly.

---

## Technology Stack
| Layer | Technology | Purpose |
|--------|-------------|----------|
| **Frontend (Desktop)** | Electron JS | Admin interface |
| **Frontend (Mobile)** | Android Studio (XML + Kotlin/Java) | Chef/Waiter apps |
| **Backend** | Firebase Firestore | Realtime data layer |
| **Auth** | Firebase Authentication | Role-based access |
| **Functions** | Firebase Cloud Functions | Automation + notifications |
| **Messaging** | Firebase Cloud Messaging | Push notifications |

---

## Notes
- Entire system runs on Firebase free tier.
- Offline persistence enabled on Android.
- Firestore listeners keep all roles in sync.
- No third-party dependencies; focus on performance and simplicity.

---

**Goal:** Create a lightweight, fully-synced ecosystem that connects admin, chef, and waiter roles in real-time with minimal latency and zero paid dependencies.

