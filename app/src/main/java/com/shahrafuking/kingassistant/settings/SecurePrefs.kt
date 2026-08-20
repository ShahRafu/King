package com.shahrafuking.kingassistant.settings

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SecurePrefs(private val ctx: Context) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val prefs = EncryptedSharedPreferences.create(
        "king_secure_prefs",
        masterKeyAlias,
        ctx,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        const val KEY_BACKEND_TOKEN = "backend_token"
    }

    fun setToken(token: String) {
        prefs.edit().putString(KEY_BACKEND_TOKEN, token).apply()
    }

    fun getToken(): String {
        return prefs.getString(KEY_BACKEND_TOKEN, "") ?: ""
    }

    fun tokenFlow(): Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_BACKEND_TOKEN) trySend(getToken())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getToken())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
