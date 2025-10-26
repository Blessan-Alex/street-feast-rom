# Firebase Restoration Guide

## Overview
This guide provides step-by-step instructions to restore Firebase functionality after the mock prototype demonstration.

---

## Phase 1: Restore Authentication System

### 1.1 Revert AuthViewModel to Use Firebase

**File:** `app/src/main/java/com/streatfeast/app/viewmodels/AuthViewModel.kt`

**Change:**
```kotlin
// FROM:
import com.streatfeast.app.repositories.MockAuthRepository
private val repository = MockAuthRepository()

// TO:
import com.streatfeast.app.repositories.AuthRepository
private val repository = AuthRepository()
```

**Restore Coroutine-based Login:**
```kotlin
fun login(email: String, password: String) {
    if (email.isBlank() || password.isBlank()) {
        _error.value = "Email and password are required"
        return
    }
    
    viewModelScope.launch {
        _isLoading.value = true
        _error.value = null
        
        val result = repository.login(email, password)
        _isLoading.value = false
        
        result.onSuccess { user ->
            _currentUser.value = user
            _isAuthenticated.value = true
        }.onFailure { exception ->
            _error.value = exception.message ?: "Login failed"
            _isAuthenticated.value = false
        }
    }
}
```

### 1.2 Restore AuthRepository Firebase Implementation

**File:** `app/src/main/java/com/streatfeast/app/repositories/AuthRepository.kt`

**Restore original Firebase implementation:**
```kotlin
suspend fun login(email: String, password: String): Result<User> {
    return try {
        // Authenticate with Firebase
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        val firebaseUser = authResult.user ?: return Result.failure(Exception("User not found"))
        
        // Get user data from Firestore
        val userDoc = db.collection(Constants.USERS_COLLECTION)
            .document(firebaseUser.uid)
            .get()
            .await()
        
        val user = userDoc.toObject(User::class.java)?.copy(id = firebaseUser.uid)
            ?: User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: "",
                role = UserRole.CHEF
            )
        
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

---

## Phase 2: Restore Order Data System

### 2.1 Revert OrdersViewModel to Use Firebase

**File:** `app/src/main/java/com/streatfeast/app/viewmodels/OrdersViewModel.kt`

**Change:**
```kotlin
// FROM:
import com.streatfeast.app.repositories.MockOrderRepository
private val repository = MockOrderRepository()

// TO:
import com.streatfeast.app.repositories.OrderRepository
import com.google.firebase.firestore.ListenerRegistration
private val repository = OrderRepository()
```

**Restore Realtime Listeners:**
```kotlin
// Add back Firestore listeners
private var newOrdersListener: ListenerRegistration? = null
private var preparingOrdersListener: ListenerRegistration? = null
private var readyOrdersListener: ListenerRegistration? = null

fun startListeningToNewOrders() {
    newOrdersListener = repository.getNewOrdersRealtime { orders ->
        viewModelScope.launch {
            val ordersWithItems = orders.map { order ->
                val itemsResult = repository.getOrderItems(order.id)
                order.copy(items = itemsResult.getOrDefault(emptyList()))
            }
            _newOrders.postValue(ordersWithItems)
        }
    }
}

fun startListeningToPreparingOrders() {
    preparingOrdersListener = repository.getPreparingOrdersRealtime { orders ->
        viewModelScope.launch {
            val ordersWithItems = orders.map { order ->
                val itemsResult = repository.getOrderItems(order.id)
                order.copy(items = itemsResult.getOrDefault(emptyList()))
            }
            _preparingOrders.postValue(ordersWithItems)
        }
    }
}

fun startListeningToReadyOrders() {
    readyOrdersListener = repository.getReadyOrdersRealtime { orders ->
        viewModelScope.launch {
            val ordersWithItems = orders.map { order ->
                val itemsResult = repository.getOrderItems(order.id)
                order.copy(items = itemsResult.getOrDefault(emptyList()))
            }
            _readyOrders.postValue(ordersWithItems)
        }
    }
}

