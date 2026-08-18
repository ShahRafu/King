package com.shahrafuking.kingassistant.plugin.voiceplugin

import android.content.Context
import android.util.Log
import com.shahrafuking.kingassistant.audio.AudioRecorder
import com.shahrafuking.kingassistant.ml.PcmUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * ProductionEmbedderAdapter
 * - Records a short PCM segment (ms length), converts to FloatArray and calls ProductionVoicePluginCompat.
 * - Defensive: if embedder not available, returns false and doesn't crash.
 */
class ProductionEmbedderAdapter(private val context: Context) {
    private val TAG = "ProductionEmbedderAdapter"
    private val recorder = AudioRecorder(context)
    private val plugin = ProductionVoicePluginCompat(context)

    /**
     * Record for given durationMs and return normalized float PCM.
     * Runs on caller coroutine context.
     */
    private suspend fun recordPcmFloats(durationMs: Int): FloatArray? = withContext(Dispatchers.IO) {
        if (!recorder.hasRecordPermission()) {
            Log.w(TAG, "No RECORD_AUDIO permission for recording")
            return@withContext null
        }
        // accumulate into Short list via callback
        val chunks = mutableListOf<Short>()
        return@withContext suspendCoroutine<FloatArray?> { cont ->
            try {
                recorder.start({ chunk, sampleRate ->
                    for (s in chunk) chunks.add(s)
                }, AudioRecorder.DEFAULT_SAMPLE_RATE)
            } catch (t: Throwable) {
                Log.w(TAG, "Recorder start failed: ${t.message}")
                cont.resume(null)
                return@suspendCoroutine
            }
            // schedule stop after duration
            Thread {
                try { Thread.sleep(durationMs.toLong()) } catch (_: Throwable) {}
                try { recorder.stop() } catch (_: Throwable) {}
                val shorts = ShortArray(chunks.size)
                for (i in shorts.indices) shorts[i] = chunks[i]
                val floats = PcmUtils.shortsToFloats(shorts)
                cont.resume(floats)
            }.start()
        }
    }

    /**
     * Enroll by recording for durationMs and calling ProductionVoicePluginCompat.enrollFromPcm()
     */
    suspend fun enroll(durationMs: Int = 2000): Boolean {
        val pcm = recordPcmFloats(durationMs) ?: return false
        return try {
            plugin.enrollFromPcm(pcm)
        } catch (t: Throwable) {
            Log.w(TAG, "enroll failed: ${t.message}")
            false
        }
    }

    /**
     * Verify by recording and calling verifyFromPcm()
     */
    suspend fun verify(durationMs: Int = 1800, threshold: Double = com.shahrafuking.kingassistant.voice.VoiceVerifier.DEFAULT_THRESHOLD): Boolean {
        val pcm = recordPcmFloats(durationMs) ?: return false
        return try {
            plugin.verifyFromPcm(pcm, threshold)
        } catch (t: Throwable) {
            Log.w(TAG, "verify failed: ${t.message}")
            false
        }
    }
}
