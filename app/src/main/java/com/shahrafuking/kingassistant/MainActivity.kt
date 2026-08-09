package com.shahrafuking.kingassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.shahrafuking.kingassistant.overlay.OverlayPermissionHelper
import com.shahrafuking.kingassistant.overlay.OverlayService

/**
 * Simple Activity to request permissions and control the overlay service.
 * Replace your existing MainActivity with this file (or merge logic into your Activity).
 */
class MainActivity : AppCompatActivity() {
    private val REQ_OVERLAY = 4242
    private val REQ_AUDIO = 4243

    private lateinit var statusText: TextView

    private val overlayBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                OverlayService.ACTION_OVERLAY_SHOW -> {
                    statusText.text = "Overlay: SHOWN"
                    // hide main UI content if desired
                    findViewById<View>(android.R.id.content)?.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Overlay shown", Toast.LENGTH_SHORT).show()
                }
                OverlayService.ACTION_OVERLAY_HIDE -> {
                    statusText.text = "Overlay: HIDDEN"
                    findViewById<View>(android.R.id.content)?.visibility = View.VISIBLE
                    Toast.makeText(this@MainActivity, "Overlay hidden", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Simple UI: status + start overlay button + stop overlay button
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        statusText = TextView(this).apply { text = "Overlay: UNKNOWN" }
        val btnStart = Button(this).apply { text = "Start Overlay (voice control)"; setOnClickListener { ensurePermissionsAndStartOverlayService() } }
        val btnStop = Button(this).apply { text = "Stop Overlay Service"; setOnClickListener { stopOverlayService() } }

        layout.addView(statusText)
        layout.addView(btnStart)
        layout.addView(btnStop)

        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(OverlayService.ACTION_OVERLAY_SHOW)
            addAction(OverlayService.ACTION_OVERLAY_HIDE)
        }
        registerReceiver(overlayBroadcastReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(overlayBroadcastReceiver) } catch (_: IllegalArgumentException) { }
    }

    private fun ensurePermissionsAndStartOverlayService() {
        // 1) RECORD_AUDIO permission
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), REQ_AUDIO)
            return
        }

        // 2) Overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            OverlayPermissionHelper.requestOverlayPermission(this, REQ_OVERLAY)
            return
        }

        // 3) Start service
        val intent = Intent(this, OverlayService::class.java).apply {
            putExtra(OverlayService.EXTRA_SHOW_OVERLAY, true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        statusText.text = "Overlay: STARTED (waiting for voice)"
    }

    private fun stopOverlayService() {
        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_STOP_SERVICE
        }
        startService(stopIntent) // service will handle stop action
        statusText.text = "Overlay: STOPPED (requested)"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                // permission granted -> try again
                ensurePermissionsAndStartOverlayService()
            } else {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ensurePermissionsAndStartOverlayService()
            } else {
                Toast.makeText(this, "Audio permission required for voice commands", Toast.LENGTH_LONG).show()
            }
        }
    }
}
