package com.shahrafuking.kingassistant.storage.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VoiceProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: VoiceProfileEntity)

    @Query("SELECT * FROM voice_profiles WHERE profileId = :id LIMIT 1")
    suspend fun findById(id: String): VoiceProfileEntity?
}
