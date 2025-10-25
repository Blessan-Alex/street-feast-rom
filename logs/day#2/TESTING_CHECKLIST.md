# Day 2 Testing Checklist

## Pre-Testing Setup

- [ ] App builds successfully (`npm run build`)
- [ ] App starts in dev mode (`npm run dev`)
- [ ] No console errors on startup
- [ ] Login works (admin@test.com / password123)

---

## 1. Dashboard Page (/dashboard)

### Initial State (No Orders)
- [ ] Page loads and displays correctly
- [ ] All 4 KPI cards show "0"
- [ ] "New Orders" section shows empty state message
- [ ] "Ready Orders" section shows empty state message
- [ ] "Create New Order" button is visible

### With Orders
- [ ] Total Active count excludes Closed/Canceled orders
- [ ] New count shows only Created orders
- [ ] In Kitchen count shows only InKitchen orders
- [ ] Ready count shows only Prepared orders
- [ ] Recent orders display correctly (max 5 each)
- [ ] Time ago formatting works ("5 mins ago")
- [ ] "View" buttons navigate to Manage Orders
- [ ] KPIs update in real-time when orders change

---

## 2. Create Order Page (/create-order)

### Layout
- [ ] Category grid displays on left
- [ ] Order Summary Card displays on right (sidebar)
- [ ] Search bar is visible at top
- [ ] Page layout is responsive

### Without Menu
- [ ] Empty state shows if no menu exists
- [ ] Message directs user to create menu first

### With Menu (use Seed Menu)
- [ ] Categories display as cards
- [ ] Category cards show item count
- [ ] Frequent items strip shows at top (if configured)

### Category Navigation
- [ ] Click category → shows items in that category
- [ ] Items display with name, sizes, veg/nonveg badge
- [ ] "Back to Categories" button works
- [ ] Items are clickable

### Frequent Items Strip
- [ ] Frequent items display horizontally
- [ ] Can scroll if many items
- [ ] Items show veg/nonveg badge
- [ ] "+" indicator visible
- [ ] Clicking adds to cart

### Search Functionality
- [ ] Can type in search box
- [ ] Results filter as you type (debounced)
- [ ] Search matches item names
- [ ] Search matches category names
- [ ] Clear button (×) appears when typing
- [ ] Clear button clears search
- [ ] Empty results show message

### Adding Items
- [ ] Click item with sizes → size selector modal opens
- [ ] Size selector shows all available sizes
- [ ] Can select size (radio buttons)
- [ ] "Add to Order" button works
- [ ] Modal closes after adding
- [ ] Click item without sizes → adds directly to cart
- [ ] Item appears in Order Summary Card

---

## 3. Order Summary Card (Right Sidebar)

### Line Items Display
- [ ] Added items appear in list
- [ ] Each line shows: name, size (if any), veg badge
- [ ] Quantity displays correctly (default 1)
- [ ] Total item count shows at top

### Quantity Controls
- [ ] "+" button increases quantity
- [ ] "−" button decreases quantity
- [ ] Cannot decrease below 1
- [ ] "−" button disabled at qty 1
- [ ] Quantity updates immediately

### Remove Items
- [ ] "×" button visible on each line
- [ ] Click "×" removes item from cart
- [ ] Removed item disappears immediately

### Chef Tip
- [ ] Textarea is visible
- [ ] Placeholder text shows
- [ ] Can type multi-line text
- [ ] Text saves with order

### Order Type
- [ ] Three radio options visible (DineIn, Parcel, Delivery)
- [ ] DineIn selected by default
- [ ] Can change selection
- [ ] Only one can be selected at a time

### Place Order Button
- [ ] Disabled when cart is empty
- [ ] Enabled when cart has items
- [ ] Click creates order
- [ ] Success toast shows with order number
- [ ] Navigates to Dashboard
- [ ] Cart clears after placing

