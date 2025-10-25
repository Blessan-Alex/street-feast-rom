# 🎯 ROMS – Final Implementation Spec

## 1. Tech Stack (Confirmed)

| Component | Technology |
|-----------|-----------|
| **Desktop (Admin)** | Electron + React + Firebase JS SDK |
| **Android (Chef)** | Android Studio (Kotlin) + Firebase Android SDK |
| **Android (Waiter)** | Android Studio (Kotlin) + Firebase Android SDK |
| **Backend** | Firebase (Firestore + Auth + Functions + Storage) |
| **Deployment** | Direct APK sharing + Electron installer |

---

## 2. Detailed Feature Breakdown

### 🧑‍💼 ADMIN APP (Electron Desktop)

#### **Feature 1: Menu Management**
**Options:**
- **Option A**: Upload Excel file (CSV/XLSX) → Parse and import menu items
- **Option B**: Manually add items via interface form

**Excel Format:**
```
Item Name | Category | Price | Available
Pizza     | Main     | 299   | Yes
Coke      | Beverage | 50    | Yes
```

**Interface:**
- "**+ Add Item**" button always visible (floating or in header)
- Form fields: Item Name, Category, Price, Available (toggle)
- Edit/Delete existing items
- Real-time sync across all devices

**Firestore:**
```javascript
menuItems/{itemId}
{
  name: "Pizza",
  category: "Main",
  price: 299,
  available: true,
  createdAt: timestamp
}
```

---

#### **Feature 2: Create Order**

**Step 1: Select Mode**
- **Dine-in** → Enter/select Table Number (dropdown or input)
- **Parcel** → No table number needed
- **Delivery** → Enter customer name, phone, address

**Step 2: Add Menu Items**
- Display all available menu items in a list/grid
- Each item shows:
  - Name
  - Price
  - **+** button (add/increase quantity)
  - **−** button (decrease quantity, min 0)
  - Current quantity badge

**Step 3: Calculate Total**
- Real-time total calculation displayed
- "**Submit Order**" button

**UI Example:**
```
┌─────────────────────────────────┐
│ Pizza (₹299)     [-] 2 [+]      │
│ Coke (₹50)       [-] 1 [+]      │
│ Burger (₹199)    [-] 0 [+]      │
├─────────────────────────────────┤
│ Total: ₹648                      │
│ [Submit Order]                   │
└─────────────────────────────────┘
```

**Firestore:**
```javascript
orders/{orderId}
{
  orderId: "ORD123",
  mode: "dine-in",
  tableNumber: "5",
  items: [
    {itemId: "id1", name: "Pizza", price: 299, quantity: 2},
    {itemId: "id2", name: "Coke", price: 50, quantity: 1}
  ],
  total: 648,
  status: "Created",
  createdAt: timestamp,
  updatedAt: timestamp,
  closedAt: null,
  archived: false,
  customerInfo: null // or {name, phone, address} for delivery
}
```

---

#### **Feature 3: Manage Orders (Orders Dashboard)**

**Display:**
- List of all active orders (not archived)
- Each order card shows:
  - Order ID
  - Table Number / Mode (Dine-in/Parcel/Delivery)
  - Items summary (e.g., "2x Pizza, 1x Coke")
  - Total amount
  - **Status badge** (color-coded)
  - Timestamp

**Status Colors:**
| Status | Color | Hex |
|--------|-------|-----|
| Created | Grey | `#9E9E9E` |
| Accepted | Blue | `#2196F3` |
| Prepared | Yellow/Amber | `#FFC107` |
| Delivered | Green | `#4CAF50` |
| Closed | Dark Grey | `#616161` |

**Filter Options (Optional):**
- Show All / Active Only / Closed Only
- Search by table number

---

#### **Feature 4: Close or Add More Items**

**Trigger:** When order status is "**Delivered**"

**Admin sees 2 buttons:**
1. **Close Order**
   - Status → "Closed"
   - `closedAt` timestamp set
   - Table marked as free (if dine-in)
   - Order archived after 30 minutes

2. **Add More Items**
   - Opens "Add Items" modal (same as Create Order Step 2)
   - Admin selects new items
   - New items added to existing order
   - Status → "Accepted" (chef needs to prepare new items)
   - `updatedAt` timestamp refreshed

**Firestore Update:**
```javascript
// Close Order
{
  status: "Closed",
  closedAt: timestamp,
  updatedAt: timestamp
}

// Add More Items
{
  status: "Accepted",
  items: [...oldItems, ...newItems],
  total: newTotal,
  updatedAt: timestamp
}
```

---

### 👨‍🍳 CHEF APP (Android)

#### **Feature 1: View Incoming Orders**
**Filter:** Show only orders with status = "**Created**"

**Display:**
- Scrollable list (RecyclerView)
- Each order card shows:
  - Order ID
  - Table Number / Mode
  - Items list with quantities
  - Total amount
  - **"Accept Order"** button (large, blue)

