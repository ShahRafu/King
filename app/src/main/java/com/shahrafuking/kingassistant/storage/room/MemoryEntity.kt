package com.shahrafuking.kingassistant.storage.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val embeddingBase64: String? = null,
    val metadataJson: String? = null
)
