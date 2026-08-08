package com.shahrafuking.kingassistant.storage.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val metaJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
