package com.streatfeast.app.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SessionUiState(
    val isLoading: Boolean = true,
    val isLoggedIn: Boolean = false
)

class SessionViewModel(
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                // Ensure session is loaded from storage
                supabase.auth.awaitInitialization()
                Log.d("Auth", "sessionRestored=${supabase.auth.currentSessionOrNull() != null}")
                Log.d("SessionViewModel", "Auth initialization completed")

                val hasSession = supabase.auth.currentSessionOrNull() != null
                val hasUser = supabase.auth.currentUserOrNull() != null
                Log.d("SessionViewModel", "Has session: $hasSession, has user: $hasUser")

                _uiState.value = SessionUiState(
                    isLoading = false,
                    isLoggedIn = hasSession
                )
            } catch (e: Exception) {
                Log.e("SessionViewModel", "Error initializing auth/session", e)
                _uiState.value = SessionUiState(
                    isLoading = false,
                    isLoggedIn = false
                )
            }
        }
    }
}

