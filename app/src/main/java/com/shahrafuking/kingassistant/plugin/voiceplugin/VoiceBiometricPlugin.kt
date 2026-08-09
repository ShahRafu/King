package com.shahrafuking.kingassistant.plugin.voiceplugin

import android.content.Context
import android.util.Log
import com.shahrafuking.kingassistant.voice.VoiceVerifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * VoiceBiometricPlugin
 *
 * Extended adapter that attempts to use an on-device TFLite speaker embedder when available.
 * If TensorFlow Lite is not added as a dependency, the plugin falls back to the BasicLocalPlugin
 * behaviour (existing VoiceVerifier/EnrollmentManager flows).
 *
 * Notes:
 * - To enable TFLite integration add the dependency to app/build.gradle:
 *     implementation "org.tensorflow:tensorflow-lite:2.11.0"
 *   and place a converted speaker embedding TFLite model in app/src/main/assets/speaker_embedder.tflite
 * - The code below uses a runtime try/catch around Interpreter usage so builds will succeed even
 *   when the TFLite dependency is missing; you must add the dependency and a model file to use it.
 */

interface VoiceBiometricPlugin {
    suspend fun enrollSample(context: Context): Boolean
    suspend fun verifySample(context: Context): Boolean
    fun clearTemplate(context: Context)
}

/**
 * TfliteSpeakerEmbedder: lightweight wrapper that will try to load a TFLite interpreter and
 * run inference to produce a speaker embedding vector (FloatArray -> DoubleArray conversion).
 *
 * This class is defensive: if the Interpreter class is not present or model load fails it will
 * throw an exception and the caller should fallback to an alternative implementation.
 */
class TfliteSpeakerEmbedder(private val context: Context, private val modelAssetPath: String = "speaker_embedder.tflite") {
    private val TAG = "TfliteSpeakerEmbedder"
    private var interpreter: Any? = null
    private var inputShape: IntArray? = null

    suspend fun load(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Use reflection so build does not require tflite on classpath at compile time
            val interpClass = Class.forName("org.tensorflow.lite.Interpreter")
            val assetManager = context.assets
            val fd = assetManager.openFd(modelAssetPath)
            val fis = fd.createInputStream()
            val bytes = fis.readBytes()
            fis.close()

            // Construct ByteBuffer for model — use Interpreter(byteBuffer) if available
            val bbClass = java.nio.ByteBuffer::class.java
            val byteBuffer = java.nio.ByteBuffer.allocateDirect(bytes.size).apply { put(bytes); rewind() }

            // Try to find a constructor that accepts ByteBuffer
            val ctor = interpClass.getConstructor(java.nio.ByteBuffer::class.java)
            interpreter = ctor.newInstance(byteBuffer)

            // If interpreter created, attempt to query input tensor shape via reflection
            try {
                val getInputTensor = interpClass.getMethod("getInputTensor", Int::class.javaPrimitiveType)
                val tensor = getInputTensor.invoke(interpreter, 0)
                val shapeMethod = tensor.javaClass.getMethod("shape")
                val shape = shapeMethod.invoke(tensor) as IntArray
                inputShape = shape
            } catch (t: Throwable) {
                Log.w(TAG, "Couldn't query tensor shape reflectively: ${t.message}")
                inputShape = intArrayOf(1, 16000) // fallback guess
            }

            Log.i(TAG, "TFLite interpreter loaded (model=$modelAssetPath)")
            true
        } catch (t: Throwable) {
            Log.w(TAG, "TFLite load failed — interpreter not available or model missing: ${t.message}")
            interpreter = null
            inputShape = null
            false
        }
    }

    /**
     * Run inference on raw PCM floats (mono) and return a DoubleArray embedding.
     * This method assumes the caller has prepared data in the expected shape.
     */
    suspend fun embed(pcmFloat: FloatArray): DoubleArray? = withContext(Dispatchers.Default) {
        if (interpreter == null) return@withContext null
        try {
            val interpClass = Class.forName("org.tensorflow.lite.Interpreter")
            // Prepare input buffer: shape is assumed [1, N] or [1, T, C]
            val inBuf = java.nio.ByteBuffer.allocateDirect(pcmFloat.size * 4).apply {
                order(java.nio.ByteOrder.nativeOrder())
                asFloatBuffer().put(pcmFloat)
                rewind()
            }
            // Prepare output buffer: assume embedding size 192 (model dependent) — allocate generously
            val outSize = 192
            val outBuf = java.nio.ByteBuffer.allocateDirect(outSize * 4).apply { order(java.nio.ByteOrder.nativeOrder()) }

            // invoke(Object, Object)
            val invokeMethod = interpClass.getMethod("run", Any::class.java, Any::class.java)
            invokeMethod.invoke(interpreter, inBuf, outBuf)

            outBuf.rewind()
            val floats = FloatArray(outSize)
            outBuf.asFloatBuffer().get(floats)
            // Convert to double
            val doubles = DoubleArray(floats.size) { i -> floats[i].toDouble() }
            return@withContext doubles
        } catch (t: Throwable) {
            Log.w(TAG, "embed failed: ${t.message}")
            return@withContext null
        }
    }
}

