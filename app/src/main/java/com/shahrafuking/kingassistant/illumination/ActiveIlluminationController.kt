package com.shahrafuking.kingassistant.illumination

import android.app.Activity
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager

/**
 * ActiveIlluminationController
 *
 * Controls adaptive illumination for iris capture. Determines best available method (torch or
 * screen pulse) and executes a single short pulse when requested. Returns a detailed result
 * indicating which method was used so the verifier can adapt its processing.
 */
class ActiveIlluminationController(private val context: Context) {
    private val TAG = "ActiveIllumination"

    enum class IlluminationResult {
        FLASH_USED,
        SCREEN_PULSE_USED,
        NONE_AVAILABLE,
        ERROR
    }

    fun isTorchAvailable(): Boolean {
        return try {
            val camManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            camManager.cameraIdList.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    fun pulseFlashlight(durationMs: Long = 200): IlluminationResult {
        try {
            val camManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = camManager.cameraIdList.firstOrNull() ?: return IlluminationResult.NONE_AVAILABLE
            camManager.setTorchMode(cameraId, true)
            Thread.sleep(durationMs)
            camManager.setTorchMode(cameraId, false)
            return IlluminationResult.FLASH_USED
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Camera access failed", e)
            return IlluminationResult.ERROR
        } catch (e: Exception) {
            Log.e(TAG, "Flashlight pulse failed", e)
            return IlluminationResult.ERROR
        }
    }

    fun pulseScreen(activity: Activity, durationMs: Long = 250): IlluminationResult {
        try {
            val window = activity.window
            val decor = window.decorView
            val handler = Handler(Looper.getMainLooper())
            // Overlay: set full-screen white background and then revert
            decor.setBackgroundColor(0xFFFFFFFF.toInt())
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            handler.postDelayed({
                // revert to default (no direct way to know original background — activity should handle redraw)
                decor.setBackgroundColor(0x00000000)
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }, durationMs)
            return IlluminationResult.SCREEN_PULSE_USED
        } catch (e: Exception) {
            Log.e(TAG, "Screen pulse failed", e)
            return IlluminationResult.ERROR
        }
    }
}
