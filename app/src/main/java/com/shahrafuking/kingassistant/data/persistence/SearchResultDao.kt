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

    // Return inserted row ids
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(items: List<SearchResultEntity>): List<Long>

    // Return number of rows deleted
    @Query("DELETE FROM search_results WHERE url = :url")
    fun deleteByUrl(url: String): Int

    @Query("SELECT * FROM search_results WHERE title LIKE '%' || :q || '%' OR snippet LIKE '%' || :q || '%' ORDER BY fetchedAt DESC")
    fun search(q: String): Flow<List<SearchResultEntity>>
}
