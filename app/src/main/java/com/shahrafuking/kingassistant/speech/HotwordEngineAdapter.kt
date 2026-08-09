package com.shahrafuking.kingassistant.speech

import android.content.Context
import android.util.Log

/**
 * Hotword engine abstraction and a Porcupine placeholder adapter.
 * Future: implement Vosk/other providers by implementing VoiceEngine interface.
 */
interface VoiceEngine {
    fun init(): Boolean
    fun process(pcm16: ShortArray, sampleRate: Int): Boolean
    fun onHotwordDetected()
    fun close()
}

class HotwordEngineAdapter private constructor(private val context: Context) : VoiceEngine {
    companion object {
        @Volatile
        private var instance: HotwordEngineAdapter? = null
        fun getInstance(context: Context): HotwordEngineAdapter = instance ?: synchronized(this) {
            instance ?: HotwordEngineAdapter(context.applicationContext).also { instance = it }
        }
    }

    private var initialized = false

    override fun init(): Boolean {
        // TODO: Initialize Porcupine or other engine here.
        // Add dependency and pass PORCOVINE_KEY from local.properties or BuildConfig.
        Log.i("HotwordEngine", "init placeholder")
        initialized = true
        return initialized
    }

    override fun process(pcm16: ShortArray, sampleRate: Int): Boolean {
        if (!initialized) init()
        // Placeholder logic: no detection performed here.
        // Replace with real engine inference.
        return false
    }

    override fun onHotwordDetected() {
        Log.i("HotwordEngine", "Hotword detected (placeholder)")
        // TODO: Trigger authentication, notification or UI broadcast
    }

    override fun close() { /* cleanup resources */ }
}
