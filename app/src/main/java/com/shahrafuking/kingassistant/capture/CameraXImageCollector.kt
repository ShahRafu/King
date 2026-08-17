package com.shahrafuking.kingassistant.capture

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.media.Image
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * CameraXImageCollector
 *
 * Helper that binds CameraX ImageAnalysis to collect a sequence of InputImage frames for a
 * given duration (in milliseconds). It returns a List<InputImage> captured from the front camera.
 *
 * Note: caller must provide a LifecycleOwner (e.g., an Activity that implements LifecycleOwner).
 */
class CameraXImageCollector(private val lifecycleOwner: LifecycleOwner, private val durationMs: Long = 3000L) {
    private val TAG = "CameraXImageCollector"
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    @SuppressLint("UnsafeOptInUsageError")
    suspend fun collectFrames(): List<InputImage> = suspendCancellableCoroutine { cont ->
        try {
            val context = lifecycleOwner as Context
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({
                try {
                    val cameraProvider = providerFuture.get()
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()

                    val frames = ArrayList<InputImage>()

                    val analyzer = ImageAnalysis.Analyzer { imageProxy: ImageProxy ->
                        try {
                            val mediaImage: Image? = imageProxy.image
                            if (mediaImage != null && imageProxy.format == ImageFormat.YUV_420_888) {
                                val rotation = imageProxy.imageInfo.rotationDegrees
                                val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
                                frames.add(inputImage)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "analyze failed", e)
                        } finally {
                            imageProxy.close()
                        }
                    }

                    imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build()

                    // bindToLifecycle requires a LifecycleOwner
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis)

                    // Schedule stop after durationMs on main looper
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            cameraProvider.unbindAll()
                            cont.resume(frames)
                        } catch (e: Exception) {
                            cont.resumeWithException(e)
                        }
                    }, durationMs)

                    cont.invokeOnCancellation {
                        try {
                            cameraProvider.unbindAll()
                        } catch (_: Exception) {}
                    }
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            cont.resumeWithException(e)
        }
    }
}
