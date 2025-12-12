package com.streatfeast.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.jan.supabase.SupabaseClient

class SessionViewModelFactory(
    private val supabase: SupabaseClient
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SessionViewModel::class.java))
        return SessionViewModel(supabase) as T
    }
}

