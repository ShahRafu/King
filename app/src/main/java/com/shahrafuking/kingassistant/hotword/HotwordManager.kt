package com.shahrafuking.kingassistant.hotword

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*

/**
 * HotwordManager: lightweight wrapper.
 * - If PORCUPINE_ENABLED = true and porcupine libs present, integrate there.
 * - Otherwise uses RMS energy threshold on microphone frames as a simple placeholder.
 *
 * API:
 *   startListening(callback: (Boolean) -> Unit)
 *   stopListening()
 */
class HotwordManager(private val context: Context) {
    companion object {
        private const val TAG = "HotwordManager"
        // Set false by default. If you add Porcupine .so/.ppn and glue, set true and implement.
        const val PORCUPINE_ENABLED = false
    }

    private var audioJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun startListening(onHotword: (Boolean) -> Unit) {
        if (PORCUPINE_ENABLED) {
            // TODO: integrate porcupine detection
            Log.i(TAG, "Porcupine enabled but not implemented in this scaffold")
            return
        }

        // If there's already a job running, don't start another
        if (audioJob?.isActive == true) return

        audioJob = scope.launch {
            var recorder: AudioRecord? = null
            try {
                val sampleRate = 16000
                val channel = AudioFormat.CHANNEL_IN_MONO
                val format = AudioFormat.ENCODING_PCM_16BIT
                val minBuf = AudioRecord.getMinBufferSize(sampleRate, channel, format)
                if (minBuf == AudioRecord.ERROR || minBuf == AudioRecord.ERROR_BAD_VALUE) {
                    Log.w(TAG, "AudioRecord.getMinBufferSize returned error: $minBuf")
                    return@launch
                }
                // ensure bufferSize reasonable
                val bufferSize = maxOf(minBuf, sampleRate / 4)

                recorder = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channel, format, bufferSize)
                if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                    Log.w(TAG, "AudioRecord not initialized (state=${recorder.state})")
                    try { recorder.release() } catch (_: Throwable) {}
                    return@launch
                }

                val shortBuf = ShortArray(bufferSize / 2)
                // Start recording in a safe manner
                recorder.startRecording()

                while (isActive) {
                    val r = try { recorder.read(shortBuf, 0, shortBuf.size) } catch (t: Throwable) {
                        Log.w(TAG, "AudioRecord.read failed: ${t.localizedMessage}", t)
                        break
                    }
                    if (r > 0) {
                        var sum = 0L
                        for (i in 0 until r) {
                            val v = shortBuf[i].toInt()
                            sum += (v * v).toLong()
                        }
                        val rms = kotlin.math.sqrt(sum.toDouble() / r.toDouble())
                        // threshold tuned for quick test; adjust as needed
                        if (rms > 1500.0) {
                            withContext(Dispatchers.Main) { onHotword(true) }
                            // Debounce a bit
                            delay(1500)
                        }
                    } else {
                        delay(10)
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Hotword listener failed: ${t.localizedMessage}", t)
            } finally {
                try {
                    recorder?.let {
                        try { it.stop() } catch (_: Throwable) {}
                        try { it.release() } catch (_: Throwable) {}
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    fun stopListening() {
        audioJob?.cancel()
        audioJob = null
    }
}
