package com.shahrafuking.kingassistant.security

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class VerificationActivity : AppCompatActivity() {
    companion object {
        const val DEFAULT_THRESHOLD = 0.75f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    /**
     * Simple verify helper. Replace with your real verification signature as required.
     * Returns true when the score meets or exceeds the threshold.
     */
    fun verify(features: FloatArray, threshold: Float = DEFAULT_THRESHOLD): Boolean {
        if (features.isEmpty()) return false
        val score = features.maxOrNull() ?: 0.0f
        return score >= threshold
    }
}