override fun onCleared() {
    super.onCleared()
    newOrdersListener?.remove()
    preparingOrdersListener?.remove()
    readyOrdersListener?.remove()
}
```

**Remove Mock Load Methods:**
```kotlin
// REMOVE these methods:
// fun loadNewOrders()
// fun loadPreparingOrders() 
// fun loadReadyOrders()
```

### 2.2 Restore OrderRepository Firebase Implementation

**File:** `app/src/main/java/com/streatfeast/app/repositories/OrderRepository.kt`

**Restore original Firebase implementation:**
```kotlin
fun getNewOrdersRealtime(callback: (List<Order>) -> Unit): ListenerRegistration {
    return getOrdersCollection()
        .whereEqualTo("status", Constants.STATUS_CREATED)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                callback(emptyList())
                return@addSnapshotListener
            }
            
            val orders = snapshot?.documents?.mapNotNull { doc ->
                try {
                    doc.toObject(Order::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
            
            callback(orders)
        }
}

// Similar implementations for getPreparingOrdersRealtime() and getReadyOrdersRealtime()
```

---

## Phase 3: Restore Fragments to Use Realtime Listeners

### 3.1 Update ChefNewOrdersFragment

**File:** `app/src/main/java/com/streatfeast/app/fragments/ChefNewOrdersFragment.kt`

**Change:**
```kotlin
// FROM:
viewModel.loadNewOrders()

// TO:
viewModel.startListeningToNewOrders()
```

**Update SwipeRefresh:**
```kotlin
private fun setupSwipeRefresh() {
    binding.swipeRefresh.setOnRefreshListener {
        // Orders are updated in real-time, just stop the refresh animation
        binding.swipeRefresh.isRefreshing = false
    }
}
```

### 3.2 Update ChefPreparingFragment

**File:** `app/src/main/java/com/streatfeast/app/fragments/ChefPreparingFragment.kt`

**Change:**
```kotlin
// FROM:
viewModel.loadPreparingOrders()

// TO:
viewModel.startListeningToPreparingOrders()
```

### 3.3 Update WaiterReadyFragment

**File:** `app/src/main/java/com/streatfeast/app/fragments/WaiterReadyFragment.kt`

**Change:**
```kotlin
// FROM:
viewModel.loadReadyOrders()

// TO:
viewModel.startListeningToReadyOrders()
```

---

## Phase 4: Restore Firebase Application Setup

### 4.1 Re-enable Firebase in Application Class

**File:** `app/src/main/java/com/streatfeast/app/StreetFeastApplication.kt`

**Uncomment Firebase initialization:**
```kotlin
override fun onCreate() {
    super.onCreate()
    
    // Initialize Firebase
    FirebaseApp.initializeApp(this)
    
    // Enable offline persistence for Firestore
    val firestore = FirebaseFirestore.getInstance()
    val settings = FirebaseFirestoreSettings.Builder()
        .setPersistenceEnabled(true)
        .build()
    firestore.firestoreSettings = settings
}
```

### 4.2 Restore Firebase Services in AndroidManifest

**File:** `app/src/main/AndroidManifest.xml`

**Uncomment Firebase Messaging Service:**
```xml
<service
    android:name=".utils.StreetFeastMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

---

## Phase 5: Firebase Console Setup Checklist

### 5.1 Authentication Setup
- [ ] Go to Firebase Console → Authentication
- [ ] Verify Email/Password provider is enabled
- [ ] Create test users:
  - `chef@streetfeast.com` / `password123`
  - `waiter@streetfeast.com` / `password123`

### 5.2 Firestore Database Setup
- [ ] Go to Firebase Console → Firestore Database
- [ ] Verify database exists and is in "test mode"
- [ ] Create user documents in `/users/{uid}`:
  ```json
  {
    "email": "chef@streetfeast.com",
    "displayName": "Test Chef",
    "role": "chef",
    "deviceTokens": []
  }
  ```

### 5.3 Firestore Rules
- [ ] Update rules to allow authenticated users:
  ```javascript
  rules_version = '2';
  service cloud.firestore {
    match /databases/{database}/documents {
      match /{document=**} {
        allow read, write: if request.auth != null;
      }
    }
  }
  ```

### 5.4 SHA-1 Fingerprint
- [ ] Get debug SHA-1:
  ```bash
  keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
  ```
- [ ] Add SHA-1 to Firebase Console → Project Settings → Your Apps

---

## Phase 6: Testing After Restoration

### 6.1 Authentication Test
1. **Clean & Rebuild** project
2. **Run app** on device/emulator
3. **Login with Firebase credentials:**
   - Email: `chef@streetfeast.com`
   - Password: `password123`
4. **Verify:** Navigation to MainActivity works
5. **Verify:** User role is correctly detected

### 6.2 Order Data Test
1. **Create test orders** in Firestore manually or via admin app
2. **Verify:** Orders appear in real-time on Android app
3. **Test order actions:** Accept, Mark Prepared, Mark Delivered
4. **Verify:** Status changes are reflected immediately

### 6.3 Realtime Updates Test
1. **Create order** via admin/desktop app
2. **Verify:** Order appears instantly on Android app
3. **Update order status** via Android app
4. **Verify:** Changes appear instantly on admin/desktop app

---

## Phase 7: Clean Up Mock Files (Optional)

### 7.1 Remove Mock Files
- [ ] Delete `MockAuthRepository.kt`
- [ ] Delete `MockOrderRepository.kt`
- [ ] Delete `chef_nav_menu.xml` (if not needed)
- [ ] Delete `waiter_nav_menu.xml` (if not needed)

### 7.2 Remove Debug Logging
- [ ] Remove `println("DEBUG: ...")` statements from:
  - `AuthViewModel.kt`
  - `OrdersViewModel.kt`
  - `LoginActivity.kt`
  - `AuthRepository.kt`

---

## Troubleshooting

### Common Issues

**1. "Not Configured" Error**
- Verify `google-services.json` is in `app/` directory
- Check Firebase Console setup
- Verify SHA-1 fingerprint is added

**2. Authentication Fails**
- Check Firebase Console → Authentication
- Verify user exists in Authentication
- Check Firestore user document exists

**3. Orders Don't Load**
- Check Firestore rules allow authenticated users
- Verify Firestore database exists
- Check network connectivity

**4. Realtime Updates Don't Work**
- Verify Firestore listeners are properly set up
- Check Firestore rules
- Ensure app has internet connection

### Debug Steps
1. **Check Android Studio logs** for Firebase errors
2. **Verify Firebase Console** shows active users
3. **Test Firestore queries** in Firebase Console
4. **Check network connectivity** on device

---

## Success Criteria

After restoration:
- ✅ Login works with Firebase Authentication
- ✅ Orders load from Firestore in real-time
- ✅ Order actions update Firestore immediately
- ✅ Realtime updates work between devices
- ✅ No Firebase errors in logs
- ✅ App works with internet connection
- ✅ Offline persistence works (if enabled)

---

## Files Modified During Restoration

**Authentication:**
- `AuthViewModel.kt` - Restore Firebase repository
- `AuthRepository.kt` - Restore Firebase implementation

**Order Data:**
- `OrdersViewModel.kt` - Restore realtime listeners
- `OrderRepository.kt` - Restore Firestore queries

**Fragments:**
- `ChefNewOrdersFragment.kt` - Restore realtime listener
- `ChefPreparingFragment.kt` - Restore realtime listener
- `WaiterReadyFragment.kt` - Restore realtime listener

**Application:**
- `StreetFeastApplication.kt` - Re-enable Firebase
- `AndroidManifest.xml` - Restore Firebase services

**Cleanup:**
- Delete mock repository files
- Remove debug logging
- Remove mock navigation menus (optional)

The app is now fully restored to Firebase functionality!
