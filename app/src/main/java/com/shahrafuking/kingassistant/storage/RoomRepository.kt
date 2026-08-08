package com.shahrafuking.kingassistant.storage

import android.content.Context
import com.shahrafuking.kingassistant.storage.room.AppDatabase
import com.shahrafuking.kingassistant.storage.room.MemoryEntity
import com.shahrafuking.kingassistant.storage.room.PendingActionEntity
import com.shahrafuking.kingassistant.storage.room.VoiceProfileEntity
import kotlinx.coroutines.flow.Flow

class RoomRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val memoryDao = db.memoryDao()
    private val pendingDao = db.pendingActionDao()
    private val voiceDao = db.voiceProfileDao()

    suspend fun saveMemory(title: String, content: String): Long {
        return memoryDao.insert(MemoryEntity(title = title, content = content))
    }

    fun observeMemories(): Flow<List<MemoryEntity>> = memoryDao.getAll()

    suspend fun savePendingAction(entity: PendingActionEntity) {
        pendingDao.insert(entity)
    }

    fun observePending(): Flow<List<PendingActionEntity>> = pendingDao.getAll()

    suspend fun saveVoiceProfile(profile: VoiceProfileEntity) {
        voiceDao.insert(profile)
    }

    suspend fun findVoiceProfile(id: String): VoiceProfileEntity? = voiceDao.findById(id)
}
