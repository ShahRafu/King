package com.shahrafuking.kingassistant.security

import android.content.Context
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import android.util.Log

/**
 * VoiceAuthStub:
 * - Lightweight scaffold: listens for phrase inclusion ("king assistant") using Android SpeechRecognizer.
 * - Also provides placeholder for future voice-print similarity check (NOT production-grade).
 *
 * NOTE: Real biometric voice verification requires a trained voice model and secure enrollment process.
 * This scaffold only detects phrase and returns a boolean; later you can replace similarityCheck(...) implementation.
 */
class VoiceAuthStub(private val activity: ComponentActivity) {
    private val TAG = "VoiceAuthStub"

    fun startAuth(callback: (Boolean) -> Unit) {
        val sr = SpeechRecognizer.createSpeechRecognizer(activity)
        sr.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Log.e(TAG, "Speech error: $error")
                callback(false)
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.joinToString(" ") ?: ""
                val phraseDetected = text.lowercase().contains("king assistant") || text.lowercase().contains("কিং অ্যাসিস্টেন্ট")
                // placeholder voice-print check: TODO replace with real model
                val voicePrintOk = simpleSimilarityCheck(text)
                callback(phraseDetected && voicePrintOk)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
        sr.startListening(intent)
    }

    private fun simpleSimilarityCheck(recognizedText: String): Boolean {
        // Placeholder: in production, compare audio embeddings of current speech vs enrolled voice-print.
        // For now, return true if recognizedText is non-empty; this is just a scaffold.
        return recognizedText.isNotBlank()
    }
}
