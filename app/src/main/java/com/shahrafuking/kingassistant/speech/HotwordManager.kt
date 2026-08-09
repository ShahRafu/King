package com.shahrafuking.kingassistant.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.shahrafuking.kingassistant.BuildConfig
import com.shahrafuking.kingassistant.audio.AudioRecorder
import kotlinx.coroutines.*

/**
 * HotwordManager
 *
 * - If PORCUPINE_ENABLED (BuildConfig) and HotwordPorcupineEngine.init() succeeds, uses AudioRecorder ->
 *   Porcupine process(pcm) path for low-latency offline wakeword detection.
 * - Otherwise uses Android SpeechRecognizer in bn-BD to listen continuously for the hotword phrase.
 *
 * Usage:
 *  val manager = HotwordManager(context)
 *  manager.setListener { onHotwordDetected() }
 *  manager.start()
 *  manager.stop()  // on destroy
 */
class HotwordManager(private val context: Context) {
    private val TAG = "HotwordManager"

    interface HotwordListener {
        fun onHotwordDetected()
    }

    private var listener: HotwordListener? = null
    fun setListener(l: HotwordListener) { listener = l }

    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Audio/Porcupine pieces
    private var audioRecorder: AudioRecorder? = null
    private var porcupineEngine: HotwordPorcupineEngine? = null

    // Fallback speech recognizer
    private var speechRecognizer: SpeechRecognizer? = null
    private var listening = false

    fun start() {
        if (isPorcupineEnabled()) {
            try {
                porcupineEngine = HotwordPorcupineEngine(context)
                val ok = porcupineEngine?.init() ?: false
                if (ok) {
                    startPorcupinePath()
                    Log.i(TAG, "HotwordManager: started Porcupine path")
                    return
                } else {
                    Log.w(TAG, "HotwordManager: Porcupine init failed, falling back")
                }
            } catch (t: Throwable) {
                Log.w(TAG, "HotwordManager: Porcupine exception", t)
            }
        }
        startSpeechRecognizerFallback()
        Log.i(TAG, "HotwordManager: started SpeechRecognizer fallback (bn-BD)")
    }

    fun stop() {
        stopPorcupinePath()
        stopSpeechRecognizerFallback()
        mainScope.cancel()
    }

    private fun isPorcupineEnabled(): Boolean {
        return try { BuildConfig.PORCUPINE_ENABLED } catch (_: Throwable) { false }
    }

    // ---------------------------
    // Porcupine + AudioRecorder path
    // ---------------------------
    private fun startPorcupinePath() {
        audioRecorder = AudioRecorder(context)
        if (!audioRecorder!!.hasRecordPermission()) {
            Log.w(TAG, "startPorcupinePath: RECORD_AUDIO permission not granted")
            return
        }
        audioRecorder!!.start({ pcm16, sampleRate ->
            try {
                val detected = porcupineEngine?.process(pcm16, sampleRate) ?: false
                if (detected) {
                    Log.i(TAG, "Porcupine hotword detected")
                    // notify on main thread
                    mainScope.launch { listener?.onHotwordDetected() }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "porcupine process error", t)
            }
        }, AudioRecorder.DEFAULT_SAMPLE_RATE)
    }

    private fun stopPorcupinePath() {
        try {
            audioRecorder?.stop()
        } catch (_: Throwable) { }
        audioRecorder = null
        try {
            porcupineEngine?.close()
        } catch (_: Throwable) { }
        porcupineEngine = null
    }

    // ---------------------------
    // SpeechRecognizer fallback (language enforced to bn-BD)
    // ---------------------------
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
            // force Bangla
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
                // restart after short delay
                if (listening) mainScope.launch { delay(400); if (listening) startSpeechRecognizerFallback() }
            }
            override fun onResults(results: Bundle?) {
                val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
                handleTextsForHotword(texts)
                // continue listening
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
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Throwable) {}
        speechRecognizer = null
    }

    private fun handleTextsForHotword(texts: ArrayList<String>) {
        for (t in texts) {
            val low = t.lowercase()
            // check Bangla phrase variants and English transliterated variants
            if (low.contains("কিং") && low.contains("অ্যাসিস্ট্যান্ট") ||
                low.contains("king assistant") ||
                low.contains("কিং অ্যাসিস্ট্যান্ট") ||
                low.contains("কিংঅ্যাসিস্ট্যান্ট") ) {
                Log.i(TAG, "fallback hotword matched: $t")
                mainScope.launch { listener?.onHotwordDetected() }
                return
            }
        }
    }
}
