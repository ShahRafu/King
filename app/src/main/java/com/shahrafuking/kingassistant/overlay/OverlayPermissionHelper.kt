package com.shahrafuking.kingassistant.overlay

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Helper to open the system settings page for 'Draw over other apps' permission.
 * Usage: OverlayPermissionHelper.requestOverlayPermission(activity, requestCode)
 */
object OverlayPermissionHelper {
    fun requestOverlayPermission(activity: Activity, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(activity)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${activity.packageName}")
                )
                activity.startActivityForResult(intent, requestCode)
            }
        }
    }
}
