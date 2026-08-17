package com.shahrafuking.kingassistant.model

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * ModelClient: simple OkHttp wrapper adapted to modern okhttp3 APIs.
 * Uses ApiClientOpenAI for API key retrieval; provides safe fallbacks.
 */
class ModelClient(private val baseUrl: String, private val apiKey: String? = null) {
    private val client = OkHttpClient()

    fun requestCompletion(prompt: String, callback: (String?) -> Unit) {
        val resolvedKey = apiKey ?: try {
            ApiClientOpenAI.API_KEY
        } catch (_: Throwable) {
            null
        }

        val json = """{"prompt":"${escape(prompt)}","max_tokens":200}"""
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val builder = Request.Builder()
            .url(baseUrl)
            .post(body)

        if (!resolvedKey.isNullOrBlank()) {
            builder.addHeader("Authorization", "Bearer $resolvedKey")
        }

        val req = builder.build()

        client.newCall(req).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                callback(null)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string()
                callback(body)
            }
        })
    }

    private fun escape(s: String) = s.replace("\"", "\\\"")
}
