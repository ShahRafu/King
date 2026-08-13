package com.shahrafuking.kingassistant.illumination

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.util.Log

/**
 * ActiveIlluminationController
 *
 * Controls device flashlight as a brief active illumination assist for iris capture.
 * This component requires CAMERA and FLASHLIGHT permissions. This is a minimal controller
 * that pulses the torch for a short duration; in production this should be used with care
 * (consent, brightness, thermal limits).
 */
class ActiveIlluminationController(private val context: Context) {
    private val TAG = "ActiveIllumination"

    fun pulseFlashlight(durationMs: Long = 200): Boolean {
        try {
            val camManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = camManager.cameraIdList.firstOrNull() ?: return false
            camManager.setTorchMode(cameraId, true)
            Thread.sleep(durationMs)
            camManager.setTorchMode(cameraId, false)
            return true
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Camera access failed", e)
        } catch (e: Exception) {
            Log.e(TAG, "Flashlight pulse failed", e)
        }
        return false
    }
}
