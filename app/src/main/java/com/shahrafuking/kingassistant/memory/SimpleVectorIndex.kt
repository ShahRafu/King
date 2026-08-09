package com.shahrafuking.kingassistant.memory

/**
 * Very small in-memory vector index used for k-NN search (cosine similarity).
 * This is intentionally simple and meant for small to medium datasets.
 *
 * For production or large datasets, replace with an ANN library (FAISS, Annoy, HNSWlib)
 * or an on-disk/vector DB service.
 */
class SimpleVectorIndex {
    private data class Item(val id: Long, val vector: FloatArray)
    private val items = mutableListOf<Item>()

    fun add(id: Long, vector: FloatArray) {
        items.add(Item(id, vector))
    }

    fun clear() {
        items.clear()
    }

    fun search(query: FloatArray, topK: Int = 5): List<Pair<Long, Float>> {
        if (items.isEmpty()) return emptyList()
        val scored = items.map { it.id to cosineSimilarity(it.vector, query) }
        return scored.sortedByDescending { it.second }.take(topK)
    }

    private fun dot(a: FloatArray, b: FloatArray): Float {
        var s = 0.0f
        val n = minOf(a.size, b.size)
        for (i in 0 until n) s += a[i] * b[i]
        return s
    }

    private fun norm(a: FloatArray): Float {
        var s = 0.0f
        for (v in a) s += v * v
        return kotlin.math.sqrt(s)
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        val n = minOf(a.size, b.size)
        if (n == 0) return 0f
        val da = if (a.size == n) a else a.copyOf(n)
        val db = if (b.size == n) b else b.copyOf(n)
        val denom = (norm(da) * norm(db))
        return if (denom == 0f) 0f else dot(da, db) / denom
    }
}
