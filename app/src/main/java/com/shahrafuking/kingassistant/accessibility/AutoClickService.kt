package com.shahrafuking.kingassistant.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * AutoClickService: minimal AccessibilityService scaffold.
 * - On connection it logs. performTap(x,y) performs a single-tap gesture via dispatchGesture.
 * - IMPORTANT: This service contains powerful abilities — keep it disabled until you review policy and ask users for explicit opt-in.
 */
class AutoClickService : AccessibilityService() {
    companion object {
        private const val TAG = "AutoClickService"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "AutoClickService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // no-op; we use this service only to dispatch gestures when explicitly requested
    }

    override fun onInterrupt() {
        // no-op
    }

    /**
     * Perform a single tap at (x,y) in screen coordinates.
     * This method should be guarded by an explicit user opt-in flag in your app before use.
     */
    fun performTap(x: Float, y: Float, callback: ((Boolean) -> Unit)? = null) {
        val p = Path().apply { moveTo(x, y) }
        val desc = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0, 50))
            .build()
        val dispatched = dispatchGesture(desc, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                callback?.invoke(true)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                callback?.invoke(false)
            }
        }, null)
        Log.i(TAG, "performTap requested -> dispatched=$dispatched")
    }
}
