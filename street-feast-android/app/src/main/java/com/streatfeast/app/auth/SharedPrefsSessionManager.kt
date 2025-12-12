package com.streatfeast.app.auth

import android.content.Context
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class SharedPrefsSessionManager(
    context: Context,
    private val prefsName: String = "supabase_auth",
    private val key: String = "user_session",
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) : SessionManager {

    private val prefs = context.applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    override suspend fun saveSession(session: UserSession) {
        val encoded = json.encodeToString(session)
        prefs.edit().putString(key, encoded).apply()
    }

    override suspend fun loadSession(): UserSession? {
        val raw = prefs.getString(key, null) ?: return null
        return runCatching { json.decodeFromString<UserSession>(raw) }.getOrNull()
    }

    override suspend fun deleteSession() {
        prefs.edit().remove(key).apply()
    }
}

