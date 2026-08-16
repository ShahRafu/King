package com.shahrafuking.kingassistant.memory

import com.shahrafuking.kingassistant.storage.room.MemoryDao
import com.shahrafuking.kingassistant.storage.room.MemoryEntity
import com.shahrafuking.kingassistant.storage.room.MemoryUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MemoryRepository(private val dao: MemoryDao) {

    suspend fun addMemory(text: String, embedding: FloatArray? = null, metadata: Map<String, String>? = null): Long {
        val embBase64 = embedding?.let { MemoryUtils.floatArrayToBase64(it) }
        val metadataJson = metadata?.let { JSONObject(it as Map<*, *>).toString() }
        val entry = MemoryEntity(text = text, embeddingBase64 = embBase64, metadataJson = metadataJson)
        return withContext(Dispatchers.IO) {
            dao.insert(entry)
        }
    }

    suspend fun getMemory(id: Long): MemoryEntity? = withContext(Dispatchers.IO) { dao.getById(id) }

    suspend fun getAll(): List<MemoryEntity> = withContext(Dispatchers.IO) { dao.getAll() }

    suspend fun delete(id: Long): Int = withContext(Dispatchers.IO) { dao.deleteById(id) }

    // clearAll omitted because DAO does not define it in storage.room version; add if needed

    suspend fun searchSimilar(embedding: FloatArray, topK: Int = 5): List<Pair<MemoryEntity, Float>> {
        return withContext(Dispatchers.Default) {
            val all = dao.getAll()
            val index = SimpleVectorIndex()
            val idToEntry = mutableMapOf<Long, MemoryEntity>()
            for (e in all) {
                val vec = MemoryUtils.base64ToFloatArray(e.embeddingBase64)
                if (vec != null) {
                    index.add(e.id, vec)
                    idToEntry[e.id] = e
                }
            }
            val results = index.search(embedding, topK)
            results.mapNotNull { (id, score) -> idToEntry[id]?.let { it to score } }
        }
    }
}
