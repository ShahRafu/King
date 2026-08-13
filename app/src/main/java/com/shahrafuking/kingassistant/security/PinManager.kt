package com.shahrafuking.kingassistant.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * PinManager
 *
 * Stores a user PIN securely using AndroidX EncryptedSharedPreferences. Provides simple
 * set/verify functions used to gate promotions/commits.
 */
class PinManager(context: Context) {
    companion object {
        private const val PREFS_NAME = "pin_prefs"
        private const val KEY_PIN = "owner_pin"
    }

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val prefs = EncryptedSharedPreferences.create(
        PREFS_NAME,
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN, pin).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val stored = prefs.getString(KEY_PIN, null) ?: return false
        return stored == pin
    }

    fun hasPin(): Boolean = prefs.contains(KEY_PIN)
}
