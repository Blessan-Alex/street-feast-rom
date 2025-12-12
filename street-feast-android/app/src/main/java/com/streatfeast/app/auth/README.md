# SharedPrefsSessionManager

A custom `SessionManager` implementation for Supabase authentication that persists user sessions using Android's `SharedPreferences`.

## Why?

- **Process Death Resilience**: Sessions survive app process death and force stops
- **Simple & Reliable**: Uses Android's native SharedPreferences (no external dependencies)
- **SDK Compatible**: Implements the official `SessionManager` interface from `supabase-kt`

## Usage

Configured in `SupabaseModule`:

```kotlin
install(Auth) {
    sessionManager = SharedPrefsSessionManager(appContext)
    alwaysAutoRefresh = true
    autoSaveToStorage = true
    autoLoadFromStorage = true
}
```

## How It Works

- **Storage**: Saves sessions to `SharedPreferences` with key `"user_session"` in file `"supabase_auth"`
- **Serialization**: Uses Kotlinx Serialization JSON to encode/decode `UserSession` objects
- **Error Handling**: Returns `null` if session data is corrupted or missing

## Defaults

- Preferences file: `"supabase_auth"`
- Session key: `"user_session"`
- JSON config: Ignores unknown keys, encodes defaults

All parameters are customizable via constructor.

