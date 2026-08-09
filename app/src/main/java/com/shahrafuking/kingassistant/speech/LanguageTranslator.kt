// LanguageTranslator.kt
package com.shahrafuking.kingassistant.speech

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * LanguageTranslator
 *
 * Responsibilities:
 *  - detectLanguage(text): ML Kit language id (suspend)
 *  - translate(text, source, target): simple HTTP translator with retries & timeouts (suspend)
 *  - speakSafely(text, localeTag): Android TTS with thread-safe queueing and lifecycle
 *
 * NOTE:
 *  - Default translator endpoint is LibreTranslate demo instance. Replace with production provider
 *    (Google/Azure/DeepL etc) and supply credentials via secure secret manager.
 *  - Keep network calls off the main thread (we use Dispatchers.IO).
 */
class LanguageTranslator(private val context: Context) {
    private val TAG = "LanguageTranslator"

    // ML Kit language identifier
    private val languageIdentifier = LanguageIdentification.getClient(
        LanguageIdentificationOptions.Builder()
            .setTrustedThreshold(0.60f)
            .build()
    )

    // OkHttp client with sensible timeouts
    private val httpClient = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // Default translation endpoint (fallback/demo). Replace in production.
    private val defaultTranslateEndpoint = "https://libretranslate.de/translate"

    // TTS and lifecycle control
    @Volatile private var tts: TextToSpeech? = null
    private val ttsInitialized = AtomicBoolean(false)
    private val ttsLock = Object()

    init {
        // Initialize lazily - do not block construction
        ensureTtsInitialized()
    }

    private fun ensureTtsInitialized() {
        if (ttsInitialized.get()) return
        synchronized(ttsLock) {
            if (ttsInitialized.get()) return
            try {
                tts = TextToSpeech(context.applicationContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        ttsInitialized.set(true)
                        Log.i(TAG, "TTS initialized")
                    } else {
                        Log.w(TAG, "TTS init failed: $status")
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "TTS init exception", t)
            }
        }
    }

    suspend fun detectLanguage(text: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext "und"
        suspendCoroutine { cont ->
            languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener { lang ->
                    if (lang == "und") cont.resume("und") else cont.resume(lang)
                }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
    }

    /**
     * Translate text with lightweight retry/backoff logic.
     * Returns translated string or empty string on failure.
     */
    suspend fun translate(text: String, sourceLang: String, targetLang: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext ""
        val endpoint = defaultTranslateEndpoint
        val mediaType = "application/json; charset=utf-8".toMediaType()
        // Prepare JSON body per LibreTranslate API; adapt if using different provider
        val jsonBody = JSONObject().apply {
            put("q", text)
            put("source", if (sourceLang == "und") "auto" else sourceLang)
            put("target", targetLang)
            put("format", "text")
        }
        val body = RequestBody.create(mediaType, jsonBody.toString())
        val req = Request.Builder().url(endpoint).post(body).build()

        var attempt = 0
        val maxRetries = 2
        var lastErr: Throwable? = null
        while (attempt <= maxRetries) {
            try {
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        Log.w(TAG, "translate failed: ${resp.code} ${resp.message}")
                        return@withContext ""
                    }
                    val respBody = resp.body?.string() ?: ""
                    if (respBody.isBlank()) return@withContext ""
                    val obj = JSONObject(respBody)
                    val translated = obj.optString("translatedText", "")
                    return@withContext translated
                }
            } catch (t: Throwable) {
                lastErr = t
                attempt++
                // simple backoff
                try { Thread.sleep(300L * attempt) } catch (_: InterruptedException) {}
            }
        }
        Log.w(TAG, "translate failed after retries", lastErr)
        return@withContext ""
    }

    /**
     * Convenience: detect language then translate to targetLang if needed.
     * Returns Pair(detectedLang, translatedText) where translatedText is original if translation skipped or fails.
     */
    suspend fun detectAndTranslateTo(text: String, targetLang: String): Pair<String, String> {
        val detected = try { detectLanguage(text) } catch (t: Throwable) { "und" }
        if (detected == targetLang || (detected != "und" && detected.startsWith(targetLang))) {
            // already the target language (or same prefix)
            return Pair(detected, text)
        }
        val translated = try { translate(text, detected, targetLang) } catch (t: Throwable) { "" }
        return Pair(detected, if (translated.isNotBlank()) translated else text)
    }

    /**
     * Translate owner's Bengali reply to the target language and speak (play) it.
     * Returns translated text (or empty if failed).
     */
    suspend fun translateOwnerReplyAndSpeak(ownerTextBn: String, targetLang: String): String {
        if (ownerTextBn.isBlank()) return ""
        val translated = translate(ownerTextBn, "bn", targetLang)
        if (translated.isNotBlank()) {
            speakSafely(translated, targetLang)
            return translated
        }
        return ""
    }

    /**
     * Speak the given text using TTS. Tries to set locale by language tag (e.g., "bn-BD" or "en-US").
     * This method is thread-safe and non-blocking.
     */
    fun speakSafely(text: String, localeTag: String) {
        if (text.isBlank()) return
        ensureTtsInitialized()
        val localTts = tts ?: return
        try {
            val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Locale.forLanguageTag(localeTag)
            } else {
                val lang = localeTag.split("-").firstOrNull() ?: "bn"
                Locale(lang)
            }
            val res = try { localTts.setLanguage(locale) } catch (t: Throwable) { TextToSpeech.LANG_NOT_SUPPORTED }
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "TTS locale not supported: $localeTag; using default")
            }
            // Use utteranceId for completion tracking if needed
            val uttId = "utt_${System.currentTimeMillis()}"
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                localTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, uttId)
            } else {
                localTts.speak(text, TextToSpeech.QUEUE_FLUSH, null)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "speakSafely error", t)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Throwable) {}
        tts = null
        ttsInitialized.set(false)
        try { languageIdentifier.close() } catch (_: Throwable) {}
    }
}
