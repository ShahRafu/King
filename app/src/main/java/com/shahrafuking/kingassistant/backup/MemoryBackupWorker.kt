*** Begin Patch
*** Update File: app/src/main/java/com/shahrafuking/kingassistant/backup/MemoryBackupWorker.kt
@@
-            Log.i("MemoryBackupWorker", "backup created: ${out.absolutePath}")
-            Result.success()
+            Log.i("MemoryBackupWorker", "backup created: ${out.absolutePath}")
+
+            // Attempt to upload to configured GitHub repo if configured
+            try {
+                val prefs = ctx.getSharedPreferences("king_prefs", android.content.Context.MODE_PRIVATE)
+                val owner = prefs.getString("backup_remote_owner", null)
+                val repo = prefs.getString("backup_remote_repo", null)
+                val remotePathBase = prefs.getString("backup_remote_path", "backups") ?: "backups"
+                if (!owner.isNullOrBlank() && !repo.isNullOrBlank()) {
+                    val uploader = com.shahrafuking.kingassistant.net.GitHubUploader(ctx)
+                    val remotePath = "$remotePathBase/${out.name}"
+                    val ok = uploader.uploadFile(owner, repo, remotePath, out, "king: weekly backup ${out.name}")
+                    if (ok) Log.i("MemoryBackupWorker", "backup uploaded to $owner/$repo:$remotePath")
+                    else Log.e("MemoryBackupWorker", "backup upload failed for $owner/$repo:$remotePath")
+                } else {
+                    Log.i("MemoryBackupWorker", "no remote repo configured; skipping upload")
+                }
+            } catch (e: Throwable) {
+                Log.e("MemoryBackupWorker", "upload attempt failed", e)
+            }
+
+            Result.success()
*** End Patch
