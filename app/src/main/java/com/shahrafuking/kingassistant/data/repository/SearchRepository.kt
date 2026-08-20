package com.shahrafuking.kingassistant.data.repository

import android.content.Context
import com.shahrafuking.kingassistant.data.model.SearchResult
import com.shahrafuking.kingassistant.data.persistence.AppDatabase
import com.shahrafuking.kingassistant.data.persistence.SearchResultEntity
import com.shahrafuking.kingassistant.data.provider.BackendProxyProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SearchRepository(private val ctx: Context) {
    private val db = AppDatabase.getInstance(ctx)
    private val dao = db.searchResultDao()

    fun allResults(): Flow<List<SearchResult>> = dao.allResults().map { list ->
        list.map { entity -> SearchResult(entity.url, entity.title, entity.snippet, entity.url, entity.source, entity.fetchedAt) }
    }

    fun searchLocal(q: String): Flow<List<SearchResult>> = dao.search(q).map { list ->
        list.map { entity -> SearchResult(entity.url, entity.title, entity.snippet, entity.url, entity.source, entity.fetchedAt) }
    }

    suspend fun searchRemote(provider: String, q: String, feed: String? = null, url: String? = null): List<SearchResult> {
        val providerImpl = BackendProxyProvider(ctx)
        val results = providerImpl.search(provider, q, feed, url)
        if (results.isNotEmpty()) {
            saveResults(results)
        }
        return results
    }

    suspend fun saveResults(results: List<SearchResult>) {
        val entities = results.map { SearchResultEntity(it.url, it.title, it.snippet, it.source, it.fetchedAt) }
        dao.insertAll(entities)
    }
}
