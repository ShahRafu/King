package com.shahrafuking.kingassistant.plugin.voiceplugin

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Compatibility wrapper to provide enrollFromPcm / verifyFromPcm used by ProductionEmbedderAdapter.
 * Delegates to ProductionVoicePlugin when possible; otherwise provides safe placeholders.
 */
class ProductionVoicePluginCompat(private val context: Context) {
    private val inner = ProductionVoicePlugin(context)

    suspend fun enrollFromPcm(pcm: FloatArray): Boolean {
        return try {
            // Placeholder: integrate with inner when you implement model-based enrollment
            true
        } catch (t: Throwable) {
            Log.w("ProductionVoicePluginCompat", "enrollFromPcm failed: ${t.message}")
            false
        }
    }

    suspend fun verifyFromPcm(pcm: FloatArray, threshold: Double): Boolean {
        return try {
            withContext(Dispatchers.Default) {
                // Placeholder verification logic: return false by default
                false
            }
        } catch (t: Throwable) {
            Log.w("ProductionVoicePluginCompat", "verifyFromPcm failed: ${t.message}")
            false
        }
    }
}
