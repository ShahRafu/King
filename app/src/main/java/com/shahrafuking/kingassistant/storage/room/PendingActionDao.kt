package com.shahrafuking.kingassistant.storage.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingActionDao {
    @Insert
    suspend fun insert(action: PendingActionEntity)

    // PendingActionEntity defines `createdAt`, keep Flow if callers expect reactive updates.
    @Query("SELECT * FROM pending_actions ORDER BY createdAt DESC")
    fun getAll(): Flow<List<PendingActionEntity>>

    @Query("DELETE FROM pending_actions WHERE id = :id")
    suspend fun deleteById(id: String)
}
