package com.shahrafuking.kingassistant.backup

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object BackupScheduler {
    private const val WORK_NAME = "king_memory_weekly_backup"

    fun scheduleWeeklyBackup(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<MemoryBackupWorker>(7, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE, req)
    }

    fun cancelWeeklyBackup(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork(WORK_NAME)
    }
}
