package com.shahrafuking.kingassistant.security

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.shahrafuking.kingassistant.capture.AudioCaptureHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * VoiceLivenessAnalyzer
 *
 * Production implementation that uses a speaker embedding TF-Lite model and an anti-spoof TF-Lite model
 * to compute a streaming liveness decision. It supports enrollment (compute centroid embedding) and
 * verification against the stored centroid.
 */
class VoiceLivenessAnalyzer(private val context: Context) {
    private val TAG = "VoiceLivenessAnalyzer"
    private val PREFS_NAME = "liveness_prefs"
    private val KEY_SPEAKER_CENTROID = "speaker_centroid"

    private val audioHelper = AudioCaptureHelper()
    private val speakerInterpreter: Interpreter? = ModelLoader.loadInterpreter(context, "speaker_embedder.tflite", 2)
    private val antispoofInterpreter: Interpreter? = ModelLoader.loadInterpreter(context, "voice_antispoof.tflite", 2)

    private val prefs by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    data class VoiceResult(val success: Boolean, val score: Float, val reason: String?)

    /**
     * Verify live voice by recording a short snippet and running the speaker embedder + anti-spoof model.
     * Returns a VoiceResult containing a combined score (average of similarity and liveness probability).
     */
    suspend fun verifyLiveVoice(durationMs: Long = 1500, similarityThreshold: Float = 0.60f, livenessThreshold: Float = 0.60f): VoiceResult = withContext(Dispatchers.IO) {
        try {
            if (speakerInterpreter == null || antispoofInterpreter == null) {
                Log.e(TAG, "TFLite interpreters not initialized")
                return@withContext VoiceResult(false, 0f, "models_missing")
            }

            val audio = audioHelper.recordForDurationMs(durationMs)

            val emb = computeEmbedding(audio)
            if (emb == null) {
                return@withContext VoiceResult(false, 0f, "embed_fail")
            }

            val liveProb = computeLiveness(audio)

            val centroid = loadStoredCentroid()
            if (centroid == null) {
                return@withContext VoiceResult(false, 0f, "no_enrollment")
            }

            val similarity = cosineSimilarity(emb, centroid)
            val success = similarity >= similarityThreshold && liveProb >= livenessThreshold
            val combined = (similarity + liveProb) / 2.0f
            val reason = when {
                !success && similarity < similarityThreshold -> "speaker_mismatch"
                !success && liveProb < livenessThreshold -> "anti_spoof_failed"
                else -> null
            }
            Log.i(TAG, "verifyLiveVoice: similarity=$similarity liveProb=$liveProb combined=$combined reason=$reason")
            return@withContext VoiceResult(success, combined, reason)
        } catch (e: Exception) {
            Log.e(TAG, "verifyLiveVoice failed", e)
            return@withContext VoiceResult(false, 0f, "exception:${e.message}")
        }
    }

