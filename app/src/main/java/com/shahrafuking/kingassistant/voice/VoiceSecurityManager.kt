package com.shahrafuking.kingassistant.voice

import android.content.Context
import android.util.Log
import com.shahrafuking.kingassistant.memory.MemoryRepository
import com.shahrafuking.kingassistant.memory.MemoryDatabase
import com.shahrafuking.kingassistant.security.KeystoreHelper
import com.shahrafuking.kingassistant.voice.preproc.AntiSpoofPreprocessor
import com.shahrafuking.kingassistant.voice.tflite.SpeakerVerifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.abs

/**
 * VoiceSecurityManager (strict unlock flow)
 * - Stores encrypted owner template in Keystore
 * - Verifies spoken challenge text via ASR transcript matching
 * - Verifies biometric via TFLite speaker verifier (or fallback)
 * - Verifies liveness via anti-spoof preprocessor or heuristics
 */
class VoiceSecurityManager(private val context: Context,
                           private val modelAssetPath: String? = null,
                           private val modelFilePath: String? = null,
                           private val antiSpoofAssetPath: String? = null) {
    private val TAG = "VoiceSecurityManager"
    private val storageKey = "voice_template_key_v3"
    private val db = MemoryDatabase.getInstance(context)
    private val repo = MemoryRepository(db.memoryDao())
    private val speakerVerifier = SpeakerVerifier(context, modelAssetPath, modelFilePath, antiSpoofAssetPath)

    // thresholds
    var similarityThreshold = 0.78f
    var livenessThreshold = 0.5f

    fun enroll(embedding: FloatArray, onComplete: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val embBase64 = MemoryDatabase.floatArrayToBase64(embedding)
                val obj = JSONObject()
                obj.put("v", embBase64)
                obj.put("t", System.currentTimeMillis())
                val ok = KeystoreHelper.encryptString(context, obj.toString(), storageKey)
                try { repo.addMemory("voice_enrollment_template", embedding, mapOf("type" to "enrollment", "ts" to System.currentTimeMillis().toString())) } catch (_: Throwable) {}
                onComplete(ok)
            } catch (t: Throwable) {
                Log.w(TAG, "enroll error", t)
                onComplete(false)
            }
        }
    }

    fun clearEnrollment(onComplete: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val ok = KeystoreHelper.clear(context, storageKey)
            try { repo.addMemory("voice_enrollment_cleared", null, mapOf("type" to "enrollment_cleared", "ts" to System.currentTimeMillis().toString())) } catch (_: Throwable) {}
            onComplete(ok)
        }
    }

    private fun loadTemplate(): FloatArray? {
        return try {
            val json = KeystoreHelper.decryptString(context, storageKey) ?: return null
            val obj = JSONObject(json)
            val b64 = obj.optString("v", "")
            MemoryDatabase.base64ToFloatArray(b64)
        } catch (t: Throwable) {
            Log.w(TAG, "loadTemplate error", t)
            null
        }
    }

    private fun computeLivenessFromPcm(pcm: ShortArray, sampleRate: Int): Float {
        // Use AntiSpoofPreprocessor to compute mel mean vector and optionally pass to antispoof model
        val mel = AntiSpoofPreprocessor.preprocessFromShorts(pcm, sampleRate)
        if (mel == null) return 0f
        // basic heuristic: energy proxy = mean absolute of mel (lower -> suspicious), we invert appropriately
        var meanAbs = 0f
        for (v in mel) meanAbs += kotlin.math.abs(v)
        meanAbs /= mel.size
        // normalize to 0..1 by a simple sigmoid-like map
        val score = (1.0f / (1.0f + kotlin.math.exp(-(meanAbs - 0.0f)))).coerceIn(0f, 1f)
        return score
    }

    // Primary combined check: verifies both content match (asrTranscript vs challengeText) and speaker biometric
    fun verifyChallenge(challengeText: String, asrTranscript: String, featureEmbedding: FloatArray, rawPcm: ShortArray?, sampleRate: Int = 16000, onResult: (Boolean, Float, Float, Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val normExpected = normalizeText(challengeText)
                val normHeard = normalizeText(asrTranscript)
                val textMatch = normHeard.contains(normExpected) || normExpected.contains(normHeard)
                if (!textMatch) {
                    onResult(false, 0f, 0f, false)
                    return@launch
                }
                // liveness
                val liveScore = rawPcm?.let { computeLivenessFromPcm(it, sampleRate) } ?: 0f
                if (liveScore < livenessThreshold) {
                    onResult(false, 0f, liveScore, false); return@launch
                }
                // biometric: compare featureEmbedding to stored template
                val stored = loadTemplate()
                if (stored == null) { onResult(false, 0f, liveScore, false); return@launch }
                val sim = speakerVerifier.cosineSimilarity(stored, featureEmbedding)
                val passed = sim >= similarityThreshold
                // log to memory
                try { repo.addMemory("challenge_verification", featureEmbedding, mapOf("similarity" to sim.toString(), "liveness" to liveScore.toString(), "textMatch" to textMatch.toString(), "passed" to passed.toString(), "challenge" to challengeText)) } catch (_: Throwable) {}
                onResult(passed, sim, liveScore, textMatch)
            } catch (t: Throwable) {
                Log.w(TAG, "verifyChallenge error", t)
                onResult(false, 0f, 0f, false)
            }
        }
    }

    private fun normalizeText(s: String): String {
        return s.lowercase().replace(Regex("[^\p{L}\p{N}\s]"), " ").trim()
    }

    fun close() { try { speakerVerifier.close() } catch (_: Throwable) {} }
}
