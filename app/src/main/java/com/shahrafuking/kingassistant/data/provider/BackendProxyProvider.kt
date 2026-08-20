package com.shahrafuking.kingassistant.data.provider

import android.content.Context
import com.shahrafuking.kingassistant.data.model.SearchResult
import com.shahrafuking.kingassistant.data.remote.RetrofitFactory
import com.shahrafuking.kingassistant.settings.SecurePrefs
import com.shahrafuking.kingassistant.ui.screens.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

class BackendProxyProvider(private val ctx: Context) {
    private val settingsRepo = SettingsRepository(ctx)

    /**
     * Perform search via configured backend proxy.
     * Reads backend URL from SettingsRepository and token from SecurePrefs.
     */
    suspend fun search(provider: String, q: String, feed: String? = null, url: String? = null): List<SearchResult> =
        withContext(Dispatchers.IO) {
            val backendUrl = settingsRepo.backendUrlFlow.first()
            if (backendUrl.isBlank()) return@withContext emptyList()

            val token = SecurePrefs(ctx).getToken()
            val api = RetrofitFactory.create(backendUrl, token)

            val resp = api.search(provider = provider, q = q, feed = feed, url = url)
            return@withContext resp.results.map { dto ->
                SearchResult(
                    id = dto.url ?: dto.link ?: dto.title ?: q,
                    title = dto.title ?: dto.link ?: dto.url ?: q,
                    snippet = dto.snippet ?: dto.paragraphs?.firstOrNull() ?: "",
                    url = dto.url ?: dto.link ?: dto.title ?: "",
                    source = dto.source ?: provider,
                    fetchedAt = System.currentTimeMillis()
                )
            }
        }
}
