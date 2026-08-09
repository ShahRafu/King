package com.shahrafuking.kingassistant.memory

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MemoryRepository(private val dao: MemoryDao) {

    suspend fun addMemory(text: String, embedding: FloatArray? = null, metadata: Map<String, String>? = null): Long {
        val embBase64 = embedding?.let { MemoryDatabase.floatArrayToBase64(it) }
        val metadataJson = metadata?.let { JSONObject(it as Map<*, *>).toString() }
        val entry = MemoryEntry(text = text, embeddingBase64 = embBase64, metadataJson = metadataJson)
        return withContext(Dispatchers.IO) {
            dao.insert(entry)
        }
    }

    suspend fun getMemory(id: Long): MemoryEntry? = withContext(Dispatchers.IO) { dao.getById(id) }

    suspend fun getAll(): List<MemoryEntry> = withContext(Dispatchers.IO) { dao.getAll() }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) { dao.deleteById(id) }

    suspend fun clearAll() = withContext(Dispatchers.IO) { dao.clearAll() }

    /**
     * Search top-k nearest memories by cosine similarity using an externally-computed embedding.
     * Returns list of Pair(MemoryEntry, similarityScore) sorted descending by score.
     */
    suspend fun searchSimilar(embedding: FloatArray, topK: Int = 5): List<Pair<MemoryEntry, Float>> {
        return withContext(Dispatchers.Default) {
            val all = dao.getAll()
            val index = SimpleVectorIndex()
            val idToEntry = mutableMapOf<Long, MemoryEntry>()
            for (e in all) {
                val vec = MemoryDatabase.base64ToFloatArray(e.embeddingBase64)
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
