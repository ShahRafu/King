package com.shahrafuking.kingassistant.plugin

import android.content.Context
import android.util.Log
import java.io.File

object PluginManager {
    private val TAG = "PluginManager"

    fun scanPlugins(context: Context): List<File> {
        val dir = File(context.filesDir, "plugins")
        if (!dir.exists()) dir.mkdirs()
        val files = dir.listFiles()?.toList() ?: emptyList()
        Log.i(TAG, "Found ${files.size} plugin files")
        return files
    }

    fun loadPluginDescriptor(file: File): String {
        return try { file.readText() } catch (ex: Exception) {
            Log.e(TAG, "Failed to read plugin file ${file.path}: ${ex.message}")
            ""
        }
    }
}
