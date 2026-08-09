package com.shahrafuking.kingassistant.storage

import android.content.Context
import com.shahrafuking.kingassistant.storage.room.VoiceProfileEntity

class RoomRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val dao = db.voiceProfileDao()

    suspend fun saveVoiceProfile(entity: VoiceProfileEntity) {
        dao.insert(entity)
    }

    suspend fun findVoiceProfile(profileId: String): VoiceProfileEntity? {
        return dao.findById(profileId)
    }
}
