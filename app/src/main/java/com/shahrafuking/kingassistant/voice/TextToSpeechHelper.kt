package com.shahrafuking.kingassistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Simple TextToSpeech wrapper.
 */
class TextToSpeechHelper(private val context: Context) : TextToSpeech.OnInitListener {
    private val TAG = "TTSHelper"
    private var tts: TextToSpeech? = null
    private val queue = ConcurrentLinkedQueue<String>()
    private var ready = false
    private var targetLocale: Locale = Locale("bn")

    init { tts = TextToSpeech(context, this) }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = targetLocale
            ready = true
            while (queue.isNotEmpty()) speakImmediate(queue.poll())
        } else Log.e(TAG, "TTS init failed: $status")
    }

    fun setLocale(locale: Locale) { targetLocale = locale; tts?.language = locale }
    fun speak(text: String) { if (!ready) { queue.offer(text); return } ; speakImmediate(text) }

    private fun speakImmediate(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
    }

    fun stop() { tts?.stop() }
    fun shutdown() { tts?.shutdown() }
}
