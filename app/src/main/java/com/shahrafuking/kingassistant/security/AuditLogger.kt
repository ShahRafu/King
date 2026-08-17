package com.shahrafuking.kingassistant.security

import android.content.Context
import android.util.Log

class AuditLogger(private val ctx: Context) {
    fun log(event: String, data: String? = null) {
        // Minimal persistent/audit logging placeholder
        Log.i("AuditLogger", "event=$event data=$data")
    }
}
