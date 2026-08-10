package com.shahrafuking.kingassistant.speech

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * LanguageTranslator
 * - Minimal interface for detect/translate/speak operations used by OverlayService.
 * - This file defines an interface and a small local fallback implementation that
 *   simply returns input (identity translation). You can plug RemoteTranslator for real translation.
 */

interface Translator {
    /**
     * Returns Pair(detectedLang, translatedTextToTarget) or null on error
     */
    suspend fun detectAndTranslateTo(text: String, targetLang: String): Pair<String, String>?

    /**
     * Translate textFromOwner (owner speaks in bn) to targetLang and (optionally) speak it.
     * Returns translated string or empty on error.
     */
    suspend fun translateOwnerReplyAndSpeak(textFromOwnerBn: String, targetLang: String): String?

    /**
     * Safely speak text using TTS in specified locale string (e.g., "bn-BD")
     */
    fun speakSafely(text: String, localeTag: String = "bn-BD")
}

/**
 * SimpleFallbackTranslator does no real translation — it simulates detection
 * and returns input as-is. Good for testing integration.
 */
class SimpleFallbackTranslator(private val context: Context, private val ttsHelper: AndroidTtsHelper) : Translator {
    private val TAG = "SimpleFallbackTranslator"

    override suspend fun detectAndTranslateTo(text: String, targetLang: String): Pair<String, String>? {
        return withContext(Dispatchers.Default) {
            try {
                // naive: undetected -> assume incoming language not bn
                val detected = "und"
                val translated = text // fallback identity
                detected to translated
            } catch (t: Throwable) {
                Log.w(TAG, "detectAndTranslateTo failed", t)
                null
            }
        }
    }

    override suspend fun translateOwnerReplyAndSpeak(textFromOwnerBn: String, targetLang: String): String? {
        return withContext(Dispatchers.Default) {
            try {
                val out = textFromOwnerBn // identity
                // speak via TTS in target locale if needed (best-effort)
                ttsHelper.speak(out)
                out
            } catch (t: Throwable) {
                Log.w(TAG, "translateOwnerReplyAndSpeak failed", t)
                null
            }
        }
    }

    override fun speakSafely(text: String, localeTag: String) {
        try { ttsHelper.speak(text) } catch (t: Throwable) { Log.w(TAG, "speakSafely failed", t) }
    }
}
