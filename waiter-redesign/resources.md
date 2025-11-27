
## 1) `res/values/colors.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Core -->
    <color name="sf_bg">#FFFFFF</color>
    <color name="sf_appbar_bg">#F7F8FB</color>

    <!-- Primary (sampled from screenshot blue) -->
    <color name="sf_primary">#4084F4</color>

    <!-- Greys -->
    <color name="sf_text_primary">#1F2937</color>
    <color name="sf_text_secondary">#6B7280</color>
    <color name="sf_text_muted">#9CA3AF</color>

    <!-- Surfaces -->
    <color name="sf_surface_soft">#E8E8EC</color>   <!-- big grey buttons/cards -->
    <color name="sf_surface_soft2">#EFF0F2</color>  <!-- smaller cards -->
    <color name="sf_chip_bg">#F1F2F4</color>
    <color name="sf_divider">#D1D5DB</color>

    <!-- Dots & icons -->
    <color name="sf_dot_active">#111827</color>
    <color name="sf_dot_inactive">#BFC4CC</color>
    <color name="sf_black">#111111</color>
    <color name="sf_white">#FFFFFF</color>

    <!-- tiny indicators -->
    <color name="sf_green">#22C55E</color>
    <color name="sf_red">#EF4444</color>
</resources>
```

---

## 2) `res/values/dimens.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <dimen name="sf_pad_screen_h">20dp</dimen>
    <dimen name="sf_pad_section_top">18dp</dimen>

    <dimen name="sf_radius_xl">22dp</dimen>
    <dimen name="sf_radius_l">18dp</dimen>
    <dimen name="sf_radius_m">14dp</dimen>

    <dimen name="sf_elev_soft">6dp</dimen>
    <dimen name="sf_elev_nav">8dp</dimen>

    <dimen name="sf_appbar_h">116dp</dimen>

    <dimen name="sf_bottom_nav_h">96dp</dimen>
    <dimen name="sf_selected_nav_h">64dp</dimen>

    <dimen name="sf_step_dot">12dp</dimen>
    <dimen name="sf_step_line_h">2dp</dimen>

    <dimen name="sf_btn_h_big">74dp</dimen>
    <dimen name="sf_btn_h_mid">66dp</dimen>

    <dimen name="sf_search_h">54dp</dimen>
</resources>
```

---

## 3) Drawables

### `res/drawable/sf_bg_round_soft.xml` (grey rounded card)

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/sf_surface_soft" />
    <corners android:radius="@dimen/sf_radius_xl" />
</shape>
```

### `res/drawable/sf_bg_round_soft2.xml` (slightly lighter)

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/sf_surface_soft2" />
    <corners android:radius="@dimen/sf_radius_xl" />
</shape>
```

### `res/drawable/sf_bg_round_primary.xml` (blue selected)

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/sf_primary" />
    <corners android:radius="@dimen/sf_radius_xl" />
</shape>
```

### `res/drawable/sf_bg_chip.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/sf_chip_bg" />
    <corners android:radius="28dp" />
</shape>
```

### `res/drawable/sf_bg_circle_dark.xml` (black circle for +)

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/sf_black" />
    <corners android:radius="999dp" />
</shape>
```

### `res/drawable/sf_bg_circle_light.xml` (close button background)

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/sf_chip_bg" />
    <corners android:radius="999dp" />
</shape>
```

### `res/drawable/sf_bg_search.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/sf_chip_bg" />
    <corners android:radius="28dp" />
</shape>
```

### `res/drawable/sf_step_dot_active.xml` / `sf_step_dot_inactive.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <size android:width="@dimen/sf_step_dot" android:height="@dimen/sf_step_dot"/>
    <solid android:color="@color/sf_dot_active"/>
    <corners android:radius="999dp"/>
</shape>
```

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <size android:width="@dimen/sf_step_dot" android:height="@dimen/sf_step_dot"/>
    <solid android:color="@color/sf_dot_inactive"/>
    <corners android:radius="999dp"/>
</shape>
```

### `res/drawable/sf_step_line.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <size android:height="@dimen/sf_step_line_h"/>
    <solid android:color="@color/sf_divider"/>
    <corners android:radius="999dp"/>
</shape>
```

---

## 4) Simple vector icons (you can also create via Android Studio → Vector Asset)

### `res/drawable/ic_close_20.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="20dp" android:height="20dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#111827"
        android:pathData="M18.3,5.71a1,1 0,0 0,-1.41 0L12,10.59 7.11,5.7A1,1 0,0 0,5.7 7.11L10.59,12 5.7,16.89a1,1 0,1 0,1.41 1.41L12,13.41l4.89,4.89a1,1 0,0 0,1.41 -1.41L13.41,12l4.89,-4.89a1,1 0,0 0,0 -1.4Z"/>
</vector>
```

### `res/drawable/ic_plus_20.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="20dp" android:height="20dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF"
        android:pathData="M11,5h2v14h-2zM5,11h14v2H5z"/>
</vector>
```

### `res/drawable/ic_search_20.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="20dp" android:height="20dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#6B7280"
        android:pathData="M10,2a8,8 0,1 1,0 16a8,8 0,0 1,0 -16zm11,19l-5.2,-5.2a9.5,9.5 0,1 0,-1.4 1.4L19.6,22z"/>
</vector>
```

### `res/drawable/ic_chev_left_20.xml` / `ic_chev_right_20.xml` / `ic_chev_down_20.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="20dp" android:height="20dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#6B7280"
        android:pathData="M15.4,7.4 14,6 8,12l6,6 1.4,-1.4L10.8,12z"/>
</vector>
```

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="20dp" android:height="20dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#9CA3AF"
        android:pathData="M8.6,16.6 10,18l6,-6 -6,-6 -1.4,1.4L13.2,12z"/>
</vector>
```

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="20dp" android:height="20dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#9CA3AF"
        android:pathData="M7.4,8.6 6,10l6,6 6,-6 -1.4,-1.4L12,13.2z"/>
</vector>
```

---

# Screen 1: Type (Dine in / Parcel / Eat away)

### `res/layout/fragment_order_type.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/sf_bg">

    <include
        android:id="@+id/appbar"
        layout="@layout/include_sf_appbar"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <TextView
        android:id="@+id/tvCreateOrder"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="@dimen/sf_pad_screen_h"
        android:layout_marginTop="14dp"
        android:text="Create order"
        android:textColor="@color/sf_text_primary"
        android:textSize="26sp"
        android:textStyle="700"
        app:layout_constraintTop_toBottomOf="@id/appbar"
        app:layout_constraintStart_toStartOf="parent" />

    <include
        android:id="@+id/stepper"
        layout="@layout/include_sf_stepper"
        android:layout_marginStart="@dimen/sf_pad_screen_h"
        android:layout_marginEnd="@dimen/sf_pad_screen_h"
        android:layout_marginTop="10dp"
        app:layout_constraintTop_toBottomOf="@id/tvCreateOrder"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Big button -->
    <androidx.cardview.widget.CardView
        android:id="@+id/btnDineIn"
        android:layout_width="0dp"
        android:layout_height="@dimen/sf_btn_h_big"
        android:layout_marginStart="@dimen/sf_pad_screen_h"
        android:layout_marginEnd="@dimen/sf_pad_screen_h"
        android:layout_marginTop="18dp"
        app:cardCornerRadius="@dimen/sf_radius_xl"
        app:cardUseCompatPadding="true"
        app:cardElevation="@dimen/sf_elev_soft"
        app:cardBackgroundColor="@color/sf_surface_soft"
        app:layout_constraintTop_toBottomOf="@id/stepper"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:gravity="center"
            android:text="Dine in"
            android:textColor="@color/sf_text_primary"
            android:textSize="20sp"
            android:textStyle="700" />
    </androidx.cardview.widget.CardView>

    <!-- Two buttons row -->
    <androidx.cardview.widget.CardView
        android:id="@+id/btnParcel"
        android:layout_width="0dp"
        android:layout_height="@dimen/sf_btn_h_mid"
        android:layout_marginStart="@dimen/sf_pad_screen_h"
        android:layout_marginTop="14dp"
        android:layout_marginEnd="10dp"
        app:cardCornerRadius="@dimen/sf_radius_xl"
        app:cardUseCompatPadding="true"
        app:cardElevation="@dimen/sf_elev_soft"
        app:cardBackgroundColor="@color/sf_surface_soft"
        app:layout_constraintTop_toBottomOf="@id/btnDineIn"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toStartOf="@id/btnEatAway">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:gravity="center"
            android:text="Parcel"
            android:textColor="@color/sf_text_primary"
            android:textSize="18sp"
            android:textStyle="700" />
    </androidx.cardview.widget.CardView>

    <androidx.cardview.widget.CardView
        android:id="@+id/btnEatAway"
        android:layout_width="0dp"
        android:layout_height="@dimen/sf_btn_h_mid"
        android:layout_marginEnd="@dimen/sf_pad_screen_h"
        android:layout_marginTop="14dp"
        android:layout_marginStart="10dp"
        app:cardCornerRadius="@dimen/sf_radius_xl"
        app:cardUseCompatPadding="true"
        app:cardElevation="@dimen/sf_elev_soft"
        app:cardBackgroundColor="@color/sf_surface_soft"
        app:layout_constraintTop_toBottomOf="@id/btnDineIn"
        app:layout_constraintStart_toEndOf="@id/btnParcel"
        app:layout_constraintEnd_toEndOf="parent">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:gravity="center"
            android:text="Eat away"
            android:textColor="@color/sf_text_primary"
            android:textSize="18sp"
            android:textStyle="700" />
    </androidx.cardview.widget.CardView>

    <include
        android:id="@+id/bottomNav"
        layout="@layout/include_sf_bottom_nav"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

# Screen 2: Where (Table no. grid + Go back)

