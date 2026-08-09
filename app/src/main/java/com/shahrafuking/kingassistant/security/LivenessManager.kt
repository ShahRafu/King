package com.shahrafuking.kingassistant.security

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.shahrafuking.kingassistant.audio.AudioRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * LivenessManager
 *
 * - generateChallenge(): returns short challenge (4-digit or short phrase)
 * - startChallenge(activity, onResult): requests mic permission, starts ASR listening and VAD,
 *   verifies ASR transcript matches challenge, then invokes onResult(success:Boolean)
 *
 * Note: This is a pragmatic on-device liveness step. Stronger ML-based liveness should be added later.
 */
class LivenessManager(private val activity: Activity) {
    companion object {
        private const val TAG = "LivenessManager"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val audioRecorder by lazy { AudioRecorder(activity) }

    fun generateChallenge(): String {
        return if (Random.nextBoolean()) {
            // 4-digit numeric
            val n = 1000 + Random.nextInt(9000)
            n.toString()
        } else {
            // short phrase
            val choices = listOf("king assistant", "open sesame", "hello world")
            choices[Random.nextInt(choices.size)]
        }
    }

    fun startChallenge(expected: String, onResult: (Boolean) -> Unit) {
        // ensure mic permission
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.RECORD_AUDIO), 4244)
            Toast.makeText(activity, "Please grant microphone permission", Toast.LENGTH_LONG).show()
            onResult(false)
            return
        }

        // Quick VAD-based early detection: use AudioRecorder for amplitude threshold
        var detectedSpeech = false
        audioRecorder.start({ chunk, sampleRate ->
            // compute RMS energy
            var sum = 0L
            for (s in chunk) sum += (s * s).toLong()
            val rms = kotlin.math.sqrt(sum.toDouble() / (chunk.size.coerceAtLeast(1))).toFloat()
            if (rms > 1500f && !detectedSpeech) { // threshold tuned per device
                detectedSpeech = true
                Log.i(TAG, "VAD detected speech (rms=$rms)")
                // stop audioRecorder stream as SpeechRecognizer will handle recognition
                audioRecorder.stop()
            }
        }, AudioRecorder.DEFAULT_SAMPLE_RATE)

        // Start SpeechRecognizer to capture transcript for challenge matching
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity)
        val intent = RecognizerIntent().apply {
            action = RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { Log.i(TAG, "ASR ready") }
            override fun onBeginningOfSpeech() { Log.i(TAG, "ASR beginning") }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Log.w(TAG, "ASR error $error")
                onResult(false)
            }
            override fun onResults(results: Bundle?) {
                val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
                val transcript = texts.firstOrNull() ?: ""
                val ok = matchTranscript(expected, transcript)
                Log.i(TAG, "ASR heard: $transcript expected: $expected ok=$ok")
                onResult(ok)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        try {
            speechRecognizer?.startListening(intent)
        } catch (t: Throwable) {
            Log.w(TAG, "startListening failed", t)
            onResult(false)
        }
    }

    private fun normalize(s: String): String {
        return s.lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), "").trim()
    }

    private fun matchTranscript(expected: String, heard: String): Boolean {
        val e = normalize(expected)
        val h = normalize(heard)
        return h.contains(e) || e.contains(h) || (levenshtein(e, h) <= 2)
    }

    // simple Levenshtein distance for tolerance
    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = listOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost).minOrNull()!!
            }
        }
        return dp[a.length][b.length]
    }

    fun stop() {
        try { speechRecognizer?.stopListening() } catch (_: Throwable) {}
        try { speechRecognizer?.destroy() } catch (_: Throwable) {}
        audioRecorder.stop()
    }
}
