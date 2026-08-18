package com.shahrafuking.kingassistant.security

import android.content.Context
import android.util.Log
import android.app.Activity

class AuditLogger(private val ctx: Context) {
    fun log(event: String, data: String? = null) {
        Log.i("AuditLogger", "event=$event data=$data")
    }

    fun logIllumination(lux: Float, status: String, activity: Activity? = null) {
        log("illumination", "lux=$lux status=$status")
    }

    fun logAuthResult(success: Boolean, reason: String? = null, activity: Activity? = null) {
        log("auth_result", "success=$success reason=$reason")
    }
}
