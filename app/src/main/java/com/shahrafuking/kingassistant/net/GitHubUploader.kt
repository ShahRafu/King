package com.shahrafuking.kingassistant.net

import android.util.Base64
import android.util.Log
import com.shahrafuking.kingassistant.security.SecretsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

/**
 * GitHubUploader - uploads a file to a GitHub repository using the Contents API.
 *
 * Requirements for runtime use:
 * - Store a personal access token with repo scope in SecretsManager under key "GITHUB_BACKUP_TOKEN"
 * - Configure target repo as owner and name in SharedPreferences (e.g., "backup_remote_owner" and "backup_remote_repo")
 *
 * Note: This is a best-effort implementation using OkHttp. Network calls must be made off the main thread.
 */
class GitHubUploader(private val ctx: android.content.Context) {
    private val client = OkHttpClient()
    private val sm = SecretsManager(ctx)

    suspend fun uploadFile(owner: String, repo: String, path: String, file: File, commitMessage: String = "king: backup upload") : Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val token = sm.getSecret("GITHUB_BACKUP_TOKEN") ?: return@withContext false
                val contentBytes = file.readBytes()
                val contentBase64 = Base64.encodeToString(contentBytes, Base64.NO_WRAP)

                // First, try to GET the existing file to learn its sha
                val getUrl = "https://api.github.com/repos/$owner/$repo/contents/$path"
                val getReq = Request.Builder()
                    .url(getUrl)
                    .addHeader("Authorization", "token $token")
                    .addHeader("Accept", "application/vnd.github+json")
                    .get()
                    .build()

                var sha: String? = null
                client.newCall(getReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string()
                        if (!body.isNullOrBlank()) {
                            val jo = JSONObject(body)
                            if (jo.has("sha")) sha = jo.getString("sha")
                        }
                    }
                }

                val putUrl = "https://api.github.com/repos/$owner/$repo/contents/$path"
                val payload = JSONObject()
                payload.put("message", commitMessage)
                payload.put("content", contentBase64)
                if (sha != null) payload.put("sha", sha)

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = payload.toString().toRequestBody(mediaType)
                val putReq = Request.Builder()
                    .url(putUrl)
                    .addHeader("Authorization", "token $token")
                    .addHeader("Accept", "application/vnd.github+json")
                    .put(body)
                    .build()

                client.newCall(putReq).execute().use { resp ->
                    val respBody = resp.body?.string()
                    if (resp.isSuccessful) {
                        Log.i("GitHubUploader", "upload successful: $path")
                        return@withContext true
                    } else {
                        Log.e("GitHubUploader", "upload failed: code=${resp.code} body=$respBody")
                        return@withContext false
                    }
                }
            } catch (e: Throwable) {
                Log.e("GitHubUploader", "upload exception", e)
                return@withContext false
            }
        }
    }
}
