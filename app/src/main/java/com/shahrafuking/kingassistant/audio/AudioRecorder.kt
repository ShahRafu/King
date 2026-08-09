package com.shahrafuking.kingassistant.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Simple AudioRecorder wrapper that creates an AudioRecord with AEC/NS if available.
 * - Default sample rate 16k (can be changed)
 * - Provides start(callback) and stop() APIs
 */
class AudioRecorder(private val ctx: Context) {
    companion object {
        const val TAG = "AudioRecorder"
        const val DEFAULT_SAMPLE_RATE = 16000
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val running = AtomicBoolean(false)
    private var aec: AcousticEchoCanceler? = null
    private var ns: NoiseSuppressor? = null

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Start recording; delivers PCM chunks to callback as ShortArray and sampleRate.
     * Uses AudioSource.VOICE_COMMUNICATION to improve echo cancellation support.
     */
    fun start(onPcmChunk: (ShortArray, Int) -> Unit, sampleRate: Int = DEFAULT_SAMPLE_RATE) {
        if (!hasRecordPermission()) {
            Log.w(TAG, "No RECORD_AUDIO permission")
            return
        }
        if (running.get()) return

        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(sampleRate * 2)
        try {
            val ar = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                channelConfig,
                audioFormat,
                minBuf
            )
            if (ar.state != AudioRecord.STATE_INITIALIZED) {
                Log.w(TAG, "AudioRecord failed to initialize (state=${ar.state})")
                return
            }
            audioRecord = ar

            // Enable AcousticEchoCanceler and NoiseSuppressor if available
            try {
                val sessionId = ar.audioSessionId
                if (AcousticEchoCanceler.isAvailable()) {
                    try {
                        aec = AcousticEchoCanceler.create(sessionId)
                        aec?.enabled = true
                        Log.i(TAG, "AEC enabled (session=$sessionId)")
                    } catch (t: Throwable) {
                        Log.w(TAG, "Failed to enable AEC", t)
                    }
                } else {
                    Log.i(TAG, "AEC not available on device")
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && NoiseSuppressor.isAvailable()) {
                    try {
                        ns = NoiseSuppressor.create(sessionId)
                        ns?.enabled = true
                        Log.i(TAG, "NoiseSuppressor enabled (session=$sessionId)")
                    } catch (t: Throwable) {
                        Log.w(TAG, "Failed to enable NoiseSuppressor", t)
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Audio effects setup failed", t)
            }

            ar.startRecording()
            running.set(true)
            recordingThread = Thread {
                val buffer = ShortArray(minBuf / 2)
                try {
                    while (running.get()) {
                        val read = ar.read(buffer, 0, buffer.size)
                        if (read > 0) {
                            val chunk = if (read == buffer.size) buffer else buffer.copyOf(read)
                            try {
                                onPcmChunk(chunk, sampleRate)
                            } catch (t: Throwable) {
                                Log.w(TAG, "onPcmChunk callback error", t)
                            }
                        } else {
                            // read returned <=0, slight sleep to avoid tight loop
                            Thread.sleep(10)
                        }
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "Recording loop error", t)
                } finally {
                    try {
                        ar.stop()
                    } catch (_: Throwable) {}
                }
            }
            recordingThread?.start()
        } catch (t: Throwable) {
            Log.w(TAG, "AudioRecord start failed", t)
        }
    }

    fun stop() {
        running.set(false)
        try {
            recordingThread?.join(300)
        } catch (_: Throwable) {}
        recordingThread = null
        try {
            audioRecord?.stop()
        } catch (_: Throwable) {}
        try {
            audioRecord?.release()
        } catch (_: Throwable) {}
        audioRecord = null
        try {
            aec?.release()
        } catch (_: Throwable) {}
        aec = null
        try {
            ns?.release()
        } catch (_: Throwable) {}
        ns = null
    }
}
