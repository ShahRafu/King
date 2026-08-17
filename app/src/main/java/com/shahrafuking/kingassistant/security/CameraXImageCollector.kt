package com.shahrafuking.kingassistant.security

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage

class CameraXImageCollector(private val imageProxy: ImageProxy) {
    fun toInputImage(): InputImage {
        val mediaImage = imageProxy.image
            ?: throw IllegalStateException("ImageProxy has null mediaImage")
        val rotation = imageProxy.imageInfo.rotationDegrees
        return InputImage.fromMediaImage(mediaImage, rotation)
    }

    fun close() {
        try { imageProxy.close() } catch (_: Throwable) {}
    }
}