### `res/layout/fragment_order_where.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/sf_bg">

    <include
        android:id="@+id/appbar"
        layout="@layout/include_sf_appbar"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <TextView
        android:id="@+id/tvCreateOrder"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="@dimen/sf_pad_screen_h"
        android:layout_marginTop="14dp"
        android:text="Create order"
        android:textColor="@color/sf_text_primary"
        android:textSize="26sp"
        android:textStyle="700"
        app:layout_constraintTop_toBottomOf="@id/appbar"
        app:layout_constraintStart_toStartOf="parent" />

    <include
        android:id="@+id/stepper"
        layout="@layout/include_sf_stepper"
        android:layout_marginStart="@dimen/sf_pad_screen_h"
        android:layout_marginEnd="@dimen/sf_pad_screen_h"
        android:layout_marginTop="10dp"
        app:layout_constraintTop_toBottomOf="@id/tvCreateOrder"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- "Table no." + go back chip -->
    <TextView
        android:id="@+id/tvTableNo"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="@dimen/sf_pad_screen_h"
        android:layout_marginTop="18dp"
        android:text="Table no."
        android:textColor="@color/sf_text_primary"
        android:textSize="22sp"
        android:textStyle="500"
        app:layout_constraintTop_toBottomOf="@id/stepper"
        app:layout_constraintStart_toStartOf="parent" />

    <LinearLayout
        android:id="@+id/btnGoBack"
        android:layout_width="124dp"
        android:layout_height="44dp"
        android:layout_marginEnd="@dimen/sf_pad_screen_h"
        android:background="@drawable/sf_bg_chip"
        android:gravity="center"
        android:orientation="horizontal"
        android:paddingStart="14dp"
        android:paddingEnd="14dp"
        android:elevation="2dp"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="@id/tvTableNo">

        <ImageView
            android:layout_width="18dp"
            android:layout_height="18dp"
            android:src="@drawable/ic_chev_left_20" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp"
            android:text="Go back"
            android:textColor="@color/sf_text_secondary"
            android:textSize="14sp"
            android:textStyle="600" />
    </LinearLayout>

    <!-- Table grid (static like screenshot) -->
    <androidx.constraintlayout.helper.widget.Flow
        android:id="@+id/tableFlow"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="@dimen/sf_pad_screen_h"
        android:layout_marginEnd="@dimen/sf_pad_screen_h"
        android:layout_marginTop="18dp"
        app:flow_wrapMode="chain"
        app:flow_horizontalStyle="packed"
        app:flow_verticalGap="16dp"
        app:flow_horizontalGap="16dp"
        app:flow_maxElementsWrap="3"
        app:constraint_referenced_ids="t1,t2,t3,t4,t5,t6,t7"
        app:layout_constraintTop_toBottomOf="@id/tvTableNo"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Unselected -->
    <androidx.cardview.widget.CardView
        android:id="@+id/t1"
        android:layout_width="0dp"
        android:layout_height="78dp"
        app:cardCornerRadius="@dimen/sf_radius_xl"
        app:cardUseCompatPadding="true"
        app:cardElevation="@dimen/sf_elev_soft"
        app:cardBackgroundColor="@color/sf_surface_soft2">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:gravity="center"
            android:text="1"
            android:textColor="@color/sf_text_primary"
            android:textSize="24sp"
            android:textStyle="500"/>
    </androidx.cardview.widget.CardView>

    <androidx.cardview.widget.CardView
        android:id="@+id/t2"
        android:layout_width="0dp"
        android:layout_height="78dp"
        app:cardCornerRadius="@dimen/sf_radius_xl"
        app:cardUseCompatPadding="true"
        app:cardElevation="@dimen/sf_elev_soft"
        app:cardBackgroundColor="@color/sf_surface_soft2">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:gravity="center"
            android:text="2"
            android:textColor="@color/sf_text_primary"
            android:textSize="24sp"/>
    </androidx.cardview.widget.CardView>

    <androidx.cardview.widget.CardView
        android:id="@+id/t3"
        android:layout_width="0dp"
        android:layout_height="78dp"
        app:cardCornerRadius="@dimen/sf_radius_xl"
        app:cardUseCompatPadding="true"
        app:cardElevation="@dimen/sf_elev_soft"
        app:cardBackgroundColor="@color/sf_surface_soft2">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:gravity="center"
            android:text="3"
            android:textColor="@color/sf_text_primary"
            android:textSize="24sp"/>
    </androidx.cardview.widget.CardView>

    <!-- Selected: 4 -->
    <androidx.cardview.widget.CardView
        android:id="@+id/t4"
        android:layout_width="0dp"
        android:layout_height="78dp"
        app:cardCornerRadius="@dimen/sf_radius_xl"
        app:cardUseCompatPadding="true"
        app:cardElevation="@dimen/sf_elev_soft"
        app:cardBackgroundColor="@color/sf_primary">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:gravity="center"
            android:text="4"
            android:textColor="@color/sf_white"
            android:textSize="24sp"
            android:textStyle="600"/>
    </androidx.cardview.widget.CardView>

    <androidx.cardview.widget.CardView
        android:id="@+id/t5"
        android:layout_width="0dp"
        android:layout_height="78dp"
        app:cardCornerRadius="@dimen/sf_radius_xl"
        app:cardUseCompatPadding="true"
        app:cardElevation="@dimen/sf_elev_soft"
        app:cardBackgroundColor="@color/sf_surface_soft2">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:gravity="center"
            android:text="5"
            android:textColor="@color/sf_text_primary"
            android:textSize="24sp"/>
    </androidx.cardview.widget.CardView>

    <androidx.cardview.widget.CardView
        android:id="@+id/t6"
        android:layout_width="0dp"
        android:layout_height="78dp"
        app:cardCornerRadius="@dimen/sf_radius_xl"
        app:cardUseCompatPadding="true"
        app:cardElevation="@dimen/sf_elev_soft"
        app:cardBackgroundColor="@color/sf_surface_soft2">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:gravity="center"
            android:text="6"
            android:textColor="@color/sf_text_primary"
            android:textSize="24sp"/>
    </androidx.cardview.widget.CardView>

    <androidx.cardview.widget.CardView
        android:id="@+id/t7"
        android:layout_width="0dp"
        android:layout_height="78dp"
        app:cardCornerRadius="@dimen/sf_radius_xl"
        app:cardUseCompatPadding="true"
        app:cardElevation="@dimen/sf_elev_soft"
        app:cardBackgroundColor="@color/sf_surface_soft2">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:gravity="center"
            android:text="7"
            android:textColor="@color/sf_text_primary"
            android:textSize="24sp"/>
    </androidx.cardview.widget.CardView>

    <include
        android:id="@+id/bottomNav"
        layout="@layout/include_sf_bottom_nav"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```
---



---


---

# Screen 3/4: Item list (search + most bought + categories)

### `res/layout/fragment_order_item.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/sf_bg">

    <!-- If you want the full header version, keep appbar + create order + stepper.
         If you want the "Table 4 handle" version, hide these and show handle block. -->
    <include
        android:id="@+id/appbar"
        layout="@layout/include_sf_appbar"
        android:visibility="visible"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <TextView
        android:id="@+id/tvCreateOrder"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="@dimen/sf_pad_screen_h"
        android:layout_marginTop="14dp"
        android:text="Create order"
        android:textColor="@color/sf_text_primary"
        android:textSize="26sp"
        android:textStyle="700"
        app:layout_constraintTop_toBottomOf="@id/appbar"
        app:layout_constraintStart_toStartOf="parent" />

    <include
        android:id="@+id/stepper"
        layout="@layout/include_sf_stepper"
        android:layout_marginStart="@dimen/sf_pad_screen_h"
        android:layout_marginEnd="@dimen/sf_pad_screen_h"
        android:layout_marginTop="10dp"
        app:layout_constraintTop_toBottomOf="@id/tvCreateOrder"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Small up chevron + go back like screenshot 4 -->
    <ImageView
        android:id="@+id/ivUp"
        android:layout_width="20dp"
        android:layout_height="20dp"
        android:layout_marginTop="6dp"
        android:src="@drawable/ic_chev_down_20"
        android:rotation="180"
        app:layout_constraintTop_toBottomOf="@id/stepper"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <LinearLayout
        android:id="@+id/btnGoBack"
        android:layout_width="124dp"
        android:layout_height="44dp"
        android:layout_marginEnd="@dimen/sf_pad_screen_h"
        android:background="@drawable/sf_bg_chip"
        android:gravity="center"
        android:orientation="horizontal"
        android:paddingStart="14dp"
        android:paddingEnd="14dp"
        android:elevation="2dp"
        app:layout_constraintTop_toBottomOf="@id/stepper"
        app:layout_constraintEnd_toEndOf="parent">

        <ImageView
            android:layout_width="18dp"
            android:layout_height="18dp"
            android:src="@drawable/ic_chev_left_20" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp"
            android:text="Go back"
            android:textColor="@color/sf_text_secondary"
            android:textSize="14sp"
            android:textStyle="600" />
    </LinearLayout>

    <androidx.core.widget.NestedScrollView
        android:id="@+id/scroll"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:fillViewport="true"
        android:overScrollMode="never"
        app:layout_constraintTop_toBottomOf="@id/ivUp"
        app:layout_constraintBottom_toTopOf="@id/bottomNav"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:paddingStart="@dimen/sf_pad_screen_h"
            android:paddingEnd="@dimen/sf_pad_screen_h"
            android:paddingTop="12dp"
            android:paddingBottom="18dp">

            <!-- Search -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="@dimen/sf_search_h"
                android:background="@drawable/sf_bg_search"
                android:gravity="center_vertical"
                android:orientation="horizontal"
                android:paddingStart="16dp"
                android:paddingEnd="16dp">

                <ImageView
                    android:layout_width="20dp"
                    android:layout_height="20dp"
                    android:src="@drawable/ic_search_20" />

                <EditText
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="12dp"
                    android:layout_weight="1"
                    android:background="@android:color/transparent"
                    android:hint="panner tikka"
                    android:inputType="text"
                    android:textColor="@color/sf_text_primary"
                    android:textColorHint="@color/sf_text_secondary"
                    android:textSize="16sp" />
            </LinearLayout>

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="18dp"
                android:text="Most bought"
                android:textColor="@color/sf_text_primary"
                android:textSize="20sp"
                android:textStyle="700" />

            <!-- 2x2 Most bought cards -->
            <androidx.constraintlayout.widget.ConstraintLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp">

                <include layout="@layout/item_most_bought"
                    android:id="@+id/mb1" />

                <include layout="@layout/item_most_bought"
                    android:id="@+id/mb2" />

                <include layout="@layout/item_most_bought"
                    android:id="@+id/mb3" />

                <include layout="@layout/item_most_bought"
                    android:id="@+id/mb4" />

                <androidx.constraintlayout.widget.Guideline
                    android:id="@+id/gMid"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    app:layout_constraintGuide_percent="0.5" />

                <!-- position -->
                <androidx.constraintlayout.widget.ConstraintSet>
                    <!-- (kept simple by constraints below - no need for custom set) -->
                </androidx.constraintlayout.widget.ConstraintSet>

            </androidx.constraintlayout.widget.ConstraintLayout>

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="18dp"
                android:text="Categories"
                android:textColor="@color/sf_text_primary"
                android:textSize="20sp"
                android:textStyle="700" />

            <!-- 3 columns categories using Flow -->
            <androidx.constraintlayout.widget.ConstraintLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp">

                <androidx.constraintlayout.helper.widget.Flow
                    android:id="@+id/catFlow"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    app:flow_wrapMode="chain"
                    app:flow_horizontalStyle="packed"
                    app:flow_verticalGap="14dp"
                    app:flow_horizontalGap="14dp"
                    app:flow_maxElementsWrap="3"
                    app:constraint_referenced_ids="c1,c2,c3,c4,c5,c6,c7,c8,c9"
                    app:layout_constraintTop_toTopOf="parent"
                    app:layout_constraintStart_toStartOf="parent"
                    app:layout_constraintEnd_toEndOf="parent" />

                <include android:id="@+id/c1" layout="@layout/item_category" />
                <include android:id="@+id/c2" layout="@layout/item_category" />
                <include android:id="@+id/c3" layout="@layout/item_category" />
                <include android:id="@+id/c4" layout="@layout/item_category" />
                <include android:id="@+id/c5" layout="@layout/item_category" />
                <include android:id="@+id/c6" layout="@layout/item_category" />
                <include android:id="@+id/c7" layout="@layout/item_category" />
                <include android:id="@+id/c8" layout="@layout/item_category" />
                <include android:id="@+id/c9" layout="@layout/item_category" />

            </androidx.constraintlayout.widget.ConstraintLayout>

        </LinearLayout>
    </androidx.core.widget.NestedScrollView>

    <include
        android:id="@+id/bottomNav"
        layout="@layout/include_sf_bottom_nav"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

