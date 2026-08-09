package com.shahrafuking.kingassistant.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.shahrafuking.kingassistant.overlay.OverlayService
import com.shahrafuking.kingassistant.voice.VoiceVerifier

/**
 * PanicController
 *
 * Central, minimal panic handler to immediately stop core services and
 * sanitize sensitive local state in case of emergency. Designed to be
 * callable from a voice command handler or any UI event.
 */
object PanicController {
    private const val TAG = "PanicController"

    /**
     * Execute an immediate panic stop: stop overlay service, clear local voice template,
     * and optionally show a short toast. Call this from the main thread.
     */
    fun executePanic(context: Context, showToast: Boolean = true) {
        try {
            // 1) Stop overlay service using same action name used in the app
            try {
                val stopIntent = Intent(context, OverlayService::class.java).apply {
                    action = OverlayService.ACTION_STOP_SERVICE
                }
                context.startService(stopIntent)
                Log.i(TAG, "Requested OverlayService stop")
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to stop OverlayService", t)
            }

            // 2) Clear on-device voice template (irreversible from UI) to make biometric unusable
            try {
                val v = VoiceVerifier(context)
                v.clearTemplate()
                Log.i(TAG, "Voice template cleared")
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to clear voice template", t)
            }

            // 3) Broadcast a panic intent so other components can react if they listen
            try {
                val b = Intent("com.shahrafuking.kingassistant.ACTION_PANIC_STOP")
                context.sendBroadcast(b)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to broadcast panic intent", t)
            }

            if (showToast && context is Activity) {
                Toast.makeText(context, "Panic stop executed", Toast.LENGTH_SHORT).show()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "executePanic fatal", t)
        }
    }
}
