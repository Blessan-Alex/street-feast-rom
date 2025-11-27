Creating a step-by-step implementation plan. Reviewing the plan and resources to map everything:


[3 tools called]


Creating the implementation plan document:


[1 tool called]


# Waiter Page Redesign — Implementation Plan

## Current state

**Current Waiter Page:**
- Fragment: `WaiterReadyFragment.kt` (simple ready orders list)
- Layout: `fragment_waiter_ready.xml` (basic RecyclerView)
- Navigation: Single "Ready" tab
- Functionality: Shows ready orders with "Mark All Delivered" button

**Target:** Redesign into 13 screens with a new order flow, preview, and order management tabs.

---

## Phase 1: Create all resources first

Create all resources before implementing screens.

### 1.1 Colors (`res/values/colors.xml`)
**Location in resources.md:** Lines 2-35 + Lines 1406-1421

Add Street Feast colors:
- Core: `sf_bg`, `sf_appbar_bg`
- Primary: `sf_primary` (#4084F4 or #4186F6)
- Text: `sf_text_primary`, `sf_text_secondary`, `sf_text_muted`
- Surfaces: `sf_surface_soft`, `sf_surface_soft2`, `sf_chip_bg`
- Additional: `sf_card_bg`, `sf_disabled_circle`, `sf_badge_red`, `sf_scrim`, `sf_success`, `sf_border`, `sf_light_blue`, `sf_card_soft`

### 1.2 Dimensions (`res/values/dimens.xml`)
**Location in resources.md:** Lines 39-67

Add SF-specific dimensions:
- Padding: `sf_pad_screen_h`, `sf_pad_section_top`
- Radius: `sf_radius_xl`, `sf_radius_l`, `sf_radius_m`
- Elevations: `sf_elev_soft`, `sf_elev_nav`
- Heights: `sf_appbar_h`, `sf_bottom_nav_h`, `sf_selected_nav_h`, `sf_btn_h_big`, `sf_btn_h_mid`, `sf_search_h`
- Stepper: `sf_step_dot`, `sf_step_line_h`

### 1.3 Drawables (shapes/backgrounds)
**Location in resources.md:** Lines 71-172, 1425-1522, 2306-2363

Create all drawable files:
- Backgrounds: `sf_bg_round_soft.xml`, `sf_bg_round_soft2.xml`, `sf_bg_round_primary.xml`, `sf_bg_chip.xml`, `sf_bg_circle_dark.xml`, `sf_bg_circle_light.xml`, `sf_bg_search.xml`, `sf_bg_card.xml`, `sf_bg_circle_disabled.xml`, `sf_bg_btn_black.xml`, `sf_bg_sheet.xml`, `sf_bg_card_soft.xml`, `sf_bg_pill_outline.xml`, `sf_bg_pill_green.xml`, `sf_bg_circle_white.xml`
- Stepper: `sf_step_dot_active.xml`, `sf_step_dot_inactive.xml`, `sf_step_line.xml`
- Dots: `sf_dot_green.xml`, `sf_dot_red.xml`
- Grabbers: `sf_grabber_bg.xml`, `sf_grabber_fg.xml`
- Badges: `sf_badge_red.xml`, `sf_badge_qty.xml`
- Radio: `sf_radio_checked.xml`, `sf_radio_unchecked.xml`, `sf_radio_selector.xml`

### 1.4 Icons (vector drawables)
**Location in resources.md:** Lines 176-238, 1526-1548, 2367-2378

Create all icon files:
- `ic_close_20.xml` (dark)
- `ic_close_white_20.xml` (white)
- `ic_plus_20.xml` (white)
- `ic_search_20.xml` (grey)
- `ic_chev_left_20.xml`
- `ic_chev_right_20.xml`
- `ic_chev_down_20.xml`
- `ic_arrow_left_20.xml`
- `ic_check_white_20.xml`

---

## Phase 2: Screen-by-screen implementation

Implement one screen at a time. For each screen:
1. Check `plan.md` for screen mapping
2. Find code in `resources.md`
3. Compare with screen image in `screens/`
4. Create layout file(s)
5. Create/update Fragment if needed

---

### Screen 1: Type Selection (Dine in / Parcel / Eat away)
**Screen Image:** `screens/screen1.png`  
**Plan.md Reference:** Line 11 (Screen 1)  
**Resources.md Code:** Lines 242-369

**Layout File:** `res/layout/fragment_order_type.xml`

**Dependencies:**
- `include_sf_appbar.xml` (Lines 1012-1077)
- `include_sf_stepper.xml` (Lines 1227-1391)
- `include_sf_bottom_nav.xml` (Lines 1079-1225)

**Implementation Steps:**
1. Create `include_sf_appbar.xml` (reusable header — similar to chef page topBar)
2. Create `include_sf_stepper.xml` (5-step progress indicator)
3. Create `include_sf_bottom_nav.xml` (bottom navigation with "New Order" selected)
4. Create `fragment_order_type.xml` (main screen with 3 buttons)

**Chef Page Reference:** Similar top bar structure to `chef_page.xml` lines 9-61

---

### Screen 2: Where (Table Selection)
**Screen Image:** `screens/screen2.png`  
**Plan.md Reference:** Line 12 (Screen 2)  
**Resources.md Code:** Lines 371-612

**Layout File:** `res/layout/fragment_order_where.xml`

**Dependencies:**
- Uses same includes as Screen 1
- `sf_bg_chip.xml` (for "Go back" button)
- `ic_chev_left_20.xml`

**Implementation Steps:**
1. Create `fragment_order_where.xml` with table grid (Flow layout)
2. Table cards use `sf_surface_soft2` background
3. Selected table (4) uses `sf_primary` background
4. "Go back" chip with left chevron icon

---

### Screen 3: Item Screen (Compact — Table Handle)
**Screen Image:** `screens/screen3.png`  
**Plan.md Reference:** Line 13 (Screen 3)  
**Resources.md Code:** Lines 622-849

**Layout File:** `res/layout/fragment_order_item.xml` (state: hide appbar, show handle)

**Dependencies:**
- `include_sf_table_handle.xml` (Lines 1554-1606)
- `item_most_bought.xml` (Lines 851-933)
- `item_category.xml` (Lines 953-1008)
- `sf_bg_search.xml`, `sf_bg_circle_dark.xml`
- `sf_dot_green.xml`, `sf_dot_red.xml`
- Icons: `ic_search_20`, `ic_plus_20`, `ic_chev_right_20`

**Implementation Steps:**
1. Create `include_sf_table_handle.xml` (Table 4 handle with grabber)
2. Create `item_most_bought.xml` (2x2 grid card item)
3. Create `item_category.xml` (category chip card)
4. Create `fragment_order_item.xml` with visibility toggles:
   - `appbar.visibility = GONE`
   - `tvCreateOrder.visibility = GONE`
   - `stepper.visibility = GONE`
   - `tableHandle.visibility = VISIBLE`

---

### Screen 4: Item Screen (With Header + Stepper)
**Screen Image:** `screens/screen4.png`  
**Plan.md Reference:** Line 14 (Screen 4)  
**Resources.md Code:** Same as Screen 3 (Lines 622-849)

**Layout File:** `res/layout/fragment_order_item.xml` (same file, different state)

**Implementation Steps:**
1. Use same `fragment_order_item.xml`
2. Set visibility:
   - `appbar.visibility = VISIBLE`
   - `tvCreateOrder.visibility = VISIBLE`
   - `stepper.visibility = VISIBLE`
   - `tableHandle.visibility = GONE`
3. Set stepper values: `v1.text = "Din in"`, `v2.text = "No 4"`, `v2.visibility = VISIBLE`
4. Add down chevron below stepper (`ic_chev_down_20` rotated 180°)

---

### Screen 5: Category Items Grid
**Screen Image:** `screens/screen5.png`  
**Plan.md Reference:** Line 15 (Screen 5)  
**Resources.md Code:** Lines 1698-1782

**Layout File:** `res/layout/fragment_order_category_items.xml`

**Dependencies:**
- `include_sf_table_handle.xml` (already created)
- `item_menu_card.xml` (Lines 1612-1694)
- RecyclerView with GridLayoutManager (spanCount=2)

**Implementation Steps:**
1. Create `item_menu_card.xml` (menu item card with dots and + button)
2. Create `fragment_order_category_items.xml` with:
   - Table handle at top
   - Search bar
   - Category title ("Chinese")
   - RecyclerView with GridLayoutManager

---

### Screen 6: Modal Step 1 (Amount)
**Screen Image:** `screens/screen6.png`  
**Plan.md Reference:** Line 16 (Screen 6)  
**Resources.md Code:** Lines 1862-2285

**Layout File:** `res/layout/sheet_item_customize.xml` (state: amountBox VISIBLE)

**Dependencies:**
- `sf_bg_sheet.xml`, `sf_bg_circle_dark.xml`, `sf_bg_btn_black.xml`, `sf_bg_chip.xml`
- `sf_step_line.xml`, `sf_step_dot_active.xml`
- `sf_radio_selector.xml`, `sf_radio_checked.xml`, `sf_radio_unchecked.xml`
- `ic_close_white_20.xml`, `ic_arrow_left_20.xml`

**Implementation Steps:**
1. Create `sheet_item_customize.xml` (single layout with 3 step containers)
2. Set visibility: `amountBox = VISIBLE`, `stepChefTip = GONE`, `stepQty = GONE`
3. Radio buttons for Small/Medium/Large
4. "Next" button (black pill)

---

### Screen 7: Modal Step 2 (Chef Tip)
**Screen Image:** `screens/screen7.png`  
**Plan.md Reference:** Line 17 (Screen 7)  
**Resources.md Code:** Same as Screen 6 (Lines 1862-2285)

**Layout File:** `res/layout/sheet_item_customize.xml` (state: stepChefTip VISIBLE)

**Implementation Steps:**
1. Use same `sheet_item_customize.xml`
2. Set visibility: `amountBox = GONE`, `stepChefTip = VISIBLE`, `stepQty = GONE`
3. Material Chips for suggestions (Sour, Spicy, Creamy)
4. EditText for custom tip

---

### Screen 8: Modal Step 3 (Quantity)
**Screen Image:** `screens/screen8.png`  
**Plan.md Reference:** Line 18 (Screen 8)  
**Resources.md Code:** Same as Screen 6 (Lines 1862-2285)

**Layout File:** `res/layout/sheet_item_customize.xml` (state: stepQty VISIBLE)

**Implementation Steps:**
1. Use same `sheet_item_customize.xml`
2. Set visibility: `amountBox = GONE`, `stepChefTip = GONE`, `stepQty = VISIBLE`
3. Minus/Plus buttons (black circles)
4. Large quantity number in center

---

### Screen 9: Preview Order Bar (Component)
**Screen Image:** `screens/screen9.png`  
**Plan.md Reference:** Line 19 (Screen 9)  
**Resources.md Code:** Lines 1788-1858

**Layout File:** `res/layout/include_preview_order_bar.xml`

**Dependencies:**
- `sf_bg_btn_black.xml` (or CardView with black background)
- `sf_badge_red.xml`

**Implementation Steps:**
1. Create `include_preview_order_bar.xml` (black pill with badge)
2. This is a component, not a full screen
3. Will be included in item/category screens above bottom nav

---

### Screen 10: Preview Order (Compact)
**Screen Image:** `screens/screen10.png`  
**Plan.md Reference:** Line 20 (Screen 10)  
**Resources.md Code:** Lines 2474-2549

**Layout File:** `res/layout/fragment_preview_order.xml`

**Dependencies:**
- `include_sf_table_handle.xml` (already created)
- `item_preview_order_card.xml` (Lines 2553-2702)
- `include_place_order_bar.xml` (Lines 2706-2757)
- `include_sf_bottom_nav_new_selected.xml` (copy of base nav, New selected)

**Implementation Steps:**
1. Create `item_preview_order_card.xml` (preview card with alter/quantity controls)
2. Create `include_place_order_bar.xml` (black "Place order" pill with badge)
3. Create `include_sf_bottom_nav_new_selected.xml` (copy base nav)
4. Create `fragment_preview_order.xml` with:
   - Table handle at top
   - "+" button (top right)
   - "Preview order" title
   - RecyclerView with preview cards
   - Place order bar above bottom nav

---

### Screen 11: Preview Order (With Appbar + Stepper)
**Screen Image:** `screens/screen11.png`  
**Plan.md Reference:** Line 21 (Screen 11)  
**Resources.md Code:** Lines 2761-2889

**Layout File:** `res/layout/fragment_preview_order_header.xml`

**Dependencies:**
- `include_sf_appbar.xml` (already created)
- `include_sf_stepper_preview.xml` (Lines 2765-2783 — stepper with values)
- Same row items as Screen 10

**Implementation Steps:**
1. Create `include_sf_stepper_preview.xml` (stepper showing Type/Where/Item values)
2. Create `fragment_preview_order_header.xml` with:
   - Appbar
   - "Create order" title
   - Stepper with values ("Din in", "No 4", "4")
   - Up chevron + "Go back" chip
   - Same preview list and place order bar

---

### Screen 12: Given Order Tab
**Screen Image:** `screens/screen12.png`  
**Plan.md Reference:** Line 22 (Screen 12)  
**Resources.md Code:** Lines 2893-3145

**Layout File:** `res/layout/fragment_given_order.xml`

**Dependencies:**
- `include_sf_bottom_nav_given_selected.xml` (Lines 2468-2470 — Given selected)
- `item_given_order_card.xml` (Lines 2980-3044)
- `item_given_order_line.xml` (Lines 3046-3127)
- `item_given_order_line_shaded.xml` (Lines 3129-3145)
- `item_table_chip.xml` (needs to be created — for table chips row)

**Implementation Steps:**
1. Create `include_sf_bottom_nav_given_selected.xml` (right tab selected)
2. Create `item_table_chip.xml` (small chip for table list: "Table 05 #1254")
3. Create `item_given_order_card.xml` (big order card)
4. Create `item_given_order_line.xml` (white row with red/green stripe)
5. Create `item_given_order_line_shaded.xml` (grey background wrapper)
6. Create `fragment_given_order.xml` with:
   - Search bar
   - Horizontal RecyclerView for table chips
   - Vertical RecyclerView for order cards
   - "Add Items" green button

---

### Screen 13: Ready Order Tab
**Screen Image:** `screens/screen13.png`  
**Plan.md Reference:** Line 23 (Screen 13)  
**Resources.md Code:** Lines 3149-3236

**Layout File:** `res/layout/fragment_ready_order.xml`

**Dependencies:**
- `include_sf_appbar.xml` (already created)
- `include_sf_bottom_nav_ready_selected.xml` (Lines 2388-2466 — Ready selected)
- `item_ready_order_card.xml` (Lines 3189-3236)
- `item_given_order_line.xml` (reuse from Screen 12)

**Implementation Steps:**
1. Create `include_sf_bottom_nav_ready_selected.xml` (middle tab selected)
2. Create `item_ready_order_card.xml` (order card with green "Deliver" footer)
3. Create `fragment_ready_order.xml` with:
   - Appbar
   - RecyclerView with ready order cards
   - Each card has green "Deliver" button at bottom

**Chef Page Reference:** Similar to current `WaiterReadyFragment` but with new design

---

## Implementation checklist

### Phase 1: Resources
- [ ] Update `colors.xml` with all SF colors
- [ ] Update `dimens.xml` with all SF dimensions
- [ ] Create all drawable shape files (20+ files)
- [ ] Create all vector icon files (9 files)

### Phase 2: Screens (one at a time)
- [ ] Screen 1: Type Selection
- [ ] Screen 2: Table Selection
- [ ] Screen 3: Item Screen (Compact)
- [ ] Screen 4: Item Screen (With Header)
- [ ] Screen 5: Category Items Grid
- [ ] Screen 6: Modal Step 1 (Amount)
- [ ] Screen 7: Modal Step 2 (Chef Tip)
- [ ] Screen 8: Modal Step 3 (Quantity)
- [ ] Screen 9: Preview Order Bar (Component)
- [ ] Screen 10: Preview Order (Compact)
- [ ] Screen 11: Preview Order (With Header)
- [ ] Screen 12: Given Order Tab
- [ ] Screen 13: Ready Order Tab

---

## Notes

1. Reusable components: Create includes first (appbar, stepper, bottom nav variants, table handle) as they're used across screens.
2. Chef page reference: The appbar structure is similar to `chef_page.xml` topBar (lines 9-61).
3. Screen images: Always compare with `screens/screenX.png` to match spacing, colors, and layout.
4. Navigation: Update `waiter_nav_menu.xml` to have 3 tabs: New Order, Ready Order, Given Order.
5. Fragment classes: Create/update Fragment classes for each screen to handle logic and state.

---

## Next steps

1. Start with Phase 1: create all resources.
2. Then implement Screen 1, verify against `screen1.png`, and proceed screen by screen.

Should I start with Phase 1 (all resources), or do you want to review this plan first?