package com.streatfeast.app.di

import android.content.Context
import com.streatfeast.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseModule {

    @Volatile
    private var supabaseClient: SupabaseClient? = null

    fun provideClient(context: Context): SupabaseClient {
        val existing = supabaseClient
        if (existing != null) return existing

        require(BuildConfig.SUPABASE_URL.isNotBlank()) {
            "SUPABASE_URL is not configured. Set it in local.properties."
        }
        require(BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
            "SUPABASE_ANON_KEY is not configured. Set it in local.properties."
        }

        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                alwaysAutoRefresh = true
                autoSaveToStorage = true
            }
            install(Postgrest)
            install(Realtime)
        }.also { client ->
            supabaseClient = client
        }
    }

    suspend fun clear() {
        supabaseClient?.close()
        supabaseClient = null
    }
}



