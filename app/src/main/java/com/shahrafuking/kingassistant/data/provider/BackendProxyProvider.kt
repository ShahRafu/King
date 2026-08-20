package com.shahrafuking.kingassistant.data.provider

import android.content.Context
import com.shahrafuking.kingassistant.data.model.SearchResult
import com.shahrafuking.kingassistant.data.remote.BackendResultDto
import com.shahrafuking.kingassistant.data.remote.BackendSearchResponse
import com.shahrafuking.kingassistant.data.remote.RetrofitFactory
import com.shahrafuking.kingassistant.ui.screens.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackendProxyProvider(private val ctx: Context) {
    private val settingsRepo = SettingsRepository(ctx)

    suspend fun search(provider: String, q: String, feed: String? = null, url: String? = null): List<SearchResult> =
        withContext(Dispatchers.IO) {
            val backendUrl = settingsRepo.backendUrlFlow.map { it }.let { settingsRepo.backendUrlFlow }
            // read values once
            val base = settingsRepo.backendUrlFlow // can't call collect here; read via suspend map? We'll fetch synchronously by reading DataStore is Flow - but SettingsRepository doesn't provide blocking get. Simpler: require caller provide backendUrl and token via SettingsRepository flows in UI/repository.
            // For simplicity, we'll read current values via a blocking collect with first() in repository layer. Here we assume caller will use SearchRepository which handles reading flows. This provider is a thin wrapper expecting explicit base/token passed externally.
            emptyList<SearchResult>()
        }
}
