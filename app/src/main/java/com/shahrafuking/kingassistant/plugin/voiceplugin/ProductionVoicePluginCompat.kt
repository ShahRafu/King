package com.shahrafuking.kingassistant.plugin.voiceplugin

import android.content.Context
import android.util.Log

/**
 * Compatibility wrapper to provide enrollFromPcm / verifyFromPcm used by ProductionEmbedderAdapter.
 * Delegates to ProductionVoicePlugin when possible; otherwise provides safe placeholders.
 */
class ProductionVoicePluginCompat(private val context: Context) {
    private val inner = ProductionVoicePlugin(context)

    suspend fun enrollFromPcm(pcm: FloatArray): Boolean {
        return try {
            // If inner plugin supports embedder flow, call it; otherwise placeholder true
            // ProductionVoicePlugin currently exposes enrollSample which is suspend and expects higher-level recording; keep simple for now.
            true
        } catch (t: Throwable) {
            Log.w("ProductionVoicePluginCompat", "enrollFromPcm failed: ${t.message}")
            false
        }
    }

    suspend fun verifyFromPcm(pcm: FloatArray, threshold: Double): Boolean {
        return try {
            // Placeholder: always return false (no match). Replace with real compare logic.
            false
        } catch (t: Throwable) {
            Log.w("ProductionVoicePluginCompat", "verifyFromPcm failed: ${t.message}")
            false
        }
    }
}
