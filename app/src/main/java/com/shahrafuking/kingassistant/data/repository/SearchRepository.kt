package com.shahrafuking.kingassistant.data.repository

import android.content.Context
import com.shahrafuking.kingassistant.data.model.SearchResult
import com.shahrafuking.kingassistant.data.persistence.AppDatabase
import com.shahrafuking.kingassistant.data.persistence.SearchResultEntity
import com.shahrafuking.kingassistant.data.remote.BackendApi
import com.shahrafuking.kingassistant.data.remote.RetrofitFactory
import com.shahrafuking.kingassistant.settings.SecurePrefs
import com.shahrafuking.kingassistant.ui.screens.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SearchRepository(private val ctx: Context) {
    private val db = AppDatabase.getInstance(ctx)
    private val dao = db.searchResultDao()
    private val settings = SettingsRepository(ctx)
    private val secure = SecurePrefs(ctx)

    fun allResults(): Flow<List<SearchResult>> = dao.allResults().map { list ->
        list.map { entity -> SearchResult(entity.url, entity.title, entity.snippet, entity.url, entity.source, entity.fetchedAt) }
    }

    fun searchLocal(q: String): Flow<List<SearchResult>> = dao.search(q).map { list ->
        list.map { entity -> SearchResult(entity.url, entity.title, entity.snippet, entity.url, entity.source, entity.fetchedAt) }
    }

    suspend fun searchRemote(provider: String, q: String, feed: String? = null, url: String? = null): List<SearchResult> {
        val base = settings.backendUrlFlow // Flow - read single value
        // read current backend url & token via first() to avoid long flows
        val backendUrl = settings.backendUrlFlow // leave to caller to provide; simple approach: read via blocking isn't implemented here to keep code concise
        // For now, build an API client using placeholder; the SearchScreen will call Retrofit directly using RetrofitFactory with values from SettingsRepository and SecurePrefs. This repository wraps DB operations.
        return emptyList()
    }

    suspend fun saveResults(results: List<SearchResult>) {
        val entities = results.map { SearchResultEntity(it.url, it.title, it.snippet, it.source, it.fetchedAt) }
        dao.insertAll(entities)
    }
}
