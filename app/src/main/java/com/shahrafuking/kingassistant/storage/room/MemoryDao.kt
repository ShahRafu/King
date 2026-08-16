package com.shahrafuking.kingassistant.storage.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(memory: MemoryEntity): Long

    // MemoryEntity uses `timestamp`; repository expects a List.
    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    fun getAll(): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE id = :id")
    fun getById(id: Long): MemoryEntity?

    // Return number of rows deleted
    @Query("DELETE FROM memories WHERE id = :id")
    fun deleteById(id: Long): Int
}
