*** Begin Patch
*** Update File: app/src/main/java/com/shahrafuking/kingassistant/memory/MemoryRepository.kt
@@
     suspend fun clearAll() = withContext(Dispatchers.IO) { dao.clearAll() }
 
     /**
      * Search top-k nearest memories by cosine similarity using an externally-computed embedding.
      * Returns list of Pair(MemoryEntry, similarityScore) sorted descending by score.
      */
     suspend fun searchSimilar(embedding: FloatArray, topK: Int = 5): List<Pair<MemoryEntry, Float>> {
@@
         }
     }
+
+    /**
+     * Return recent memory entries filtered by metadata 'type' values or by text keyword.
+     * This helper is useful to show audit/history views in UI. Uses getAll() and filters in-memory
+     * to avoid adding new DAO queries for this POC.
+     */
+    suspend fun getRecentByTypes(types: List<String>, limit: Int = 50): List<MemoryEntry> = withContext(Dispatchers.IO) {
+        val all = dao.getAll()
+        val filtered = all.filter { e ->
+            val md = e.metadataJson ?: ""
+            val text = e.text ?: ""
+            types.any { t -> md.contains("\"type\":\"$t\"") || text.contains(t, ignoreCase = true) }
+        }
+        return@withContext filtered.take(limit)
+    }
*** End Patch
