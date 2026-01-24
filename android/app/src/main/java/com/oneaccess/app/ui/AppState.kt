package com.oneaccess.app.ui

import android.content.Context
import android.provider.Settings
import androidx.core.content.edit

object AppState {
    private const val PREFS = "oneaccess_prefs"
    private const val KEY_BACKEND = "backend_url"
    private const val KEY_EMAIL = "email"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_GATE_ID = "gate_id"

    fun deviceId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN"

    fun backendUrl(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_BACKEND, "http://10.0.2.2:8000")!! // emulator default

    fun setBackendUrl(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(KEY_BACKEND, value.trim()) }
    }

    fun email(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_EMAIL, "alice@acme.com")!!

    fun setEmail(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(KEY_EMAIL, value.trim()) }
    }

    fun accessToken(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ACCESS_TOKEN, null)

    fun setAccessToken(context: Context, value: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            if (value == null) remove(KEY_ACCESS_TOKEN) else putString(KEY_ACCESS_TOKEN, value)
        }
    }

    fun gateId(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_GATE_ID, "MAIN_GATE")!!

    fun setGateId(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(KEY_GATE_ID, value.trim()) }
    }
}