---

#### **Feature 2: Accept Order**
**Action:** Tap "**Accept Order**"

**Effect:**
- Status changes to "**Accepted**"
- Order moves to "In Progress" section
- Timestamp recorded
- Admin sees status change (realtime)

---

#### **Feature 3: Mark as Prepared**
**Display:** Orders with status = "**Accepted**" (my orders in progress)

**Action:** Tap "**Mark Prepared**"

**Effect:**
- Status changes to "**Prepared**"
- Order disappears from Chef's view (or moves to "Completed" tab)
- Waiter app receives realtime update
- Status badge turns yellow

---

### 🧍‍♂ WAITER APP (Android)

#### **Feature 1: View Prepared Orders**
**Filter:** Show only orders with status = "**Prepared**"

**Display:**
- Scrollable list (RecyclerView)
- Each order card shows:
  - Order ID
  - Table Number / Mode
  - Items list with quantities
  - Customer info (if delivery)
  - **"Mark Delivered"** button (large, green)

---

#### **Feature 2: Mark as Delivered**
**Action:** Tap "**Mark Delivered**"

**Effect:**
- Status changes to "**Delivered**"
- Order disappears from Waiter's view
- Admin receives realtime update
- Admin sees 2 options (Close / Add More)

---

## 3. Complete Order Lifecycle with Colors

```
┌──────────────────────────────────────────────────────────┐
│ 1. Admin Creates Order                                   │
│    Status: Created        [Grey Badge]                   │
├──────────────────────────────────────────────────────────┤
│ 2. Chef Accepts Order                                    │
│    Status: Accepted       [Blue Badge]                   │
├──────────────────────────────────────────────────────────┤
│ 3. Chef Marks Prepared                                   │
│    Status: Prepared       [Yellow Badge]                 │
├──────────────────────────────────────────────────────────┤
│ 4. Waiter Marks Delivered                                │
│    Status: Delivered      [Green Badge]                  │
├──────────────────────────────────────────────────────────┤
│ 5. Admin Chooses:                                        │
│    Option A: Close Order → [Dark Grey Badge]             │
│    Option B: Add Items → Back to Accepted [Blue Badge]   │
└──────────────────────────────────────────────────────────┘
```

---

## 4. Firebase Firestore Schema (Complete)

### Collection: `menuItems`
```javascript
{
  id: auto-generated,
  name: string,
  category: string,
  price: number,
  available: boolean,
  createdAt: timestamp
}
```

### Collection: `orders`
```javascript
{
  orderId: auto-generated,
  mode: "dine-in" | "parcel" | "delivery",
  tableNumber: string | null,
  customerInfo: {
    name: string,
    phone: string,
    address: string
  } | null,
  items: [
    {
      itemId: string,
      name: string,
      price: number,
      quantity: number
    }
  ],
  total: number,
  status: "Created" | "Accepted" | "Prepared" | "Delivered" | "Closed",
  createdAt: timestamp,
  updatedAt: timestamp,
  closedAt: timestamp | null,
  archived: boolean
}
```

### Collection: `users`
```javascript
{
  uid: string (Auth UID),
  email: string,
  role: "admin" | "chef" | "waiter",
  name: string
}
```

---

## 5. Realtime Queries (Firestore)

### Admin: View All Orders
```javascript
db.collection('orders')
  .where('archived', '==', false)
  .orderBy('createdAt', 'desc')
  .onSnapshot(snapshot => {
    // Update UI in realtime
  });
```

### Chef: View Created Orders
```javascript
db.collection('orders')
  .where('status', '==', 'Created')
  .where('archived', '==', false)
  .orderBy('createdAt', 'asc')
  .onSnapshot(snapshot => {
    // Update UI in realtime
  });
```

### Chef: View Accepted Orders (In Progress)
```javascript
db.collection('orders')
  .where('status', '==', 'Accepted')
  .where('archived', '==', false)
  .orderBy('createdAt', 'asc')
  .onSnapshot(snapshot => {
    // Update UI in realtime
  });
```

### Waiter: View Prepared Orders
```javascript
db.collection('orders')
  .where('status', '==', 'Prepared')
  .where('archived', '==', false)
  .orderBy('createdAt', 'asc')
  .onSnapshot(snapshot => {
    // Update UI in realtime
  });
```

---

## 6. Cloud Function: Auto-Archive

**Trigger:** Every 5 minutes (Cloud Scheduler)

**Logic:**
```javascript
exports.archiveOldOrders = functions.pubsub
  .schedule('every 5 minutes')
  .onRun(async (context) => {
    const cutoffTime = admin.firestore.Timestamp.fromDate(
      new Date(Date.now() - 30 * 60 * 1000) // 30 minutes ago
    );
    
    const snapshot = await admin.firestore()
      .collection('orders')
      .where('status', '==', 'Closed')
      .where('closedAt', '<=', cutoffTime)
      .where('archived', '==', false)
      .get();
    
    const batch = admin.firestore().batch();
    snapshot.docs.forEach(doc => {
      batch.update(doc.ref, { archived: true });
    });
    
    await batch.commit();
    console.log(`Archived ${snapshot.size} orders`);
  });
```

