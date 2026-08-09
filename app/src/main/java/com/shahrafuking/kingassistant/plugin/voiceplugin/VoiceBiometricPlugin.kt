
package com.shahrafuking.kingassistant.plugin.voiceplugin

import android.content.Context
import android.util.Log
import com.shahrafuking.kingassistant.voice.VoiceVerifier

/**
 * VoiceBiometricPlugin
 *
 * Simple placeholder adapter for voice biometric engines.
 * Purpose: provide a consistent place to plug in different implementations (TFLite on-device encoder,
 * server‑side verifier, or 3rd-party SDK).
 *
 * Current file is a minimal local adapter that uses the existing VoiceVerifier as the backend.
 * Replace or extend this class to integrate a proper speaker embedding model or remote verification.
 */

interface VoiceBiometricPlugin {
    suspend fun enrollSample(context: Context): Boolean
    suspend fun verifySample(context: Context): Boolean
    fun clearTemplate(context: Context)
}

/**
 * BasicLocalPlugin: prototype implementation that delegates to existing VoiceEnrollmentManager & VoiceVerifier.
 * Keep this as a reference; replace with real model integration later.
 */
class BasicLocalPlugin : VoiceBiometricPlugin {
    private val TAG = "BasicLocalPlugin"

    override suspend fun enrollSample(context: Context): Boolean {
        // This method should orchestrate recording multiple samples, feature extraction and saving template.
        // For now delegate to higher-level UI flow (EnrollmentActivity) — placeholder stub.
        Log.i(TAG, "BasicLocalPlugin.enrollSample() called — placeholder (use EnrollmentActivity flow).")
        return false
    }

    override suspend fun verifySample(context: Context): Boolean {
        // Placeholder: delegate to VoiceVerifier if you have builtin sample extraction elsewhere.
        // Return false to indicate not-implemented here.
        Log.i(TAG, "BasicLocalPlugin.verifySample() called — placeholder.")
        return false
    }

    override fun clearTemplate(context: Context) {
        try {
            val v = VoiceVerifier(context)
            v.clearTemplate()
            Log.i(TAG, "Template cleared by plugin.")
        } catch (t: Throwable) {
            Log.w(TAG, "clearTemplate error", t)
        }
    }
}
