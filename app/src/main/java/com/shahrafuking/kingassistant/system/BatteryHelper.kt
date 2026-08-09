package com.shahrafuking.kingassistant.system

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

object BatteryHelper {
    private const val TAG = "BatteryHelper"

    fun isIgnoringBatteryOptimizations(ctx: Context): Boolean {
        return try {
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(ctx.packageName)
        } catch (t: Throwable) {
            Log.w(TAG, "isIgnoringBatteryOptimizations error", t)
            false
        }
    }

    /**
     * Request user to ignore battery optimizations for this app.
     * Must be called from an Activity.
     */
    fun requestIgnoreBatteryOptimization(activity: Activity, requestCode: Int = 4245) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:${activity.packageName}")
            activity.startActivityForResult(intent, requestCode)
        } catch (t: Throwable) {
            Log.w(TAG, "requestIgnoreBatteryOptimization failed, falling back to settings page", t)
            // Fallback: open app settings page
            try {
                val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                i.data = Uri.parse("package:${activity.packageName}")
                activity.startActivityForResult(i, requestCode)
            } catch (ex: Throwable) {
                Log.w(TAG, "fallback settings launch failed", ex)
            }
        }
    }
}
