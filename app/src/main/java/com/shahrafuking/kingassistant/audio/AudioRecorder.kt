package com.shahrafuking.kingassistant.audio

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

/**
 * Simple AudioRecord wrapper that streams PCM16LE frames via callback.
 * - Uses mono, 16-bit PCM
 * - Caller must run heavy work on background thread
 */
class AudioRecorder(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private var running = false

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PermissionChecker.PERMISSION_GRANTED
    }

    fun start(onPcmFrame: (ShortArray, Int) -> Unit) {
        if (!hasRecordPermission()) throw IllegalStateException("RECORD_AUDIO permission is required")
        val sampleRate = 16000
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT)

        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
        audioRecord?.startRecording()
        running = true

        Thread {
            val shortBuffer = ShortArray(bufferSize / 2)
            while (running && audioRecord != null) {
                val read = audioRecord!!.read(shortBuffer, 0, shortBuffer.size)
                if (read > 0) {
                    // send a copy
                    onPcmFrame(shortBuffer.copyOf(read), sampleRate)
                }
            }
        }.start()
    }

    fun stop() {
        running = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (t: Throwable) {
            Log.w("AudioRecorder", "stop error", t)
        }
        audioRecord = null
    }
}
