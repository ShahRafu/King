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
     */
    fun clearTemplate() {
        Log.i("VoiceVerifier", "clearTemplate() called (no-op placeholder)")
        // TODO: securely delete template/storage
    }

    /**
     * Verify using FloatArray features (existing).
     */
    fun verify(features: FloatArray, threshold: Double = DEFAULT_THRESHOLD): Boolean {
        if (features.isEmpty()) return false
        val score = (features.maxOrNull() ?: 0f).toDouble()
        return score >= threshold
    }

    /**
     * Verify using DoubleArray features (recordAndExtract returns DoubleArray).
     */
    fun verify(features: DoubleArray, threshold: Double = DEFAULT_THRESHOLD): Boolean {
        if (features.isEmpty()) return false
        val score = features.maxOrNull() ?: 0.0
        return score >= threshold
    }
}
