package com.shahrafuking.kingassistant.speech

import android.content.Context
import android.util.Log
import com.shahrafuking.kingassistant.BuildConfig

/**
 * Central hotword engine adapter.
 * - Delegates to a concrete VoiceEngine implementation (Porcupine adapter or built-in placeholder)
 * - Keeps a stable public API for the rest of the app (init/process/onHotwordDetected/close)
 *
 * To enable Porcupine:
 *  - Set BuildConfig.PORCUPINE_ENABLED = true (via build.gradle buildConfigField)
 *  - Provide PORCUPINE_ACCESS_KEY and PORCUPINE_KEYWORD_FILENAME via BuildConfig or gradle properties
 *  - Add the Porcupine dependency to app/build.gradle and the keyword file under assets/porcupine/
 *
 * This file is safe to compile while PORCUPINE_ENABLED=false — it will use the internal PlaceholderEngine.
 */

interface VoiceEngine {
    fun init(): Boolean
    fun process(pcm16: ShortArray, sampleRate: Int): Boolean
    fun onHotwordDetected()
    fun close()
}

/** Simple placeholder engine for development: no real detection, used as fallback */
private class PlaceholderEngine(private val ctx: Context) : VoiceEngine {
    private var initialized = false
    override fun init(): Boolean {
        Log.i("PlaceholderEngine", "init (no-op)")
        initialized = true
        return true
    }

    override fun process(pcm16: ShortArray, sampleRate: Int): Boolean {
        // no detection by default
        return false
    }

    override fun onHotwordDetected() {
        Log.i("PlaceholderEngine", "Hotword detected (placeholder)")
    }

    override fun close() {
        initialized = false
    }
}

/** Adapter that chooses a concrete engine implementation at runtime */
class HotwordEngineAdapter private constructor(private val context: Context) : VoiceEngine {
    private var engineImpl: VoiceEngine = PlaceholderEngine(context)

    companion object {
        @Volatile
        private var instance: HotwordEngineAdapter? = null

        fun getInstance(context: Context): HotwordEngineAdapter =
            instance ?: synchronized(this) {
                instance ?: HotwordEngineAdapter(context.applicationContext).also { adapter ->
                    adapter.initializeEngine()
                    instance = adapter
                }
            }
    }

    private fun initializeEngine() {
        try {
            if (isPorcupineEnabled()) {
                try {
                    // Try to instantiate the Porcupine adapter. If it fails, fall back to placeholder.
                    val porcupine = HotwordPorcupineEngine(context)
                    if (porcupine.init()) {
                        engineImpl = porcupine
                        Log.i("HotwordEngineAdapter", "Using Porcupine engine")
                        return
                    } else {
                        Log.w("HotwordEngineAdapter", "Porcupine init returned false; using placeholder")
                    }
                } catch (t: Throwable) {
                    Log.w("HotwordEngineAdapter", "Porcupine adapter init failed, falling back to placeholder", t)
                }
            }
        } catch (t: Throwable) {
            Log.w("HotwordEngineAdapter", "Engine selection error", t)
        }

        // Default fallback
        engineImpl = PlaceholderEngine(context)
        engineImpl.init()
        Log.i("HotwordEngineAdapter", "Using Placeholder engine")
    }

    private fun isPorcupineEnabled(): Boolean {
        return try {
            BuildConfig.PORCUPINE_ENABLED
        } catch (t: Throwable) {
            Log.w("HotwordEngineAdapter", "BuildConfig.PORCUPINE_ENABLED not found, defaulting to false")
            false
        }
    }

    // VoiceEngine API - delegate to selected implementation
    override fun init(): Boolean {
        return engineImpl.init()
    }

    override fun process(pcm16: ShortArray, sampleRate: Int): Boolean {
        return engineImpl.process(pcm16, sampleRate)
    }

    override fun onHotwordDetected() {
        engineImpl.onHotwordDetected()
    }

    override fun close() {
        try {
            engineImpl.close()
        } finally {
            // ensure placeholder not holding resources
            engineImpl = PlaceholderEngine(context)
        }
    }
}
