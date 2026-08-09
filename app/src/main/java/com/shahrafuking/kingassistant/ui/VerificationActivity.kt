package com.shahrafuking.kingassistant.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
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

class VerificationActivity : AppCompatActivity() {
    private lateinit var infoTv: TextView
    private lateinit var recBtn: Button

    private val verifier by lazy { VoiceVerifier(this) }
    private val enrollmentManager by lazy { VoiceEnrollmentManager(this) }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_LONG).show()
            finish()
        } else {
            startVerification()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification)
        infoTv = findViewById(R.id.verify_status)
        recBtn = findViewById(R.id.verify_btn)

        recBtn.setOnClickListener {
            val perm = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            if (perm != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                return@setOnClickListener
            }
            startVerification()
        }
    }

    private fun startVerification() {
        infoTv.text = "Please say the passphrase: \"King Assistant\" (or Bengali equivalent)"
        // First, run quick ASR check to match phrase
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            infoTv.text = "Speech recognizer unavailable. Cannot verify."
            return
        }
        val sr = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = RecognizerIntent().apply {
            action = RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() { infoTv.text = "Listening..." }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                infoTv.text = "ASR error: $error — try again"
                sr.destroy()
            }

            override fun onResults(results: Bundle?) {
                val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
                val transcript = texts.firstOrNull() ?: ""
                sr.destroy()
                val low = transcript.lowercase()
                if (isHotwordMatch(low)) {
                    // proceed with biometric check (record short sample & compare)
                    infoTv.text = "Passphrase detected. Verifying voice..."
                    CoroutineScope(Dispatchers.Main).launch {
                        val features = withContext(Dispatchers.Default) { enrollmentManager.recordAndExtract(1800) }
                        if (features == null) {
                            infoTv.text = "Recording failed during verification."
                            return@launch
                        }
                        val ok = verifier.verify(features, threshold = 0.78) // threshold tuned for prototype
                        if (ok) {
                            infoTv.text = "Verification successful — welcome Shah Rafu King."
                            Toast.makeText(this@VerificationActivity, "Verified", Toast.LENGTH_LONG).show()
                            // proceed to main activity
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            infoTv.text = "Verification failed — voice did not match."
                            Toast.makeText(this@VerificationActivity, "Verification failed", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    infoTv.text = "Passphrase not recognized. Try again."
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        try {
            sr.startListening(intent)
        } catch (t: Throwable) {
            infoTv.text = "ASR start failed"
            sr.destroy()
        }
    }

    private fun isHotwordMatch(low: String): Boolean {
        return ((low.contains("কিং") && low.contains("অ্যাসিস্ট্যান্ট")) ||
                low.contains("king assistant") ||
                low.contains("কিং অ্যাসিস্ট্যান্ট") ||
                low.contains("কিংঅ্যাসিস্ট্যান্ট"))
    }
}
