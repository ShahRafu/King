package com.shahrafuking.kingassistant.security

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.time.Instant

/**
 * ConsentManager
 *
 * Stores the owner's consent for using active illumination (flash or screen pulse) in a
 * one-time persistent encrypted preference. Consent can be revoked in Security settings.
 */
class ConsentManager(private val context: Context) {
    private val TAG = "ConsentManager"
    private val PREFS_NAME = "consent_prefs"
    private val KEY_ILLUMINATION_CONSENT = "illumination_consent"

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val prefs = EncryptedSharedPreferences.create(
        PREFS_NAME,
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun hasIlluminationConsent(): Boolean {
        return prefs.contains(KEY_ILLUMINATION_CONSENT)
    }

    fun setIlluminationConsent(consented: Boolean) {
        prefs.edit().putBoolean(KEY_ILLUMINATION_CONSENT, consented).apply()
        Log.i(TAG, "Illumination consent set=$consented at ${Instant.now()}")
    }

    fun getIlluminationConsent(): Boolean {
        return prefs.getBoolean(KEY_ILLUMINATION_CONSENT, false)
    }

    fun revokeConsent() {
        prefs.edit().remove(KEY_ILLUMINATION_CONSENT).apply()
        Log.i(TAG, "Illumination consent revoked at ${Instant.now()}")
    }
}
