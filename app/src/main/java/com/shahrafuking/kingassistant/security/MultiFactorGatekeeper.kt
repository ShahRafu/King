package com.shahrafuking.kingassistant.security

import android.app.Activity
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MultiFactorGatekeeper
 *
 * Orchestrates authentication flows: NORMAL (voice-only) and CRITICAL (voice + iris + lip-sync).
 * This is a scaffolding implementation: verifiers are mocked/stubbed to allow immediate UX testing.
 */
class MultiFactorGatekeeper(private val activity: Activity) {
    private val TAG = "MultiFactorGatekeeper"

    enum class AuthLevel { NORMAL, CRITICAL }

    data class AuthResult(val approved: Boolean, val details: String?)

    private val voice = com.shahrafuking.kingassistant.selfheal.VoiceAuthGatekeeper(activity)
    private val voiceAnalyzer = VoiceLivenessAnalyzer(activity)
    private val irisVerifier = IrisRobustVerifier(activity)
    private val lipSync = LipSyncVerifier(activity)

    /**
     * Request owner approval. For NORMAL level this performs voice-only verification.
     * For CRITICAL level it performs voice + iris + lip-sync verification in a synchronized flow.
     */
    suspend fun requestOwnerApproval(prompt: String, level: AuthLevel = AuthLevel.NORMAL): AuthResult = withContext(Dispatchers.Main) {
        Log.i(TAG, "requestOwnerApproval(level=$level)")

        // Level 1: voice only
        if (level == AuthLevel.NORMAL) {
            val ok = voice.requestOwnerApproval(prompt)
            return@withContext AuthResult(ok, if (ok) "voice_ok" else "voice_denied")
        }

        // Level 2: critical — orchestrate synchronized capture
        // Present challenge via voice gatekeeper and simultaneously capture camera frames in the HighSecurityApprovalActivity UI
        // For scaffolding, we call the components sequentially but the UI will surface synchronized capture behavior.

        val voiceOk = voice.requestOwnerApproval(prompt)
        if (!voiceOk) {
            return@withContext AuthResult(false, "voice_denied")
        }

        // Run voice liveness / anti-spoof analysis (mock)
        val vres = voiceAnalyzer.verifyLiveVoice()
        if (!vres.success) {
            return@withContext AuthResult(false, "voice_liveness_failed: ${vres.reason}")
        }

        // Capture camera frames (HighSecurityApprovalActivity is expected to capture and store them temporarily)
        // For this scaffold, call iris and lipsync verifiers which will internally (mock) request frames if needed.
        val lip = lipSync.verifyLipSync()
        if (!lip.success) {
            return@withContext AuthResult(false, "lip_sync_failed: ${lip.reason}")
        }

        val ires = irisVerifier.verifyIrisCapture()
        if (!ires.success) {
            return@withContext AuthResult(false, "iris_failed: ${ires.reason}")
        }

        // All passed
        return@withContext AuthResult(true, "voice+lip+iris_ok")
    }
}
