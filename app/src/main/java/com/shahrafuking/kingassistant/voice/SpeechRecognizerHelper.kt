package com.shahrafuking.kingassistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Lightweight wrapper around Android SpeechRecognizer for short phrases / commands.
 */
class SpeechRecognizerHelper(private val context: Context) {
    private val TAG = "SpeechRecognizerHelper"
    private var sr: SpeechRecognizer? = null
    private var intent: Intent? = null

    interface SpeechCallback {
        fun onResult(text: String)
        fun onError(error: String)
    }

    fun startListening(callback: SpeechCallback) {
        try {
            if (sr == null) {
                sr = SpeechRecognizer.createSpeechRecognizer(context)
                sr?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        callback.onError("SpeechRecognizer error $error")
                    }
                    override fun onResults(results: Bundle?) {
                        val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = list?.firstOrNull() ?: ""
                        callback.onResult(text)
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
            if (intent == null) {
                intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }
            }
            sr?.startListening(intent)
        } catch (ex: Exception) {
            Log.e(TAG, "startListening failed: ${ex.message}")
            callback.onError(ex.message ?: "unknown")
        }
    }

    fun stopListening() {
        try { sr?.stopListening() } catch (_: Exception) {}
    }

    fun destroy() {
        try { sr?.destroy(); sr = null } catch (_: Exception) {}
    }

    companion object {
        fun isSpeechAvailable(context: Context): Boolean {
            return SpeechRecognizer.isRecognitionAvailable(context)
        }
    }
}
