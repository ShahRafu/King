package com.shahrafuking.kingassistant.storage.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceProfileDao {
    @Insert
    suspend fun insert(profile: VoiceProfileEntity)

    @Query("SELECT * FROM voice_profiles WHERE profileId = :id LIMIT 1")
    suspend fun findById(id: String): VoiceProfileEntity?

    @Query("SELECT * FROM voice_profiles ORDER BY createdAt DESC")
    fun getAll(): Flow<List<VoiceProfileEntity>>

    @Query("DELETE FROM voice_profiles WHERE profileId = :id")
    suspend fun deleteById(id: String)
}
