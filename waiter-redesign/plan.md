Yes — across the whole chat you’ve sent **13 UI screens/states**, and we now have a single coherent plan. Some of those “screens” are **states of the same XML** (ex: item screen with/without appbar, and the 3-step modal).

Below is the **full 13-screen map** (screen → XML name → what it depends on).

---

## ✅ Master Plan: All 13 Screens/States (mapped)

| #  | What you showed                                                                        | XML layout to use                   | Key includes / child layouts                                                                                                                                      | Notes (state / toggles)                                              | Bottom nav                      |
| -- | -------------------------------------------------------------------------------------- | ----------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------- | ------------------------------- |
| 1  | **Type** (Dine in / Parcel / Eat away)                                                 | `fragment_order_type.xml`           | `include_sf_appbar.xml`, `include_sf_stepper.xml`, `include_sf_bottom_nav.xml`                                                                                    | Stepper: Type active                                                 | New selected (base nav)         |
| 2  | **Where** (Table no. grid + Go back, 4 selected)                                       | `fragment_order_where.xml`          | `include_sf_appbar.xml`, `include_sf_stepper.xml`, `include_sf_bottom_nav.xml`                                                                                    | Uses Go-back chip                                                    | New selected (base nav)         |
| 3  | **Item screen (compact)** (Table 4 handle top + search + most bought + categories)     | `fragment_order_item.xml`           | `item_most_bought.xml`, `item_category.xml`, `include_sf_bottom_nav.xml`                                                                                          | Hide appbar + stepper, show handle                                   | New selected (base nav)         |
| 4  | **Item screen (header)** (Waiter Page + Create order + stepper + lists)                | `fragment_order_item.xml`           | `include_sf_appbar.xml`, `include_sf_stepper.xml`, `item_most_bought.xml`, `item_category.xml`, `include_sf_bottom_nav.xml`                                       | Show appbar + stepper; set stepper values `v1="Din in"`, `v2="No 4"` | New selected (base nav)         |
| 5  | **Category items grid** (Chinese + many Ramen cards)                                   | `fragment_order_category_items.xml` | `include_sf_table_handle.xml`, `item_menu_card.xml`, `include_sf_bottom_nav.xml`                                                                                  | RecyclerView Grid span=2                                             | New selected (base nav)         |
| 6  | **Modal step 1** (Spring roll → Amount)                                                | `sheet_item_customize.xml`          | —                                                                                                                                                                 | `amountBox=VISIBLE`, others GONE                                     | (Over current screen)           |
| 7  | **Modal step 2** (Spring roll → cheif tip)                                             | `sheet_item_customize.xml`          | —                                                                                                                                                                 | `stepChefTip=VISIBLE`, others GONE                                   | (Over current screen)           |
| 8  | **Modal step 3** (Spring roll → Qnty)                                                  | `sheet_item_customize.xml`          | —                                                                                                                                                                 | `stepQty=VISIBLE`, others GONE                                       | (Over current screen)           |
| 9  | **Preview order bar** (black pill “Preview order” + badge 4)                           | `include_preview_order_bar.xml`     | —                                                                                                                                                                 | This is a **component include**, not a full screen                   | N/A                             |
| 10 | **Preview order (compact)** (Table handle + “Preview order” + list + Place order pill) | `fragment_preview_order.xml`        | `include_sf_table_handle.xml`, `item_preview_order_card.xml`, `include_place_order_bar.xml`, `include_sf_bottom_nav_new_selected.xml`                             | Has top “+” button                                                   | New selected (new-selected nav) |
| 11 | **Preview order (with appbar + stepper)**                                              | `fragment_preview_order_header.xml` | `include_sf_appbar.xml`, `include_sf_stepper_preview.xml`, `item_preview_order_card.xml`, `include_place_order_bar.xml`, `include_sf_bottom_nav_new_selected.xml` | Stepper shows Type/Where/Item values (`Din in`, `No 4`, `4`)         | New selected (new-selected nav) |
| 12 | **Given Order tab** (search + table chips + big order card + “Add Items”)              | `fragment_given_order.xml`          | `item_given_order_card.xml`, `item_given_order_line.xml`, `item_given_order_line_shaded.xml`, `include_sf_bottom_nav_given_selected.xml`                          | Needs a small “table chip” row item (see note below)                 | Given selected                  |
| 13 | **Ready Order tab** (cards + big green Deliver footer)                                 | `fragment_ready_order.xml`          | `include_sf_appbar.xml`, `item_ready_order_card.xml`, `item_given_order_line.xml`, `include_sf_bottom_nav_ready_selected.xml`                                     | Deliver footer uses green pill                                       | Ready selected                  |

