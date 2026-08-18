package com.shahrafuking.kingassistant.backup

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shahrafuking.kingassistant.net.GitHubUploader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * UploadWorker uploads a single file to GitHub. It requires network connectivity constraint
 * at the WorkManager level. It reads inputData parameters: "filePath", "owner", "repo", "remotePath".
 */
class UploadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val filePath = inputData.getString("filePath")
        val owner = inputData.getString("owner")
        val repo = inputData.getString("repo")
        val remotePath = inputData.getString("remotePath")

        if (filePath.isNullOrBlank() || owner.isNullOrBlank() || repo.isNullOrBlank() || remotePath.isNullOrBlank()) {
            Log.e("UploadWorker", "missing input data")
            return Result.failure()
        }

        val file = File(filePath)
        if (!file.exists()) {
            Log.e("UploadWorker", "file not found: $filePath")
            return Result.failure()
        }

        return try {
            val uploader = GitHubUploader(ctx)
            val ok = withContext(Dispatchers.IO) { uploader.uploadFile(owner, repo, remotePath, file, "king: upload ${file.name}") }
            if (ok) {
                // on success, try to remove any pending metadata file
                try { removePendingMetadataFor(file) } catch (_: Throwable) {}
                Result.success()
            } else {
                Log.e("UploadWorker", "upload returned false; will retry")
                Result.retry()
            }
        } catch (e: Throwable) {
            Log.e("UploadWorker", "exception during upload", e)
            Result.retry()
        }
    }

    private fun removePendingMetadataFor(file: File) {
        try {
            val pendingDir = File(applicationContext.filesDir, "pending_uploads")
            if (!pendingDir.exists()) return
            val metas = pendingDir.listFiles() ?: return
            for (m in metas) {
                try {
                    val text = m.readText()
                    if (text.contains(file.name)) {
                        m.delete()
                    }
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}
    }
}
