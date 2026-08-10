package com.shahrafuking.kingassistant.llm

import android.content.Context
import android.util.Log
import com.shahrafuking.kingassistant.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Minimal OpenAI-style Chat client using OkHttp.
 *
 * - Uses BuildConfig.API_KEY or an empty string if not provided.
 * - Calls the chat completions endpoint and returns assistant text (or null on error).
 *
 * Notes:
 * - Do NOT commit API keys. Set API_KEY in local.properties or CI secrets and wire it into
 *   app/build.gradle as a BuildConfig field: buildConfigField "String", "API_KEY", '"<key>"'
 */
class ApiClientOpenAI(private val context: Context, private val baseUrl: String = "https://api.openai.com/v1") {
    companion object {
        private const val TAG = "ApiClientOpenAI"
        private val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() = try { BuildConfig.API_KEY } catch (t: Throwable) { "" }

    suspend fun chatCompletion(systemPrompt: String, messages: List<Pair<String, String>>, model: String = "gpt-3.5-turbo"): String? {
        return withContext(Dispatchers.IO) {
            try {
                val arr = JSONArray()
                // system message
                val sysObj = JSONObject().put("role", "system").put("content", systemPrompt)
                arr.put(sysObj)
                messages.forEach { (role, content) ->
                    val m = JSONObject().put("role", role).put("content", content)
                    arr.put(m)
                }

                val bodyJson = JSONObject()
                    .put("model", model)
                    .put("messages", arr)
                    .put("temperature", 0.6)

                val body = bodyJson.toString().toRequestBody(JSON)
                val reqBuilder = Request.Builder()
                    .url("$baseUrl/chat/completions")
                    .post(body)

                if (apiKey.isNotBlank()) {
                    reqBuilder.addHeader("Authorization", "Bearer $apiKey")
                }

                val req = reqBuilder.build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        Log.w(TAG, "chatCompletion failed http=${resp.code} msg=${resp.message}")
                        return@withContext null
                    }
                    val txt = resp.body?.string() ?: return@withContext null
                    val obj = JSONObject(txt)
                    val choices = obj.optJSONArray("choices") ?: return@withContext null
                    if (choices.length() == 0) return@withContext null
                    val msg = choices.getJSONObject(0).getJSONObject("message").optString("content", null)
                    return@withContext msg
                }
            } catch (t: Throwable) {
                Log.w(TAG, "chatCompletion error", t)
                null
            }
        }
    }
}