    /**
     * Enroll a set of raw PCM samples (byte arrays). Computes per-sample embeddings and stores
     * their centroid encrypted in preferences.
     */
    suspend fun enrollVoiceSamples(samples: List<ByteArray>): Boolean = withContext(Dispatchers.IO) {
        try {
            if (speakerInterpreter == null) {
                Log.e(TAG, "speaker interpreter missing during enrollment")
                return@withContext false
            }
            val embeddings = ArrayList<FloatArray>()
            for (s in samples) {
                val e = computeEmbedding(s)
                if (e != null) embeddings.add(e)
            }
            if (embeddings.isEmpty()) {
                Log.e(TAG, "no embeddings produced during enrollment")
                return@withContext false
            }
            // compute centroid
            val dim = embeddings[0].size
            val centroid = FloatArray(dim)
            for (i in 0 until dim) {
                var sum = 0.0f
                for (e in embeddings) sum += e[i]
                centroid[i] = sum / embeddings.size
            }
            storeCentroid(centroid)
            Log.i(TAG, "enrollment complete: stored centroid dim=${centroid.size}")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "enrollVoiceSamples failed", e)
            return@withContext false
        }
    }

    private fun computeEmbedding(audioBytes: ByteArray): FloatArray? {
        try {
            val interp = speakerInterpreter ?: return null
            val inputTensor = interp.getInputTensor(0)
            val inputShape = inputTensor.shape() // e.g. [1, N]
            val batch = inputShape[0]
            val req = if (inputShape.size >= 2) inputShape[1] else inputShape[0]

            // Convert PCM16 little-endian to float [-1,1]
            val shortCount = audioBytes.size / 2
            val audioFloats = FloatArray(shortCount)
            var j = 0
            var i = 0
            while (i + 1 < audioBytes.size && j < shortCount) {
                val lo = audioBytes[i].toInt() and 0xFF
                val hi = audioBytes[i + 1].toInt()
                val s = (hi shl 8) or lo
                audioFloats[j++] = s / 32768.0f
                i += 2
            }

            // Resample/truncate/pad to req
            val inputArray = Array(batch) { FloatArray(req) }
            for (k in 0 until req) {
                val srcIdx = (k.toLong() * audioFloats.size / req).toInt().coerceIn(0, audioFloats.size - 1)
                inputArray[0][k] = audioFloats[srcIdx]
            }

            // Prepare output buffer
            val outTensor = interp.getOutputTensor(0)
            val outShape = outTensor.shape()
            val outDim = outShape.last()
            val outputArray = Array(1) { FloatArray(outDim) }

            interp.run(inputArray, outputArray)
            return outputArray[0]
        } catch (e: Exception) {
            Log.e(TAG, "computeEmbedding failed", e)
            return null
        }
    }

    private fun computeLiveness(audioBytes: ByteArray): Float {
        try {
            val interp = antispoofInterpreter ?: return 0f
            val inputTensor = interp.getInputTensor(0)
            val inputShape = inputTensor.shape()
            val batch = inputShape[0]
            val req = if (inputShape.size >= 2) inputShape[1] else inputShape[0]

            val shortCount = audioBytes.size / 2
            val audioFloats = FloatArray(shortCount)
            var j = 0
            var i = 0
            while (i + 1 < audioBytes.size && j < shortCount) {
                val lo = audioBytes[i].toInt() and 0xFF
                val hi = audioBytes[i + 1].toInt()
                val s = (hi shl 8) or lo
                audioFloats[j++] = s / 32768.0f
                i += 2
            }

            val inputArray = Array(batch) { FloatArray(req) }
            for (k in 0 until req) {
                val srcIdx = (k.toLong() * audioFloats.size / req).toInt().coerceIn(0, audioFloats.size - 1)
                inputArray[0][k] = audioFloats[srcIdx]
            }

            // output is probability
            val output = Array(1) { FloatArray(1) }
            interp.run(inputArray, output)
            val prob = output[0][0]
            return prob.coerceIn(0f, 1f)
        } catch (e: Exception) {
            Log.e(TAG, "computeLiveness failed", e)
            return 0f
        }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0.0f
        var na = 0.0f
        var nb = 0.0f
        val n = minOf(a.size, b.size)
        for (i in 0 until n) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        val denom = (Math.sqrt(na.toDouble()) * Math.sqrt(nb.toDouble())).toFloat()
        if (denom == 0f) return 0f
        return dot / denom
    }

    private fun storeCentroid(centroid: FloatArray) {
        val bb = ByteBuffer.allocate(centroid.size * 4).order(ByteOrder.nativeOrder())
        for (f in centroid) bb.putFloat(f)
        val bytes = bb.array()
        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        prefs.edit().putString(KEY_SPEAKER_CENTROID, b64).apply()
    }

    private fun loadStoredCentroid(): FloatArray? {
        val b64 = prefs.getString(KEY_SPEAKER_CENTROID, null) ?: return null
        val bytes = Base64.decode(b64, Base64.NO_WRAP)
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder())
        val n = bytes.size / 4
        val out = FloatArray(n)
        for (i in 0 until n) out[i] = bb.getFloat()
        return out
    }
}
