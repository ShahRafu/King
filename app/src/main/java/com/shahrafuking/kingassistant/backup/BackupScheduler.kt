*** Begin Patch
*** Update File: app/src/main/java/com/shahrafuking/kingassistant/backup/BackupScheduler.kt
@@
-        val req = PeriodicWorkRequestBuilder<MemoryBackupWorker>(7, TimeUnit.DAYS)
-            .setBackoffCriteria(
-                androidx.work.BackoffPolicy.EXPONENTIAL,
-                java.time.Duration.ofMinutes(15)
-            )
-            .build()
+        val constraints = androidx.work.Constraints.Builder()
+            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
+            .build()
+
+        val req = PeriodicWorkRequestBuilder<MemoryBackupWorker>(7, TimeUnit.DAYS)
+            .setConstraints(constraints)
+            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, java.time.Duration.ofMinutes(15))
+            .build()
         WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE, req)
     }
*** End Patch