### `res/layout/item_most_bought.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="0dp"
    android:layout_height="74dp"
    android:layout_marginBottom="12dp"
    app:cardCornerRadius="@dimen/sf_radius_xl"
    app:cardUseCompatPadding="true"
    app:cardElevation="4dp"
    app:cardBackgroundColor="@color/sf_surface_soft2">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:paddingStart="14dp"
        android:paddingEnd="12dp">

        <!-- small red/green dots -->
        <View
            android:id="@+id/dotGreen"
            android:layout_width="6dp"
            android:layout_height="6dp"
            android:background="@drawable/sf_dot_green"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            android:layout_marginTop="10dp"/>

        <View
            android:id="@+id/dotRed"
            android:layout_width="6dp"
            android:layout_height="6dp"
            android:layout_marginStart="6dp"
            android:background="@drawable/sf_dot_red"
            app:layout_constraintTop_toTopOf="@id/dotGreen"
            app:layout_constraintStart_toEndOf="@id/dotGreen"/>

        <TextView
            android:id="@+id/tvName"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:text="Ramen"
            android:textColor="@color/sf_text_primary"
            android:textSize="18sp"
            android:textStyle="800"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toStartOf="@id/btnAdd"
            android:layout_marginTop="20dp"/>

        <TextView
            android:id="@+id/tvQty"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:text="qnty: Small,mid,large"
            android:textColor="@color/sf_text_secondary"
            android:textSize="13sp"
            app:layout_constraintTop_toBottomOf="@id/tvName"
            app:layout_constraintStart_toStartOf="@id/tvName"
            app:layout_constraintEnd_toStartOf="@id/btnAdd"
            android:layout_marginTop="2dp"/>

        <FrameLayout
            android:id="@+id/btnAdd"
            android:layout_width="34dp"
            android:layout_height="34dp"
            android:background="@drawable/sf_bg_circle_dark"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintBottom_toBottomOf="parent">

            <ImageView
                android:layout_width="18dp"
                android:layout_height="18dp"
                android:layout_gravity="center"
                android:src="@drawable/ic_plus_20" />
        </FrameLayout>

    </androidx.constraintlayout.widget.ConstraintLayout>
</androidx.cardview.widget.CardView>
```

### `res/drawable/sf_dot_green.xml` / `sf_dot_red.xml`

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <size android:width="6dp" android:height="6dp"/>
    <solid android:color="@color/sf_green"/>
    <corners android:radius="999dp"/>
</shape>
```

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <size android:width="6dp" android:height="6dp"/>
    <solid android:color="@color/sf_red"/>
    <corners android:radius="999dp"/>
</shape>
```

### `res/layout/item_category.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="0dp"
    android:layout_height="66dp"
    app:cardCornerRadius="@dimen/sf_radius_l"
    app:cardUseCompatPadding="true"
    app:cardElevation="4dp"
    app:cardBackgroundColor="@color/sf_surface_soft2">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:paddingStart="14dp"
        android:paddingEnd="12dp">

        <TextView
            android:id="@+id/tvCat"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:text="Chinese"
            android:textColor="@color/sf_text_primary"
            android:textSize="16sp"
            android:textStyle="800"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintEnd_toStartOf="@id/ivArrow"
            android:layout_marginTop="12dp"/>

        <TextView
            android:id="@+id/tvItems"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:text="Items 10"
            android:textColor="@color/sf_text_secondary"
            android:textSize="13sp"
            app:layout_constraintStart_toStartOf="@id/tvCat"
            app:layout_constraintTop_toBottomOf="@id/tvCat"
            app:layout_constraintEnd_toStartOf="@id/ivArrow"
            android:layout_marginTop="2dp"/>

        <ImageView
            android:id="@+id/ivArrow"
            android:layout_width="18dp"
            android:layout_height="18dp"
            android:src="@drawable/ic_chev_right_20"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintBottom_toBottomOf="parent"/>

    </androidx.constraintlayout.widget.ConstraintLayout>
</androidx.cardview.widget.CardView>
```

## 5) Reusable Includes

### `res/layout/include_sf_appbar.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="@dimen/sf_appbar_h"
    android:background="@color/sf_appbar_bg"
    android:paddingStart="@dimen/sf_pad_screen_h"
    android:paddingEnd="@dimen/sf_pad_screen_h"
    android:paddingTop="18dp">

    <FrameLayout
        android:id="@+id/btnClose"
        android:layout_width="34dp"
        android:layout_height="34dp"
        android:background="@drawable/sf_bg_circle_light"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent">

        <ImageView
            android:layout_width="20dp"
            android:layout_height="20dp"
            android:layout_gravity="center"
            android:src="@drawable/ic_close_20" />
    </FrameLayout>

    <TextView
        android:id="@+id/tvBrand"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="10dp"
        android:text="Street Feast"
        android:textColor="@color/sf_text_primary"
        android:textSize="18sp"
        android:textStyle="600"
        app:layout_constraintStart_toEndOf="@id/btnClose"
        app:layout_constraintTop_toTopOf="@id/btnClose"
        app:layout_constraintBottom_toBottomOf="@id/btnClose" />

    <TextView
        android:id="@+id/tvDate"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Mon, Nov 28"
        android:textColor="@color/sf_text_muted"
        android:textSize="14sp"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="@id/btnClose"
        app:layout_constraintBottom_toBottomOf="@id/btnClose" />

    <TextView
        android:id="@+id/tvTitle"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="10dp"
        android:text="Waiter Page"
        android:textColor="@color/sf_text_primary"
        android:textSize="34sp"
        android:textStyle="700"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/btnClose" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

### `res/layout/include_sf_bottom_nav.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/bottomNav"
    android:layout_width="match_parent"
    android:layout_height="@dimen/sf_bottom_nav_h"
    app:cardElevation="@dimen/sf_elev_nav"
    app:cardUseCompatPadding="true"
    app:cardCornerRadius="22dp"
    app:cardBackgroundColor="@color/sf_bg">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:paddingStart="@dimen/sf_pad_screen_h"
        android:paddingEnd="@dimen/sf_pad_screen_h"
        android:paddingTop="14dp"
        android:paddingBottom="14dp">

        <!-- Selected: New Order -->
        <androidx.cardview.widget.CardView
            android:id="@+id/navNewOrder"
            android:layout_width="0dp"
            android:layout_height="@dimen/sf_selected_nav_h"
            app:cardBackgroundColor="@color/sf_primary"
            app:cardCornerRadius="18dp"
            app:cardElevation="0dp"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toStartOf="@id/navReadyOrder"
            app:layout_constraintHorizontalWeight="1.2">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:gravity="center"
                android:orientation="vertical">

                <FrameLayout
                    android:layout_width="28dp"
                    android:layout_height="28dp"
                    android:background="@android:color/transparent"
                    android:foreground="?attr/selectableItemBackgroundBorderless">

                    <androidx.cardview.widget.CardView
                        android:layout_width="26dp"
                        android:layout_height="26dp"
                        app:cardCornerRadius="13dp"
                        app:cardBackgroundColor="@android:color/transparent"
                        app:cardElevation="0dp"
                        app:cardUseCompatPadding="false"
                        android:foreground="?attr/selectableItemBackgroundBorderless">

                        <FrameLayout
                            android:layout_width="match_parent"
                            android:layout_height="match_parent"
                            android:background="@android:color/transparent">

                            <ImageView
                                android:layout_width="18dp"
                                android:layout_height="18dp"
                                android:layout_gravity="center"
                                android:src="@drawable/ic_plus_20" />
                        </FrameLayout>
                    </androidx.cardview.widget.CardView>
                </FrameLayout>

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="6dp"
                    android:text="New Order"
                    android:textColor="@color/sf_white"
                    android:textSize="13sp"
                    android:textStyle="600" />
            </LinearLayout>
        </androidx.cardview.widget.CardView>

        <!-- Ready Order -->
        <LinearLayout
            android:id="@+id/navReadyOrder"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:gravity="center"
            android:orientation="vertical"
            app:layout_constraintStart_toEndOf="@id/navNewOrder"
            app:layout_constraintEnd_toStartOf="@id/navGivenOrder"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintHorizontalWeight="1">

            <!-- replace with your icon -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="☝"
                android:textSize="20sp"
                android:textColor="@color/sf_text_muted" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="6dp"
                android:text="Ready Order"
                android:textColor="@color/sf_text_muted"
                android:textSize="13sp"
                android:textStyle="600"/>
        </LinearLayout>

        <!-- Given Order -->
        <LinearLayout
            android:id="@+id/navGivenOrder"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:gravity="center"
            android:orientation="vertical"
            app:layout_constraintStart_toEndOf="@id/navReadyOrder"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintHorizontalWeight="1">

            <!-- replace with your icon -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="✓"
                android:textSize="20sp"
                android:textColor="@color/sf_text_muted" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="6dp"
                android:text="Given Order"
                android:textColor="@color/sf_text_muted"
                android:textSize="13sp"
                android:textStyle="600"/>
        </LinearLayout>

    </androidx.constraintlayout.widget.ConstraintLayout>
