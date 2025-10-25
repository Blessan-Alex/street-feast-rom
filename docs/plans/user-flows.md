### ROMS – User Flows (Admin, Chef, Waiter)

This document captures key user flows across all roles, aligned to spec-sheet.md. Flows are minimal, linear where possible, and optimized for speed and clarity.

## Legend (statuses)
- Created → Accepted → Prepared → Delivered → Closed
- Cancelled (can happen any time before Closed)

## 1) Admin – Create and Manage Order

Flow (text)
1. Open Admin app → Login
2. Choose: Create Order
3. Select Mode: Dine-in | Parcel | Delivery
   - If Dine-in: enter/select Table Number (enforce one active order per table)
   - If Delivery: mark as Delivery (no extra fields in MVP)
4. Add Items: search/select; adjust quantities with +/-; see live Total
5. Submit → Order status = Created
6. Monitor in Orders (Active tab)
7. Optional (pre-Delivery): Edit items/quantities → Chef notified if kitchen work changes
8. After Waiter marks Delivered:
   - Close Order → status Closed (auto-archive after 30 min), table freed
   - OR Add More Items → status returns to Accepted
9. Optional: Cancel at any time before Closed → status Cancelled (auto-archive after 30 min)

Diagram (ASCII)
```
Login → Create Order → Mode → Table/Mark → Add Items → Submit
                                               ↓
                                         Status: Created
                                               ↓
                                [Edit pre-Delivery?] — yes → Update (notify Chef)
                                               ↓
                 Delivered? — no → Monitor
                              yes → [Close] or [Add More] → (Accepted)
                                               ↓
                                          Auto-archive (Closed/Cancelled after 30m)
```

## 2) Chef – Accept and Prepare

Flow (text)
1. Open Chef app → Login
2. Incoming list shows Created orders (realtime)
3. Tap Accept → status = Accepted → moves to In Progress
4. Prepare items
5. Tap Mark Prepared → status = Prepared → exits Chef list
6. Admin edits (pre-Delivery) that increase kitchen work: card shows Updated chip; proceed as needed
7. If Admin cancels: order disappears immediately

Diagram (ASCII)
```
Login → Incoming (Created) → Accept → In Progress (Accepted) → Mark Prepared → (Prepared, removed)
              ↑
        New orders via push/listen
```

## 3) Waiter – Deliver

Flow (text)
1. Open Waiter app → Login
2. List shows Prepared orders (realtime)
3. Tap Mark Delivered → status = Delivered → exits list
4. If Admin adds more items post-Delivery: order reappears (Accepted) after kitchen prep; waiter sees again at Prepared
5. If Admin cancels: order disappears immediately

Diagram (ASCII)
```
Login → Prepared list → Mark Delivered → (Delivered, removed)
               ↑                     
         Prepared via Chef
```

## 4) Push Notification Triggers
- Chef: Created, Reopened/Add More, impactful Admin edits increasing kitchen work
- Waiter: Prepared, Reopened/Add More

## 5) Error/Edge Handling (lightweight)
- Offline: show banner; keep list; resync on reconnect
- Conflicts (rare with single chef): first writer wins; UI refresh from Firestore
- Validation: one active order per table (dine-in); cannot Close before Delivered


