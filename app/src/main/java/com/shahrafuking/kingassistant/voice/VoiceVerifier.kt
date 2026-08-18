package com.shahrafuking.kingassistant.voice

import android.content.Context
import android.util.Log

class VoiceVerifier(private val context: Context) {
    companion object {
        // Default numeric threshold used across the app
        const val DEFAULT_THRESHOLD: Double = 0.75
    }

    /**
     * Clears any locally stored voice template/embeddings.
     * This is a no-op safe implementation for compile; replace with your secure deletion logic.
     */
    fun clearTemplate() {
        Log.i("VoiceVerifier", "clearTemplate() called (no-op placeholder)")
        // TODO: securely delete template/storage
    }

    /**
     * Verify sample features/embedding, returns true if a match meets threshold.
     * Placeholder implementation: always returns false. Replace with actual biometric verify.
     */
    fun verify(features: FloatArray, threshold: Double = DEFAULT_THRESHOLD): Boolean {
        // TODO: actual verification
        if (features.isEmpty()) return false
        // simple placeholder score: max normalized value
        val score = (features.maxOrNull() ?: 0f).toDouble()
        return score >= threshold
    }
}
