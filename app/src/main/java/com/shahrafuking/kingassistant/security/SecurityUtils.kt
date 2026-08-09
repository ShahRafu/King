package com.shahrafuking.kingassistant.security

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File

object SecurityUtils {
    private val TAG = "SecurityUtils"

    fun writeEncryptedFile(context: Context, filename: String, data: ByteArray) {
        try {
            val file = File(context.filesDir, filename)
            val mainKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            val encFile = EncryptedFile.Builder(context, file, mainKey, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build()
            encFile.openFileOutput().use { it.write(data) }
        } catch (ex: Exception) {
            Log.e(TAG, "writeEncryptedFile failed: ${ex.message}")
        }
    }

    fun readEncryptedFile(context: Context, filename: String): ByteArray? {
        return try {
            val file = File(context.filesDir, filename)
            val mainKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            val encFile = EncryptedFile.Builder(context, file, mainKey, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build()
            encFile.openFileInput().use { it.readBytes() }
        } catch (ex: Exception) {
            Log.e(TAG, "readEncryptedFile failed: ${ex.message}")
            null
        }
    }
}
