package com.shahrafuking.kingassistant.voice.tflite

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * SampleModelLoader
 * - Loads a TFLite model from assets or a file path and returns an Interpreter
 * - If model not found or fails to load, returns null (caller should fallback).
 */
object SampleModelLoader {
    private const val TAG = "SampleModelLoader"

    /**
     * Try to load model from app assets by assetPath, returns Interpreter or null
     */
    fun loadFromAssets(context: Context, assetPath: String): Interpreter? {
        return try {
            val file = File(context.cacheDir, assetPath)
            if (!file.exists()) {
                // copy asset to cache
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(file).use { out ->
                        input.copyTo(out)
                    }
                }
            }
            Interpreter(file)
        } catch (t: Throwable) {
            Log.w(TAG, "loadFromAssets failed for $assetPath", t)
            null
        }
    }

    /**
     * Load from absolute file path if available (external storage).
     */
    fun loadFromFilePath(filePath: String): Interpreter? {
        return try {
            val f = File(filePath)
            if (!f.exists()) return null
            Interpreter(f)
        } catch (t: Throwable) {
            Log.w(TAG, "loadFromFilePath failed for $filePath", t)
            null
        }
    }
}
