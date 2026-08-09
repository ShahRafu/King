package com.shahrafuking.kingassistant.storage.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_profiles")
data class VoiceProfileEntity(
    @PrimaryKey val profileId: String,
    val ownerName: String,
    val samplePathsCsv: String,
    val embeddingHash: String
)
