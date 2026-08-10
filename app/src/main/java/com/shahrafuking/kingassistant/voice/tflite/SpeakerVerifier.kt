package com.shahrafuking.kingassistant.voice.tflite

import android.content.Context
import android.util.Log
import kotlin.math.sqrt
import org.tensorflow.lite.Interpreter

/**
 * SpeakerVerifier
 *
 * Responsibilities:
 * - Load a TFLite speaker‑verification model (optional).
 * - Provide computeEmbedding(input) → FloatArray and similarity() utilities.
 *
 * Notes:
 * - This wrapper is tolerant: if no model is available it will fall back to a
 *   simple embedding derived from input features (normalized copy). This allows
 *   integration & tests without shipping an actual .tflite model.
 *
 * Model contract (typical):
 * - Input: [1, N] float32 (raw features or wave features depending on model)
 * - Output: [1, D] float32 embedding vector (normalized)
 */
class SpeakerVerifier(private val context: Context, modelAssetPath: String? = null, modelFilePath: String? = null) {
    companion object {
        private const val TAG = "SpeakerVerifier"
    }

    private var interpreter: Interpreter? = null
    private var embeddingDim: Int = 0

    init {
        try {
            if (modelFilePath != null) {
                interpreter = SampleModelLoader.loadFromFilePath(modelFilePath)
            } else if (modelAssetPath != null) {
                interpreter = SampleModelLoader.loadFromAssets(context, modelAssetPath)
            }
            if (interpreter != null) {
                // attempt to deduce output shape by running a dummy input if possible
                // we won't crash if this fails; fallback embeddingDim stays 0
                try {
                    val inputShape = interpreter!!.getInputTensor(0).shape()
                    val outShape = interpreter!!.getOutputTensor(0).shape()
                    if (outShape.size >= 2) embeddingDim = outShape[1]
                } catch (t: Throwable) {
                    Log.w(TAG, "Could not probe tflite tensor shape", t)
                }
            } else {
                Log.i(TAG, "No TFLite model loaded: using fallback embedding path")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "SpeakerVerifier init error", t)
        }
    }

    /**
     * Compute embedding from a supplied feature vector.
     * - If model present: runs model.
     * - Else: returns a normalized copy of input (fallback).
     *
     * Input: FloatArray of shape [N] (model-dependent). Returns FloatArray embedding (or null on error).
     */
    fun computeEmbedding(features: FloatArray): FloatArray? {
        return try {
            interpreter?.let { interp ->
                // Prepare input and output buffers in shapes matching the model
                val input = arrayOf(features)
                val out = Array(1) { FloatArray(if (embeddingDim > 0) embeddingDim else 256) }
                interp.run(input, out)
                val emb = out[0]
                l2Normalize(emb)
            } ?: run {
                // fallback: simple normalized copy (expand/truncate to 256)
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

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var na = 0f
        var nb = 0f
        val n = minOf(a.size, b.size)
        for (i in 0 until n) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
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

    /**
     * release interpreter resources if any
     */
    fun close() {
        try { interpreter?.close() } catch (_: Throwable) {}
        interpreter = null
    }
}
