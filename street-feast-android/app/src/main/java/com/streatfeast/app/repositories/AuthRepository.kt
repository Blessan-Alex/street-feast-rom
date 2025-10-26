package com.streatfeast.app.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.streatfeast.app.models.User
import com.streatfeast.app.models.UserRole
import com.streatfeast.app.utils.Constants
import kotlinx.coroutines.tasks.await

class AuthRepository {
    
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    /**
     * Get current user
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean = auth.currentUser != null
    
    /**
     * Login with email and password
     */
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            // Authenticate with Firebase
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("User not found"))
            
            println("DEBUG: Firebase Auth successful for user: ${firebaseUser.uid}")
            
            // TEMPORARY: Skip Firestore fetch and create default user to test navigation
            println("DEBUG: Creating default user (skipping Firestore fetch)")
            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: "Test User",
                role = UserRole.CHEF
            )
            
            println("DEBUG: User object created: $user")
            Result.success(user)
            
            /* ORIGINAL CODE WITH FIRESTORE FETCH (commented out for debugging):
            // Get user data from Firestore
            println("DEBUG: Fetching user data from Firestore...")
            val userDoc = db.collection(Constants.USERS_COLLECTION)
                .document(firebaseUser.uid)
                .get()
                .await()
            
            println("DEBUG: Firestore fetch completed")
            
            val user = userDoc.toObject(User::class.java)?.copy(id = firebaseUser.uid)
                ?: User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "",
                    role = UserRole.CHEF
                )
            
            println("DEBUG: User object created: $user")
            Result.success(user)
            */
        } catch (e: Exception) {
            println("DEBUG: Login error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Logout current user
     */
    fun logout() {
        auth.signOut()
    }
    
    /**
     * Get user role from Firestore
     */
    suspend fun getUserRole(userId: String): Result<UserRole> {
        return try {
            val userDoc = db.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            val roleString = userDoc.getString("role") ?: "chef"
            val role = UserRole.fromString(roleString)
            
            Result.success(role)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


