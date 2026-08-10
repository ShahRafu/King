*** Begin Patch
*** Update File: app/src/main/java/com/shahrafuking/kingassistant/memory/MemoryRepository.kt
@@
     suspend fun getAll(): List<MemoryEntry> = withContext(Dispatchers.IO) { dao.getAll() }
@@
     suspend fun clearAll() = withContext(Dispatchers.IO) { dao.clearAll() }
+
+    /**
+     * Return recent memory entries filtered by metadata 'type' values or by text keyword.
+     * This helper is useful to show audit/history views in UI.
+     */
+    suspend fun getRecentByTypes(types: List<String>, limit: Int = 50): List<MemoryEntry> = withContext(Dispatchers.IO) {
+        val all = dao.getAll()
+        val filtered = all.filter { e ->
+            val md = e.metadataJson ?: ""
+            val text = e.text ?: ""
+            types.any { t -> md.contains("\"type\":\"$t\"") || text.contains(t) }
+        }
+        return@withContext filtered.take(limit)
+    }
*** End Patch
