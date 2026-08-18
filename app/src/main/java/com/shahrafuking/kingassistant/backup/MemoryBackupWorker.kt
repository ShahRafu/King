@@
 import java.text.SimpleDateFormat
 import java.util.*
 
 class MemoryBackupWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
     override suspend fun doWork(): Result {
         return try {
             val ctx = applicationContext
             val backups = File(ctx.filesDir, "backups")
             if (!backups.exists()) backups.mkdirs()
             val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
             val out = File(backups, "memory_backup_$timestamp.txt")
             out.writeText("Backup created at $timestamp\n(placeholder for memory dump)")
             Log.i("MemoryBackupWorker", "backup created: ${out.absolutePath}")
-
-            // Attempt to upload to configured GitHub repo if configured
-            try {
-                val prefs = ctx.getSharedPreferences("king_prefs", android.content.Context.MODE_PRIVATE)
-                val owner = prefs.getString("backup_remote_owner", null)
-                val repo = prefs.getString("backup_remote_repo", null)
-                val remotePathBase = prefs.getString("backup_remote_path", "backups") ?: "backups"
-                if (!owner.isNullOrBlank() && !repo.isNullOrBlank()) {
-                    val uploader = com.shahrafuking.kingassistant.net.GitHubUploader(ctx)
-                    val remotePath = "$remotePathBase/${out.name}"
-                    val ok = uploader.uploadFile(owner, repo, remotePath, out, "king: weekly backup ${out.name}")
-                    if (ok) Log.i("MemoryBackupWorker", "backup uploaded to $owner/$repo:$remotePath")
-                    else Log.e("MemoryBackupWorker", "backup upload failed for $owner/$repo:$remotePath")
-                } else {
-                    Log.i("MemoryBackupWorker", "no remote repo configured; skipping upload")
-                }
-            } catch (e: Throwable) {
-                Log.e("MemoryBackupWorker", "upload attempt failed", e)
-            }
-
-            Result.success()
+            // Attempt to upload to configured GitHub repo if configured
+            val prefs = ctx.getSharedPreferences("king_prefs", android.content.Context.MODE_PRIVATE)
+            val owner = prefs.getString("backup_remote_owner", null)
+            val repo = prefs.getString("backup_remote_repo", null)
+            val remotePathBase = prefs.getString("backup_remote_path", "backups") ?: "backups"
+            if (!owner.isNullOrBlank() && !repo.isNullOrBlank()) {
+                try {
+                    val uploader = com.shahrafuking.kingassistant.net.GitHubUploader(ctx)
+                    val remotePath = "$remotePathBase/${out.name}"
+                    val ok = uploader.uploadFile(owner, repo, remotePath, out, "king: weekly backup ${out.name}")
+                    if (ok) {
+                        Log.i("MemoryBackupWorker", "backup uploaded to $owner/$repo:$remotePath")
+                        return Result.success()
+                    } else {
+                        Log.e("MemoryBackupWorker", "backup upload failed for $owner/$repo:$remotePath")
+                        return Result.retry()
+                    }
+                } catch (e: Throwable) {
+                    Log.e("MemoryBackupWorker", "upload attempt failed", e)
+                    return Result.retry()
+                }
+            }
+
+            // No remote configured - keep local backup and succeed
+            Log.i("MemoryBackupWorker", "no remote repo configured; skipping upload")
+            return Result.success()
         } catch (e: Throwable) {
             Log.e("MemoryBackupWorker", "backup failed", e)
             Result.retry()
         }
     }
 }
