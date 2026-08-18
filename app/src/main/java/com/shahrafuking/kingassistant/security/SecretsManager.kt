package com.shahrafuking.kingassistant.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * SecretsManager
 * - Simple wrapper around EncryptedSharedPreferences for API keys / secrets
 */
class SecretsManager(private val ctx: Context) {
    private val masterKeyAlias = MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val prefsName = "king_secrets"

    private val prefs by lazy {
        EncryptedSharedPreferences.create(
            ctx,
            prefsName,
            masterKeyAlias,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun putSecret(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getSecret(key: String): String? {
        return prefs.getString(key, null)
    }

    fun removeSecret(key: String) {
        prefs.edit().remove(key).apply()
    }
}
