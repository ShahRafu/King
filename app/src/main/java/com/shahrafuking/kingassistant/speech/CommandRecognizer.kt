package com.shahrafuking.kingassistant.speech

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.*

/**
 * One-shot command recognizer configured for Bengali (bn-BD).
 *
 * Usage:
 *   val rec = CommandRecognizer(context)
 *   rec.listenOnce { text -> /* handle command text (bn-BD) */ }
 *   rec.cancel()  // if needed
 */
class CommandRecognizer(private val context: Context) {
    private val TAG = "CommandRecognizer"
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    interface CommandListener {
        fun onCommandResult(text: String)
        fun onCommandError(reason: String)
    }

    fun listenOnce(listener: CommandListener) {
        try {
            speechRecognizer?.destroy()
        } catch (_: Throwable) {}
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onCommandError("SpeechRecognizer not available")
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = RecognizerIntent().apply {
            action = RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // Force Bengali
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Log.w(TAG, "command recognizer error: $error")
                listener.onCommandError("error:$error")
                try { speechRecognizer?.destroy() } catch (_: Throwable) {}
            }
            override fun onResults(results: Bundle?) {
                val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val result = texts?.firstOrNull() ?: ""
                listener.onCommandResult(result)
                try { speechRecognizer?.destroy() } catch (_: Throwable) {}
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        try {
            speechRecognizer?.startListening(intent)
            // Safety timeout in case recognizer never returns
            mainScope.launch {
                delay(8000)
                listener.onCommandError("timeout")
                try { speechRecognizer?.cancel(); speechRecognizer?.destroy() } catch (_: Throwable) {}
            }
        } catch (t: Throwable) {
            listener.onCommandError("startListening failed")
        }
    }

    fun cancel() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Throwable) {}
        speechRecognizer = null
        mainScope.cancel()
    }
}
