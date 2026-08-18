package com.shahrafuking.kingassistant.net

import android.util.Log
import com.shahrafuking.kingassistant.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Simple OpenAI Chat client (OkHttp).
 * Reads API key from BuildConfig or environment.
 */
class ApiClientOpenAI(
    private val baseUrl: String = "https://api.openai.com/v1/chat/completions",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    private val TAG = "ApiClientOpenAI"

    suspend fun chat(systemPrompt: String? = null, userPrompt: String): String = withContext(Dispatchers.IO) {
        val apiKey: String = try {
            // prefer BUILD config key name OPENAI_API_KEY
            try {
                BuildConfig::class.java.getField("OPENAI_API_KEY").get(null) as? String ?: ""
            } catch (_: Throwable) {
                try { BuildConfig::class.java.getField("API_KEY").get(null) as? String ?: "" } catch (_: Throwable) { "" }
            }
        } catch (_: Throwable) {
            System.getenv("OPENAI_API_KEY") ?: ""
        }

        if (apiKey.isBlank()) {
            Log.w(TAG, "OpenAI API key not set. Returning demo fallback from ApiClientOpenAI.chat()")
            return@withContext "[DEMO_FALLBACK] OpenAI API key not configured. Configure OPENAI_API_KEY via buildConfig or env."
        }

        val messages = JSONArray()
        systemPrompt?.let {
            messages.put(JSONObject().put("role", "system").put("content", it))
        }
        messages.put(JSONObject().put("role", "user").put("content", userPrompt))

        val payload = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", messages)
            put("max_tokens", 800)
            put("temperature", 0.3)
        }

        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder()
            .url(baseUrl)
            .post(body)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "application/json")
            .build()

        try {
            client.newCall(req).execute().use { res ->
                val code = res.code
                val respBody = res.body?.string()
                if (!res.isSuccessful) {
                    Log.e(TAG, "OpenAI error ($code): $respBody")
                    return@withContext "[ERROR] OpenAI error: $code"
                }
                if (respBody == null) return@withContext "[ERROR] Empty response body from OpenAI"
                val j = JSONObject(respBody)
                val choices = j.optJSONArray("choices") ?: JSONArray()
                if (choices.length() == 0) {
                    return@withContext j.toString()
                }
                val msg = choices.getJSONObject(0).optJSONObject("message") ?: JSONObject()
                return@withContext msg.optString("content", j.toString())
            }
        } catch (e: Exception) {
            Log.e(TAG, "OpenAI request failed: ${e.message}", e)
            return@withContext "[ERROR] OpenAI request failed: ${e.message}"
        }
    }
}
