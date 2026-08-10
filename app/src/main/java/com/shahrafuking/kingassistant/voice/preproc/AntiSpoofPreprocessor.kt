package com.shahrafuking.kingassistant.voice.preproc

import android.util.Log

/**
 * AntiSpoofPreprocessor
 * - Accepts raw PCM (ShortArray) and produces a FloatArray feature suitable for antispoof model
 * - Default: compute log-mel spectrogram and flatten (frames x melBins)
 */
object AntiSpoofPreprocessor {
    private const val TAG = "AntiSpoofPreprocessor"

    fun preprocessFromShorts(pcmShorts: ShortArray, sampleRate: Int): FloatArray? {
        return try {
            // convert to float [-1,1]
            val f = FloatArray(pcmShorts.size) { i -> pcmShorts[i].toFloat() / Short.MAX_VALUE }
            val mel = Spectrogram.logMelSpectrogram(f, sampleRate, frameSize = 512, hop = 160, melBins = 40)
            // compute mean across time to get a fixed-size vector (melBins)
            val frames = mel.size
            if (frames == 0) return null
            val melBins = mel[0].size
            val out = FloatArray(melBins)
            for (i in 0 until melBins) {
                var s = 0f
                for (t in 0 until frames) s += mel[t][i]
                out[i] = s / frames
            }
            // normalize (zero-mean, unit-variance)
            val mean = out.average().toFloat()
            var varSum = 0f
            for (i in out.indices) varSum += (out[i] - mean) * (out[i] - mean)
            val std = kotlin.math.sqrt((varSum / out.size).toDouble()).toFloat().coerceAtLeast(1e-6f)
            for (i in out.indices) out[i] = (out[i] - mean) / std
            out
        } catch (t: Throwable) {
            Log.w(TAG, "preprocess failed", t)
            null
        }
    }
}
