package com.shahrafuking.kingassistant.model

import okhttp3.*
import java.io.IOException

/**
 * ModelClient: simple OkHttp wrapper. Uses LLM_BASE_URL from gradle.properties (placeholder).
 * Replace endpoint and payload per the inference API you choose.
 */

class ModelClient(private val baseUrl: String, private val apiKey: String?) {
    private val client = OkHttpClient()

    fun requestCompletion(prompt: String, callback: (String?) -> Unit) {
        val json = """{"prompt": "${escape(prompt)}", "max_tokens": 200}"""
        val req = Request.Builder()
            .url(baseUrl)
            .post(RequestBody.create(MediaType.get("application/json; charset=utf-8"), json))
            .also {
                if (!apiKey.isNullOrBlank()) it.addHeader("Authorization", "Bearer $apiKey")
            }
            .build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                callback(body)
            }
        })
    }

    private fun escape(s: String) = s.replace("\"", "\\\"")
}