---

## 7. UI/UX Requirements

### Color Palette
```css
Created:   #9E9E9E (Grey)
Accepted:  #2196F3 (Blue)
Prepared:  #FFC107 (Amber/Yellow)
Delivered: #4CAF50 (Green)
Closed:    #616161 (Dark Grey)

Background: #FFFFFF (White)
Text:       #212121 (Dark Grey)
Accent:     #FF5722 (Orange for buttons)
```

### Typography
- **Headings**: 20-24px, Bold
- **Body**: 14-16px, Regular
- **Buttons**: 16px, Medium

### Components
- **Buttons**: Rounded corners (8px), shadow on hover
- **Cards**: White background, subtle shadow, 12px padding
- **Badges**: Pill-shaped, status color background, white text
- **Lists**: Smooth scrolling, pull-to-refresh on Android

### Interactions
- **Loading**: Show spinner while fetching data
- **Success**: Brief toast message (e.g., "Order accepted!")
- **Error**: Red toast with retry option
- **Empty State**: "No orders yet" with icon

---

## 8. Missing Clarifications (Need Decisions)

1. **Table Management**: 
   - Should Admin create/manage table list beforehand?
   - Or free-text input for table numbers?

2. **User Accounts**:
   - How do you create Chef/Waiter accounts? Admin panel or Firebase Console?

3. **Multiple Chefs**:
   - Can multiple chefs see same "Created" order?
   - First to accept gets it, or all chefs can work on it?

4. **Edit Orders**:
   - Can Admin edit items/quantities after order is created?
   - Or only add more items after delivery?

5. **Order Cancellation**:
   - Can Admin cancel an order mid-flow?
   - Or only after delivery (Close option)?

6. **Notifications**:
   - Should Chef/Waiter get push notifications for new orders?
   - Or just rely on realtime list updates?

7. **Delivery Mode Details**:
   - Do you need to capture customer address, phone for delivery?
   - Or just mark it as "Delivery" without details?

---

## 9. Recommended Simplifications for MVP

### Include:
✅ Manual menu creation + Excel upload  
✅ All 3 modes (Dine-in, Parcel, Delivery)  
✅ Complete status flow  
✅ Close/Add More logic  
✅ 30-min auto-archive  
✅ Realtime sync  
✅ Status color coding  

### Exclude (Add Later):
❌ Push notifications (realtime list is enough)  
❌ Order editing (only add more items after delivery)  
❌ Multi-restaurant support  
❌ Analytics/reports  
❌ Payment integration  
❌ Table QR codes  

---

## 10. Development Checklist

### Setup (Week 1)
- [ ] Create Firebase project
- [ ] Enable Firestore, Auth, Functions, Storage
- [ ] Create 3 user accounts (admin, chef, waiter) in Auth
- [ ] Add user docs in Firestore with roles
- [ ] Set up Firestore Security Rules

### Admin App (Week 1-2)
- [ ] Electron + React project setup
- [ ] Firebase SDK integration
- [ ] Login screen
- [ ] Menu management (CRUD + Excel upload)
- [ ] Create order flow (mode selection + item picker)
- [ ] Orders dashboard (realtime list)
- [ ] Close/Add More logic

### Chef App (Week 2)
- [ ] Android project setup (Kotlin)
- [ ] Firebase SDK integration
- [ ] Login screen
- [ ] Created orders list (realtime)
- [ ] Accept order button
- [ ] Accepted orders list
- [ ] Mark Prepared button

### Waiter App (Week 2-3)
- [ ] Android project setup (Kotlin)
- [ ] Firebase SDK integration
- [ ] Login screen
- [ ] Prepared orders list (realtime)
- [ ] Mark Delivered button

### Cloud Functions (Week 3)
- [ ] Auto-archive function (30 min rule)
- [ ] Deploy to Firebase

### Testing (Week 3-4)
- [ ] Multi-device testing (realtime sync)
- [ ] Status flow end-to-end
- [ ] UI polish (smooth scrolling, colors, loading states)
- [ ] Edge cases (no internet, empty lists, etc.)

### Deployment (Week 4)
- [ ] Build Electron installers (Windows/Mac)
- [ ] Build signed APKs (Chef + Waiter apps)
- [ ] Share with client
- [ ] Write basic user guide

---

## 11. Final Summary

**This spec covers:**
✅ All Admin functionalities (menu, create order, manage, close/add)  
✅ Chef workflow (accept, prepare)  
✅ Waiter workflow (deliver)  
✅ Complete status lifecycle with color coding  
✅ Realtime sync across all apps  
✅ 30-minute auto-archive  
✅ Simple, minimalistic UI  
✅ Direct APK + installer deployment  

**Ready to implement!** 🚀