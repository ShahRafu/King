package com.shahrafuking.kingassistant.voice

import android.content.Context
import android.util.Log
import com.shahrafuking.kingassistant.audio.AudioRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

// Lightweight feature extractor (prototype): per-frame spectral features averaged over recording
class VoiceEnrollmentManager(private val context: Context) {
    private val TAG = "VoiceEnrollmentManager"

    data class Template(val vector: DoubleArray, val samplesUsed: Int, val timestamp: Long)

    suspend fun recordAndExtract(sampleDurationMs: Long = 2500L, sampleRate: Int = AudioRecorder.DEFAULT_SAMPLE_RATE): DoubleArray? {
        return withContext(Dispatchers.Default) {
            try {
                // Use AudioRecorder to record for sampleDurationMs, accumulating PCM
                val pcmList = mutableListOf<Short>()
                val recorder = AudioRecorder(context)
                if (!recorder.hasRecordPermission()) {
                    Log.w(TAG, "No RECORD_AUDIO permission")
                    return@withContext null
                }
                val done = Object()
                var finished = false
                recorder.start({ pcmChunk, sr ->
                    synchronized(pcmList) {
                        for (s in pcmChunk) pcmList.add(s)
                    }
                }, sampleRate)

                // wait duration
                Thread.sleep(sampleDurationMs)
                recorder.stop()

                // copy to ShortArray
                val pcmShorts = synchronized(pcmList) {
                    val arr = ShortArray(pcmList.size)
                    for (i in pcmList.indices) arr[i] = pcmList[i]
                    arr
                }
                if (pcmShorts.isEmpty()) return@withContext null

                // extract features
                val features = extractFeaturesFromPcm(pcmShorts, sampleRate)
                return@withContext features
            } catch (t: Throwable) {
                Log.w(TAG, "recordAndExtract failed", t)
                return@withContext null
            }
        }
    }

