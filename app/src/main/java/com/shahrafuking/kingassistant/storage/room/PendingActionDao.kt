package com.shahrafuking.kingassistant.storage.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingActionDao {
    @Insert
    fun insert(action: PendingActionEntity): Long

    // PendingActionEntity has `createdAt`
    @Query("SELECT * FROM pending_actions ORDER BY createdAt DESC")
    fun getAll(): List<PendingActionEntity>

    @Query("DELETE FROM pending_actions WHERE id = :id")
    fun deleteById(id: String): Int
}
