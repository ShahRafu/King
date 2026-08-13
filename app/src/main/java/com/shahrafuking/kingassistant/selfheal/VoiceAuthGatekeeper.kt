package com.shahrafuking.kingassistant.selfheal

import android.app.Activity
import android.content.Context
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/**
 * VoiceAuthGatekeeper
 *
 * Provides a simple voice-confirmation API to require owner approval before applying
 * any staged code modifications. This is a conservative, on-device guard: before a
 * change is written to persistent storage the app should call requestOwnerApproval
 * and require a positive voice confirmation result.
 *
 * This implementation delegates to an existing LivenessManager and uses a simple
 * challenge/response flow. Replace or extend with your own speaker verification
 * and anti-spoof checks as needed.
 */
class VoiceAuthGatekeeper(private val activity: Activity) {
    private val TAG = "VoiceAuthGatekeeper"
    private val liveness = com.shahrafuking.kingassistant.security.LivenessManager(activity)

    suspend fun requestOwnerApproval(prompt: String, timeoutMs: Long = 15000): Boolean = withContext(Dispatchers.Main) {
        Log.i(TAG, "requestOwnerApproval: $prompt")
        val challenge = liveness.generateChallenge()
        // In a production flow: speak the prompt + challenge via TTS and ask owner to repeat.
        // For now we use the challenge phrase as the required spoken response.

        // Show TTS / UI prompt here (left to app UI to present).

        // Start challenge and await result
        return@withContext kotlinx.coroutines.suspendCancellableCoroutine<Boolean> { cont ->
            try {
                liveness.startChallenge(challenge) { success ->
                    cont.resume(success) {}
                }
            } catch (t: Throwable) {
                Log.w(TAG, "voice auth failed", t)
                cont.resume(false) {}
            }
        }
    }
}
