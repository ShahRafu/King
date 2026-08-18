package com.shahrafuking.kingassistant.backup

import android.content.Context
import android.widget.Toast
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object BackupTrigger {
    fun runImmediateBackup(ctx: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val req = OneTimeWorkRequestBuilder<MemoryBackupWorker>().build()
                WorkManager.getInstance(ctx).enqueue(req)
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(ctx, "Backup job scheduled", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Throwable) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(ctx, "Failed to schedule backup: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