    companion object {
        private const val TAG = "VoiceFeature"

        // simple frame + spectral features
        fun extractFeaturesFromPcm(pcm: ShortArray, sampleRate: Int): DoubleArray {
            // convert to double [-1,1]
            val x = DoubleArray(pcm.size)
            for (i in pcm.indices) x[i] = pcm[i].toDouble() / Short.MAX_VALUE

            val frameSize = 512 // ~32 ms @16k
            val hop = 160 // 10 ms hop
            val frames = mutableListOf<DoubleArray>()
            var i = 0
            while (i + frameSize <= x.size) {
                val frame = DoubleArray(frameSize)
                System.arraycopy(x, i, frame, 0, frameSize)
                frames.add(frame)
                i += hop
            }
            if (frames.isEmpty()) {
                // pad/truncate
                val frame = DoubleArray(frameSize)
                for (k in x.indices) if (k < frameSize) frame[k] = x[k]
                frames.add(frame)
            }

            val centroidList = mutableListOf<Double>()
            val flatnessList = mutableListOf<Double>()
            val zcrList = mutableListOf<Double>()
            val energyList = mutableListOf<Double>()

            for (f in frames) {
                applyHammingWindow(f)
                val mag = magnitudeSpectrum(f)
                val centroid = spectralCentroid(mag, sampleRate)
                val flatness = spectralFlatness(mag)
                val zcr = zeroCrossingRate(f)
                val energy = frameEnergy(f)

                centroidList.add(centroid)
                flatnessList.add(flatness)
                zcrList.add(zcr)
                energyList.add(energy)
            }

            // compute mean & variance for each feature
            val centroidMean = centroidList.average()
            val centroidVar = variance(centroidList, centroidMean)
            val flatMean = flatnessList.average()
            val flatVar = variance(flatnessList, flatMean)
            val zcrMean = zcrList.average()
            val zcrVar = variance(zcrList, zcrMean)
            val enMean = energyList.average()
            val enVar = variance(energyList, enMean)

            // feature vector (8-D)
            return doubleArrayOf(centroidMean, centroidVar, flatMean, flatVar, zcrMean, zcrVar, enMean, enVar)
        }

        private fun applyHammingWindow(frame: DoubleArray) {
            val n = frame.size
            for (i in frame.indices) {
                frame[i] = frame[i] * (0.54 - 0.46 * cos(2.0 * Math.PI * i / (n - 1)))
            }
        }

        // naive FFT magnitude using real-input FFT (Cooley‑Tukey) — returns half-spectrum mags
        private fun magnitudeSpectrum(frame: DoubleArray): DoubleArray {
            val n = frame.size
            // zero-pad to next pow2
            var m = 1
            while (m < n) m = m shl 1
            val re = DoubleArray(m)
            val im = DoubleArray(m)
            for (i in 0 until n) re[i] = frame[i]
            fft(re, im)
            val half = m / 2
            val mag = DoubleArray(half)
            for (i in 0 until half) mag[i] = sqrt(re[i] * re[i] + im[i] * im[i]) + 1e-12
            return mag
        }

        // Cooley-Tukey iterative FFT (in-place)
        private fun fft(re: DoubleArray, im: DoubleArray) {
            val n = re.size
            var j = 0
            var i = 1
            while (i < n - 1) {
                var k = n / 2
                while (j >= k) {
                    j -= k
                    k /= 2
                }
                j += k
                val xr = re[i]; val xi = im[i]
                re[i] = re[j]; im[i] = im[j]
                re[j] = xr; im[j] = xi
                i++
            }
            var len = 2
            while (len <= n) {
                val ang = -2.0 * Math.PI / len
                val wlen_r = cos(ang)
                val wlen_i = sin(ang)
                var i2 = 0
                while (i2 < n) {
                    var wr = 1.0
                    var wi = 0.0
                    var j2 = 0
                    while (j2 < len / 2) {
                        val u_r = re[i2 + j2]
                        val u_i = im[i2 + j2]
                        val v_r = re[i2 + j2 + len / 2] * wr - im[i2 + j2 + len / 2] * wi
                        val v_i = re[i2 + j2 + len / 2] * wi + im[i2 + j2 + len / 2] * wr
                        re[i2 + j2] = u_r + v_r
                        im[i2 + j2] = u_i + v_i
                        re[i2 + j2 + len / 2] = u_r - v_r
                        im[i2 + j2 + len / 2] = u_i - v_i
                        val tmp = wr * wlen_r - wi * wlen_i
                        wi = wr * wlen_i + wi * wlen_r
                        wr = tmp
                        j2++
                    }
                    i2 += len
                }
                len = len shl 1
            }
        }

        private fun spectralCentroid(mag: DoubleArray, sampleRate: Int): Double {
            var num = 0.0
            var den = 0.0
            val n = mag.size
            for (i in 0 until n) {
                val f = i.toDouble() * sampleRate / (2.0 * n)
                num += f * mag[i]
                den += mag[i]
            }
            return if (den == 0.0) 0.0 else num / den
        }

        private fun spectralFlatness(mag: DoubleArray): Double {
            var geo = 1.0
            var arith = 0.0
            val n = mag.size
            val eps = 1e-12
            for (i in 0 until n) {
                geo *= (mag[i] + eps)
                arith += mag[i]
            }
            val g = exp(ln(geo) / n)
            val a = arith / n + eps
            return g / a
        }

        private fun zeroCrossingRate(frame: DoubleArray): Double {
            var count = 0
            for (i in 1 until frame.size) {
                if ((frame[i - 1] >= 0 && frame[i] < 0) || (frame[i - 1] < 0 && frame[i] >= 0)) count++
            }
            return count.toDouble() / frame.size
        }

        private fun frameEnergy(frame: DoubleArray): Double {
            var e = 0.0
            for (v in frame) e += v * v
            return e / frame.size
        }

        private fun variance(list: List<Double>, mean: Double): Double {
            if (list.isEmpty()) return 0.0
            var v = 0.0
            for (x in list) v += (x - mean) * (x - mean)
            return v / list.size
        }
    }
}
