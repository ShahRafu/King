package com.shahrafuking.kingassistant.security

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.*

/**
 * VoiceEnrollmentManager
 * - Records multiple audio samples (PCM) and stores file paths
 * - Computes a simple SHA-256 hash over audio bytes as a placeholder "embedding"
 * - Saves VoiceProfileEntity via RoomRepository
 *
 * NOTE: This is a scaffold. For production voice biometrics use proper audio embeddings and privacy-preserving storage.
 */
class VoiceEnrollmentManager(private val context: Context) {
    private val TAG = "VoiceEnroll"
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    suspend fun recordSample(sampleName: String, durationMs: Int = 2000): String = withContext(Dispatchers.IO) {
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val recorder = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBuf)

        val audioData = ShortArray(minBuf / 2)
        val file = File(context.filesDir, "voice_samples")
        if (!file.exists()) file.mkdir()
        val outFile = File(file, "${sampleName}_${System.currentTimeMillis()}.pcm")

        try {
            recorder.startRecording()
            var totalRead = 0
            val fos = FileOutputStream(outFile)
            val endTime = System.currentTimeMillis() + durationMs
            while (System.currentTimeMillis() < endTime) {
                val read = recorder.read(audioData, 0, audioData.size)
                totalRead += read
                // write little-endian PCM16
                val buffer = ByteArray(read * 2)
                var idx = 0
                for (i in 0 until read) {
                    val s = audioData[i]
                    buffer[idx++] = (s and 0x00FF).toByte()
                    buffer[idx++] = ((s.toInt() shr 8) and 0xFF).toByte()
                }
                fos.write(buffer)
            }
            fos.flush(); fos.close()
            recorder.stop(); recorder.release()
            Log.d(TAG, "Recorded sample to ${outFile.absolutePath} (bytes=$totalRead)")
            return@withContext outFile.absolutePath
        } catch (ex: Exception) {
            try { recorder.release() } catch (_: Exception) {}
            Log.e(TAG, "Recording failed: ${ex.message}")
            throw ex
        }
    }

    suspend fun computeEmbeddingHash(filePath: String): String = withContext(Dispatchers.IO) {
        val f = File(filePath)
        val bytes = f.readBytes()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return@withContext digest.joinToString(separator = "") { String.format(Locale.US, "%02x", it) }
    }

    suspend fun enrollProfile(ownerName: String, samplePaths: List<String>): String {
        // compute combined hash as placeholder embedding
        val md = MessageDigest.getInstance("SHA-256")
        for (p in samplePaths) {
            val bytes = File(p).readBytes()
            md.update(bytes)
        }
        val embedding = md.digest().joinToString(separator = "") { String.format(Locale.US, "%02x", it) }
        val profileId = "vp_" + System.currentTimeMillis().toString(36)
        val repo = com.shahrafuking.kingassistant.storage.RoomRepository(context)
        val entity = com.shahrafuking.kingassistant.storage.room.VoiceProfileEntity(
            profileId = profileId,
            ownerName = ownerName,
            samplePathsCsv = samplePaths.joinToString(","),
            embeddingHash = embedding
        )
        repo.saveVoiceProfile(entity)
        return profileId
    }

    suspend fun verifyVoice(profileId: String, samplePath: String, tolerancePercent: Double = 5.0): Boolean {
        val repo = com.shahrafuking.kingassistant.storage.RoomRepository(context)
        val profile = repo.findVoiceProfile(profileId) ?: return false
        val sampleHash = computeEmbeddingHash(samplePath)
        // Placeholder comparison: exact equality or small hamming diff of hex strings
        val dist = hammingDistanceHex(sampleHash, profile.embeddingHash)
        val maxLen = maxOf(sampleHash.length, profile.embeddingHash.length)
        val pct = (dist.toDouble() / maxLen.toDouble()) * 100.0
        return pct <= tolerancePercent
    }

    private fun hammingDistanceHex(a: String, b: String): Int {
        val len = minOf(a.length, b.length)
        var diff = 0
        for (i in 0 until len) if (a[i] != b[i]) diff++
        diff += abs(a.length - b.length)
        return diff
    }
}