</androidx.cardview.widget.CardView>
```

### `res/layout/include_sf_stepper.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="86dp">

    <!-- Step titles -->
    <TextView
        android:id="@+id/s1"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:gravity="center"
        android:text="Type"
        android:textColor="@color/sf_text_secondary"
        android:textSize="12sp"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toStartOf="@id/s2"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintHorizontalWeight="1" />

    <TextView
        android:id="@+id/s2"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:gravity="center"
        android:text="Where"
        android:textColor="@color/sf_text_secondary"
        android:textSize="12sp"
        app:layout_constraintStart_toEndOf="@id/s1"
        app:layout_constraintEnd_toStartOf="@id/s3"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintHorizontalWeight="1" />

    <TextView
        android:id="@+id/s3"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:gravity="center"
        android:text="Item"
        android:textColor="@color/sf_text_secondary"
        android:textSize="12sp"
        app:layout_constraintStart_toEndOf="@id/s2"
        app:layout_constraintEnd_toStartOf="@id/s4"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintHorizontalWeight="1" />

    <TextView
        android:id="@+id/s4"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:gravity="center"
        android:text="Place"
        android:textColor="@color/sf_text_secondary"
        android:textSize="12sp"
        app:layout_constraintStart_toEndOf="@id/s3"
        app:layout_constraintEnd_toStartOf="@id/s5"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintHorizontalWeight="1" />

    <TextView
        android:id="@+id/s5"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:gravity="center"
        android:text="Complete"
        android:textColor="@color/sf_text_secondary"
        android:textSize="12sp"
        app:layout_constraintStart_toEndOf="@id/s4"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintHorizontalWeight="1" />

    <!-- Line -->
    <View
        android:id="@+id/stepLine"
        android:layout_width="0dp"
        android:layout_height="@dimen/sf_step_line_h"
        android:layout_marginTop="18dp"
        android:background="@drawable/sf_step_line"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toBottomOf="@id/s1" />

    <!-- Dots -->
    <View
        android:id="@+id/d1"
        android:layout_width="@dimen/sf_step_dot"
        android:layout_height="@dimen/sf_step_dot"
        android:background="@drawable/sf_step_dot_active"
        app:layout_constraintTop_toTopOf="@id/stepLine"
        app:layout_constraintBottom_toBottomOf="@id/stepLine"
        app:layout_constraintStart_toStartOf="@id/s1"
        app:layout_constraintEnd_toEndOf="@id/s1" />

    <View
        android:id="@+id/d2"
        android:layout_width="@dimen/sf_step_dot"
        android:layout_height="@dimen/sf_step_dot"
        android:background="@drawable/sf_step_dot_active"
        app:layout_constraintTop_toTopOf="@id/stepLine"
        app:layout_constraintBottom_toBottomOf="@id/stepLine"
        app:layout_constraintStart_toStartOf="@id/s2"
        app:layout_constraintEnd_toEndOf="@id/s2" />

    <View
        android:id="@+id/d3"
        android:layout_width="@dimen/sf_step_dot"
        android:layout_height="@dimen/sf_step_dot"
        android:background="@drawable/sf_step_dot_inactive"
        app:layout_constraintTop_toTopOf="@id/stepLine"
        app:layout_constraintBottom_toBottomOf="@id/stepLine"
        app:layout_constraintStart_toStartOf="@id/s3"
        app:layout_constraintEnd_toEndOf="@id/s3" />

    <View
        android:id="@+id/d4"
        android:layout_width="@dimen/sf_step_dot"
        android:layout_height="@dimen/sf_step_dot"
        android:background="@drawable/sf_step_dot_inactive"
        app:layout_constraintTop_toTopOf="@id/stepLine"
        app:layout_constraintBottom_toBottomOf="@id/stepLine"
        app:layout_constraintStart_toStartOf="@id/s4"
        app:layout_constraintEnd_toEndOf="@id/s4" />

    <View
        android:id="@+id/d5"
        android:layout_width="@dimen/sf_step_dot"
        android:layout_height="@dimen/sf_step_dot"
        android:background="@drawable/sf_step_dot_inactive"
        app:layout_constraintTop_toTopOf="@id/stepLine"
        app:layout_constraintBottom_toBottomOf="@id/stepLine"
        app:layout_constraintStart_toStartOf="@id/s5"
        app:layout_constraintEnd_toEndOf="@id/s5" />

    <!-- Selected values (shown under first steps in your screenshots; toggle visibility as needed) -->
    <TextView
        android:id="@+id/v1"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:text="Din in"
        android:textColor="@color/sf_text_primary"
        android:textSize="12sp"
        app:layout_constraintTop_toBottomOf="@id/stepLine"
        app:layout_constraintStart_toStartOf="@id/s1"
        app:layout_constraintEnd_toEndOf="@id/s1" />

    <TextView
        android:id="@+id/v2"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:text="No 4"
        android:textColor="@color/sf_text_primary"
        android:textSize="12sp"
        android:visibility="gone"
        app:layout_constraintTop_toBottomOf="@id/stepLine"
        app:layout_constraintStart_toStartOf="@id/s2"
        app:layout_constraintEnd_toEndOf="@id/s2" />

</androidx.constraintlayout.widget.ConstraintLayout>
```


Yep — these **new 5 screens/states** are:

1. **Category items grid** (“Chinese” + lots of Ramen cards)
2. **Modal step 1** (Spring roll → Amount)
3. **Modal step 2** (Spring roll → cheif tip)
4. **Modal step 3** (Spring roll → Qnty)
5. **Preview order bar** (black pill with badge “4”)

Below is the **Android XML** you need to add on top of the previous set.

---

## A) Update / Add colors (patch `res/values/colors.xml`)

Use these sampled-from-screenshot exacts (important):

```xml
<!-- update these -->
<color name="sf_primary">#4186F6</color>      <!-- sampled -->
<color name="sf_black">#202124</color>        <!-- sampled -->
<color name="sf_chip_bg">#F1F3F4</color>      <!-- sampled (search, chips, inner panels) -->

<!-- add these -->
<color name="sf_card_bg">#FBFBFB</color>      <!-- item card surface -->
<color name="sf_disabled_circle">#9A9A9A</color>
<color name="sf_badge_red">#D54040</color>
<color name="sf_scrim">#80000000</color>
```

---

## B) New drawables

### `res/drawable/sf_bg_card.xml` (menu item card bg look)

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/sf_card_bg"/>
    <corners android:radius="22dp"/>
</shape>
```

### `res/drawable/sf_bg_circle_disabled.xml`

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/sf_disabled_circle"/>
    <corners android:radius="999dp"/>
</shape>
```

### `res/drawable/sf_grabber_bg.xml` (grey base bar)

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <size android:width="54dp" android:height="6dp"/>
    <solid android:color="#E0E3E7"/>
    <corners android:radius="999dp"/>
</shape>
```

### `res/drawable/sf_grabber_fg.xml` (black shorter bar)

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <size android:width="30dp" android:height="6dp"/>
    <solid android:color="@color/sf_black"/>
    <corners android:radius="999dp"/>
</shape>
```

### `res/drawable/sf_bg_btn_black.xml` (Next / Preview pill)

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/sf_black"/>
    <corners android:radius="999dp"/>
</shape>
```

### `res/drawable/sf_bg_sheet.xml` (bottom sheet card)

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/sf_bg"/>
    <corners android:radius="28dp"/>
</shape>
```

### `res/drawable/sf_badge_red.xml`

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <size android:width="22dp" android:height="22dp"/>
    <solid android:color="@color/sf_badge_red"/>
    <corners android:radius="999dp"/>
</shape>
```

### Custom radio (matches the right-side circle list)

`res/drawable/sf_radio_checked.xml`

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <size android:width="20dp" android:height="20dp"/>
    <solid android:color="@color/sf_bg"/>
    <stroke android:width="2dp" android:color="@color/sf_primary"/>
</shape>
```

`res/drawable/sf_radio_unchecked.xml`

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <size android:width="20dp" android:height="20dp"/>
    <solid android:color="@android:color/transparent"/>
    <stroke android:width="2dp" android:color="#D3D7DD"/>
</shape>
```

`res/drawable/sf_radio_selector.xml`

```xml
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_checked="true" android:drawable="@drawable/sf_radio_checked"/>
    <item android:drawable="@drawable/sf_radio_unchecked"/>
</selector>
```

---

## C) New icons

### `res/drawable/ic_close_white_20.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="20dp" android:height="20dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF"
        android:pathData="M18.3,5.71a1,1 0,0 0,-1.41 0L12,10.59 7.11,5.7A1,1 0,0 0,5.7 7.11L10.59,12 5.7,16.89a1,1 0,1 0,1.41 1.41L12,13.41l4.89,4.89a1,1 0,0 0,1.41 -1.41L13.41,12l4.89,-4.89a1,1 0,0 0,0 -1.4Z"/>
</vector>
```

### `res/drawable/ic_arrow_left_20.xml` (rounder than chevron)

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="20dp" android:height="20dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#202124"
        android:pathData="M19,11H7.83l4.58,-4.59L11,5l-7,7 7,7 1.41,-1.41L7.83,13H19z"/>
</vector>
```

---

## D) Reusable top “Table 4” handle include

### `res/layout/include_sf_table_handle.xml`

```xml
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="64dp"
    android:paddingTop="10dp">

    <TextView
        android:id="@+id/tvTable"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Table 4"
        android:textColor="@color/sf_text_primary"
        android:textSize="14sp"
        android:fontFamily="sans-serif-medium"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <FrameLayout
        android:id="@+id/grabberWrap"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        app:layout_constraintTop_toBottomOf="@id/tvTable"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <View
            android:layout_width="54dp"
            android:layout_height="6dp"
            android:background="@drawable/sf_grabber_bg"/>

        <View
            android:layout_width="30dp"
            android:layout_height="6dp"
            android:layout_gravity="center"
            android:background="@drawable/sf_grabber_fg"/>
    </FrameLayout>

    <ImageView
        android:id="@+id/ivDown"
        android:layout_width="20dp"
        android:layout_height="20dp"
        android:layout_marginTop="6dp"
        android:src="@drawable/ic_chev_down_20"
        app:layout_constraintTop_toBottomOf="@id/grabberWrap"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>
</androidx.constraintlayout.widget.ConstraintLayout>
```

---

## E) Menu item card (used in the Chinese grid)

### `res/layout/item_menu_card.xml`

