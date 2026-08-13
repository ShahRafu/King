package com.shahrafuking.kingassistant.security

import android.app.Activity
import android.util.Log

/**
 * Updated MultiFactorGatekeeper to use real VoiceLivenessAnalyzer and LipSyncVerifier implementations.
 */
class MultiFactorGatekeeper(private val activity: Activity) {
    private val TAG = "MultiFactorGatekeeper"

    enum class AuthLevel { NORMAL, CRITICAL }

    data class AuthResult(val approved: Boolean, val details: String?)

    private val voice = com.shahrafuking.kingassistant.selfheal.VoiceAuthGatekeeper(activity)
    private val voiceAnalyzer = VoiceLivenessAnalyzer(activity)
    private val irisVerifier = IrisRobustVerifier(activity)
    private val lipSync = LipSyncVerifier(activity)

    suspend fun requestOwnerApproval(prompt: String, level: AuthLevel = AuthLevel.NORMAL): AuthResult {
        Log.i(TAG, "requestOwnerApproval(level=$level)")

        if (level == AuthLevel.NORMAL) {
            val ok = voice.requestOwnerApproval(prompt)
            return AuthResult(ok, if (ok) "voice_ok" else "voice_denied")
        }

        // CRITICAL flow
        val voiceOk = voice.requestOwnerApproval(prompt)
        if (!voiceOk) return AuthResult(false, "voice_denied")

        // Voice liveness (real)
        val vres = voiceAnalyzer.verifyLiveVoice()
        if (!vres.success) return AuthResult(false, "voice_liveness_failed:${vres.reason}")

        // Lip-sync (real) — requires an Activity; using the provided activity to capture
        val lres = lipSync.verifyLipSync(activity)
        if (!lres.success) return AuthResult(false, "lip_sync_failed:${lres.reason}")

        // Iris (existing verifier)
        val ires = irisVerifier.verifyIrisCapture()
        if (!ires.success) return AuthResult(false, "iris_failed:${ires.reason}")

        return AuthResult(true, "voice+lip+iris_ok")
    }
}
