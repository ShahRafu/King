package com.shahrafuking.kingassistant.security

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * ModelLoader
 *
 * Small helper to load TensorFlow Lite interpreters from app assets/models/ with safe defaults.
 */
object ModelLoader {
    private const val MODELS_DIR = "models"
    private const val TAG = "ModelLoader"

    fun loadInterpreter(context: Context, modelFileName: String, threads: Int = 2): Interpreter? {
        try {
            val afd = context.assets.openFd("$MODELS_DIR/$modelFileName")
            val inputStream = FileInputStream(afd.fileDescriptor)
            val channel = inputStream.channel
            val mapped = channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
            val options = Interpreter.Options().apply { setNumThreads(threads) }
            return Interpreter(mapped, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model $modelFileName", e)
            return null
        }
    }
}
