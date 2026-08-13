package com.shahrafuking.kingassistant.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.shahrafuking.kingassistant.R
import com.shahrafuking.kingassistant.security.MultiFactorGatekeeper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * HighSecurityApprovalActivity
 *
 * Minimal activity to trigger a CRITICAL approval flow (voice + iris + lip-sync). This activity
 * demonstrates the synchronous capture UI; actual capture is mocked in the verifier stubs.
 */
class HighSecurityApprovalActivity : ComponentActivity() {
    private val TAG = "HighSecurityApproval"
    private val REQUEST_CAMERA = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_high_security_approval)

        // Permission check for camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
        }

        val info = findViewById<TextView>(R.id.hsa_info)
        val btn = findViewById<Button>(R.id.hsa_request)
        val resultView = findViewById<TextView>(R.id.hsa_result)

        info.text = "Critical action requested. This will require voice + iris verification."

        btn.setOnClickListener {
            // Start critical approval flow
            CoroutineScope(Dispatchers.Main).launch {
                val gatekeeper = MultiFactorGatekeeper(this@HighSecurityApprovalActivity)
                val res = gatekeeper.requestOwnerApproval("Authorize critical action", MultiFactorGatekeeper.AuthLevel.CRITICAL)
                resultView.text = if (res.approved) "Approval granted" else "Approval denied: ${res.details}"
                Log.i(TAG, "High-level approval result: ${res.approved} details=${res.details}")
            }
        }
    }
}
