package com.streatfeast.app.di

import android.content.Context
import android.util.Log
import com.streatfeast.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import com.streatfeast.app.auth.SharedPrefsSessionManager
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseModule {

    @Volatile
    private var supabaseClient: SupabaseClient? = null

    fun provideClient(context: Context): SupabaseClient {
        supabaseClient?.let { return it }

        require(BuildConfig.SUPABASE_URL.isNotBlank()) {
            "SUPABASE_URL is not configured. Set it in local.properties."
        }
        require(BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
            "SUPABASE_ANON_KEY is not configured. Set it in local.properties."
        }

        val appContext = context.applicationContext

        val client = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                sessionManager = SharedPrefsSessionManager(appContext)
                alwaysAutoRefresh = true
                autoSaveToStorage = true
                autoLoadFromStorage = true
            }
            install(Postgrest)
            install(Realtime)
        }

        Log.d("SupabaseModule", "Supabase client created (Auth installed)")
        supabaseClient = client
        return client
    }

    suspend fun clear() {
        supabaseClient?.close()
        supabaseClient = null
    }
}



