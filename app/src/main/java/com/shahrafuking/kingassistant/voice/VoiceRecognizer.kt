package com.shahrafuking.kingassistant.voice

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Simple wrapper around Android SpeechRecognizer. Call startListening() to begin and
 * stopListening() to stop. Recognized text lines are delivered to the onResult callback.
 *
 * Note: host Activity/Service must ensure RECORD_AUDIO runtime permission is granted.
 */
class VoiceRecognizer(private val context: Context) {
    private val tag = "VoiceRecognizer"
    private var recognizer: SpeechRecognizer? = null
    private var listener: RecognitionListener? = null

    fun startListening(onResult: (String) -> Unit, onError: ((String) -> Unit)? = null) {
        stopListening()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError?.invoke("Speech recognition not available on this device")
            return
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { sr ->
            listener = object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onPartialResults(partialResults: android.os.Bundle?) {}
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    val msg = "SpeechRecognizer error: $error"
                    Log.w(tag, msg)
                    onError?.invoke(msg)
                }

                override fun onResults(results: android.os.Bundle?) {
                    val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = list?.firstOrNull() ?: ""
                    onResult(text)
                }
            }
            sr.setRecognitionListener(listener)

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                // Prefer device locale; callers can post-filter languages as needed
            }
            sr.startListening(intent)
        }
    }

    fun stopListening() {
        try {
            recognizer?.apply {
                cancel()
                destroy()
            }
        } catch (t: Throwable) {
            Log.w(tag, "stopListening failed: ${t.localizedMessage}")
        }
        recognizer = null
        listener = null
    }
}
