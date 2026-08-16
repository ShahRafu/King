package com.shahrafuking.kingassistant.storage.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingActionDao {
    @Insert
    suspend fun insert(action: PendingActionEntity)

    // PendingActionEntity defines `createdAt`, so the query orders by createdAt.
    // Keep Flow here as the existing DAO used Flow<List<PendingActionEntity>>.
    @Query("SELECT * FROM pending_actions ORDER BY createdAt DESC")
    fun getAll(): Flow<List<PendingActionEntity>>

    // Keep signature consistent with existing code (returns Unit); callers can treat it as suspend.
    @Query("DELETE FROM pending_actions WHERE id = :id")
    suspend fun deleteById(id: String)
}
