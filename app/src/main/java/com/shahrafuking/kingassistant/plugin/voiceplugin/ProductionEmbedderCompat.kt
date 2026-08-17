package com.shahrafuking.kingassistant.plugin.voiceplugin

import android.content.Context
import android.util.Log
import com.shahrafuking.kingassistant.ml.PcmUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductionVoicePluginCompat(private val context: Context) {
    private val inner = ProductionVoicePlugin(context)

    suspend fun enrollFromPcm(pcm: FloatArray): Boolean {
        return try {
            // Placeholder: delegate or implement
            true
        } catch (t: Throwable) {
            Log.w("ProductionVoicePluginCompat", "enrollFromPcm failed: ${t.message}")
            false
        }
    }

    suspend fun verifyFromPcm(pcm: FloatArray, threshold: Double): Boolean {
        return try {
            // Placeholder verification using VoiceVerifier or model compare - return false by default
            withContext(Dispatchers.Default) {
                false
            }
        } catch (t: Throwable) {
            Log.w("ProductionVoicePluginCompat", "verifyFromPcm failed: ${t.message}")
            false
        }
    }
}
