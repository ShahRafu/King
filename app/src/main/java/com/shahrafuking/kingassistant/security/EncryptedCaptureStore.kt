package com.shahrafuking.kingassistant.security

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import java.io.File

/**
 * EncryptedCaptureStore
 *
 * Stores raw captures (audio/video) encrypted on disk until a manual deletion request.
 */
class EncryptedCaptureStore(private val context: Context) {
    private val TAG = "EncryptedCaptureStore"
    private val dir = File(context.filesDir, "captures")
    private val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    init {
        if (!dir.exists()) dir.mkdirs()
    }

    fun saveCapture(name: String, data: ByteArray): Boolean {
        try {
            val file = File(dir, name)
            val encryptedFile = EncryptedFile.Builder(
                file,
                context,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            encryptedFile.openFileOutput().use { it.write(data) }
            Log.i(TAG, "saved encrypted capture: ${file.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "saveCapture failed", e)
            return false
        }
    }

    fun listCaptures(): List<String> {
        return dir.listFiles()?.map { it.name } ?: emptyList()
    }

    fun deleteCapture(name: String): Boolean {
        val file = File(dir, name)
        if (!file.exists()) return false
        return file.delete()
    }
}
