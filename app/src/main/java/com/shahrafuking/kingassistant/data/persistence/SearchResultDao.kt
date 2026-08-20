package com.shahrafuking.kingassistant.data.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchResultDao {
    @Query("SELECT * FROM search_results ORDER BY fetchedAt DESC")
    fun allResults(): Flow<List<SearchResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SearchResultEntity>)

    @Query("DELETE FROM search_results WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Query("SELECT * FROM search_results WHERE title LIKE '%' || :q || '%' OR snippet LIKE '%' || :q || '%' ORDER BY fetchedAt DESC")
    fun search(q: String): Flow<List<SearchResultEntity>>
}
