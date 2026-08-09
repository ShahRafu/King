package com.shahrafuking.kingassistant.memory

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * MemoryRecord: simple persistable memory entry.
 * embedding is optional and stored as comma-separated string when present.
 */
data class MemoryRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val text: String,
    val embedding: DoubleArray? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("title", title)
        o.put("text", text)
        o.put("createdAt", createdAt)
        if (embedding != null) {
            val sb = StringBuilder()
            for (i in embedding.indices) {
                if (i > 0) sb.append(',')
                sb.append(embedding[i])
            }
            o.put("embedding", sb.toString())
        }
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): MemoryRecord {
            val id = o.optString("id", UUID.randomUUID().toString())
            val title = o.optString("title", "")
            val text = o.optString("text", "")
            val createdAt = o.optLong("createdAt", System.currentTimeMillis())
            val embStr = o.optString("embedding", null)
            val emb = if (embStr == null || embStr.isEmpty()) null else {
                embStr.split(',').mapNotNull { it.toDoubleOrNull() }.toDoubleArray()
            }
            return MemoryRecord(id = id, title = title, text = text, embedding = emb, createdAt = createdAt)
        }
    }
}
