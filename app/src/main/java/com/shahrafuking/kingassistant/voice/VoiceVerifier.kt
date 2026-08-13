package com.shahrafuking.kingassistant.voice

import android.content.Context
import android.util.Base64
import com.shahrafuking.kingassistant.security.KeystoreHelper
import java.security.MessageDigest

/**
 * VoiceVerifier: very small on-device verifier that compares a stored enrollment template
 * (SHA-256 of enrollment PCM in EnrollmentActivity) with a new PCM sample.
 *
 * This intentionally mirrors EnrollmentActivity's simplistic template approach used for POC.
 * Replace with ML embedding-based verification for production.
 */
object VoiceVerifier {
    private const val TEMPLATE_KEY = "voice_enrollment_template_v1"

    fun loadTemplate(context: Context): String? = KeystoreHelper.decryptString(context, TEMPLATE_KEY)

    fun clearTemplate(context: Context) {
        KeystoreHelper.clearStoredValue(context, TEMPLATE_KEY)
    }

    /**
     * Verify by taking raw PCM bytes and comparing SHA-256(base64) equality with stored template.
     */
    fun verifyPcm(context: Context, pcmBytes: ByteArray): Boolean {
        val stored = loadTemplate(context) ?: return false
        val sha = MessageDigest.getInstance("SHA-256").digest(pcmBytes)
        val b64 = Base64.encodeToString(sha, Base64.NO_WRAP)
        return stored == b64
    }
}