```xml
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="78dp"
    android:layout_margin="10dp"
    app:cardCornerRadius="22dp"
    app:cardUseCompatPadding="true"
    app:cardElevation="5dp"
    app:cardBackgroundColor="@color/sf_card_bg">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:paddingStart="14dp"
        android:paddingEnd="12dp">

        <View
            android:id="@+id/dotG"
            android:layout_width="6dp"
            android:layout_height="6dp"
            android:layout_marginTop="12dp"
            android:background="@drawable/sf_dot_green"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toStartOf="parent"/>

        <View
            android:id="@+id/dotR"
            android:layout_width="6dp"
            android:layout_height="6dp"
            android:layout_marginStart="6dp"
            android:background="@drawable/sf_dot_red"
            app:layout_constraintTop_toTopOf="@id/dotG"
            app:layout_constraintStart_toEndOf="@id/dotG"/>

        <TextView
            android:id="@+id/tvName"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="20dp"
            android:text="Ramen"
            android:textColor="@color/sf_text_primary"
            android:textSize="18sp"
            android:fontFamily="sans-serif-black"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toStartOf="@id/btnAdd"/>

        <TextView
            android:id="@+id/tvDesc"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:text="qnty: Small,mid,large"
            android:textColor="@color/sf_text_secondary"
            android:textSize="13sp"
            app:layout_constraintTop_toBottomOf="@id/tvName"
            app:layout_constraintStart_toStartOf="@id/tvName"
            app:layout_constraintEnd_toStartOf="@id/btnAdd"/>

        <FrameLayout
            android:id="@+id/btnAdd"
            android:layout_width="34dp"
            android:layout_height="34dp"
            android:background="@drawable/sf_bg_circle_dark"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintBottom_toBottomOf="parent">

            <ImageView
                android:layout_width="18dp"
                android:layout_height="18dp"
                android:layout_gravity="center"
                android:src="@drawable/ic_plus_20"/>
        </FrameLayout>

        <!-- If you need the “disabled +” state like behind the modal:
             set btnAdd background to @drawable/sf_bg_circle_disabled in code. -->

    </androidx.constraintlayout.widget.ConstraintLayout>
</androidx.cardview.widget.CardView>
```

---

## F) Screen: Chinese items grid

### `res/layout/fragment_order_category_items.xml`

```xml
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/sf_bg">

    <include
        android:id="@+id/tableHandle"
        layout="@layout/include_sf_table_handle"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <!-- Search -->
    <LinearLayout
        android:id="@+id/searchWrap"
        android:layout_width="0dp"
        android:layout_height="54dp"
        android:layout_marginTop="8dp"
        android:layout_marginStart="20dp"
        android:layout_marginEnd="20dp"
        android:background="@drawable/sf_bg_search"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        android:paddingStart="16dp"
        android:paddingEnd="16dp"
        app:layout_constraintTop_toBottomOf="@id/tableHandle"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <ImageView
            android:layout_width="20dp"
            android:layout_height="20dp"
            android:src="@drawable/ic_search_20" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="12dp"
            android:text="panner tikka"
            android:textColor="@color/sf_text_secondary"
            android:textSize="16sp"/>
    </LinearLayout>

    <TextView
        android:id="@+id/tvCategory"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="20dp"
        android:layout_marginTop="18dp"
        android:text="Chinese"
        android:textColor="@color/sf_text_primary"
        android:textSize="20sp"
        android:fontFamily="sans-serif-medium"
        app:layout_constraintTop_toBottomOf="@id/searchWrap"
        app:layout_constraintStart_toStartOf="parent"/>

    <!-- Grid list -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvMenu"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:layout_marginTop="8dp"
        android:paddingStart="10dp"
        android:paddingEnd="10dp"
        android:clipToPadding="false"
        app:layout_constraintTop_toBottomOf="@id/tvCategory"
        app:layout_constraintBottom_toTopOf="@id/bottomNav"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <!-- Bottom nav (same include you already have) -->
    <include
        android:id="@+id/bottomNav"
        layout="@layout/include_sf_bottom_nav"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>
</androidx.constraintlayout.widget.ConstraintLayout>
```

> Use GridLayoutManager span=2 and inflate `item_menu_card.xml`.

---

## G) Preview order bar (black pill + badge)

### `res/layout/include_preview_order_bar.xml`

```xml
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/previewBar"
    android:layout_width="0dp"
    android:layout_height="66dp"
    android:layout_marginStart="18dp"
    android:layout_marginEnd="18dp"
    app:cardCornerRadius="28dp"
    app:cardElevation="10dp"
    app:cardBackgroundColor="@color/sf_black"
    app:cardUseCompatPadding="true">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:paddingStart="18dp"
        android:paddingEnd="18dp">

        <TextView
            android:id="@+id/tvPreview"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Preview order"
            android:textColor="@color/sf_white"
            android:textSize="16sp"
            android:fontFamily="sans-serif-medium"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent"/>

        <FrameLayout
            android:id="@+id/badge"
            android:layout_width="22dp"
            android:layout_height="22dp"
            android:background="@drawable/sf_badge_red"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintBottom_toBottomOf="parent"
            android:layout_marginEnd="-2dp"
            android:layout_marginBottom="6dp">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:gravity="center"
                android:text="4"
                android:textColor="@color/sf_white"
                android:textSize="12sp"
                android:fontFamily="sans-serif-black"/>
        </FrameLayout>

    </androidx.constraintlayout.widget.ConstraintLayout>
</androidx.cardview.widget.CardView>
```

To match your screenshot, include it **above bottom nav** in your main item fragment:

```xml
<include
    android:id="@+id/previewBar"
    layout="@layout/include_preview_order_bar"
    app:layout_constraintBottom_toTopOf="@id/bottomNav"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintEnd_toEndOf="parent"
    android:layout_marginBottom="10dp"/>
```

---

## H) Bottom sheet UI (Spring roll modal) — single XML (3 steps inside)

### `res/layout/sheet_item_customize.xml`

This matches all 3 modal screenshots; you just toggle visibility of the step containers.

