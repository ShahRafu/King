package com.shahrafuking.kingassistant.voice.preproc

import kotlin.math.*

object Spectrogram {
    // Compute log-mel spectrogram from PCM float signal (-1..1)
    // Returns mel spectrogram as 2D FloatArray [frames][melBins]
    fun logMelSpectrogram(pcm: FloatArray, sampleRate: Int, frameSize: Int = 512, hop: Int = 160, melBins: Int = 40, fmin: Float = 50f, fmax: Float = sampleRate / 2f): Array<FloatArray> {
        val n = pcm.size
        val window = FloatArray(frameSize) { i -> (0.54 - 0.46 * cos(2.0 * Math.PI * i / (frameSize - 1))).toFloat() }
        // number of frames
        val framesCount = if (n < frameSize) 1 else 1 + (n - frameSize) / hop
        // FFT size: next pow2
        var fftSize = 1
        while (fftSize < frameSize) fftSize = fftSize shl 1
        // precompute mel filterbank
        val melFilter = melFilterbank(melBins, fftSize, sampleRate, fmin, fmax)

        val spectrogram = Array(framesCount) { FloatArray(melBins) }
        val re = DoubleArray(fftSize)
        val im = DoubleArray(fftSize)

        var frameIndex = 0
        var ptr = 0
        while (frameIndex < framesCount) {
            // windowed frame
            for (i in 0 until frameSize) {
                val x = if (ptr + i < n) pcm[ptr + i] else 0f
                re[i] = x * window[i]
                im[i] = 0.0
            }
            for (i in frameSize until fftSize) { re[i] = 0.0; im[i] = 0.0 }
            // perform FFT (real-input)
            fft(re, im)
            // magnitude
            val half = fftSize / 2
            val mag = DoubleArray(half)
            for (i in 0 until half) mag[i] = sqrt(re[i] * re[i] + im[i] * im[i]) + 1e-12
            // apply mel filterbank
            for (m in 0 until melBins) {
                var sum = 0.0
                val filt = melFilter[m]
                for (k in filt.indices) sum += mag[k] * filt[k]
                val valLog = kotlin.math.log(sum + 1e-12)
                spectrogram[frameIndex][m] = valLog.toFloat()
            }
            frameIndex++
            ptr += hop
        }
        return spectrogram
    }

    private fun melFilterbank(melBins: Int, fftSize: Int, sampleRate: Int, fmin: Float, fmax: Float): Array<FloatArray> {
        val half = fftSize / 2
        val melLow = hzToMel(fmin)
        val melHigh = hzToMel(fmax)
        val melPoints = FloatArray(melBins + 2) { i -> melLow + (i.toFloat() / (melBins + 1)) * (melHigh - melLow) }
        val hzPoints = FloatArray(melPoints.size) { i -> melToHz(melPoints[i]) }
        val bin = IntArray(hzPoints.size) { i -> ((hzPoints[i] / sampleRate) * fftSize).toInt().coerceIn(0, half - 1) }
        val fb = Array(melBins) { FloatArray(half) { 0f } }
        for (m in 1..melBins) {
            val f_m_minus = bin[m - 1]
            val f_m = bin[m]
            val f_m_plus = bin[m + 1]
            if (f_m == f_m_minus || f_m_plus == f_m) continue
            for (k in f_m_minus until f_m) {
                fb[m - 1][k] = ((k - f_m_minus).toFloat() / (f_m - f_m_minus)).coerceAtMost(1f)
            }
            for (k in f_m until f_m_plus) {
                fb[m - 1][k] = ((f_m_plus - k).toFloat() / (f_m_plus - f_m)).coerceAtMost(1f)
            }
        }
        return fb
    }

    private fun hzToMel(hz: Float) = 2595f * ln10((hz / 700f) + 1f)
    private fun melToHz(mel: Float) = 700f * (10f.pow(mel / 2595f) - 1f)
    private fun ln10(x: Float) = kotlin.math.ln(x.toDouble()).toFloat()

    // in-place Cooley-Tukey FFT (radix-2) on re/im arrays
    private fun fft(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        var j = 0
        var i = 1
        while (i < n - 1) {
            var k = n / 2
            while (j >= k) { j -= k; k /= 2 }
            j += k
            val xr = re[i]; val xi = im[i]
            re[i] = re[j]; im[i] = im[j]
            re[j] = xr; im[j] = xi
            i++
        }
        var len = 2
        while (len <= n) {
            val ang = -2.0 * Math.PI / len
            val wlen_r = kotlin.math.cos(ang)
            val wlen_i = kotlin.math.sin(ang)
            var i2 = 0
            while (i2 < n) {
                var wr = 1.0
                var wi = 0.0
                var j2 = 0
                while (j2 < len / 2) {
                    val u_r = re[i2 + j2]; val u_i = im[i2 + j2]
                    val v_r = re[i2 + j2 + len / 2] * wr - im[i2 + j2 + len / 2] * wi
                    val v_i = re[i2 + j2 + len / 2] * wi + im[i2 + j2 + len / 2] * wr
                    re[i2 + j2] = u_r + v_r; im[i2 + j2] = u_i + v_i
                    re[i2 + j2 + len / 2] = u_r - v_r; im[i2 + j2 + len / 2] = u_i - v_i
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
}
