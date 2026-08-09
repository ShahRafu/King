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
    private val keywordFileName: String = BuildConfig.PORCUPINE_KEYWORD_FILENAME // set in build.gradle or local.properties
    private val accessKey: String = BuildConfig.PORCUPINE_ACCESS_KEY // set in build.gradle or local.properties

    // Placeholder for actual Porcupine instance (replace Any with real type)
    private var porcupineInstance: Any? = null

    override fun init(): Boolean {
        if (initialized) return true

        try {
            // 1) Ensure keyword file exists in file-system (Porcupine may require file path, not raw asset)
            val keywordFile = extractAssetToFile("$keywordAssetSubpath/$keywordFileName")
            Log.i("PorcupineEngine", "Keyword file extracted: ${keywordFile.absolutePath}")

            // 2) TODO: Initialize real Porcupine engine here.
            // Example (pseudocode—replace with SDK-specific calls):
            // porcupineInstance = Porcupine.create(accessKey, listOf(keywordFile.absolutePath), listOf(sensitivity));
            // OR using PorcupineManager (if SDK provides):
            // porcupineManager = PorcupineManager.Builder()
            //     .setAccessKey(accessKey)
            //     .setKeywordPath(keywordFile.absolutePath)
            //     .setSensitivity(0.6f)
            //     .build(context)
            // porcupineManager.start(...)

            // For now we keep a placeholder:
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
        // If you initialize a frame-based Porcupine instance, call its process() here:
        // val result = porcupineInstance.process(pcm16)
        // return result == keywordIndex (or boolean)
        // Since SDK variants differ, leave placeholder:
        return false
    }

    override fun onHotwordDetected() {
        Log.i("PorcupineEngine", "Hotword detected (porcupine adapter)")
        // TODO: Broadcast a local intent or use callback to notify UI/service
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
