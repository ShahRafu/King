package com.shahrafuking.kingassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.shahrafuking.kingassistant.overlay.OverlayPermissionHelper
import com.shahrafuking.kingassistant.overlay.OverlayService
import com.shahrafuking.kingassistant.voice.VoiceVerifier

class MainActivity : AppCompatActivity() {
    private val REQ_OVERLAY = 4242
    private val REQ_AUDIO = 4243

    private lateinit var statusText: TextView
    private val verifier by lazy { VoiceVerifier(this) }

    // ActivityResult launchers for Enrollment & Verification activities
    private val enrollLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Enrollment completed. Please verify.", Toast.LENGTH_SHORT).show()
            // After enrollment, start verification
            startVerificationActivity()
        } else {
            Toast.makeText(this, "Enrollment not completed.", Toast.LENGTH_SHORT).show()
        }
    }

    private val verifyLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Verification successful.", Toast.LENGTH_SHORT).show()
            // Now ensure permissions and start overlay service
            ensurePermissionsAndStartOverlayService()
        } else {
            Toast.makeText(this, "Verification failed or cancelled.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Simple UI: status + start overlay button + stop overlay button
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        statusText = TextView(this).apply { text = "King Assistant — status: idle" }
        val btnStart = Button(this).apply {
            text = "Start (ensure verification)"
            setOnClickListener { onStartClicked() }
        }
        val btnStop = Button(this).apply {
            text = "Stop Overlay Service"
            setOnClickListener { stopOverlayService() }
        }

        layout.addView(statusText)
        layout.addView(btnStart)
        layout.addView(btnStop)
        setContentView(layout)

        // On app start, check whether user is enrolled; route accordingly
        // Ensure RECORD_AUDIO permission first (both enrollment & verification need mic)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQ_AUDIO)
        } else {
            routeForEnrollmentOrVerification()
        }
    }

    private fun onStartClicked() {
        // Allow user to manually trigger verification if desired
        routeForEnrollmentOrVerification()
    }

    private fun routeForEnrollmentOrVerification() {
        val template = verifier.loadTemplate()
        if (template == null) {
            // not enrolled -> launch EnrollmentActivity
            startEnrollmentActivity()
        } else {
            // already enrolled -> launch VerificationActivity
            startVerificationActivity()
        }
    }

    private fun startEnrollmentActivity() {
        val intent = Intent(this, com.shahrafuking.kingassistant.ui.EnrollmentActivity::class.java)
        enrollLauncher.launch(intent)
    }

    private fun startVerificationActivity() {
        val intent = Intent(this, com.shahrafuking.kingassistant.ui.VerificationActivity::class.java)
        verifyLauncher.launch(intent)
    }

    // Called after enrollment/verification success to ensure overlay and mic permissions and start overlay
    fun ensurePermissionsAndStartOverlayService() {
        // 1) RECORD_AUDIO permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQ_AUDIO)
            return
        }

        // 2) Overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            OverlayPermissionHelper.requestOverlayPermission(this, REQ_OVERLAY)
            return
        }

        // 3) Start overlay service
        val intent = Intent(this, OverlayService::class.java).apply {
            putExtra(OverlayService.EXTRA_SHOW_OVERLAY, true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        statusText.text = "King Assistant — Overlay started (awaiting voice)"
    }

    private fun stopOverlayService() {
        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_STOP_SERVICE
        }
        startService(stopIntent)
        statusText.text = "King Assistant — Overlay stop requested"
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                routeForEnrollmentOrVerification()
            } else {
                Toast.makeText(this, "Microphone permission required for enrollment/verification", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                ensurePermissionsAndStartOverlayService()
            } else {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_LONG).show()
            }
        }
    }
}
