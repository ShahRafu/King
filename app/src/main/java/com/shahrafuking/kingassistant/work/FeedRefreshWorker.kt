package com.shahrafuking.kingassistant.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shahrafuking.kingassistant.data.remote.RetrofitFactory
import com.shahrafuking.kingassistant.settings.SecurePrefs
import com.shahrafuking.kingassistant.ui.screens.SettingsRepository
import com.shahrafuking.kingassistant.data.repository.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

class FeedRefreshWorker(appContext: Context, params: WorkerParameters): CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val feedUrl = inputData.getString("feed_url") ?: return@withContext Result.failure()
        val settings = SettingsRepository(applicationContext)
        val secure = SecurePrefs(applicationContext)

        try {
            val backendUrl = settings.backendUrlFlow.first()
            val token = secure.getToken()
            if (backendUrl.isBlank()) return@withContext Result.failure()

            val api = RetrofitFactory.create(backendUrl, token)
            val resp = api.search(provider = "rss", q = "", feed = feedUrl)

            val repo = SearchRepository(applicationContext)
            val results = resp.results.map { dto ->
                com.shahrafuking.kingassistant.data.model.SearchResult(
                    id = dto.url ?: dto.link ?: dto.title ?: feedUrl,
                    title = dto.title ?: dto.link ?: dto.url ?: "",
                    snippet = dto.snippet ?: dto.paragraphs?.firstOrNull() ?: "",
                    url = dto.url ?: dto.link ?: dto.title ?: feedUrl,
                    source = dto.source ?: "rss",
                    fetchedAt = System.currentTimeMillis()
                )
            }
            if (results.isNotEmpty()) repo.saveResults(results)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
