package com.shahrafuking.kingassistant.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

/**
 * AndroidTtsHelper - thin wrapper around TextToSpeech
 * - Creates TTS engine on demand and speaks strings.
 */
class AndroidTtsHelper(private val context: Context) : TextToSpeech.OnInitListener {
    private val TAG = "AndroidTtsHelper"
    private var tts: TextToSpeech? = null
    private var ready = false
    private var queue = mutableListOf<String>()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ready = true
            tts?.language = Locale("bn", "BD")
            // flush queued
            for (q in queue) speak(q)
            queue.clear()
        } else {
            Log.w(TAG, "TTS init failed: $status")
        }
    }

    fun speak(text: String) {
        if (!ready) {
            queue.add(text)
            return
        }
        try {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "KING_ASSISTANT_UTTERANCE")
        } catch (t: Throwable) {
            Log.w(TAG, "TTS speak failed", t)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Throwable) {}
        tts = null
        ready = false
    }
}
