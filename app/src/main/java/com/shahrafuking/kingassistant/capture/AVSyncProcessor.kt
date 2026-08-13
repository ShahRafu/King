package com.shahrafuking.kingassistant.capture

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.*

/**
 * AVSyncProcessor
 *
 * Loads an AV-sync TF-Lite model from assets (models/av_sync.tflite) and provides
 * a method to compute a sync score given raw audio PCM16 (little-endian) and a sequence
 * of mouth landmarks aggregated into a feature vector.
 *
 * NOTE: The model input shapes and preprocessing must match the provided model. This implementation
 * follows a common pattern: compute log-mel spectrogram (n_mels=64) and concatenate with
 * landmark features. Adjust constants to match your model.
 */
class AVSyncProcessor(private val context: Context, private val modelAssetPath: String = "models/av_sync.tflite") {
    private val TAG = "AVSyncProcessor"
    private val interpreter: Interpreter

    // Audio processing defaults
    private val sampleRate = 48000
    private val nFft = 1024
    private val hop = 512
    private val nMels = 64

    init {
        interpreter = Interpreter(loadModelFile(context, modelAssetPath))
    }

    private fun loadModelFile(ctx: Context, assetPath: String): MappedByteBuffer {
        val fileDescriptor = ctx.assets.openFd(assetPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val channel = inputStream.channel
        val start = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return channel.map(FileChannel.MapMode.READ_ONLY, start, declaredLength)
    }

    /**
     * Compute a sync score in [0,1] where higher means better audio-visual sync.
     * audioPcm: raw PCM16 little-endian bytes
     * mouthLandmarks: FloatArray representing concatenated mouth landmark (x,y) normalized coordinates per frame or aggregated summary
     */
    fun computeSyncScore(audioPcm: ByteArray, mouthLandmarks: FloatArray): Float {
        try {
            // 1) compute log-mel spectrogram
            val mel = computeLogMelSpectrogram(audioPcm)
            // 2) prepare model input: assuming model expects [1, T, n_mels] and landmarks as [1, L]
            // We'll flatten mel into a single array and concatenate landmarks for a single input vector; adapt to your model.
            val melFlatten = FloatArray(mel.size * mel[0].size)
            var idx = 0
            for (t in mel.indices) {
                for (m in mel[0].indices) {
                    melFlatten[idx++] = mel[t][m]
                }
            }

            val inputLen = melFlatten.size + mouthLandmarks.size
            val inputBuffer = ByteBuffer.allocateDirect(inputLen * 4).order(ByteOrder.nativeOrder())
            for (f in melFlatten) inputBuffer.putFloat(f)
            for (f in mouthLandmarks) inputBuffer.putFloat(f)
            inputBuffer.rewind()

            val output = Array(1) { FloatArray(1) }
            interpreter.run(inputBuffer, output)
            val score = output[0][0]
            return score.coerceIn(0f, 1f)
        } catch (e: Exception) {
            Log.e(TAG, "computeSyncScore failed", e)
            return 0f
        }
    }

    private fun computeLogMelSpectrogram(audioPcm: ByteArray): Array<FloatArray> {
        // Convert PCM bytes to floats [-1,1]
        val shortCount = audioPcm.size / 2
        val audio = FloatArray(shortCount)
        var si = 0
        var i = 0
        while (i < audioPcm.size) {
            val lo = audioPcm[i].toInt() and 0xFF
            val hi = audioPcm[i + 1].toInt()
            val s = (hi shl 8) or lo
            audio[si++] = s / 32768.0f
            i += 2
        }

        val frames = frameSignal(audio, nFft, hop)
        val window = hammingWindow(nFft)
        val melFilter = melFilterBank(nFft, sampleRate, nMels)

        val melSpec = Array(frames.size) { FloatArray(nMels) }
        val fftRe = DoubleArray(nFft)
        val fftIm = DoubleArray(nFft)

        for (t in frames.indices) {
            val frame = frames[t]
            for (j in 0 until nFft) {
                fftRe[j] = (frame[j] * window[j]).toDouble()
                fftIm[j] = 0.0
            }
            fft(fftRe, fftIm)
            val powerSpec = DoubleArray(nFft / 2 + 1)
            for (k in 0 until powerSpec.size) {
                val re = fftRe[k]
                val im = fftIm[k]
                powerSpec[k] = re * re + im * im
            }
            val mel = FloatArray(nMels)
            for (m in 0 until nMels) {
                var sum = 0.0
                val fb = melFilter[m]
                for (k in fb.indices) {
                    sum += fb[k] * (if (k < powerSpec.size) powerSpec[k] else 0.0)
                }
                val v = ln(sum + 1e-9)
                mel[m] = v.toFloat()
            }
            melSpec[t] = mel
        }
        return melSpec
    }

    private fun frameSignal(samples: FloatArray, frameSize: Int, hop: Int): Array<FloatArray> {
        if (samples.size < frameSize) {
            val padded = FloatArray(frameSize)
            samples.copyInto(padded)
            return arrayOf(padded)
        }
        val nFrames = 1 + (samples.size - frameSize) / hop
        val frames = Array(nFrames) { FloatArray(frameSize) }
        var i = 0
        for (f in 0 until nFrames) {
            val start = f * hop
            System.arraycopy(samples, start, frames[f], 0, frameSize)
        }
        return frames
    }

    private fun hammingWindow(N: Int): FloatArray {
        val w = FloatArray(N)
        for (n in 0 until N) {
            w[n] = (0.54 - 0.46 * cos(2.0 * Math.PI * n / (N - 1))).toFloat()
        }
        return w
    }

    private fun melFilterBank(nFft: Int, sampleRate: Int, nMels: Int): Array<FloatArray> {
        val fMin = 0.0
        val fMax = sampleRate / 2.0
        val melMin = hzToMel(fMin)
        val melMax = hzToMel(fMax)
        val melPoints = DoubleArray(nMels + 2)
        for (i in melPoints.indices) melPoints[i] = melMin + (melMax - melMin) * i / (nMels + 1)
        val hzPoints = melPoints.map { melToHz(it) }
        val binF = IntArray(hzPoints.size)
        for (i in hzPoints.indices) binF[i] = floor((nFft + 1) * hzPoints[i] / sampleRate).toInt()

        val filters = Array(nMels) { FloatArray(nFft / 2 + 1) }
        for (m in 1..nMels) {
            val fMMinus = binF[m - 1]
            val fM = binF[m]
            val fMPlus = binF[m + 1]
            for (k in fMMinus until fM) {
                filters[m - 1][k] = (k - fMMinus).toFloat() / (fM - fMMinus).toFloat()
            }
            for (k in fM until fMPlus) {
                filters[m - 1][k] = (fMPlus - k).toFloat() / (fMPlus - fM).toFloat()
            }
        }
        return filters
    }

    private fun hzToMel(hz: Double): Double = 2595.0 * ln10(1 + hz / 700.0)
    private fun melToHz(mel: Double): Double = 700.0 * (Math.pow(10.0, mel / 2595.0) - 1.0)

    // Natural log base 10 helper
    private fun ln10(x: Double) = Math.log10(x)

    // Cooley-Tukey in-place radix-2 FFT (complex arrays: re[], im[])
    private fun fft(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        if (n == 1) return
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j >= bit) {
                j -= bit
                bit = bit shr 1
            }
            j += bit
            if (i < j) {
                val tmpRe = re[i]
                val tmpIm = im[i]
                re[i] = re[j]
                im[i] = im[j]
                re[j] = tmpRe
                im[j] = tmpIm
            }
        }
        var len = 2
        while (len <= n) {
            val angle = -2.0 * Math.PI / len
            val wlenRe = Math.cos(angle)
            val wlenIm = Math.sin(angle)
            var i = 0
            while (i < n) {
                var wRe = 1.0
                var wIm = 0.0
                var j2 = 0
                while (j2 < len / 2) {
                    val uRe = re[i + j2]
                    val uIm = im[i + j2]
                    val vRe = re[i + j2 + len / 2] * wRe - im[i + j2 + len / 2] * wIm
                    val vIm = re[i + j2 + len / 2] * wIm + im[i + j2 + len / 2] * wRe
                    re[i + j2] = uRe + vRe
                    im[i + j2] = uIm + vIm
                    re[i + j2 + len / 2] = uRe - vRe
                    im[i + j2 + len / 2] = uIm - vIm
                    val nextWRe = wRe * wlenRe - wIm * wlenIm
                    val nextWIm = wRe * wlenIm + wIm * wlenRe
                    wRe = nextWRe
                    wIm = nextWIm
                    j2++
                }
                i += len
            }
            len = len shl 1
        }
    }
}
