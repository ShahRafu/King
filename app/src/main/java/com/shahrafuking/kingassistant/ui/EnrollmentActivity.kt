package com.shahrafuking.kingassistant.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.shahrafuking.kingassistant.R
import com.shahrafuking.kingassistant.voice.VoiceEnrollmentManager
import com.shahrafuking.kingassistant.voice.VoiceVerifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EnrollmentActivity : AppCompatActivity() {
    private lateinit var statusTv: TextView
    private lateinit var startBtn: Button
    private lateinit var progress: ProgressBar

    private val enrollmentManager by lazy { VoiceEnrollmentManager(this) }
    private val verifier by lazy { VoiceVerifier(this) }

    private val samplesRequired = 3
    private val samples = mutableListOf<DoubleArray>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_LONG).show()
        } else {
            startEnrollmentFlow()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enrollment)
        statusTv = findViewById(R.id.enroll_status)
        startBtn = findViewById(R.id.enroll_btn)
        progress = findViewById(R.id.enroll_progress)

        startBtn.setOnClickListener {
            val perm = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            if (perm != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                return@setOnClickListener
            }
            startEnrollmentFlow()
        }
    }

    private fun startEnrollmentFlow() {
        samples.clear()
        progress.max = samplesRequired
        progress.progress = 0
        statusTv.text = "Enrollment: speak the passphrase when prompted.\nSamples needed: $samplesRequired"

        CoroutineScope(Dispatchers.Main).launch {
            for (i in 1..samplesRequired) {
                statusTv.text = "Recording sample $i of $samplesRequired — Speak: \"King Assistant\" (or Bengali equivalent)"
                // small delay to let user get ready
                withContext(Dispatchers.IO) { Thread.sleep(600) }
                val features = enrollmentManager.recordAndExtract(2400)
                if (features == null) {
                    Toast.makeText(this@EnrollmentActivity, "Recording failed. Try again.", Toast.LENGTH_SHORT).show()
                    i - 1
                    // retry same index
                    // simple approach: break
                    statusTv.text = "Recording failed. Please retry enrollment."
                    return@launch
                }
                samples.add(features)
                progress.progress = samples.size
                statusTv.text = "Captured sample $i"
                // short pause
                withContext(Dispatchers.IO) { Thread.sleep(400) }
            }

            // average vectors
            if (samples.isNotEmpty()) {
                val n = samples.size
                val len = samples[0].size
                val avg = DoubleArray(len)
                for (s in samples) for (k in 0 until len) avg[k] += s[k] / n
                val success = verifier.saveTemplate(avg)
                if (success) {
                    statusTv.text = "Enrollment complete — voice template saved securely."
                    Toast.makeText(this@EnrollmentActivity, "Enrollment successful", Toast.LENGTH_LONG).show()
                    // goto verification or main
                    setResult(RESULT_OK)
                    finish()
                } else {
                    statusTv.text = "Failed to save template."
                    Toast.makeText(this@EnrollmentActivity, "Save failed", Toast.LENGTH_LONG).show()
                }
            } else {
                statusTv.text = "No samples captured."
            }
        }
    }
}
