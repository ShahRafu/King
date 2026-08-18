package com.shahrafuking.kingassistant.ui

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.shahrafuking.kingassistant.R
import com.shahrafuking.kingassistant.illumination.ActiveIlluminationController
import com.shahrafuking.kingassistant.illumination.AmbientLightChecker
import com.shahrafuking.kingassistant.security.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * HighSecurityApprovalActivity
 *
 * CRITICAL approval flow: voice -> ambient check -> consent-if-needed -> adaptive illumination -> lip-sync -> iris.
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

        // Instantiate helpers
        val voiceGate = com.shahrafuking.kingassistant.selfheal.VoiceAuthGatekeeper(this)
        val voiceAnalyzer = VoiceLivenessAnalyzer(this)
        val ambientChecker = AmbientLightChecker(this)
        val consentManager = ConsentManager(this)
        val illum = ActiveIlluminationController(this)
        val lipSync = LipSyncVerifier(this)
        val irisVerifier = IrisRobustVerifier(this)
        val audit = AuditLogger(this)

        btn.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                // 1) Voice approval (wake + challenge)
                val voiceOk = voiceGate.requestOwnerApproval("Authorize critical action")
                if (!voiceOk) {
                    resultView.text = "Approval denied: voice rejected"
                    Log.i(TAG, "voice rejected")
                    return@launch
                }

                // 2) Voice liveness/anti-spoof check (scaffold)
                val vres = voiceAnalyzer.verifyLiveVoice()
                if (!vres.success) {
                    resultView.text = "Approval denied: voice liveness failed"
                    Log.i(TAG, "voice liveness failed: ${vres.reason}")
                    return@launch
                }

                // 3) Ambient light check
                val lux = ambientChecker.getAmbientLux(500) // 500ms wait
                Log.i(TAG, "Ambient lux: $lux")
                val low = ambientChecker.isLowLight(lux)

                if (low) {
                    // 4) Consent (consent-once) and illumination
                    if (!consentManager.getIlluminationConsent()) {
                        // show one-time consent dialog
                        val approved = showIlluminationConsentDialog()
                        if (!approved) {
                            resultView.text = "Approval aborted: owner declined illumination"
                            Log.i(TAG, "illumination consent denied by owner")
                            audit.logIllumination(lux, "CONSENT_DENIED")
                            return@launch
                        } else {
                            consentManager.setIlluminationConsent(true)
                        }
                    }

                    // perform a single short pulse (torch if available, otherwise screen)
                    val illRes = if (illum.isTorchAvailable()) {
                        illum.pulseFlashlight(200)
                    } else {
                        illum.pulseScreen(this@HighSecurityApprovalActivity, 250)
                    }

                    Log.i(TAG, "Illumination decision: $illRes")
                    audit.logIllumination(lux, illRes.name)
                } else {
                    audit.logIllumination(lux, "NONE_NEEDED")
                }

                // 5) Lip-sync verification (pass activity)
                val lres = lipSync.verifyLipSync(this@HighSecurityApprovalActivity)
                if (!lres.success) {
                    resultView.text = "Approval denied: lip-sync failed"
                    Log.i(TAG, "lip-sync failed: ${lres.reason}")
                    return@launch
                }

                // 6) Iris verification (scaffold) - pass illumination info if needed in future
                val ires = irisVerifier.verifyIrisCapture()
                if (!ires.success) {
                    resultView.text = "Approval denied: iris verification failed"
                    Log.i(TAG, "iris failed: ${ires.reason}")
                    return@launch
                }

                // All checks passed
                resultView.text = "Approval granted"
                Log.i(TAG, "High-level approval granted")
                audit.logAuthResult(true, "voice+illum+lip+iris")
            }
        }
    }

    /**
     * Show a blocking consent dialog (runs on UI thread). Returns true if owner consents.
     * (This is a simple helper; you can replace with a more sophisticated UX.)
     */
    private fun showIlluminationConsentDialog(): Boolean {
        var approved = false
        val latch = java.util.concurrent.CountDownLatch(1)
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle("Allow brief flash for iris capture?")
                .setMessage("To verify your iris in low light, the app may briefly flash the torch or display a short white screen. This will only happen once per capture and only if needed. Allow this behavior?")
                .setPositiveButton("Allow") { _, _ ->
                    approved = true
                    latch.countDown()
                }
                .setNegativeButton("Deny") { _, _ ->
                    approved = false
                    latch.countDown()
                }
                .setCancelable(false)
                .show()
        }
        latch.await()
        return approved
    }
}