```xml
<androidx.core.widget.NestedScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:fillViewport="true"
    android:padding="18dp">

    <androidx.cardview.widget.CardView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:cardCornerRadius="28dp"
        app:cardElevation="14dp"
        app:cardUseCompatPadding="true"
        app:cardBackgroundColor="@color/sf_bg">

        <androidx.constraintlayout.widget.ConstraintLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:padding="20dp">

            <TextView
                android:id="@+id/tvSheetTitle"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:text="Spring roll"
                android:textSize="28sp"
                android:textColor="@color/sf_text_primary"
                android:fontFamily="sans-serif-black"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toTopOf="parent"
                app:layout_constraintEnd_toStartOf="@id/btnClose"/>

            <FrameLayout
                android:id="@+id/btnClose"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:background="@drawable/sf_bg_circle_dark"
                app:layout_constraintTop_toTopOf="parent"
                app:layout_constraintEnd_toEndOf="parent">

                <ImageView
                    android:layout_width="20dp"
                    android:layout_height="20dp"
                    android:layout_gravity="center"
                    android:src="@drawable/ic_close_white_20"/>
            </FrameLayout>

            <!-- 3-step header -->
            <TextView
                android:id="@+id/l1"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:text="Amount"
                android:textSize="13sp"
                android:textColor="@color/sf_text_primary"
                android:fontFamily="sans-serif-medium"
                app:layout_constraintTop_toBottomOf="@id/tvSheetTitle"
                android:layout_marginTop="12dp"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintEnd_toStartOf="@id/l2"
                app:layout_constraintHorizontalWeight="1"/>

            <TextView
                android:id="@+id/l2"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:text="cheif tip"
                android:textSize="13sp"
                android:textColor="@color/sf_text_secondary"
                android:fontFamily="sans-serif-medium"
                app:layout_constraintTop_toTopOf="@id/l1"
                app:layout_constraintStart_toEndOf="@id/l1"
                app:layout_constraintEnd_toStartOf="@id/l3"
                app:layout_constraintHorizontalWeight="1"
                android:gravity="center"/>

            <TextView
                android:id="@+id/l3"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:text="Qnty"
                android:textSize="13sp"
                android:textColor="@color/sf_text_secondary"
                android:fontFamily="sans-serif-medium"
                app:layout_constraintTop_toTopOf="@id/l1"
                app:layout_constraintStart_toEndOf="@id/l2"
                app:layout_constraintEnd_toEndOf="parent"
                app:layout_constraintHorizontalWeight="1"
                android:gravity="end"/>

            <View
                android:id="@+id/stepLine"
                android:layout_width="0dp"
                android:layout_height="3dp"
                android:layout_marginTop="12dp"
                android:background="@drawable/sf_step_line"
                app:layout_constraintTop_toBottomOf="@id/l1"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintEnd_toEndOf="parent"/>

            <View
                android:id="@+id/sd1"
                android:layout_width="12dp"
                android:layout_height="12dp"
                android:background="@drawable/sf_step_dot_active"
                app:layout_constraintTop_toTopOf="@id/stepLine"
                app:layout_constraintBottom_toBottomOf="@id/stepLine"
                app:layout_constraintStart_toStartOf="@id/l1" />

            <View
                android:id="@+id/sd2"
                android:layout_width="12dp"
                android:layout_height="12dp"
                android:background="@drawable/sf_step_dot_active"
                app:layout_constraintTop_toTopOf="@id/stepLine"
                app:layout_constraintBottom_toBottomOf="@id/stepLine"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintEnd_toEndOf="parent" />

            <View
                android:id="@+id/sd3"
                android:layout_width="12dp"
                android:layout_height="12dp"
                android:background="@drawable/sf_step_dot_active"
                app:layout_constraintTop_toTopOf="@id/stepLine"
                app:layout_constraintBottom_toBottomOf="@id/stepLine"
                app:layout_constraintEnd_toEndOf="parent" />

            <!-- ========== STEP 1: Amount ========== -->
            <TextView
                android:id="@+id/tvStepTitle1"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Enter amount"
                android:textColor="@color/sf_text_primary"
                android:textSize="16sp"
                android:fontFamily="sans-serif-medium"
                android:layout_marginTop="14dp"
                app:layout_constraintTop_toBottomOf="@id/stepLine"
                app:layout_constraintStart_toStartOf="parent"/>

            <androidx.cardview.widget.CardView
                android:id="@+id/amountBox"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginTop="10dp"
                app:cardCornerRadius="20dp"
                app:cardElevation="0dp"
                app:cardBackgroundColor="@color/sf_chip_bg"
                app:layout_constraintTop_toBottomOf="@id/tvStepTitle1"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintEnd_toEndOf="parent">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="44dp"
                        android:gravity="center_vertical"
                        android:orientation="horizontal">

                        <TextView
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="Small"
                            android:textColor="@color/sf_text_primary"
                            android:textSize="15sp"
                            android:fontFamily="sans-serif-medium"/>

                        <RadioButton
                            android:id="@+id/rbSmall"
                            android:layout_width="20dp"
                            android:layout_height="20dp"
                            android:button="@drawable/sf_radio_selector"
                            android:checked="true"/>
                    </LinearLayout>

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="44dp"
                        android:gravity="center_vertical"
                        android:orientation="horizontal">

                        <TextView
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="medium"
                            android:textColor="@color/sf_text_primary"
                            android:textSize="15sp"
                            android:fontFamily="sans-serif-medium"/>

                        <RadioButton
                            android:id="@+id/rbMedium"
                            android:layout_width="20dp"
                            android:layout_height="20dp"
                            android:button="@drawable/sf_radio_selector"/>
                    </LinearLayout>

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="44dp"
                        android:gravity="center_vertical"
                        android:orientation="horizontal">

                        <TextView
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="Large"
                            android:textColor="@color/sf_text_primary"
                            android:textSize="15sp"
                            android:fontFamily="sans-serif-medium"/>

                        <RadioButton
                            android:id="@+id/rbLarge"
                            android:layout_width="20dp"
                            android:layout_height="20dp"
                            android:button="@drawable/sf_radio_selector"/>
                    </LinearLayout>

                </LinearLayout>
            </androidx.cardview.widget.CardView>

            <!-- ========== STEP 2: Chief tip (initially GONE) ========== -->
            <LinearLayout
                android:id="@+id/stepChefTip"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginTop="14dp"
                android:orientation="vertical"
                android:visibility="gone"
                app:layout_constraintTop_toBottomOf="@id/stepLine"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintEnd_toEndOf="parent">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Enter chief tip"
                    android:textColor="@color/sf_text_primary"
                    android:textSize="16sp"
                    android:fontFamily="sans-serif-medium"/>

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="12dp"
                    android:gravity="center_vertical"
                    android:orientation="horizontal">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Suggestion :"
                        android:textColor="@color/sf_text_secondary"
                        android:textSize="13sp"/>

                    <com.google.android.material.chip.Chip
                        android:layout_width="wrap_content"
                        android:layout_height="36dp"
                        android:layout_marginStart="10dp"
                        android:text="Sour"
                        app:chipCornerRadius="18dp"
                        app:chipBackgroundColor="#E7F0FF"
                        android:textColor="@color/sf_primary"/>

                    <com.google.android.material.chip.Chip
                        android:layout_width="wrap_content"
                        android:layout_height="36dp"
                        android:layout_marginStart="8dp"
                        android:text="Spicy"
                        app:chipCornerRadius="18dp"
                        app:chipBackgroundColor="@color/sf_chip_bg"
                        android:textColor="@color/sf_text_secondary"/>

                    <com.google.android.material.chip.Chip
                        android:layout_width="wrap_content"
                        android:layout_height="36dp"
                        android:layout_marginStart="8dp"
                        android:text="Creamy"
                        app:chipCornerRadius="18dp"
                        app:chipBackgroundColor="@color/sf_chip_bg"
                        android:textColor="@color/sf_text_secondary"/>
                </LinearLayout>

                <EditText
                    android:layout_width="match_parent"
                    android:layout_height="54dp"
                    android:layout_marginTop="14dp"
                    android:background="@drawable/sf_bg_search"
                    android:hint="Type here"
                    android:paddingStart="18dp"
                    android:paddingEnd="18dp"
                    android:textColor="@color/sf_text_primary"
                    android:textColorHint="@color/sf_text_secondary"/>
            </LinearLayout>

            <!-- ========== STEP 3: Qty (initially GONE) ========== -->
            <LinearLayout
                android:id="@+id/stepQty"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginTop="14dp"
                android:orientation="vertical"
                android:visibility="gone"
                app:layout_constraintTop_toBottomOf="@id/stepLine"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintEnd_toEndOf="parent">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Enter qnty"
                    android:textColor="@color/sf_text_primary"
                    android:textSize="16sp"
                    android:fontFamily="sans-serif-medium"/>

                <androidx.constraintlayout.widget.ConstraintLayout
                    android:layout_width="match_parent"
                    android:layout_height="120dp"
                    android:layout_marginTop="18dp">

                    <FrameLayout
                        android:id="@+id/btnMinus"
                        android:layout_width="64dp"
                        android:layout_height="64dp"
                        android:background="@drawable/sf_bg_circle_dark"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintTop_toTopOf="parent"
                        app:layout_constraintBottom_toBottomOf="parent">

                        <TextView
                            android:layout_width="match_parent"
                            android:layout_height="match_parent"
                            android:gravity="center"
                            android:text="−"
                            android:textColor="@color/sf_white"
                            android:textSize="28sp"
                            android:fontFamily="sans-serif-black"/>
                    </FrameLayout>

                    <TextView
                        android:id="@+id/tvQty"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="1"
                        android:textColor="@color/sf_text_primary"
                        android:textSize="56sp"
                        android:fontFamily="sans-serif-black"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintEnd_toEndOf="parent"
                        app:layout_constraintTop_toTopOf="parent"
                        app:layout_constraintBottom_toBottomOf="parent"/>

                    <FrameLayout
                        android:id="@+id/btnPlus"
                        android:layout_width="64dp"
                        android:layout_height="64dp"
                        android:background="@drawable/sf_bg_circle_dark"
                        app:layout_constraintEnd_toEndOf="parent"
                        app:layout_constraintTop_toTopOf="parent"
                        app:layout_constraintBottom_toBottomOf="parent">

                        <TextView
                            android:layout_width="match_parent"
                            android:layout_height="match_parent"
                            android:gravity="center"
                            android:text="+"
                            android:textColor="@color/sf_white"
                            android:textSize="28sp"
                            android:fontFamily="sans-serif-black"/>
                    </FrameLayout>

                </androidx.constraintlayout.widget.ConstraintLayout>
            </LinearLayout>

            <!-- Bottom actions -->
            <FrameLayout
                android:id="@+id/btnBack"
                android:layout_width="56dp"
                android:layout_height="56dp"
                android:layout_marginTop="18dp"
                android:background="@drawable/sf_bg_chip"
                app:layout_constraintTop_toBottomOf="@id/amountBox"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintBottom_toBottomOf="parent">

                <ImageView
                    android:layout_width="22dp"
                    android:layout_height="22dp"
                    android:layout_gravity="center"
                    android:src="@drawable/ic_arrow_left_20"/>
            </FrameLayout>

            <TextView
                android:id="@+id/btnNext"
                android:layout_width="160dp"
                android:layout_height="56dp"
                android:background="@drawable/sf_bg_btn_black"
                android:gravity="center"
                android:text="Next"
                android:textColor="@color/sf_white"
                android:textSize="16sp"
                android:fontFamily="sans-serif-black"
                app:layout_constraintTop_toTopOf="@id/btnBack"
                app:layout_constraintBottom_toBottomOf="@id/btnBack"
                app:layout_constraintEnd_toEndOf="parent"/>
        </androidx.constraintlayout.widget.ConstraintLayout>
    </androidx.cardview.widget.CardView>
</androidx.core.widget.NestedScrollView>
```

**How it maps to your modal screenshots:**

* **Amount modal:** `amountBox` VISIBLE, `stepChefTip` GONE, `stepQty` GONE
* **Chief tip modal:** `amountBox` GONE, `stepChefTip` VISIBLE, `stepQty` GONE
* **Qty modal:** `amountBox` GONE, `stepChefTip` GONE, `stepQty` VISIBLE

---

Yep — these **4 new UIs** you sent are covered by the XML below:

1. **Preview Order (no appbar)**
2. **Preview Order (with Waiter Page appbar + stepper)**
3. **Given Order tab** (table chips + big order card + “Add Items”)
4. **Ready Order tab** (cards with “Deliver” green footer)

I’m giving you **only the new files** you need to add, plus a couple tiny theme resources.

---

## 1) Add/Update resources

### `res/values/colors.xml` (add these if not already)

```xml
<color name="sf_success">#48B878</color>
<color name="sf_border">#E0E3E7</color>
<color name="sf_light_blue">#E7F0FF</color>
<color name="sf_card_soft">#F3F4F6</color>
```

### New drawables

`res/drawable/sf_bg_card_soft.xml`

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/sf_card_soft"/>
    <corners android:radius="24dp"/>
</shape>
```

`res/drawable/sf_bg_pill_outline.xml` (Alter order)

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/sf_bg"/>
    <stroke android:width="1dp" android:color="@color/sf_border"/>
    <corners android:radius="14dp"/>
</shape>
```

`res/drawable/sf_bg_pill_green.xml` (Deliver/Add Items)

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/sf_success"/>
    <corners android:radius="22dp"/>
</shape>
```

`res/drawable/sf_bg_circle_white.xml` (qty +/- circle)

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/sf_bg"/>
    <corners android:radius="999dp"/>
</shape>
```

`res/drawable/sf_badge_qty.xml` (x2 badge)

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#E5E7EB"/>
    <corners android:radius="10dp"/>
</shape>
```

---

## 2) Icons (new)

`res/drawable/ic_check_white_20.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="20dp" android:height="20dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF"
        android:pathData="M9,16.2 4.8,12 3.4,13.4 9,19 21,7 19.6,5.6z"/>
