package com.shahrafuking.kingassistant.net

import android.content.Context
import android.util.Log

/**
 * IPRotationManager - a small stub to manage ip rotation state.
 * Real implementation should integrate with a VPNService or a proxy provider.
 */
class IPRotationManager(private val ctx: Context) {
    private var enabled: Boolean = false

    fun isEnabled(): Boolean = enabled

    fun enable() {
        enabled = true
        Log.i("IPRotationManager", "IP rotation enabled (stub)")
    }

    fun disable() {
        enabled = false
        Log.i("IPRotationManager", "IP rotation disabled (stub)")
    }

    fun rotateNow(callback: (Boolean) -> Unit) {
        // Placeholder: perform a rotation call to backend or local VPN; report result via callback
        callback(true)
    }
}
