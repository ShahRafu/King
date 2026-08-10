package com.shahrafuking.kingassistant.core

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.shahrafuking.kingassistant.overlay.OverlayService

/**
 * PanicManager: central small helper to engage/release panic mode.
 * - Stores panic flag in SharedPreferences
 * - When engaging panic, broadcasts ACTION_PANIC_STOP so services can immediate cleanup
 */
object PanicManager {
    private const val PREFS = "king_prefs"
    private const val KEY_PANIC = "panic_engaged"
    private const val TAG = "PanicManager"

    private fun prefs(ctx: Context): SharedPreferences = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun engage(ctx: Context) {
        try {
            prefs(ctx).edit().putBoolean(KEY_PANIC, true).apply()
            // Broadcast immediate stop intent so any listening service (OverlayService) can stop
            try {
                val i = Intent(OverlayService.ACTION_PANIC_STOP)
                i.action = OverlayService.ACTION_PANIC_STOP
                ctx.sendBroadcast(i)
            } catch (t: Throwable) {
                Log.w(TAG, "failed to broadcast panic stop", t)
            }
            Log.i(TAG, "panic engaged")
        } catch (t: Throwable) {
            Log.w(TAG, "engage error", t)
        }
    }

    fun release(ctx: Context) {
        try {
            prefs(ctx).edit().putBoolean(KEY_PANIC, false).apply()
            Log.i(TAG, "panic released")
        } catch (t: Throwable) {
            Log.w(TAG, "release error", t)
        }
    }

    fun isEngaged(ctx: Context): Boolean = try {
        prefs(ctx).getBoolean(KEY_PANIC, false)
    } catch (t: Throwable) { false }
}
