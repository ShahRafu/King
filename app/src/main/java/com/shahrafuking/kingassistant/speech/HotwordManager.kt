package com.shahrafuking.kingassistant.speech

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.*

/**
 * HotwordManager (strict detection)
 * - Only triggers for strong variants of the phrase "King Assistant" (english/bangla)
 * - On detection, calls HotwordListener.onHotwordDetected() and does NOT perform any further actions itself.
 */
class HotwordManager(private val context: Context) {
    private val TAG = "HotwordManager"

    interface HotwordListener { fun onHotwordDetected() }
    private var listener: HotwordListener? = null
    fun setListener(l: HotwordListener) { listener = l }

    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var speechRecognizer: SpeechRecognizer? = null
    private var listening = false

    fun start() {
        if (listening) return
        listening = true
        startSpeechRecognizerFallback()
    }

    fun stop() {
        listening = false
        try { speechRecognizer?.cancel(); speechRecognizer?.destroy() } catch (_: Throwable) {}
        speechRecognizer = null
        try { mainScope.cancel() } catch (_: Throwable) {}
    }

    private fun startSpeechRecognizerFallback() {
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.w(TAG, "SpeechRecognizer not available")
                return
            }
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val intent = RecognizerIntent().apply {
                action = RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    if (listening) mainScope.launch { delay(500); restartRecognizer() }
                }
                override fun onResults(results: Bundle?) {
                    val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
                    handleTextsForHotword(texts)
                    if (listening) mainScope.launch { delay(200); restartRecognizer() }
                }
                override fun onPartialResults(partial: Bundle?) {
                    val texts = partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
                    handleTextsForHotword(texts)
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            speechRecognizer?.startListening(intent)
        } catch (t: Throwable) {
            Log.w(TAG, "startSpeechRecognizerFallback failed", t)
        }
    }

    private fun restartRecognizer() {
        try { speechRecognizer?.cancel(); speechRecognizer?.destroy() } catch (_: Throwable) {}
        speechRecognizer = null
        if (listening) startSpeechRecognizerFallback()
    }

    private fun handleTextsForHotword(texts: ArrayList<String>) {
        for (t in texts) {
            val low = t.lowercase().replace("\u200C", " ").trim()
            // strict checks: require both words in english or well-formed bangla phrase
            val englishOk = low.contains("king") && low.contains("assistant")
            val banglaOk = low.contains("কিং") && (low.contains("অ্যাসিস্ট্যান্ট") || low.contains("অ্যাসিস্টেন্ট") || low.contains("অ্যাসিস্ট"))
            if (englishOk || banglaOk) {
                Log.i(TAG, "hotword matched strict: $t")
                try { listener?.onHotwordDetected() } catch (_: Throwable) {}
                return
            }
        }
    }
}