### Cancel Order Button
- [ ] Always visible
- [ ] Click shows confirmation dialog
- [ ] Confirmation has "Yes, Cancel" and "No, Keep"
- [ ] "Yes" clears cart
- [ ] "No" keeps cart as-is
- [ ] Toast shows "Order cancelled"

---

## 4. Manage Orders Page (/manage-orders)

### Status Filter Pills
- [ ] "All" pill shows total count
- [ ] Each status pill shows count for that status
- [ ] Active filter is highlighted (green)
- [ ] Clicking filter updates the list
- [ ] Count updates as orders change

### Order List Table
- [ ] Table displays with headers
- [ ] Order # column shows sequential numbers
- [ ] Type column shows DineIn/Parcel/Delivery
- [ ] Items column shows count
- [ ] Chef Tip column shows "✓" if present, "-" if not
- [ ] Status column shows colored chip
- [ ] Time column shows time created (12-hour format)
- [ ] Time column shows "time ago" below
- [ ] Actions column has "View" button
- [ ] Newest orders at top (sorted by createdAt DESC)

### Empty States
- [ ] Shows message when no orders
- [ ] Shows message when filtered status has no orders
- [ ] Messages are helpful and clear

### Order Detail Modal
- [ ] "View" button opens modal
- [ ] Modal displays over page (with backdrop)
- [ ] Click backdrop closes modal
- [ ] "×" button closes modal

### Modal Content
- [ ] Header shows "Order #[number]"
- [ ] Shows created date/time
- [ ] Shows order type badge
- [ ] Shows current status chip
- [ ] Chef tip displays in highlighted box (if present)
- [ ] All line items listed with details
- [ ] Each line shows: name, size, qty, veg badge
- [ ] Status transition buttons section visible

### Status Transitions
- [ ] Only allowed transitions show as buttons
- [ ] Green buttons for forward transitions
- [ ] Red button for Cancel
- [ ] No buttons if no allowed transitions
- [ ] "Cancel Order" shows confirmation dialog
- [ ] Confirming cancel changes status to Canceled
- [ ] Other transitions happen immediately
- [ ] Success toast shows after transition
- [ ] Modal stays open, status updates
- [ ] Available buttons update after transition

### Status Transition Rules
- [ ] Created → can go to Accepted or Canceled
- [ ] Accepted → can go to InKitchen or Canceled
- [ ] InKitchen → can go to Prepared or Canceled
- [ ] Prepared → can only go to Delivered
- [ ] Delivered → can only go to Closed
- [ ] Closed → no transitions available
- [ ] Canceled → no transitions available

---

## 5. Settings Page - Delete All Data

### Danger Zone Section
- [ ] Section has red background
- [ ] "DANGER ZONE" heading visible
- [ ] Warning text explains consequences
- [ ] "Delete All Data" button is red

### First Confirmation
- [ ] Click button → first dialog opens
- [ ] Dialog explains what will be deleted
- [ ] "Cancel" button closes dialog
- [ ] "Yes, Continue" button proceeds

### Second Confirmation
- [ ] Second dialog opens
- [ ] Requires typing "DELETE"
- [ ] Input field is focused
- [ ] "Confirm Delete" disabled until typed
- [ ] Typing "DELETE" enables button
- [ ] Wrong text keeps button disabled
- [ ] "Cancel" closes dialog
- [ ] "Confirm Delete" proceeds

### Deletion Results
- [ ] Success toast shows
- [ ] Menu data cleared (categories, items)
- [ ] Orders data cleared
- [ ] Draft order cleared
- [ ] Order counter cleared
- [ ] Frequent items cleared
- [ ] User logged out
- [ ] Redirected to login page
- [ ] After re-login, all screens show empty states

---

## 6. Data Persistence

### localStorage
- [ ] Orders persist after page refresh
- [ ] Draft order persists during order creation
- [ ] Order counter persists
- [ ] After placing order, counter increments
- [ ] Sequential order numbers work (1001, 1002, 1003...)

### Cross-Page Updates
- [ ] Create order → Dashboard updates
- [ ] Change status → Dashboard KPIs update
- [ ] Change status → filter counts update in Manage Orders
- [ ] All pages stay in sync

