package com.shahrafuking.kingassistant.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * AccessibilityService scaffold for auto-click gestures.
 * WARNING: may violate broker TOS; use only with legal consent.
 */
class AutoClickService : AccessibilityService() {
    private val TAG = "AutoClickService"

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { Log.i(TAG, "AutoClickService interrupted") }
    override fun onServiceConnected() { Log.i(TAG, "AutoClickService connected") }

    fun clickAt(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val desc = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()
        dispatchGesture(desc, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.i(TAG, "Gesture completed at $x,$y")
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.w(TAG, "Gesture cancelled")
            }
        }, null)
    }
}
