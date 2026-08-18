package com.shahrafuking.kingassistant.plugin

import android.content.Context
import android.util.Log
import java.io.File

/**
 * PluginManager (v1 stub)
 * - Scans a plugins directory under app filesDir/plugins
 * - Provides simple enable/disable bookkeeping using a preferences file
 * - This is intentionally minimal and safe; real plugin loading/execution is NOT performed.
 */
class PluginManager(private val context: Context) {
    private val TAG = "PluginManager"
    private val pluginsDir = File(context.filesDir, "plugins")
    private val prefs = context.getSharedPreferences("king_plugins", Context.MODE_PRIVATE)

    init {
        if (!pluginsDir.exists()) {
            pluginsDir.mkdirs()
        }
    }

    data class PluginInfo(val id: String, val file: File, val enabled: Boolean)

    fun listPlugins(): List<PluginInfo> {
        val files = pluginsDir.listFiles() ?: arrayOf()
        return files.map { f ->
            val id = f.name
            val enabled = prefs.getBoolean(id, false)
            PluginInfo(id, f, enabled)
        }
    }

    fun enablePlugin(id: String) {
        prefs.edit().putBoolean(id, true).apply()
        Log.i(TAG, "Enabled plugin: $id")
    }

    fun disablePlugin(id: String) {
        prefs.edit().putBoolean(id, false).apply()
        Log.i(TAG, "Disabled plugin: $id")
    }

    fun addPluginFromBytes(filename: String, bytes: ByteArray): Boolean {
        return try {
            val out = File(pluginsDir, filename)
            out.writeBytes(bytes)
            true
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to write plugin: $filename", t)
            false
        }
    }
}