---

## 📌 One important note (so you’re not stuck)

For **Screen 12 (Given Order)** you have a horizontal “Table 05 #1254 / Table 06 #1256 …” chip list.
You’ll need **one small extra row layout** for that RecyclerView, e.g.

* `res/layout/item_table_chip.xml` (not generated earlier as a separate file)

Also: I gave full XML for `include_sf_bottom_nav_ready_selected.xml`, and told you how to create `include_sf_bottom_nav_given_selected.xml` (same structure). If you want, I can paste the *exact* `given_selected` XML too so you can copy-paste without editing.

---

## ✅ Full file inventory (single place)

### Main screen layouts

* `fragment_order_type.xml`
* `fragment_order_where.xml`
* `fragment_order_item.xml` *(used by screens #3 and #4 as two states)*
* `fragment_order_category_items.xml`
* `fragment_preview_order.xml`
* `fragment_preview_order_header.xml`
* `fragment_given_order.xml`
* `fragment_ready_order.xml`

### Includes / reusable UI blocks

* `include_sf_appbar.xml`
* `include_sf_stepper.xml`
* `include_sf_stepper_preview.xml`
* `include_sf_bottom_nav.xml` *(base New selected)*
* `include_sf_bottom_nav_new_selected.xml`
* `include_sf_bottom_nav_ready_selected.xml`
* `include_sf_bottom_nav_given_selected.xml`
* `include_sf_table_handle.xml`
* `include_preview_order_bar.xml`
* `include_place_order_bar.xml`

### RecyclerView row items / cards

* `item_most_bought.xml`
* `item_category.xml`
* `item_menu_card.xml`
* `item_preview_order_card.xml`
* `item_given_order_card.xml`
* `item_given_order_line.xml`
* `item_given_order_line_shaded.xml`
* `item_ready_order_card.xml`
* *(recommended to add)* `item_table_chip.xml`

### Modal / bottom sheet

* `sheet_item_customize.xml` *(used by #6/#7/#8 with visibility toggles)*

### Values (global theme)

* `res/values/colors.xml`
* `res/values/dimens.xml`

### Drawables/icons (from all packs)

Includes: step dots/line, chip bg, search bg, circles (dark/light/disabled), grabber bg/fg, pill black/green/outline, badges, radio selector, plus/search/close/chevrons/check.

---

If you want, I can now produce a **single “Navigation + Screen Flow” diagram** (New Order flow → preview → place, and separate Ready/Given tabs), and also paste the missing `item_table_chip.xml` and `include_sf_bottom_nav_given_selected.xml` so your project becomes fully copy-paste complete.


## ✅ Screen → Layout (XML) names

1. **Screen 1 (Type: Dine in / Parcel / Eat away)**
   **Layout:** `res/layout/fragment_order_type.xml`

2. **Screen 2 (Where: Table no. grid + Go back, table 4 selected)**
   **Layout:** `res/layout/fragment_order_where.xml`

3. **Screen 3 (Item page “Table 4” top + search + most bought + categories)**
   **Layout:** `res/layout/fragment_order_item.xml`
   *(Use “Table 4 handle” variant by hiding appbar/createorder/stepper and showing a small header — I’ll add that header include when you send the next screens, but the list UI is already here.)*

4. **Screen 4 (Create order header visible + stepper shows “Din in” + “No 4” + down chevron + search/lists)**
   **Layout:** same `res/layout/fragment_order_item.xml`
   *(Just keep appbar, create order, stepper visible, and set stepper `v2` visible + text “No 4”.)*

So: **you have 3 XML screens but covers 4 UI states** (screen 3 and 4 are two states of the same item screen).

---

## ✅ Common includes used by screens

### Shared header

* `res/layout/include_sf_appbar.xml`

### Shared stepper

* `res/layout/include_sf_stepper.xml`

### Shared bottom navigation

* `res/layout/include_sf_bottom_nav.xml`

### Item card + category chip (used only on item screens)

* `res/layout/item_most_bought.xml`
* `res/layout/item_category.xml`

---

## ✅ Resource files needed (and who uses them)

### Values

* `res/values/colors.xml` → **All screens**
* `res/values/dimens.xml` → **All screens**

### Drawables (shapes)

* `sf_bg_round_soft.xml` → Type screen buttons (via CardView bg color mostly), optional
* `sf_bg_round_soft2.xml` → Table/category/mostBought cards
* `sf_bg_round_primary.xml` → optional if you want shape bg for selected states
* `sf_bg_chip.xml` → Go back chip (Where + Item screen)
* `sf_bg_circle_dark.xml` → “+” button circle (Item screen)
* `sf_bg_circle_light.xml` → close button bg (Appbar)
* `sf_bg_search.xml` → search bar background (Item screen)
* `sf_step_dot_active.xml` / `sf_step_dot_inactive.xml` / `sf_step_line.xml` → Stepper (all screens that show stepper)
* `sf_dot_green.xml` / `sf_dot_red.xml` → small dots on “Most bought” cards (Item screen)

### Icons (vector)

* `ic_close_20.xml` → appbar (all screens)
* `ic_plus_20.xml` → bottom nav selected + most bought add button
* `ic_search_20.xml` → search bar
* `ic_chev_left_20.xml` → Go back chip (Where + Item screen)
* `ic_chev_right_20.xml` → Category cards chevron
* `ic_chev_down_20.xml` → the small arrow under stepper on item screen (screen 4)

---

## ✅ Screen-to-files mapping (quick checklist)

### Screen 1 → `fragment_order_type.xml`

Uses:

* `include_sf_appbar.xml`
* `include_sf_stepper.xml`
* `include_sf_bottom_nav.xml`
* colors/dimens
* icons: `ic_close_20`, `ic_plus_20` (bottom nav)

### Screen 2 → `fragment_order_where.xml`

Uses:

* `include_sf_appbar.xml`
* `include_sf_stepper.xml`
* `include_sf_bottom_nav.xml`
* `sf_bg_chip.xml`
* `ic_chev_left_20`
* colors/dimens

### Screen 3 → `fragment_order_item.xml` (Table 4 handle variant)

Uses:

* `item_most_bought.xml`
* `item_category.xml`
* `sf_bg_search.xml`, `sf_bg_circle_dark.xml`, `sf_dot_green.xml`, `sf_dot_red.xml`
* `ic_search_20`, `ic_plus_20`, `ic_chev_right_20`
* bottom nav include

### Screen 4 → `fragment_order_item.xml` (Header+stepper variant)

Uses **everything from Screen 3**, plus:

* `include_sf_appbar.xml`
* `include_sf_stepper.xml`
* `ic_chev_down_20` (rotated)

---
Yes — with the **new pack** I just gave, you now have code to recreate **all 5 screens/states** you sent in this message. Here’s the clean mapping.

---

## ✅ 5 Screens/States → Layout names

### 1) **Category items list (Chinese + many Ramen cards)**

* **Screen name:** Category Items Grid
* **Layout:** `res/layout/fragment_order_category_items.xml`
* **Row item layout:** `res/layout/item_menu_card.xml`

### 2) **Popup/Modal step 1 (Spring roll → Amount)**

* **Screen name:** Customize Item Sheet (Step 1)
* **Layout:** `res/layout/sheet_item_customize.xml`
* **State:** `amountBox = VISIBLE`, `stepChefTip = GONE`, `stepQty = GONE`

### 3) **Popup/Modal step 2 (Spring roll → cheif tip)**

* **Screen name:** Customize Item Sheet (Step 2)
* **Layout:** `res/layout/sheet_item_customize.xml`
* **State:** `amountBox = GONE`, `stepChefTip = VISIBLE`, `stepQty = GONE`

### 4) **Popup/Modal step 3 (Spring roll → Qnty)**

* **Screen name:** Customize Item Sheet (Step 3)
* **Layout:** `res/layout/sheet_item_customize.xml`
* **State:** `amountBox = GONE`, `stepChefTip = GONE`, `stepQty = VISIBLE`

### 5) **Preview Order bar (black pill “Preview order” + badge 4)**

* **Component name:** Preview Order Bar
* **Layout:** `res/layout/include_preview_order_bar.xml`
* **Usage:** include inside your item/category screens above bottom nav

---

## ✅ Which screen uses which files (full dependency map)

### Common value files (used by ALL 5)

* `res/values/colors.xml`
* `res/values/dimens.xml`

---

### Screen 5 — `fragment_order_category_items.xml`

Uses:

* `res/layout/include_sf_table_handle.xml`
* `res/layout/include_sf_bottom_nav.xml` (from previous pack)
* `res/layout/item_menu_card.xml` (RecyclerView rows)
  Drawables/icons used:
* `sf_grabber_bg.xml`, `sf_grabber_fg.xml`
* `sf_bg_search.xml`
* `sf_bg_circle_dark.xml` (or `sf_bg_circle_disabled.xml` when disabled)
* `sf_dot_green.xml`, `sf_dot_red.xml`
* `ic_search_20.xml`, `ic_plus_20.xml`, `ic_chev_down_20.xml`

---

### Screens 6/7/8 — `sheet_item_customize.xml` (same layout, different state)

Uses:
Drawables/icons used:

* `sf_bg_sheet.xml` (Card styling)
* `sf_bg_circle_dark.xml`
* `sf_bg_btn_black.xml`
* `sf_bg_chip.xml`
* `sf_step_line.xml`, `sf_step_dot_active.xml`
* `sf_radio_selector.xml` + `sf_radio_checked.xml` + `sf_radio_unchecked.xml`
* `ic_close_white_20.xml`
* `ic_arrow_left_20.xml`

(And Step 2 additionally uses Material Chips via `com.google.android.material.chip.Chip`.)

---

### Screen 9 — `include_preview_order_bar.xml`

Uses:

* `sf_bg_btn_black.xml` (black pill look, optional)
* `sf_badge_red.xml`
  No special icons needed.

---

## ✅ Quick “file list” you should have now (new ones from this message)

**Layouts**

* `fragment_order_category_items.xml`
* `item_menu_card.xml`
* `include_sf_table_handle.xml`
* `include_preview_order_bar.xml`
* `sheet_item_customize.xml`

**Drawables**

* `sf_bg_card.xml`
* `sf_bg_circle_disabled.xml`
* `sf_grabber_bg.xml`
* `sf_grabber_fg.xml`
* `sf_bg_btn_black.xml`
* `sf_bg_sheet.xml`
* `sf_badge_red.xml`
* `sf_radio_checked.xml`
* `sf_radio_unchecked.xml`
* `sf_radio_selector.xml`

**Icons**

* `ic_close_white_20.xml`
* `ic_arrow_left_20.xml`

**Values**

* updated `colors.xml` (primary/black/chip + added badge/scrim/card)
* dims unchanged (you can keep your previous `dimens.xml`)

---
Yes — for the **4 screens you just sent**, we have XML for **all 4**. Here’s the exact mapping (screen → layout → required files).

---

## ✅ 4 Screens → Layout names

### 1) **Preview order (Table 4 handle + “Place order” pill, no appbar)**

* **Layout:** `res/layout/fragment_preview_order.xml`

### 2) **Preview order (Waiter Page appbar + Create order stepper + cards + “Place order”)**

* **Layout:** `res/layout/fragment_preview_order_header.xml`

### 3) **Given Order tab (search + table chips + order card + “Add Items”)**

* **Layout:** `res/layout/fragment_given_order.xml`

### 4) **Ready Order tab (cards + big green “Deliver” footer, Ready selected in nav)**

* **Layout:** `res/layout/fragment_ready_order.xml`

---

## ✅ Screen → what XML files it needs

### Screen 1 → `fragment_preview_order.xml`

**Includes / layouts used**

* `include_sf_table_handle.xml`
* `include_place_order_bar.xml`
* `include_sf_bottom_nav_new_selected.xml`
* Row item: `item_preview_order_card.xml`

**Drawables/icons used**

* `sf_grabber_bg.xml`, `sf_grabber_fg.xml`
* `sf_bg_circle_dark.xml`
* `sf_bg_pill_outline.xml`
* `sf_badge_red.xml`
* `ic_plus_20.xml`, `ic_close_white_20.xml`, `ic_chev_down_20.xml`

---

### Screen 2 → `fragment_preview_order_header.xml`

**Includes / layouts used**

* `include_sf_appbar.xml`
* `include_sf_stepper_preview.xml` *(your stepper variant that shows Type/Where/Item values)*
* `include_place_order_bar.xml`
* `include_sf_bottom_nav_new_selected.xml`
* Row item: `item_preview_order_card.xml`

**Drawables/icons used**

* from appbar/stepper: `ic_close_20.xml`, `sf_step_line.xml`, `sf_step_dot_active.xml`, `sf_step_dot_inactive.xml`, `sf_bg_chip.xml`, `ic_chev_left_20.xml`, `ic_chev_down_20.xml`
* from preview card + place bar: `sf_bg_circle_dark.xml`, `sf_bg_pill_outline.xml`, `sf_badge_red.xml`, `ic_close_white_20.xml`

---

### Screen 3 → `fragment_given_order.xml`

**Includes / layouts used**

* Bottom nav: `include_sf_bottom_nav_given_selected.xml`
* Main card list item: `item_given_order_card.xml`
* Inside that card:

  * `item_given_order_line.xml`
  * `item_given_order_line_shaded.xml`

**Drawables/icons used**

* `sf_bg_search.xml`, `ic_search_20.xml`, `ic_chev_down_20.xml`
* `sf_bg_pill_green.xml` (Add Items)
* `sf_bg_pill_outline.xml` (Alter order)
* `sf_badge_qty.xml`
* uses `sf_dot_red.xml` / `sf_green` (vertical colored strip is `sf_red`/`sf_green` colors)

---

### Screen 4 → `fragment_ready_order.xml`

**Includes / layouts used**

* `include_sf_appbar.xml`
* Bottom nav: `include_sf_bottom_nav_ready_selected.xml`
* Card item: `item_ready_order_card.xml`
* Inside it: `item_given_order_line.xml`

**Drawables/icons used**

* `sf_bg_pill_green.xml` + `ic_check_white_20.xml` (Deliver button uses green; icon optional, your screenshot shows check)
* appbar uses `ic_close_20.xml`

---

## ✅ Common “base” files (needed by ALL 4)

* `res/values/colors.xml`
* `res/values/dimens.xml`
* If you use CardView shadows: `androidx.cardview:cardview`
* Material: `com.google.android.material:material`

---

If you want, I can also give you a **single navigation map** (Fragments + which bottom nav include to inflate for each tab) so your implementation stays clean.
