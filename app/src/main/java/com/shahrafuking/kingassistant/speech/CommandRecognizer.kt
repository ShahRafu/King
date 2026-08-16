package com.shahrafuking.kingassistant.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

/**
 * CommandRecognizer
 * - Wrapper around Android SpeechRecognizer. Provides listenOnce(callback) API.
 * - Also provides a tiny rule-based extractor for basic entities like budget amounts.
 */
class CommandRecognizer(private val context: Context) {
    private val TAG = "CommandRecognizer"
    private var recognizer: SpeechRecognizer? = null

    interface CommandListener {
        fun onCommandResult(text: String)
        fun onCommandError(reason: String)
    }

    fun listenOnce(listener: CommandListener) {
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                listener.onCommandError("SpeechRecognizer not available")
                return
            }
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    listener.onCommandError("recognizer error $error")
                }
                override fun onResults(results: Bundle?) {
                    val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val t = texts?.firstOrNull() ?: ""
                    // deliver raw recognized text
                    listener.onCommandResult(t)
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    // ignore partials for command
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            recognizer?.startListening(intent)
            // Safety: caller may cancel externally by calling cancel()
        } catch (t: Throwable) {
            Log.w(TAG, "listenOnce error", t)
            listener.onCommandError("listenOnce exception")
        }
    }

    fun cancel() {
        try { recognizer?.cancel(); recognizer?.destroy() } catch (_: Throwable) {}
        recognizer = null
    }

    /** tiny budget extractor: finds a currency amount number in text (e.g., '২০ ডলার' or '20 dollar') */
    suspend fun extractBudgetFrom(text: String): Double? = withContext(Dispatchers.Default) {
        try {
            // naive regex: numbers with optional decimals
            val p = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)")
            val m = p.matcher(text.replace(",", "."))
            if (m.find()) {
                val v = m.group(1)
                return@withContext v.toDoubleOrNull()
            }
            return@withContext null
        } catch (t: Throwable) {
            null
        }
    }
}
