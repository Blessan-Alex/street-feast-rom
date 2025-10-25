# Streat Feast — Test & Acceptance Checklist

## Purpose
This checklist ensures all core functionality, UI behaviors, and backend processes of Streat Feast work as expected across the Admin (Electron), Chef, and Waiter (Android) applications before final deployment.

---

## 1. Authentication & Access
| Test ID | Scenario | Expected Result | Status |
|----------|-----------|-----------------|---------|
| A1 | Admin login with valid credentials | Redirects to Dashboard | ☐ |
| A2 | Admin login with wrong password | Shows clear red error | ☐ |
| A3 | Chef login | Loads New Orders tab | ☐ |
| A4 | Waiter login | Loads Prepared tab | ☐ |
| A5 | Unauthorized role tries to access Admin panel | Access denied | ☐ |

---

## 2. Menu Management
| Test ID | Scenario | Expected Result | Status |
|----------|-----------|-----------------|---------|
| M1 | Upload valid CSV (with correct headers) | Menu imported successfully | ☐ |
| M2 | Upload invalid CSV | Error message with line details | ☐ |
| M3 | Add manual category and item | Saved and visible in summary | ☐ |
| M4 | Delete category | Category removed from menu | ☐ |
| M5 | Edit existing item | Changes reflected instantly | ☐ |

---

## 3. Order Creation (Admin)
| Test ID | Scenario | Expected Result | Status |
|----------|-----------|-----------------|---------|
| O1 | Create new order with valid items | Appears in Dashboard + Chef’s New Orders | ☐ |
| O2 | Add Chef Tip | Visible to Chef only | ☐ |
| O3 | Select order type (Dine-in / Parcel / Delivery) | Stored correctly in Firestore | ☐ |
| O4 | Edit order before Chef accepts | Edits saved and synced | ☐ |
| O5 | Add more items while In Kitchen | Creates add-on ticket | ☐ |
| O6 | Cancel order in Created/Accepted status | Order marked canceled | ☐ |

---

## 4. Chef App (Android)
| Test ID | Scenario | Expected Result | Status |
|----------|-----------|-----------------|---------|
| C1 | Receive new order notification | Appears in New Orders list | ☐ |
| C2 | Accept order | Moves to Preparing tab | ☐ |
| C3 | Mark Prepared | Sends FCM to Waiter + updates Admin | ☐ |
| C4 | View Chef Tip | Tip visible in Preparing view | ☐ |
| C5 | Offline mode (network loss) | Orders remain visible (cached) | ☐ |

---

## 5. Waiter App (Android)
| Test ID | Scenario | Expected Result | Status |
|----------|-----------|-----------------|---------|
| W1 | Receive Prepared notification | Appears in Ready list | ☐ |
| W2 | Mark Delivered | Status updates in Admin Dashboard | ☐ |
| W3 | Offline mode (network loss) | Prepared orders still viewable | ☐ |
| W4 | Attempt to deliver non-Prepared order | Error shown, no update | ☐ |

---

## 6. Dashboard & Reporting (Admin)
| Test ID | Scenario | Expected Result | Status |
|----------|-----------|-----------------|---------|
| D1 | Live KPIs update in realtime | Correct counts for all statuses | ☐ |
| D2 | Sort/filter orders | Works without lag | ☐ |
| D3 | View completed order details | All fields correct | ☐ |
| D4 | Frequent items list | Loads correctly on POS | ☐ |
| D5 | Delete all data | Requires double confirmation, clears DB | ☐ |

---

## 7. Notifications & Sounds
| Test ID | Event | Expected Message | Sound | Status |
|----------|--------|------------------|--------|---------|
| N1 | New order created | “Order #ID ready to accept.” | Ping | ☐ |
| N2 | Order prepared | “Order #ID ready to serve.” | Ping | ☐ |
| N3 | Order delivered | “Order #ID delivered.” | Click | ☐ |
| N4 | Order canceled | “Order #ID canceled.” | Buzzer | ☐ |

---

## 8. Data & Backend Validation
| Test ID | Scenario | Expected Result | Status |
|----------|-----------|-----------------|---------|
| B1 | Firestore entries created for order & orderItems | Correct document structure | ☐ |
| B2 | ServerTimestamp fields populated | Accurate timestamps | ☐ |
| B3 | Role-based permissions enforced | Only allowed status transitions succeed | ☐ |
| B4 | Cloud Functions trigger notifications correctly | Correct recipients notified | ☐ |
| B5 | Sequential order numbers generated | No duplicates | ☐ |

---

## 9. UX Consistency & Visual Checks
| Test ID | Scenario | Expected Result | Status |
|----------|-----------|-----------------|---------|
| U1 | Buttons colored consistently (Green = OK, Red = Cancel) | Matches design spec | ☐ |
| U2 | Text readable (≥14pt, good contrast) | Meets accessibility | ☐ |
| U3 | Empty states visible | Displays friendly message | ☐ |
| U4 | Icons consistent | Matches function context | ☐ |

---

## 10. Final Acceptance Criteria
| Criteria | Expected Outcome | Status |
|-----------|------------------|---------|
| All roles can perform their end-to-end workflow | Yes | ☐ |
| Firestore data accurate and synced | Yes | ☐ |
| All major features stable offline | Yes | ☐ |
| Notifications trigger reliably | Yes | ☐ |
| App runs error-free for 2+ hours continuous use | Yes | ☐ |

---

**Acceptance Decision:** ✅ Approved ☐ Rework Needed

