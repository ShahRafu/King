package com.shahrafuking.kingassistant.voice

import android.util.Log

/**
 * Simple hotword detector stub (string-match).
 * Replace with Porcupine or robust engine for production.
 */
class HotwordDetector(private val onDetect: (String) -> Unit) {
    private val TAG = "HotwordDetector"
    private val keywords = listOf("king assistant", "king", "king assistant stop")

    fun consumeRecognizedText(text: String) {
        val s = text.trim().lowercase()
        for (k in keywords) {
            if (s.contains(k)) {
                Log.d(TAG, "Hotword matched: $k")
                onDetect(k)
                return
            }
        }
    }
}
