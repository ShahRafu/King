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
import androidx.lifecycle.lifecycleScope
import com.shahrafuking.kingassistant.overlay.OverlayPermissionHelper
import com.shahrafuking.kingassistant.overlay.OverlayService
import com.shahrafuking.kingassistant.voice.VoiceCommandManager
import com.shahrafuking.kingassistant.voice.VoiceRecognizer
import com.shahrafuking.kingassistant.voice.VoiceVerifier
import com.shahrafuking.kingassistant.trade.BudgetManager
import com.shahrafuking.kingassistant.trading.PanicStopManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val REQ_OVERLAY = 4242
    private val REQ_AUDIO = 4243

    private lateinit var statusText: TextView
    private val recognizer by lazy { VoiceRecognizer(this) }

    private val requestMicrophone = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            Toast.makeText(this, "Microphone permission is required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        statusText = TextView(this).apply { text = "King Assistant — status: idle" }

        val btnStart = Button(this).apply {
            text = "Start Overlay (ensure verification)"
            setOnClickListener { onStartClicked() }
        }

        val btnDemoListen = Button(this).apply {
            text = "Demo: Start voice listen"
            setOnClickListener { demoStartListening() }
        }

        val btnSimSetBudget = Button(this).apply {
            text = "Demo: Set budget $100"
            setOnClickListener { demoSetBudget(100.0) }
        }

        val btnSimTrade = Button(this).apply {
            text = "Demo: Simulate trade $10"
            setOnClickListener { demoSimulateTrade(10.0) }
        }

        val btnPanic = Button(this).apply {
            text = "Trigger PanicStop"
            setOnClickListener { demoPanicStop() }
        }

        layout.addView(statusText)
        layout.addView(btnStart)
        layout.addView(btnDemoListen)
        layout.addView(btnSimSetBudget)
        layout.addView(btnSimTrade)
        layout.addView(btnPanic)
        setContentView(layout)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQ_AUDIO)
        }
    }

    private fun onStartClicked() {
        // Start overlay service after permissions
        ensurePermissionsAndStartOverlayService()
    }

    private fun ensurePermissionsAndStartOverlayService() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQ_AUDIO)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            OverlayPermissionHelper.requestOverlayPermission(this, REQ_OVERLAY)
            return
        }

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

    private fun demoStartListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestMicrophone.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        statusText.text = "Listening (demo)..."
        recognizer.startListening({ text ->
            runOnUiThread {
                statusText.text = "Heard: $text"
                handleRecognizedText(text)
            }
        }, { err ->
            runOnUiThread { Toast.makeText(this, "Recognizer error: $err", Toast.LENGTH_LONG).show() }
        })
    }

    private fun handleRecognizedText(text: String) {
        val cmd = VoiceCommandManager.parse(text)
        when (cmd) {
            is com.shahrafuking.kingassistant.voice.Command.Trade -> {
                // Require enrollment present (demo-only: no live verification with PCM here)
                val template = VoiceVerifier.loadTemplate(this)
                if (template == null) {
                    Toast.makeText(this, "Not enrolled: run Enrollment first", Toast.LENGTH_LONG).show()
                    return
                }
                lifecycleScope.launch {
                    val bm = BudgetManager(this@MainActivity)
                    val ok = bm.checkAndReserve(cmd.amount)
                    runOnUiThread {
                        if (ok) Toast.makeText(this@MainActivity, "Trade reserved: ${cmd.amount}", Toast.LENGTH_SHORT).show()
                        else Toast.makeText(this@MainActivity, "Insufficient budget for ${cmd.amount}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            is com.shahrafuking.kingassistant.voice.Command.SetBudget -> {
                // require enrollment
                val template = VoiceVerifier.loadTemplate(this)
                if (template == null) {
                    Toast.makeText(this, "Not enrolled: run Enrollment first", Toast.LENGTH_LONG).show()
                    return
                }
                lifecycleScope.launch {
                    val bm = BudgetManager(this@MainActivity)
                    bm.setBudget(cmd.amount)
                    runOnUiThread { Toast.makeText(this@MainActivity, "Budget set: ${cmd.amount}", Toast.LENGTH_SHORT).show() }
                }
            }
            is com.shahrafuking.kingassistant.voice.Command.PanicStop -> {
                PanicStopManager.triggerPanicStop()
                Toast.makeText(this, "PanicStop triggered", Toast.LENGTH_SHORT).show()
            }
            is com.shahrafuking.kingassistant.voice.Command.QueryStatus -> {
                Toast.makeText(this, "QueryStatus (demo)", Toast.LENGTH_SHORT).show()
            }
            is com.shahrafuking.kingassistant.voice.Command.Unknown -> {
                Toast.makeText(this, "Unknown command: ${cmd.raw}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun demoSetBudget(amount: Double) {
        lifecycleScope.launch {
            val bm = BudgetManager(this@MainActivity)
            bm.setBudget(amount)
            runOnUiThread { Toast.makeText(this@MainActivity, "Budget set: $amount", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun demoSimulateTrade(amount: Double) {
        lifecycleScope.launch {
            val bm = BudgetManager(this@MainActivity)
            val ok = bm.checkAndReserve(amount)
            runOnUiThread {
                if (ok) Toast.makeText(this@MainActivity, "Simulated trade reserved: $amount", Toast.LENGTH_SHORT).show()
                else Toast.makeText(this@MainActivity, "Insufficient budget", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun demoPanicStop() {
        PanicStopManager.triggerPanicStop()
        Toast.makeText(this, "PanicStop triggered (demo)", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // ok
            } else {
                Toast.makeText(this, "Microphone permission required", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_OVERLAY) {
            ensurePermissionsAndStartOverlayService()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recognizer.stopListening()
    }
}
