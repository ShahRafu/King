package com.shahrafuking.kingassistant.voice.tflite

import android.content.Context
import android.util.Log
import kotlin.math.sqrt
import org.tensorflow.lite.Interpreter

/**
 * SpeakerVerifier
 *
 * - Loads a TFLite speaker‑verification model (optional).
 * - Optionally loads an anti‑spoof TFLite model.
 * - Provides computeEmbedding, runAntiSpoofModel, cosineSimilarity utilities.
 *
 * Model expectations:
 * - Speaker model: input [1, N] float32, output [1, D] float32 embedding
 * - Anti‑spoof model (optional): input matching preprocessor output, output [1] score (0..1)
 */
class SpeakerVerifier(
    private val context: Context,
    modelAssetPath: String? = null,
    modelFilePath: String? = null,
    antiSpoofAssetPath: String? = null
) {
    companion object { private const val TAG = "SpeakerVerifier" }

    private var interpreter: Interpreter? = null
    private var antiSpoofInterpreter: Interpreter? = null
    private var embeddingDim: Int = 0

    init {
        try {
            if (modelFilePath != null) interpreter = SampleModelLoader.loadFromFilePath(modelFilePath)
            else if (modelAssetPath != null) interpreter = SampleModelLoader.loadFromAssets(context, modelAssetPath)

            if (antiSpoofAssetPath != null) {
                antiSpoofInterpreter = SampleModelLoader.loadFromAssets(context, antiSpoofAssetPath)
            }

            if (interpreter != null) {
                try {
                    val outShape = interpreter!!.getOutputTensor(0).shape()
                    if (outShape.size >= 2) embeddingDim = outShape[1]
                } catch (t: Throwable) { Log.w(TAG, "Could not probe tflite tensor shape", t) }
            } else {
                Log.i(TAG, "No TFLite speaker model loaded; using fallback embedding")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "SpeakerVerifier init error", t)
        }
    }

    /**
     * Compute embedding from features. If model available, run it; otherwise use normalized fallback.
     */
    fun computeEmbedding(features: FloatArray): FloatArray? {
        return try {
            interpreter?.let { interp ->
                val input = arrayOf(features)
                val out = Array(1) { FloatArray(if (embeddingDim > 0) embeddingDim else 256) }
                interp.run(input, out)
                l2Normalize(out[0])
            } ?: run {
                val outDim = 256
                val emb = FloatArray(outDim)
                val n = minOf(features.size, outDim)
                for (i in 0 until n) emb[i] = features[i]
                for (i in n until outDim) emb[i] = 0f
                l2Normalize(emb)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "computeEmbedding failed", t)
            null
        }
    }

    /**
     * Run optional anti‑spoof model. Returns score in 0..1 or null if not available.
     */
    fun runAntiSpoofModel(features: FloatArray): Float? {
        return try {
            antiSpoofInterpreter?.let { interp ->
                val input = arrayOf(features)
                val out = Array(1) { FloatArray(1) }
                interp.run(input, out)
                out[0][0].coerceIn(0f, 1f)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "runAntiSpoofModel failed", t)
            null
        }
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f; var na = 0f; var nb = 0f
        val n = minOf(a.size, b.size)
        for (i in 0 until n) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i] }
        if (na == 0f || nb == 0f) return 0f
        return dot / (sqrt(na) * sqrt(nb))
    }

    private fun l2Normalize(arr: FloatArray): FloatArray {
        var s = 0f
        for (v in arr) s += v * v
        val norm = sqrt(s)
        if (norm == 0f) return arr
        val out = FloatArray(arr.size)
        for (i in arr.indices) out[i] = arr[i] / norm
        return out
    }

    fun close() {
        try { interpreter?.close() } catch (_: Throwable) {}
        try { antiSpoofInterpreter?.close() } catch (_: Throwable) {}
        interpreter = null; antiSpoofInterpreter = null
    }
}