/**
 * ProductionVoicePlugin: tries to use the TFLite embedder; falls back to a basic local flow if
 * the model or runtime isn't available.
 */
class ProductionVoicePlugin(private val context: Context) : VoiceBiometricPlugin {
    private val TAG = "ProductionVoicePlugin"
    private val verifier = VoiceVerifier(context)
    private var embedder: TfliteSpeakerEmbedder? = null

    suspend fun ensureEmbedder(): Boolean {
        if (embedder != null) return true
        val e = TfliteSpeakerEmbedder(context)
        val ok = e.load()
        if (ok) embedder = e
        return ok
    }

    override suspend fun enrollSample(context: Context): Boolean {
        // Try to use embedder; otherwise fallback to user flow
        val have = ensureEmbedder()
        if (!have) {
            Log.i(TAG, "No embedder available — use EnrollmentActivity flow instead")
            return false
        }
        // Here you'd record audio, convert to float PCM and call embedder.embed()
        // This plugin doesn't own the recorder; higher-level UI should supply PCM.
        Log.i(TAG, "ProductionVoicePlugin.enrollSample() requires PCM input from UI/recorder")
        return false
    }

    override suspend fun verifySample(context: Context): Boolean {
        // If embedder present, request a short recording from higher layer, compute embedding and compare
        val have = ensureEmbedder()
        if (!have) {
            Log.i(TAG, "No embedder available — fallback to VoiceVerifier.verify flow")
            return false
        }
        Log.i(TAG, "ProductionVoicePlugin.verifySample() requires PCM input from UI/recorder")
        return false
    }

    override fun clearTemplate(context: Context) {
        try {
            verifier.clearTemplate()
            Log.i(TAG, "Template cleared via ProductionVoicePlugin")
        } catch (t: Throwable) {
            Log.w(TAG, "clearTemplate error", t)
        }
    }
}

/**
 * BasicLocalPlugin kept for backwards compatibility and as a safe fallback.
 */
class BasicLocalPlugin : VoiceBiometricPlugin {
    private val TAG = "BasicLocalPlugin"

    override suspend fun enrollSample(context: Context): Boolean {
        Log.i(TAG, "BasicLocalPlugin.enrollSample() called — use EnrollmentActivity")
        return false
    }

    override suspend fun verifySample(context: Context): Boolean {
        Log.i(TAG, "BasicLocalPlugin.verifySample() called — placeholder")
        return false
    }

    override fun clearTemplate(context: Context) {
        try {
            val v = VoiceVerifier(context)
            v.clearTemplate()
            Log.i(TAG, "Template cleared by BasicLocalPlugin.")
        } catch (t: Throwable) {
            Log.w(TAG, "clearTemplate error", t)
        }
    }
}
