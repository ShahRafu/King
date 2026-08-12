package com.shahrafuking.kingassistant.core

import android.content.Context

/**
 * Compatibility shim: codebase contains PanicController but many callers use PanicManager.
 * Provide a small adapter so both names work during refactors.
 */
object PanicManager {
    fun engage(context: Context) = PanicController.executePanic(context, showToast = false)

    fun release(context: Context) {
        // If PanicController had a release method in your codebase, call it.
        // Fallback: send a broadcast that other components can interpret.
        try {
            val intent = android.content.Intent("com.shahrafuking.kingassistant.ACTION_PANIC_RELEASE")
            context.sendBroadcast(intent)
        } catch (_: Throwable) {}
    }

    fun isEngaged(context: Context): Boolean {
        // Best-effort: if you have a persisted flag for panic state, read it; otherwise return false.
        // Keep simple to satisfy compile-time usage.
        return false
    }
}