</vector>
```

---

## 3) Bottom nav — 3 exact variants (so selected tab matches screenshots)

### `res/layout/include_sf_bottom_nav_new_selected.xml`

Copy your existing `include_sf_bottom_nav.xml` (it already shows New selected) and rename to this file.

### `res/layout/include_sf_bottom_nav_ready_selected.xml`

Same structure, but make the **middle item** the blue pill and others grey (like your Ready Order screen):

```xml
<!-- Keep your existing bottom nav card wrapper exactly the same -->
<!-- Replace the 3 inside blocks with this: -->
<androidx.cardview.widget.CardView ... >  <!-- same wrapper as before -->

    <androidx.constraintlayout.widget.ConstraintLayout ... >

        <!-- New Order (unselected) -->
        <LinearLayout
            android:id="@+id/navNewOrder"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:gravity="center"
            android:orientation="vertical"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toStartOf="@id/navReadyOrder"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintHorizontalWeight="1">

            <TextView android:text="+" android:textSize="20sp"
                android:textColor="@color/sf_text_muted" />
            <TextView android:text="New Order" android:textSize="13sp"
                android:textColor="@color/sf_text_muted" android:textStyle="600"/>
        </LinearLayout>

        <!-- Ready Order (selected) -->
        <androidx.cardview.widget.CardView
            android:id="@+id/navReadyOrder"
            android:layout_width="0dp"
            android:layout_height="@dimen/sf_selected_nav_h"
            app:cardBackgroundColor="@color/sf_primary"
            app:cardCornerRadius="18dp"
            app:cardElevation="0dp"
            app:layout_constraintStart_toEndOf="@id/navNewOrder"
            app:layout_constraintEnd_toStartOf="@id/navGivenOrder"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintHorizontalWeight="1.2">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:gravity="center"
                android:orientation="vertical">

                <TextView android:text="☝" android:textSize="20sp"
                    android:textColor="@color/sf_white"/>
                <TextView android:text="Ready Order" android:textSize="13sp"
                    android:textColor="@color/sf_white" android:textStyle="700"/>
            </LinearLayout>
        </androidx.cardview.widget.CardView>

        <!-- Given Order (unselected) -->
        <LinearLayout
            android:id="@+id/navGivenOrder"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:gravity="center"
            android:orientation="vertical"
            app:layout_constraintStart_toEndOf="@id/navReadyOrder"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintHorizontalWeight="1">

            <TextView android:text="✓" android:textSize="20sp"
                android:textColor="@color/sf_text_muted" />
            <TextView android:text="Given Order" android:textSize="13sp"
                android:textColor="@color/sf_text_muted" android:textStyle="600"/>
        </LinearLayout>

    </androidx.constraintlayout.widget.ConstraintLayout>
</androidx.cardview.widget.CardView>
```

### `res/layout/include_sf_bottom_nav_given_selected.xml`

Same idea but the **right** item is selected (like your Given Order screen). (Structure identical to above; just move the blue pill to Given.)

---

## 4) Preview Order (screen 1 — minimal top “Table 4”)

### `res/layout/fragment_preview_order.xml`

```xml
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/sf_bg">

    <include
        android:id="@+id/tableHandle"
        layout="@layout/include_sf_table_handle"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <TextView
        android:id="@+id/tvPreview"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="20dp"
        android:text="Preview order"
        android:textSize="22sp"
        android:textColor="@color/sf_text_primary"
        android:fontFamily="sans-serif-black"
        app:layout_constraintTop_toBottomOf="@id/tableHandle"
        app:layout_constraintStart_toStartOf="parent"/>

    <FrameLayout
        android:id="@+id/btnAdd"
        android:layout_width="44dp"
        android:layout_height="44dp"
        android:layout_marginEnd="20dp"
        android:background="@drawable/sf_bg_circle_dark"
        app:layout_constraintTop_toTopOf="@id/tvPreview"
        app:layout_constraintBottom_toBottomOf="@id/tvPreview"
        app:layout_constraintEnd_toEndOf="parent">

        <ImageView
            android:layout_width="20dp"
            android:layout_height="20dp"
            android:layout_gravity="center"
            android:src="@drawable/ic_plus_20"/>
    </FrameLayout>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvPreview"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:layout_marginTop="14dp"
        android:paddingStart="18dp"
        android:paddingEnd="18dp"
        android:clipToPadding="false"
        app:layout_constraintTop_toBottomOf="@id/tvPreview"
        app:layout_constraintBottom_toTopOf="@id/placeOrderBar"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <include
        android:id="@+id/placeOrderBar"
        layout="@layout/include_place_order_bar"
        app:layout_constraintBottom_toTopOf="@id/bottomNav"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginBottom="10dp"/>

    <include
        android:id="@+id/bottomNav"
        layout="@layout/include_sf_bottom_nav_new_selected"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>
</androidx.constraintlayout.widget.ConstraintLayout>
```

### Row card for preview list

`res/layout/item_preview_order_card.xml`

```xml
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="14dp"
    app:cardCornerRadius="24dp"
    app:cardElevation="6dp"
    app:cardUseCompatPadding="true"
    app:cardBackgroundColor="@color/sf_card_soft">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="18dp">

        <TextView
            android:id="@+id/tvName"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:text="Malia tikka"
            android:textColor="@color/sf_text_primary"
            android:textSize="20sp"
            android:fontFamily="sans-serif-black"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintEnd_toStartOf="@id/tvAmount"/>

        <TextView
            android:id="@+id/tvAmount"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="amount : large"
            android:textColor="@color/sf_text_secondary"
            android:textSize="14sp"
            app:layout_constraintTop_toTopOf="@id/tvName"
            app:layout_constraintEnd_toEndOf="parent"/>

        <FrameLayout
            android:id="@+id/btnRemove"
            android:layout_width="38dp"
            android:layout_height="38dp"
            android:background="@drawable/sf_bg_circle_dark"
            android:layout_marginTop="-8dp"
            android:layout_marginEnd="-8dp"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintEnd_toEndOf="parent">

            <ImageView
                android:layout_width="18dp"
                android:layout_height="18dp"
                android:layout_gravity="center"
                android:src="@drawable/ic_close_white_20"/>
        </FrameLayout>

        <TextView
            android:id="@+id/tvTip"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:text="Chef tip : make it Spicer"
            android:textColor="@color/sf_text_secondary"
            android:textSize="15sp"
            app:layout_constraintTop_toBottomOf="@id/tvName"
            app:layout_constraintStart_toStartOf="@id/tvName"
            app:layout_constraintEnd_toEndOf="parent"/>

        <TextView
            android:id="@+id/btnAlter"
            android:layout_width="120dp"
            android:layout_height="40dp"
            android:layout_marginTop="14dp"
            android:background="@drawable/sf_bg_pill_outline"
            android:gravity="center"
            android:text="Alter order"
            android:textColor="@color/sf_text_secondary"
            android:textSize="14sp"
            android:fontFamily="sans-serif-medium"
            app:layout_constraintTop_toBottomOf="@id/tvTip"
            app:layout_constraintStart_toStartOf="parent"/>

        <TextView
            android:id="@+id/tvQtyLabel"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="qty"
            android:textColor="@color/sf_text_secondary"
            android:textSize="14sp"
            android:layout_marginEnd="8dp"
            app:layout_constraintBottom_toBottomOf="@id/btnAlter"
            app:layout_constraintEnd_toStartOf="@id/btnMinus"/>

        <androidx.cardview.widget.CardView
            android:id="@+id/btnMinus"
            android:layout_width="40dp"
            android:layout_height="40dp"
            app:cardCornerRadius="20dp"
            app:cardElevation="2dp"
            app:cardBackgroundColor="@color/sf_bg"
            app:layout_constraintBottom_toBottomOf="@id/btnAlter"
            app:layout_constraintEnd_toStartOf="@id/tvQty"
            android:layout_marginEnd="10dp">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:gravity="center"
                android:text="−"
                android:textSize="22sp"
                android:textColor="@color/sf_text_secondary"
                android:fontFamily="sans-serif-black"/>
        </androidx.cardview.widget.CardView>

        <TextView
            android:id="@+id/tvQty"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="1"
            android:textColor="@color/sf_text_primary"
            android:textSize="20sp"
            android:fontFamily="sans-serif-black"
            app:layout_constraintBottom_toBottomOf="@id/btnAlter"
            app:layout_constraintEnd_toStartOf="@id/btnPlus"
            android:layout_marginEnd="10dp"/>

        <androidx.cardview.widget.CardView
            android:id="@+id/btnPlus"
            android:layout_width="40dp"
            android:layout_height="40dp"
            app:cardCornerRadius="20dp"
            app:cardElevation="2dp"
            app:cardBackgroundColor="@color/sf_bg"
            app:layout_constraintBottom_toBottomOf="@id/btnAlter"
            app:layout_constraintEnd_toEndOf="parent">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:gravity="center"
                android:text="+"
                android:textSize="22sp"
                android:textColor="@color/sf_text_secondary"
                android:fontFamily="sans-serif-black"/>
        </androidx.cardview.widget.CardView>

    </androidx.constraintlayout.widget.ConstraintLayout>
</androidx.cardview.widget.CardView>
```

### Bottom “Place order” pill (with badge)

`res/layout/include_place_order_bar.xml`

```xml
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="0dp"
    android:layout_height="70dp"
    android:layout_marginStart="18dp"
    android:layout_marginEnd="18dp"
    app:cardCornerRadius="28dp"
    app:cardElevation="12dp"
    app:cardUseCompatPadding="true"
    app:cardBackgroundColor="@color/sf_black">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Place order"
            android:textColor="@color/sf_white"
            android:textSize="18sp"
            android:fontFamily="sans-serif-black"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent"/>

        <FrameLayout
            android:layout_width="22dp"
            android:layout_height="22dp"
            android:background="@drawable/sf_badge_red"
            android:layout_marginEnd="-2dp"
            android:layout_marginBottom="10dp"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintBottom_toBottomOf="parent">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:gravity="center"
                android:text="4"
                android:textColor="@color/sf_white"
                android:textSize="12sp"
                android:fontFamily="sans-serif-black"/>
        </FrameLayout>

    </androidx.constraintlayout.widget.ConstraintLayout>
</androidx.cardview.widget.CardView>
```

---

## 5) Preview Order (screen 2 — with Waiter Page + stepper)

Use your existing header include + a stepper include that shows **Type/Where/Item values**.

### `res/layout/include_sf_stepper_preview.xml`

(Just like your stepper, but values under first 3)

```xml
<!-- Same as include_sf_stepper.xml but ensure v1, v2, v3 are visible -->
<!-- Add this v3 under Item column -->
<TextView
    android:id="@+id/v3"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginTop="8dp"
    android:text="4"
    android:textColor="@color/sf_text_primary"
    android:textSize="12sp"
    app:layout_constraintTop_toBottomOf="@id/stepLine"
    app:layout_constraintStart_toStartOf="@id/s3"
    app:layout_constraintEnd_toEndOf="@id/s3" />
