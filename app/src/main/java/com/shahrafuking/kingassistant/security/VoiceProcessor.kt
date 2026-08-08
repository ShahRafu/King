package com.shahrafuking.kingassistant.security

import android.util.Log
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.*

/**
 * VoiceProcessor
 * - Simple on-device audio fingerprint generator.
 * - Input: raw PCM16 file (little-endian) or byte array.
 * - Output: hex fingerprint string (placeholder embedding).
 *
 * NOTE: This is a lightweight on-device scaffold for similarity checking.
 * For production biometric embeddings use specialized models and anti-spoofing.
 */
object VoiceProcessor {
    private const val TAG = "VoiceProcessor"

    // compute a compact fingerprint by:
    // - interpreting PCM16 bytes to floats
    // - computing short-time FFT blocks
    // - averaging band magnitudes into buckets
    // - quantizing and hashing with SHA-256
    fun fingerprintFromPcmFile(pcmPath: String, sampleRate: Int = 16000, windowSize: Int = 1024, hopSize: Int = 512): String {
        try {
            val f = File(pcmPath)
            val bytes = f.readBytes()
            val shorts = byteArrayToShorts(bytes)
            val floats = shorts.map { it.toFloat() / Short.MAX_VALUE }.toFloatArray()
            val bands = computeAvgBandMagnitudes(floats, windowSize, hopSize)
            val quant = quantizeBands(bands)
            return sha256Hex(quant)
        } catch (ex: Exception) {
            Log.e(TAG, "Fingerprint error: ${ex.message}")
            throw ex
        }
    }

    private fun byteArrayToShorts(bytes: ByteArray): ShortArray {
        val n = bytes.size / 2
        val s = ShortArray(n)
        var idx = 0
        var i = 0
        while (i + 1 < bytes.size) {
            val low = bytes[i].toInt() and 0xFF
            val high = bytes[i + 1].toInt() and 0xFF
            s[idx++] = ((high shl 8) or low).toShort()
            i += 2
        }
        return s
    }

    private fun computeAvgBandMagnitudes(signal: FloatArray, win: Int, hop: Int, bands: Int = 32): FloatArray {
        val fft = RealFFT(win)
        val bandAcc = FloatArray(bands) { 0f }
        var frames = 0
        var pos = 0
        while (pos + win <= signal.size) {
            val frame = signal.copyOfRange(pos, pos + win)
            // apply Hanning
            for (i in frame.indices) frame[i] = frame[i] * (0.5f - 0.5f * cos(2.0 * Math.PI * i / (win - 1)).toFloat())
            val mag = fft.magnitude(frame)
            // aggregate into bands
            val bandSize = max(1, mag.size / bands)
            for (b in 0 until bands) {
                var sum = 0f
                val start = b * bandSize
                val end = min(mag.size, start + bandSize)
                for (k in start until end) sum += mag[k]
                bandAcc[b] += sum / (end - start).coerceAtLeast(1)
            }
            frames++
            pos += hop
        }
        return if (frames == 0) FloatArray(bands) { 0f } else bandAcc.map { it / frames }.toFloatArray()
    }

    private fun quantizeBands(bands: FloatArray, levels: Int = 256): ByteArray {
        val max = bands.maxOrNull() ?: 1f
        val min = bands.minOrNull() ?: 0f
        val range = (max - min).takeIf { it > 0f } ?: 1f
        val out = ByteArray(bands.size)
        for (i in bands.indices) {
            val norm = ((bands[i] - min) / range).coerceIn(0f, 1f)
            out[i] = (floor(norm * (levels - 1))).toInt().toByte()
        }
        return out
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val dig = md.digest(bytes)
        return dig.joinToString("") { String.format(Locale.US, "%02x", it) }
    }

    private fun sha256Hex(s: String): String = sha256Hex(s.toByteArray(Charsets.UTF_8))

    // Small real FFT implementation for real inputs (power‑of‑two window size)
    private class RealFFT(private val n: Int) {
        private val cosTable = DoubleArray(n / 2)
        private val sinTable = DoubleArray(n / 2)

        init {
            for (i in 0 until n / 2) {
                cosTable[i] = cos(2.0 * Math.PI * i / n)
                sinTable[i] = sin(2.0 * Math.PI * i / n)
            }
        }

        // returns magnitude array (length n/2)
        fun magnitude(realInput: FloatArray): FloatArray {
            val re = DoubleArray(n)
            val im = DoubleArray(n)
            for (i in realInput.indices) re[i] = realInput[i].toDouble()
            fft(re, im)
            val m = FloatArray(n / 2)
            for (k in 0 until n / 2) {
                m[k] = sqrt(re[k] * re[k] + im[k] * im[k]).toFloat()
            }
            return m
        }

        // Cooley-Tukey radix-2 in-place
        private fun fft(re: DoubleArray, im: DoubleArray) {
            val n = re.size
            var j = 0
            for (i in 1 until n - 1) {
                var bit = n shr 1
                while (j >= bit) {
                    j -= bit
                    bit = bit shr 1
                }
                j += bit
                if (i < j) {
                    val tmpR = re[i]; re[i] = re[j]; re[j] = tmpR
                    val tmpI = im[i]; im[i] = im[j]; im[j] = tmpI
                }
            }
            var len = 2
            while (len <= n) {
                val half = len / 2
                val step = n / len
                for (i0 in 0 until n step len) {
                    var k = 0
                    for (j0 in i0 until i0 + half) {
                        val l = j0 + half
                        val tRe = re[l] * cosTable[k] + im[l] * sinTable[k]
                        val tIm = -re[l] * sinTable[k] + im[l] * cosTable[k]
                        re[l] = re[j0] - tRe
                        im[l] = im[j0] - tIm
                        re[j0] += tRe
                        im[j0] += tIm
                        k += step
                    }
                }
                len = len shl 1
            }
        }
    }
}
