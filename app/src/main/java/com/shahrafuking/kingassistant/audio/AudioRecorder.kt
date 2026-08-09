package com.shahrafuking.kingassistant.audio

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Robust AudioRecord wrapper that streams PCM16LE frames via callback.
 * - Uses mono, 16-bit PCM, default sampleRate 16000 (compatible with many hotword engines)
 * - Caller must run heavy work on background thread (this class offloads read to its own thread)
 */
class AudioRecorder(private val context: Context) {
    companion object {
        const val DEFAULT_SAMPLE_RATE = 16000
    }

    private var audioRecord: AudioRecord? = null
    private val running = AtomicBoolean(false)
    private var readThread: Thread? = null

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PermissionChecker.PERMISSION_GRANTED
    }

    /**
     * Start streaming PCM frames. The callback receives a ShortArray (PCM16) and the sampleRate.
     * Throws IllegalStateException if RECORD_AUDIO permission not granted.
     */
    fun start(onPcmFrame: (ShortArray, Int) -> Unit, sampleRate: Int = DEFAULT_SAMPLE_RATE) {
        if (!hasRecordPermission()) throw IllegalStateException("RECORD_AUDIO permission is required")
        if (running.get()) return

        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(sampleRate / 2) // safety lower bound

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuffer
            )
            audioRecord?.startRecording()
        } catch (t: Throwable) {
            Log.e("AudioRecorder", "start error", t)
            audioRecord = null
            throw t
        }

        running.set(true)
        readThread = Thread {
            val shortBuffer = ShortArray(minBuffer / 2)
            while (running.get() && audioRecord != null) {
                try {
                    val read = audioRecord!!.read(shortBuffer, 0, shortBuffer.size)
                    if (read > 0) {
                        onPcmFrame(shortBuffer.copyOf(read), sampleRate)
                    }
                } catch (t: Throwable) {
                    Log.w("AudioRecorder", "read loop error", t)
                }
            }
        }.also { it.name = "AudioRecord-Reader" }.apply { start() }
    }

    /**
     * Stop recording and release resources. This returns immediately but attempts to stop gracefully.
     */
    fun stop() {
        running.set(false)
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (t: Throwable) {
            Log.w("AudioRecorder", "stop error", t)
        } finally {
            audioRecord = null
        }
        try {
            readThread?.join(200)
        } catch (_: InterruptedException) { /* ignore */ }
        readThread = null
    }
}
