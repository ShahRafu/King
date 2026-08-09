package com.shahrafuking.kingassistant.speech

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.shahrafuking.kingassistant.audio.AudioRecorder
import com.shahrafuking.kingassistant.BuildConfig
import kotlinx.coroutines.*

/**
 * HotwordManager
 *
 * - Uses Porcupine (if BuildConfig.PORCUPINE_ENABLED & HotwordPorcupineEngine.init() succeeds)
 *   feeding audio from AudioRecorder to hotword engine.
 * - Otherwise uses SpeechRecognizer fallback configured for Bangla (bn-BD) to detect hotword phrases.
 *
 * Deliverable: start()/stop(), setListener(), HotwordListener.onHotwordDetected()
 */
class HotwordManager(private val context: Context) {
    private val TAG = "HotwordManager"

    interface HotwordListener {
        fun onHotwordDetected()
    }

    private var listener: HotwordListener? = null
    fun setListener(l: HotwordListener) { listener = l }

    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var audioRecorder: AudioRecorder? = null
    private var porcupineEngine: HotwordPorcupineEngine? = null

    private var speechRecognizer: SpeechRecognizer? = null
    @Volatile private var listening = false

    fun start() {
        if (isPorcupineEnabled()) {
            try {
                porcupineEngine = HotwordPorcupineEngine(context)
                val ok = porcupineEngine?.init() ?: false
                if (ok) {
                    startPorcupinePath()
                    Log.i(TAG, "Started Porcupine path")
                    return
                } else {
                    Log.w(TAG, "Porcupine init returned false")
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Porcupine init exception", t)
            }
        }
        startSpeechRecognizerFallback()
        Log.i(TAG, "Started SpeechRecognizer fallback")
    }

    fun stop() {
        stopPorcupinePath()
        stopSpeechRecognizerFallback()
        try { mainScope.cancel() } catch (_: Throwable) {}
    }

    private fun isPorcupineEnabled(): Boolean {
        return try { BuildConfig.PORCUPINE_ENABLED } catch (_: Throwable) { false }
    }

    // ---------------- Porcupine path ----------------
    private fun startPorcupinePath() {
        audioRecorder = AudioRecorder(context)
        if (!audioRecorder!!.hasRecordPermission()) {
            Log.w(TAG, "No RECORD_AUDIO permission for Porcupine path")
            return
        }
        audioRecorder!!.start({ pcm16, sampleRate ->
            try {
                val detected = porcupineEngine?.process(pcm16, sampleRate) ?: false
                if (detected) {
                    Log.i(TAG, "Porcupine detected hotword")
                    mainScope.launch { listener?.onHotwordDetected() }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Porcupine process error", t)
            }
        }, AudioRecorder.DEFAULT_SAMPLE_RATE)
    }

    private fun stopPorcupinePath() {
        try { audioRecorder?.stop() } catch (_: Throwable) {}
        audioRecorder = null
        try { porcupineEngine?.close() } catch (_: Throwable) {}
        porcupineEngine = null
    }

    // ------------- SpeechRecognizer fallback (bn-BD) -------------
    private fun startSpeechRecognizerFallback() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "SpeechRecognizer not available")
            return
        }
        stopSpeechRecognizerFallback()
        listening = true
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = RecognizerIntent().apply {
            action = RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Log.w(TAG, "fallback recognizer error: $error")
                if (listening) mainScope.launch { delay(400); if (listening) startSpeechRecognizerFallback() }
            }
            override fun onResults(results: Bundle?) {
                val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
                handleTextsForHotword(texts)
                if (listening) mainScope.launch { delay(200); if (listening) startSpeechRecognizerFallback() }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val texts = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
                handleTextsForHotword(texts)
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            speechRecognizer?.startListening(intent)
        } catch (t: Throwable) {
            Log.w(TAG, "startListening failed", t)
        }
    }

    private fun stopSpeechRecognizerFallback() {
        listening = false
        try { speechRecognizer?.cancel(); speechRecognizer?.destroy() } catch (_: Throwable) {}
        speechRecognizer = null
    }

    private fun handleTextsForHotword(texts: ArrayList<String>) {
        for (t in texts) {
            val low = t.lowercase()
            // check Bangla phrase variants and English transliterated variants
            if ((low.contains("কিং") && low.contains("অ্যাসিস্ট্যান্ট")) ||
                low.contains("king assistant") ||
                low.contains("কিং অ্যাসিস্ট্যান্ট") ||
                low.contains("কিংঅ্যাসিস্ট্যান্ট")) {
                Log.i(TAG, "fallback hotword matched: $t")
                mainScope.launch { listener?.onHotwordDetected() }
                return
            }
        }
    }
}