```

### `res/layout/fragment_preview_order_header.xml`

```xml
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/sf_bg">

    <include
        android:id="@+id/appbar"
        layout="@layout/include_sf_appbar"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <TextView
        android:id="@+id/tvCreateOrder"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="@dimen/sf_pad_screen_h"
        android:layout_marginTop="14dp"
        android:text="Create order"
        android:textColor="@color/sf_text_primary"
        android:textSize="26sp"
        android:textStyle="700"
        app:layout_constraintTop_toBottomOf="@id/appbar"
        app:layout_constraintStart_toStartOf="parent"/>

    <include
        android:id="@+id/stepper"
        layout="@layout/include_sf_stepper_preview"
        android:layout_marginStart="@dimen/sf_pad_screen_h"
        android:layout_marginEnd="@dimen/sf_pad_screen_h"
        android:layout_marginTop="10dp"
        app:layout_constraintTop_toBottomOf="@id/tvCreateOrder"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <ImageView
        android:id="@+id/ivUp"
        android:layout_width="20dp"
        android:layout_height="20dp"
        android:layout_marginTop="8dp"
        android:src="@drawable/ic_chev_down_20"
        android:rotation="180"
        app:layout_constraintTop_toBottomOf="@id/stepper"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <LinearLayout
        android:id="@+id/btnGoBack"
        android:layout_width="124dp"
        android:layout_height="44dp"
        android:layout_marginEnd="@dimen/sf_pad_screen_h"
        android:background="@drawable/sf_bg_chip"
        android:gravity="center"
        android:orientation="horizontal"
        android:paddingStart="14dp"
        android:paddingEnd="14dp"
        android:elevation="2dp"
        app:layout_constraintTop_toBottomOf="@id/stepper"
        app:layout_constraintEnd_toEndOf="parent">

        <ImageView android:layout_width="18dp" android:layout_height="18dp"
            android:src="@drawable/ic_chev_left_20" />
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp"
            android:text="Go back"
            android:textColor="@color/sf_text_secondary"
            android:textSize="14sp"
            android:textStyle="600" />
    </LinearLayout>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvPreview"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:layout_marginTop="16dp"
        android:paddingStart="18dp"
        android:paddingEnd="18dp"
        android:clipToPadding="false"
        app:layout_constraintTop_toBottomOf="@id/ivUp"
        app:layout_constraintBottom_toTopOf="@id/placeOrderBar"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <include
        android:id="@+id/placeOrderBar"
        layout="@layout/include_place_order_bar"
        app:layout_constraintBottom_toTopOf="@id/bottomNav"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginBottom="10dp"/>

    <include
        android:id="@+id/bottomNav"
        layout="@layout/include_sf_bottom_nav_new_selected"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>
</androidx.constraintlayout.widget.ConstraintLayout>
```

---

## 6) Given Order screen (table chips + big card + Add Items)

### `res/layout/fragment_given_order.xml`

```xml
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/sf_bg">

    <!-- top small caret -->
    <ImageView
        android:id="@+id/ivDownTop"
        android:layout_width="20dp"
        android:layout_height="20dp"
        android:layout_marginTop="6dp"
        android:src="@drawable/ic_chev_down_20"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <!-- search -->
    <LinearLayout
        android:id="@+id/searchWrap"
        android:layout_width="0dp"
        android:layout_height="54dp"
        android:layout_marginTop="10dp"
        android:layout_marginStart="20dp"
        android:layout_marginEnd="20dp"
        android:background="@drawable/sf_bg_search"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        android:paddingStart="16dp"
        android:paddingEnd="16dp"
        app:layout_constraintTop_toBottomOf="@id/ivDownTop"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <ImageView android:layout_width="20dp" android:layout_height="20dp"
            android:src="@drawable/ic_search_20"/>
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="12dp"
            android:text="Search order or table"
            android:textColor="@color/sf_text_secondary"
            android:textSize="16sp"/>
    </LinearLayout>

    <!-- table chips -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvTables"
        android:layout_width="0dp"
        android:layout_height="54dp"
        android:layout_marginTop="12dp"
        android:paddingStart="18dp"
        android:clipToPadding="false"
        app:layout_constraintTop_toBottomOf="@id/searchWrap"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <!-- big order card -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvGivenBody"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:layout_marginTop="12dp"
        android:paddingStart="18dp"
        android:paddingEnd="18dp"
        android:clipToPadding="false"
        app:layout_constraintTop_toBottomOf="@id/rvTables"
        app:layout_constraintBottom_toTopOf="@id/bottomNav"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <include
        android:id="@+id/bottomNav"
        layout="@layout/include_sf_bottom_nav_given_selected"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>
</androidx.constraintlayout.widget.ConstraintLayout>
```

### Given Order: big card item

`res/layout/item_given_order_card.xml`

```xml
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="16dp"
    app:cardCornerRadius="22dp"
    app:cardUseCompatPadding="true"
    app:cardElevation="10dp"
    app:cardBackgroundColor="@color/sf_bg">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <TextView
            android:text="Table 05 - #1254"
            android:textColor="@color/sf_text_primary"
            android:textSize="20sp"
            android:fontFamily="sans-serif-black"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"/>

        <TextView
            android:text="12 min ago"
            android:textColor="@color/sf_text_muted"
            android:textSize="13sp"
            android:layout_marginTop="6dp"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"/>

        <View
            android:layout_width="match_parent"
            android:layout_height="1dp"
            android:layout_marginTop="10dp"
            android:layout_marginBottom="10dp"
            android:background="@color/sf_border"/>

        <!-- Repeat this block per item (or use another RecyclerView inside) -->
        <include layout="@layout/item_given_order_line"/>

        <include layout="@layout/item_given_order_line_shaded"/>

        <include layout="@layout/item_given_order_line"/>

        <include layout="@layout/item_given_order_line_shaded"/>

        <TextView
            android:id="@+id/btnAddItems"
            android:layout_width="match_parent"
            android:layout_height="58dp"
            android:layout_marginTop="14dp"
            android:background="@drawable/sf_bg_pill_green"
            android:gravity="center"
            android:text="Add Items"
            android:textColor="@color/sf_white"
            android:textSize="18sp"
            android:fontFamily="sans-serif-black"/>
    </LinearLayout>
</androidx.cardview.widget.CardView>
```

`res/layout/item_given_order_line.xml` (white row)

```xml
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:paddingTop="10dp"
    android:paddingBottom="10dp">

    <View
        android:id="@+id/stripe"
        android:layout_width="4dp"
        android:layout_height="44dp"
        android:background="@color/sf_red"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"/>

    <TextView
        android:id="@+id/tvItem"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="14dp"
        android:text="Spicy Ramen Burger"
        android:textColor="@color/sf_text_primary"
        android:textSize="16sp"
        android:fontFamily="sans-serif-black"
        app:layout_constraintStart_toEndOf="@id/stripe"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintEnd_toStartOf="@id/btnAlter"/>

    <TextView
        android:id="@+id/badge"
        android:layout_width="34dp"
        android:layout_height="22dp"
        android:layout_marginStart="10dp"
        android:background="@drawable/sf_badge_qty"
        android:gravity="center"
        android:text="x2"
        android:textColor="@color/sf_text_primary"
        android:textSize="12sp"
        android:fontFamily="sans-serif-black"
        app:layout_constraintStart_toEndOf="@id/tvItem"
        app:layout_constraintTop_toTopOf="@id/tvItem"/>

    <TextView
        android:id="@+id/tvSize"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Size: Large"
        android:textColor="@color/sf_text_secondary"
        android:textSize="14sp"
        app:layout_constraintEnd_toStartOf="@id/btnAlter"
        app:layout_constraintTop_toTopOf="@id/tvItem"
        android:layout_marginEnd="10dp"/>

    <TextView
        android:id="@+id/tvTips"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginTop="6dp"
        android:text="Tips: Extra hot sauce, no onions"
        android:textColor="@color/sf_text_muted"
        android:textSize="13sp"
        app:layout_constraintStart_toStartOf="@id/tvItem"
        app:layout_constraintTop_toBottomOf="@id/tvItem"
        app:layout_constraintEnd_toEndOf="@id/tvItem"/>

    <TextView
        android:id="@+id/btnAlter"
        android:layout_width="110dp"
        android:layout_height="36dp"
        android:background="@drawable/sf_bg_pill_outline"
        android:gravity="center"
        android:text="Alter order"
        android:textColor="@color/sf_text_secondary"
        android:textSize="13sp"
        android:fontFamily="sans-serif-medium"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="@id/tvItem"/>
</androidx.constraintlayout.widget.ConstraintLayout>
```

`res/layout/item_given_order_line_shaded.xml` (grey inner row like screenshot)

```xml
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@color/sf_chip_bg"
    android:layout_marginTop="8dp"
    android:layout_marginBottom="8dp"
    android:paddingStart="10dp"
    android:paddingEnd="10dp"
    android:paddingTop="6dp"
    android:paddingBottom="6dp">

    <include layout="@layout/item_given_order_line"/>
</FrameLayout>
```

---

## 7) Ready Order screen (Deliver cards)

### `res/layout/fragment_ready_order.xml`

```xml
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/sf_bg">

    <include
        android:id="@+id/appbar"
        layout="@layout/include_sf_appbar"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvReady"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:layout_marginTop="12dp"
        android:paddingStart="18dp"
        android:paddingEnd="18dp"
        android:clipToPadding="false"
        app:layout_constraintTop_toBottomOf="@id/appbar"
        app:layout_constraintBottom_toTopOf="@id/bottomNav"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <include
        android:id="@+id/bottomNav"
        layout="@layout/include_sf_bottom_nav_ready_selected"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>
</androidx.constraintlayout.widget.ConstraintLayout>
```

`res/layout/item_ready_order_card.xml`

```xml
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="18dp"
    app:cardCornerRadius="22dp"
    app:cardUseCompatPadding="true"
    app:cardElevation="10dp"
    app:cardBackgroundColor="@color/sf_bg">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <TextView
                android:text="Table 05 - #1254"
                android:textColor="@color/sf_text_primary"
                android:textSize="20sp"
                android:fontFamily="sans-serif-black"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"/>

            <include layout="@layout/item_given_order_line"/>
        </LinearLayout>

        <TextView
            android:id="@+id/btnDeliver"
            android:layout_width="match_parent"
            android:layout_height="72dp"
            android:background="@drawable/sf_bg_pill_green"
            android:gravity="center"
            android:text="Deliver"
            android:textColor="@color/sf_white"
            android:textSize="22sp"
            android:fontFamily="sans-serif-black"/>
    </LinearLayout>
</androidx.cardview.widget.CardView>
```

---

### What to use where (quick)

* **Preview order list**: `fragment_preview_order.xml` + `item_preview_order_card.xml` + `include_place_order_bar.xml`
* **Preview order with appbar**: `fragment_preview_order_header.xml` (uses same row + place bar)
* **Given Order tab**: `fragment_given_order.xml` + `item_given_order_card.xml` (+ `item_given_order_line*.xml`)
* **Ready Order tab**: `fragment_ready_order.xml` + `item_ready_order_card.xml`

Send the next screens whenever—I'll keep extending the same design system so everything stays consistent (colors, radius, elevations, spacing).
