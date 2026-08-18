@@
-            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
-            val out = File(backups, "memory_backup_$timestamp.txt")
-            out.writeText("Backup created at $timestamp\n(placeholder for memory dump)")
-            Log.i("MemoryBackupWorker", "backup created: ${out.absolutePath}")
-
-            // Attempt to upload to configured GitHub repo if configured
-            val prefs = ctx.getSharedPreferences("king_prefs", android.content.Context.MODE_PRIVATE)
-            val owner = prefs.getString("backup_remote_owner", null)
-            val repo = prefs.getString("backup_remote_repo", null)
-            val remotePathBase = prefs.getString("backup_remote_path", "backups") ?: "backups"
-            if (!owner.isNullOrBlank() && !repo.isNullOrBlank()) {
-                try {
-                    val uploader = com.shahrafuking.kingassistant.net.GitHubUploader(ctx)
-                    val remotePath = "$remotePathBase/${out.name}"
-                    val ok = uploader.uploadFile(owner, repo, remotePath, out, "king: weekly backup ${out.name}")
-                    if (ok) {
-                        Log.i("MemoryBackupWorker", "backup uploaded to $owner/$repo:$remotePath")
-                        return Result.success()
-                    } else {
-                        Log.e("MemoryBackupWorker", "backup upload failed for $owner/$repo:$remotePath")
-                        return Result.retry()
-                    }
-                } catch (e: Throwable) {
-                    Log.e("MemoryBackupWorker", "upload attempt failed", e)
-                    return Result.retry()
-                }
-            }
-
-            // No remote configured - keep local backup and succeed
-            Log.i("MemoryBackupWorker", "no remote repo configured; skipping upload")
-            return Result.success()
+            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
+            val out = File(backups, "memory_backup_$timestamp.txt")
+            out.writeText("Backup created at $timestamp\n(placeholder for memory dump)")
+            Log.i("MemoryBackupWorker", "backup created: ${out.absolutePath}")
+
+            // Attempt to enqueue an UploadWorker with network constraints
+            val prefs = ctx.getSharedPreferences("king_prefs", android.content.Context.MODE_PRIVATE)
+            val owner = prefs.getString("backup_remote_owner", null)
+            val repo = prefs.getString("backup_remote_repo", null)
+            val remotePathBase = prefs.getString("backup_remote_path", "backups") ?: "backups"
+            if (!owner.isNullOrBlank() && !repo.isNullOrBlank()) {
+                try {
+                    val remotePath = "$remotePathBase/${out.name}"
+                    // persist pending metadata so uploads survive app restarts/device reboots
+                    val pendingDir = File(ctx.filesDir, "pending_uploads")
+                    if (!pendingDir.exists()) pendingDir.mkdirs()
+                    val meta = File(pendingDir, "pending_${out.name}.json")
+                    val json = "{" + "\"file\":\"${out.absolutePath}\",\"owner\":\"$owner\",\"repo\":\"$repo\",\"remotePath\":\"$remotePath\"}" 
+                    meta.writeText(json)
+
+                    // Enqueue UploadWorker with network constraint
+                    val input = androidx.work.Data.Builder()
+                        .putString("filePath", out.absolutePath)
+                        .putString("owner", owner)
+                        .putString("repo", repo)
+                        .putString("remotePath", remotePath)
+                        .build()
+
+                    val constraints = androidx.work.Constraints.Builder()
+                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
+                        .build()
+
+                    val req = androidx.work.OneTimeWorkRequestBuilder<com.shahrafuking.kingassistant.backup.UploadWorker>()
+                        .setInputData(input)
+                        .setConstraints(constraints)
+                        .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, java.time.Duration.ofMinutes(15))
+                        .build()
+
+                    androidx.work.WorkManager.getInstance(ctx).enqueue(req)
+                    Log.i("MemoryBackupWorker", "enqueued upload worker for ${out.name}")
+                    return Result.success()
+                } catch (e: Throwable) {
+                    Log.e("MemoryBackupWorker", "enqueue upload failed", e)
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
