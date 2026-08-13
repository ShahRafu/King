package com.shahrafuking.kingassistant.illumination

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

/**
 * AmbientLightChecker
 *
 * Attempts to read ambient light (lux) from the device light sensor. If no light sensor
 * is available, callers should fall back to a camera-based luminance check (not implemented
 * here — provide a helper in the UI layer).
 */
class AmbientLightChecker(private val context: Context) {
    private val TAG = "AmbientLightChecker"
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val lastLux = AtomicReference<Float?>(null)

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null) return
            if (event.sensor.type == Sensor.TYPE_LIGHT) {
                lastLux.set(event.values[0])
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // ignore
        }
    }

    suspend fun getAmbientLux(timeoutMs: Long = 500): Float? = withContext(Dispatchers.Main) {
        if (lightSensor == null) {
            Log.w(TAG, "No light sensor available on device")
            return@withContext null
        }

        lastLux.set(null)
        sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        try {
            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < timeoutMs) {
                val v = lastLux.get()
                if (v != null) {
                    return@withContext v
                }
                Thread.sleep(50)
            }
        } finally {
            sensorManager.unregisterListener(listener)
        }
        Log.w(TAG, "Timed out waiting for ambient lux")
        return@withContext null
    }

    fun isLowLight(lux: Float?): Boolean {
        // default threshold 10 lux
        if (lux == null) return true
        return lux < 10.0f
    }
}
