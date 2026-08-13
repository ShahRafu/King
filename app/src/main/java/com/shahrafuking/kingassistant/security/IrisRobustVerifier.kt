package com.shahrafuking.kingassistant.security

import android.content.Context
import android.util.Log

/**
 * IrisRobustVerifier
 *
 * Scaffolding for robust iris verification. Production implementation should perform
 * multi-frame fusion, low-light enhancement, anti-spoof checks, and iris embeddings.
 */
class IrisRobustVerifier(private val context: Context) {
    private val TAG = "IrisRobustVerifier"

    data class IrisResult(val success: Boolean, val score: Float, val reason: String?)

    fun enrollIrisSamples(frames: List<ByteArray>): Boolean {
        Log.i(TAG, "enrollIrisSamples (mock) count=${frames.size}")
        return true
    }

    fun verifyIrisCapture(): IrisResult {
        Log.i(TAG, "verifyIrisCapture (mock) called")
        // Mock success
        return IrisResult(true, 0.93f, null)
    }
}
