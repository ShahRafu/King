package com.shahrafuking.kingassistant.voice

import android.content.Context
import android.util.Log
import org.json.JSONObject
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.shahrafuking.kingassistant.memory.MemoryDatabase
import com.shahrafuking.kingassistant.memory.MemoryRepository

/**
 * VoiceVerifier handles enrollment storage (Keystore) and integrates with the
 * local MemoryRepository so enrollment & verification attempts are persisted
 * into the app's Memory DB for later analysis or adaptive learning.
 *
 * Note: KeystoreHelper is still used for the primary secure template storage.
 */
class VoiceVerifier(private val context: Context) {
    companion object {
        // Centralized default threshold for verification (tune on real device)
        const val DEFAULT_THRESHOLD = 0.78
    }

    private val TAG = "VoiceVerifier"
    private val storageKey = "voice_template_key"

    fun saveTemplate(vector: DoubleArray): Boolean {
        try {
            val obj = JSONObject()
            val arr = vector.joinToString(separator = ",")
            obj.put("v", arr)
            obj.put("t", System.currentTimeMillis())
            val ok = KeystoreHelper.encryptString(context, obj.toString(), storageKey)

            // Also persist a copy of the embedding into Memory DB (non-blocking)
            try {
                val floatEmb = doubleArrayToFloatArray(vector)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = MemoryRepository(MemoryDatabase.getInstance(context).memoryDao())
                        repo.addMemory(
                            text = "voice_enrollment_template",
                            embedding = floatEmb,
                            metadata = mapOf("type" to "enrollment", "ts" to System.currentTimeMillis().toString())
                        )
                    } catch (t: Throwable) {
                        Log.w(TAG, "failed to save enrollment to memory db", t)
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "embedding conversion error", t)
            }

            return ok
        } catch (t: Throwable) {
            Log.w(TAG, "saveTemplate error", t)
            return false
        }
    }

    fun loadTemplate(): DoubleArray? {
        try {
            val json = KeystoreHelper.decryptString(context, storageKey) ?: return null
            val obj = JSONObject(json)
            val arrStr = obj.optString("v", "")
            if (arrStr.isBlank()) return null
            val parts = arrStr.split(",")
            val vec = DoubleArray(parts.size)
            for (i in parts.indices) vec[i] = parts[i].toDoubleOrNull() ?: 0.0
            return vec
        } catch (t: Throwable) {
            Log.w(TAG, "loadTemplate error", t)
            return null
        }
    }

    fun clearTemplate() {
        try {
            KeystoreHelper.clear(context, storageKey)
            // Optionally record clearance in memory DB
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = MemoryRepository(MemoryDatabase.getInstance(context).memoryDao())
                    repo.addMemory(
                        text = "voice_enrollment_cleared",
                        embedding = null,
                        metadata = mapOf("type" to "enrollment_cleared", "ts" to System.currentTimeMillis().toString())
                    )
                } catch (t: Throwable) {
                    Log.w(TAG, "failed to record clear action", t)
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "clearTemplate error", t)
        }
    }

    fun verify(sampleVector: DoubleArray, threshold: Double = DEFAULT_THRESHOLD): Boolean {
        val template = loadTemplate() ?: return false
        val sim = cosineSimilarity(template, sampleVector)
        Log.i(TAG, "verify similarity=$sim threshold=$threshold")

        // Record verification attempt asynchronously to Memory DB
        try {
            val floatEmb = doubleArrayToFloatArray(sampleVector)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = MemoryRepository(MemoryDatabase.getInstance(context).memoryDao())
                    repo.addMemory(
                        text = "voice_verification_attempt",
                        embedding = floatEmb,
                        metadata = mapOf(
                            "type" to "verification",
                            "ts" to System.currentTimeMillis().toString(),
                            "similarity" to sim.toString(),
                            "passed" to (sim >= threshold).toString()
                        )
                    )
                } catch (t: Throwable) {
                    Log.w(TAG, "failed to save verification attempt to memory db", t)
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "embedding conversion error on verify", t)
        }

        return sim >= threshold
    }

    private fun cosineSimilarity(a: DoubleArray, b: DoubleArray): Double {
        if (a.size != b.size) {
            val n = maxOf(a.size, b.size)
            val aa = DoubleArray(n); val bb = DoubleArray(n)
            for (i in 0 until n) {
                aa[i] = if (i < a.size) a[i] else 0.0
                bb[i] = if (i < b.size) b[i] else 0.0
            }
            return cosineSimilarity(aa, bb)
        }
        var dot = 0.0
        var na = 0.0
        var nb = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        if (na == 0.0 || nb == 0.0) return 0.0
        return dot / (sqrt(na) * sqrt(nb))
    }

    // public helper used across the module
    fun doubleArrayToFloatArray(d: DoubleArray): FloatArray {
        val f = FloatArray(d.size)
        for (i in d.indices) f[i] = d[i].toFloat()
        return f
    }
}
