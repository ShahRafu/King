*** Begin Patch
*** Update File: app/src/main/java/com/shahrafuking/kingassistant/backup/BackupScheduler.kt
@@
     fun scheduleWeeklyBackup(ctx: Context) {
-        val req = PeriodicWorkRequestBuilder<MemoryBackupWorker>(7, TimeUnit.DAYS)
-            .build()
+        val req = PeriodicWorkRequestBuilder<MemoryBackupWorker>(7, TimeUnit.DAYS)
+            .setBackoffCriteria(
+                androidx.work.BackoffPolicy.EXPONENTIAL,
+                java.time.Duration.ofMinutes(15)
+            )
+            .build()
         WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE, req)
     }
*** End Patch
