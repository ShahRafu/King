package com.shahrafuking.kingassistant.storage.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryEntity): Long

    // MemoryEntity defines `timestamp`, so order by that column.
    // MemoryRepository expects a suspend List, so return a suspend List here.
    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    suspend fun getAll(): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getById(id: Long): MemoryEntity?

    // Return number of rows deleted (Int) to match callers that expect a status.
    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
