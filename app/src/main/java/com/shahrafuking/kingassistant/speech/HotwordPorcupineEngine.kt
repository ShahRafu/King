package com.shahrafuking.kingassistant.speech

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Porcupine adapter skeleton.
 *
 * USAGE:
 * - Put your Porcupine keyword file (e.g. my_keyword.ppn) under app/src/main/assets/porcupine/
 * - Add PORCUPINE_ACCESS_KEY and PORCUPINE_KEYWORD_FILENAME in local.properties OR BuildConfig (see build.gradle changes below)
 * - Add Porcupine Android dependency to app/build.gradle (uncomment suggested line)
 *
 * IMPORTANT:
 * - This file contains TODO placeholders where you must add the real Picovoice/Porcupine initialization calls
 *   matching the SDK version you choose. The adapter is structured so real calls are isolated here.
 */
class HotwordPorcupineEngine(private val context: Context) : VoiceEngine {
    private var initialized = false

    // Path inside app assets where keyword is expected
    private val keywordAssetSubpath = "porcupine" // assets/porcupine/<keyword.ppn>

    private val keywordFileName: String = try {
        com.shahrafuking.kingassistant.BuildConfig::class.java.getField("PORCUPINE_KEYWORD_FILENAME").get(null) as? String
            ?: com.shahrafuking.kingassistant.speech.PORCUPINE_KEYWORD_FILENAME
    } catch (_: Throwable) {
        com.shahrafuking.kingassistant.speech.PORCUPINE_KEYWORD_FILENAME
    }

    private val accessKey: String = try {
        com.shahrafuking.kingassistant.BuildConfig::class.java.getField("PORCUPINE_ACCESS_KEY").get(null) as? String
            ?: com.shahrafuking.kingassistant.speech.PORCUPINE_ACCESS_KEY
    } catch (_: Throwable) {
        com.shahrafuking.kingassistant.speech.PORCUPINE_ACCESS_KEY
    }

    // Placeholder for actual Porcupine instance (replace Any with real type)
    private var porcupineInstance: Any? = null

    override fun init(): Boolean {
        if (initialized) return true

        try {
            // If no keyword configured, behave as non-crashing placeholder (demo mode)
            if (keywordFileName.isBlank()) {
                Log.w("PorcupineEngine", "No PORCUPINE_KEYWORD_FILENAME configured — running placeholder mode")
                porcupineInstance = null
                initialized = true
                return true
            }

            // 1) Ensure keyword file exists in file-system (Porcupine may require file path, not raw asset)
            val keywordPath = "$keywordAssetSubpath/$keywordFileName"
            val keywordFile = try { extractAssetToFile(keywordPath) } catch (e: Exception) {
                Log.e("PorcupineEngine", "Keyword asset missing: $keywordPath", e)
                return false
            }
            Log.i("PorcupineEngine", "Keyword file extracted: ${keywordFile.absolutePath}")

            // 2) TODO: Initialize real Porcupine engine here.
            // Example (pseudocode—replace with SDK-specific calls):
            // porcupineInstance = Porcupine.create(accessKey, listOf(keywordFile.absolutePath), listOf(sensitivity));

            // For now we keep a placeholder that doesn't crash.
            porcupineInstance = Any()
            initialized = true
            Log.i("PorcupineEngine", "Porcupine adapter initialized (placeholder)")
            return true
        } catch (t: Throwable) {
            Log.e("PorcupineEngine", "init error", t)
            return false
        }
    }

    override fun process(pcm16: ShortArray, sampleRate: Int): Boolean {
        if (!initialized) init()
        // If you initialize a frame-based Porcupine instance, call its process() here.
        // Placeholder returns false (no hotword detected).
        return false
    }

    override fun onHotwordDetected() {
        Log.i("PorcupineEngine", "Hotword detected (porcupine adapter)")
    }

    override fun close() {
        try {
            // TODO: stop and release porcupine manager/instance if present
            porcupineInstance = null
            initialized = false
        } catch (t: Throwable) {
            Log.w("PorcupineEngine", "close error", t)
        }
    }

    private fun extractAssetToFile(assetPath: String): File {
        val assetManager = context.assets
        val input = assetManager.open(assetPath)
        val outFile = File(context.filesDir, assetPath.substringAfterLast('/'))
        outFile.parentFile?.mkdirs()
        FileOutputStream(outFile).use { fos ->
            input.copyTo(fos)
        }
        input.close()
        return outFile
    }
}
