package com.streatfeast.app.repositories

import android.util.Log
import com.streatfeast.app.models.User
import com.streatfeast.app.models.UserRole
import com.streatfeast.app.network.SupabaseUserDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val client: SupabaseClient
) {

    suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        val user = client.auth.currentUserOrNull() ?: return@withContext null
        fetchUser(user.id)
    }

    suspend fun isLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        client.auth.currentUserOrNull() != null
    }

    suspend fun login(email: String, password: String): Result<User> = runCatching {
        Log.d("AuthRepository", "Attempting login for: $email")
        
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        
        Log.d("AuthRepository", "Auth sign in successful")
        
        val userId = client.auth.currentUserOrNull()?.id
        Log.d("AuthRepository", "Got user ID from auth: $userId")
        
        if (userId == null) {
            Log.e("AuthRepository", "User ID is null after sign in")
            error("Missing user id")
        }
        
        Log.d("AuthRepository", "Fetching user profile for ID: $userId")
        val user = fetchUser(userId)
        Log.d("AuthRepository", "Fetched user result: ${if (user != null) "SUCCESS - ${user.email}, role: ${user.role}" else "NULL"}")

        user ?: error("User profile missing")
    }.onFailure { e ->
        Log.e("AuthRepository", "Login failed", e)
        Log.e("AuthRepository", "Error message: ${e.message}")
        Log.e("AuthRepository", "Error type: ${e.javaClass.simpleName}")
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        client.auth.signOut()
    }

    suspend fun getUserRole(userId: String): Result<UserRole> = runCatching {
        fetchUser(userId)?.role ?: UserRole.CHEF
    }

    suspend fun registerDevice(playerId: String) = withContext(Dispatchers.IO) {
        client.postgrest.rpc("register_device", mapOf("player_id" to playerId))
    }

    private suspend fun fetchUser(userId: String): User? = withContext(Dispatchers.IO) {
        Log.d("AuthRepository", "fetchUser: Querying users table for ID: $userId")
        
        try {
            val dto = client.postgrest["users"]
                .select {
                    filter { eq("id", userId) }
                    limit(1)
                }
                .decodeList<SupabaseUserDto>()
            
            Log.d("AuthRepository", "fetchUser: Query returned ${dto.size} result(s)")
            
            val userDto = dto.firstOrNull()
            if (userDto != null) {
                Log.d("AuthRepository", "fetchUser: Found user - email: ${userDto.email}, role: ${userDto.role}")
                userDto.toUser()
            } else {
                Log.w("AuthRepository", "fetchUser: No user found in database for ID: $userId")
                null
            }
        } catch (e: CancellationException) {
            // This is expected when ViewModel is cleared during activity destruction
            // Don't log as error, just rethrow to allow proper cancellation
            throw e
        } catch (e: Exception) {
            Log.e("AuthRepository", "fetchUser: Error querying users table", e)
            Log.e("AuthRepository", "fetchUser: Error message: ${e.message}")
            null
        }
    }

    private fun SupabaseUserDto.toUser(): User = User(
        id = id,
        email = email ?: "",
        displayName = displayName ?: email ?: "",
        role = UserRole.fromString(role?.trim()?.lowercase() ?: "chef"),
        deviceTokens = emptyList()
    )
}
