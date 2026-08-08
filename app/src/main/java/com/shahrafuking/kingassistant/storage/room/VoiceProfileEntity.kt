package com.shahrafuking.kingassistant.storage.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_profiles")
data class VoiceProfileEntity(
    @PrimaryKey val profileId: String,
    val ownerName: String,
    val samplePathsCsv: String, // comma separated file paths of enrollment samples
    val embeddingHash: String, // placeholder hash representing enrolled voice-print
    val createdAt: Long = System.currentTimeMillis()
)
