package com.shahrafuking.kingassistant.logging

import android.content.Context
import android.util.Log

object AuditLogger {
    private const val TAG = "AuditLogger"

    fun logEvent(context: Context?, event: String, details: String? = null) {
        Log.i(TAG, "AUDIT: $event ${details ?: ""}")
        // TODO: persist to secure storage or forward to remote logger when available
    }
}
