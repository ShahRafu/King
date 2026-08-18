package com.shahrafuking.kingassistant.plugin

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Simple plugin update checker (stub).
 * Replace parsing with proper JSON library when integrating.
 */
class PluginUpdateChecker(private val ctx: Context) {
    data class RemoteInfo(val id: String, val version: String, val checksum: String?)

    suspend fun fetchRemoteInfo(metadataUrl: String): RemoteInfo? = withContext(Dispatchers.IO) {
        try {
            val u = URL(metadataUrl)
            val conn = (u.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
            }
            if (conn.responseCode != 200) return@withContext null
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val id = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1) ?: "unknown"
            val version = Regex("\"version\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1) ?: "0.0.0"
            val checksum = Regex("\"checksum\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1)
            return@withContext RemoteInfo(id = id, version = version, checksum = checksum)
        } catch (e: Throwable) {
            return@withContext null
        }
    }

    fun isNewer(localVersion: String, remoteVersion: String): Boolean {
        val lv = localVersion.split('.').mapNotNull { it.toIntOrNull() }
        val rv = remoteVersion.split('.').mapNotNull { it.toIntOrNull() }
        return rv > lv
    }
}
