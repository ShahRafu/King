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
import java.util.Locale
import kotlin.math.abs

/**
 * VoiceEnrollmentManager
 * - Records short PCM samples from the microphone and saves to app files dir.
 * - Computes a simple fingerprint via VoiceProcessor (placeholder embedding).
 * - Enrolls a voice profile (saves via RoomRepository scaffold) and verifies samples.
 *
 * NOTES:
 * - This is a scaffold for on-device voice enrollment. For production use a proper
 *   embedding model, anti-spoofing, and secure storage/encryption.
 * - RoomRepository and VoiceProfileEntity are referenced as existing storage scaffolds.
 */
class VoiceEnrollmentManager(private val context: Context) {
    private val TAG = "VoiceEnrollmentManager"
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    /**
     * Record a short PCM16 (little-endian) sample and return the saved file path.
     * durationMs default 2000ms (2s). Increase if you want longer samples.
     */
    suspend fun recordSample(sampleName: String, durationMs: Int = 2000): String = withContext(Dispatchers.IO) {
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = if (minBuf == AudioRecord.ERROR || minBuf == AudioRecord.ERROR_BAD_VALUE) sampleRate * 2 else minBuf
        val recorder = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize)

        val audioData = ShortArray(bufferSize / 2)
        val samplesDir = File(context.filesDir, "voice_samples")
        if (!samplesDir.exists()) samplesDir.mkdirs()
        val outFile = File(samplesDir, "${sampleName}_${System.currentTimeMillis()}.pcm")

        try {
            recorder.startRecording()
            val fos = FileOutputStream(outFile)
            val endTime = System.currentTimeMillis() + durationMs
            while (System.currentTimeMillis() < endTime) {
                val read = recorder.read(audioData, 0, audioData.size)
                if (read > 0) {
                    // write little-endian PCM16
                    val buffer = ByteArray(read * 2)
                    var idx = 0
                    for (i in 0 until read) {
                        val s = audioData[i].toInt()
                        buffer[idx++] = (s and 0x00FF).toByte()
                        buffer[idx++] = ((s shr 8) and 0xFF).toByte()
                    }
                    fos.write(buffer)
                }
            }
            fos.flush()
            fos.close()
            recorder.stop()
            recorder.release()
            Log.d(TAG, "Recorded sample to ${outFile.absolutePath}")
            return@withContext outFile.absolutePath
        } catch (ex: Exception) {
            try { recorder.release() } catch (_: Exception) {}
            Log.e(TAG, "Recording failed: ${ex.message}")
            throw ex
        }
    }

    /**
     * Compute fingerprint (embedding hash) for a saved PCM file using VoiceProcessor.
     */
    suspend fun computeEmbeddingHash(filePath: String): String = withContext(Dispatchers.IO) {
        return@withContext VoiceProcessor.fingerprintFromPcmFile(filePath, sampleRate = sampleRate, windowSize = 1024, hopSize = 512)
    }

    /**
     * Enroll a new voice profile given ownerName and list of sample file paths.
     * Returns generated profileId.
     *
     * NOTE: This combines sample bytes into a SHA-256 as placeholder embedding.
     * In production use robust embeddings and protect the stored data.
     */
    suspend fun enrollProfile(ownerName: String, samplePaths: List<String>): String = withContext(Dispatchers.IO) {
        val md = MessageDigest.getInstance("SHA-256")
        for (p in samplePaths) {
            try {
                val bytes = File(p).readBytes()
                md.update(bytes)
            } catch (ex: Exception) {
                Log.w(TAG, "Could not read sample $p: ${ex.message}")
            }
        }
        val embedding = md.digest().joinToString(separator = "") { String.format(Locale.US, "%02x", it) }
        val profileId = "vp_" + System.currentTimeMillis().toString(36)

        // save via RoomRepository scaffold (implement repository and entity separately)
        try {
            val repo = com.shahrafuking.kingassistant.storage.RoomRepository(context)
            val entity = com.shahrafuking.kingassistant.storage.room.VoiceProfileEntity(
                profileId = profileId,
                ownerName = ownerName,
                samplePathsCsv = samplePaths.joinToString(","),
                embeddingHash = embedding
            )
            repo.saveVoiceProfile(entity)
        } catch (ex: Exception) {
            Log.w(TAG, "RoomRepository not available or save failed: ${ex.message}")
        }

        return@withContext profileId
    }

    /**
     * Verify a sample against an enrolled profile (placeholder comparison).
     * Uses Hamming-like difference on hex embedding strings and compares percentage.
     */
    suspend fun verifyVoice(profileId: String, samplePath: String, tolerancePercent: Double = 5.0): Boolean = withContext(Dispatchers.IO) {
        try {
            val repo = com.shahrafuking.kingassistant.storage.RoomRepository(context)
            val profile = repo.findVoiceProfile(profileId) ?: return@withContext false
            val sampleHash = computeEmbeddingHash(samplePath)
            val dist = hammingDistanceHex(sampleHash, profile.embeddingHash)
            val maxLen = maxOf(sampleHash.length, profile.embeddingHash.length)
            val pct = (dist.toDouble() / maxLen.toDouble()) * 100.0
            return@withContext pct <= tolerancePercent
        } catch (ex: Exception) {
            Log.w(TAG, "Verify failed: ${ex.message}")
            return@withContext false
        }
    }

    private fun hammingDistanceHex(a: String, b: String): Int {
        val len = minOf(a.length, b.length)
        var diff = 0
        for (i in 0 until len) if (a[i] != b[i]) diff++
        diff += abs(a.length - b.length)
        return diff
    }
}
