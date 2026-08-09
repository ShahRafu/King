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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * LanguageTranslator
 *
 * - detectLanguage(text): uses ML Kit language identification to return language code (e.g., "en", "bn").
 * - translate(text, source, target): uses LibreTranslate HTTP API (demo) to translate text.
 * - speak(text, localeTag): uses Android TextToSpeech to speak text in the requested locale (best effort).
 *
 * Usage:
 *   val lt = LanguageTranslator(context)
 *   val lang = lt.detectLanguage(text) // suspend
 *   val translated = lt.translate(text, lang, "bn")
 *   lt.speak(translated, "bn-BD")
 *
 * NOTE: For production use you should replace LibreTranslate endpoint with a reliable paid translator (Google/Azure/DeepL)
 * and manage API keys in local.properties/secure storage.
 */
class LanguageTranslator(private val context: Context) {
    private val TAG = "LanguageTranslator"

    // ML Kit language identifier
    private val languageIdentifier = LanguageIdentification.getClient(
        LanguageIdentificationOptions.Builder()
            .setTrustedThreshold(0.60f) // tweak threshold as needed
            .build()
    )

    // OkHttp client for translate HTTP calls
    private val httpClient = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    // TextToSpeech
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "TTS init failed: $status")
        }
    }

    suspend fun detectLanguage(text: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext "und"
        suspendCoroutine { cont ->
            languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener { lang ->
                    if (lang == "und") {
                        cont.resume("und")
                    } else {
                        cont.resume(lang)
                    }
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }
    }

    /**
     * Translate text. Returns translated string.
     * Uses LibreTranslate demo endpoint by default.
     * Replace endpoint & headers for production provider.
     */
    suspend fun translate(text: String, sourceLang: String, targetLang: String): String = withContext(Dispatchers.IO) {
        try {
            val endpoint = "https://libretranslate.de/translate" // demo; change to your instance or provider
            val json = JSONObject().apply {
                put("q", text)
                // LibreTranslate accepts source="auto" or explicit code
                put("source", if (sourceLang == "und") "auto" else sourceLang)
                put("target", targetLang)
                put("format", "text")
            }
            val body = RequestBody.create("application/json; charset=utf-8".toMediaType(), json.toString())
            val req = Request.Builder()
                .url(endpoint)
                .post(body)
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "translate failed: ${resp.code} ${resp.message}")
                    return@withContext ""
                }
                val respBody = resp.body?.string() ?: ""
                val obj = JSONObject(respBody)
                val translated = obj.optString("translatedText", "")
                return@withContext translated
            }
        } catch (t: Throwable) {
            Log.w(TAG, "translate error", t)
            return@withContext ""
        }
    }

    /**
     * Convenience: detect language then translate to targetLang if needed.
     * Returns Pair(detectedLang, translatedText) where translatedText is original if no translation performed.
     */
    suspend fun detectAndTranslateTo(text: String, targetLang: String): Pair<String, String> {
        val detected = try { detectLanguage(text) } catch (t: Throwable) { "und" }
        if (detected == "bn" || detected.startsWith("bn")) {
            // already Bengali — no translation needed
            return Pair(detected, text)
        }
        // attempt translation
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
     * Speak the given text using TTS. Tries to set locale by language tag (e.g., "bn-BD" or "en-US" or "hi-IN").
     * Use speakSafely to handle main thread invocation.
     */
    fun speakSafely(text: String, localeTag: String) {
        try {
            val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Locale.forLanguageTag(localeTag)
            } else {
                // fallback: split tag like "bn-BD" -> language "bn"
                val lang = localeTag.split("-").firstOrNull() ?: "bn"
                Locale(lang)
            }
            val res = tts.setLanguage(locale)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "TTS locale not supported: $localeTag, fallback to default")
            }
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lang_id_${System.currentTimeMillis()}")
        } catch (t: Throwable) {
            Log.w(TAG, "speakSafely error", t)
        }
    }

    fun shutdown() {
        try {
            tts.stop()
            tts.shutdown()
        } catch (_: Throwable) {}
        try {
            languageIdentifier.close()
        } catch (_: Throwable) {}
    }
}
