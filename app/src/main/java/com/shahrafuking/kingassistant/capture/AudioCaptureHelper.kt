package com.shahrafuking.kingassistant.capture

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * AudioCaptureHelper
 *
 * Captures PCM 16-bit mono audio at a configurable sample rate (default 48000 Hz). Provides
 * a helper to record for a given duration and return raw PCM bytes (little-endian).
 */
class AudioCaptureHelper(private val sampleRate: Int = 48000) {
    private val TAG = "AudioCaptureHelper"

    suspend fun recordForDurationMs(durationMs: Long): ByteArray = withContext(Dispatchers.IO) {
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = if (minBuf > 0) minBuf else sampleRate * 2

        val recorder = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate, channelConfig, audioFormat, bufferSize)
        val outStream = ByteArrayOutputStream()
        try {
            recorder.startRecording()
            val buffer = ShortArray(bufferSize / 2)
            val endTime = System.currentTimeMillis() + durationMs
            while (System.currentTimeMillis() < endTime) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    for (i in 0 until read) {
                        val s = buffer[i]
                        outStream.write((s.toInt() and 0xFF).toByte().toInt())
                        outStream.write(((s.toInt() shr 8) and 0xFF).toByte().toInt())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Audio capture failed", e)
        } finally {
            try { recorder.stop() } catch (ignored: Exception) {}
            try { recorder.release() } catch (ignored: Exception) {}
        }
        return@withContext outStream.toByteArray()
    }
}
