package com.shahrafuking.kingassistant.selfheal

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * FileIsolator
 *
 * Responsibilities:
 * - Identify the single file implicated by an error message (best-effort).
 * - Create a safe backup copy of the file to app-internal backups directory.
 * - Provide write helpers that write to a staged area rather than the real source tree.
 *
 * This class intentionally writes to app-internal storage (context.filesDir) to avoid
 * modifying the checked-in repo until the owner explicitly promotes a staged change.
 */
class FileIsolator(private val context: Context) {
    private val TAG = "FileIsolator"
    private val backupsDir = File(context.filesDir, "selfheal_backups").also { it.mkdirs() }
    private val stagedDir = File(context.filesDir, "selfheal_staged").also { it.mkdirs() }

    data class IsolateResult(val originalPath: String, val backupPath: String, val stagedPath: String)

    /** Best-effort: identify a file path from an error string by looking for ".kt"/".java" occurrences. */
    fun identifyFileForError(errorText: String): String? {
        val regex = Regex("[A-Za-z0-9_\\/\\.-]+\\.(kt|java|xml|gradle)")
        val m = regex.find(errorText)
        return m?.value
    }

    fun backupFileOnDisk(sourcePath: String): String? {
        try {
            val srcFile = File(sourcePath)
            if (!srcFile.exists()) return null
            val dest = File(backupsDir, srcFile.name + ".backup.${System.currentTimeMillis()}")
            srcFile.copyTo(dest)
            return dest.absolutePath
        } catch (e: IOException) {
            Log.w(TAG, "backup failed", e)
            return null
        }
    }

    fun stageFileContents(fileName: String, newContents: String): String {
        val out = File(stagedDir, fileName)
        out.writeText(newContents)
        return out.absolutePath
    }

    fun readStaged(fileName: String): String? {
        val f = File(stagedDir, fileName)
        return if (f.exists()) f.readText() else null
    }
}