---

## 7. Navigation

### Menu Links
- [ ] Dashboard link works
- [ ] Create Order link works
- [ ] Manage Orders link works
- [ ] Menu link works (from Day 1)
- [ ] Settings link works
- [ ] Active page highlighted in sidebar

### Navigation Flow
- [ ] Can navigate between all pages
- [ ] Back/forward browser buttons work
- [ ] URL updates correctly
- [ ] No navigation errors

---

## 8. Edge Cases

### Empty Menu
- [ ] Create Order shows empty state
- [ ] Can still access other pages
- [ ] Dashboard works with no orders

### Empty Orders
- [ ] Dashboard shows all zeros
- [ ] Manage Orders shows empty state
- [ ] No console errors

### Large Order
- [ ] Can add 20+ items to cart
- [ ] Scrolling works in Order Summary Card
- [ ] All items display correctly
- [ ] Place order works

### Long Chef Tip
- [ ] Textarea expands for long text
- [ ] Text saves correctly
- [ ] Displays fully in order detail

### Rapid Clicking
- [ ] Status transitions debounced
- [ ] Cannot create duplicate orders
- [ ] Cannot double-add items

---

## 9. Accessibility

### Keyboard Navigation
- [ ] Can tab through all interactive elements
- [ ] Focus rings visible
- [ ] Enter key works on buttons
- [ ] Escape closes modals

### Visual
- [ ] Text is at least 14px
- [ ] High contrast between text and background
- [ ] Status not indicated by color alone
- [ ] Icons/badges supplement color

### Touch Targets
- [ ] All buttons at least 44px height
- [ ] Easy to tap on touchscreen
- [ ] No accidental clicks

---

## 10. Error Handling

### Component Errors
- [ ] Error Boundary catches crashes
- [ ] No white screen of death
- [ ] Error message displays

### Invalid Actions
- [ ] Cannot place empty order
- [ ] Cannot reduce qty below 1
- [ ] Cannot make invalid status transitions
- [ ] Helpful error messages show

---

## Performance

- [ ] Search debouncing works (no lag)
- [ ] Status updates are instant
- [ ] No freezing or stuttering
- [ ] KPI calculations fast
- [ ] Large order lists perform well

---

## Final Checks

- [ ] No console errors
- [ ] No console warnings
- [ ] Build passes (`npm run build`)
- [ ] No linter errors
- [ ] No TypeScript errors
- [ ] All Day 1 features still work
- [ ] Can complete full order flow end-to-end

---

## End-to-End Test Flow

Complete this flow to verify everything works:

1. [ ] Login (admin@test.com / password123)
2. [ ] Go to Menu → Seed Menu (if no menu)
3. [ ] Go to Dashboard → Verify 0 orders
4. [ ] Go to Create Order
5. [ ] Add 3 items (test categories, frequent, search)
6. [ ] Test qty controls
7. [ ] Add chef tip
8. [ ] Change order type to Parcel
9. [ ] Place order
10. [ ] Verify redirect to Dashboard
11. [ ] Check KPIs updated (1 new order)
12. [ ] Go to Manage Orders
13. [ ] Verify order appears in list
14. [ ] Click "View"
15. [ ] Verify all details correct
16. [ ] Mark as Accepted
17. [ ] Mark as InKitchen
18. [ ] Mark as Prepared
19. [ ] Check Dashboard shows 1 in "Ready"
20. [ ] Go back to Manage Orders
21. [ ] Mark as Delivered
22. [ ] Mark as Closed
23. [ ] Verify order no longer in "Active"
24. [ ] Refresh page → all data persists
25. [ ] Go to Settings
26. [ ] Test "Delete All Data"
27. [ ] Verify everything cleared
28. [ ] Verify logged out

---

**Testing Status:** ⬜ Not Started | 🟡 In Progress | ✅ Complete

**Date Tested:** _____________

**Tested By:** _____________

**Issues Found:** _____________

**Notes:** _____________

