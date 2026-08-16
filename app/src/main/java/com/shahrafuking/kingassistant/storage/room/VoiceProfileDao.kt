package com.shahrafuking.kingassistant.storage.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VoiceProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(profile: VoiceProfileEntity): Long

    @Query("SELECT * FROM voice_profiles WHERE profileId = :id LIMIT 1")
    fun findById(id: String): VoiceProfileEntity?
}
