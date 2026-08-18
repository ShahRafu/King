package com.shahrafuking.kingassistant.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.shahrafuking.kingassistant.R
import com.shahrafuking.kingassistant.audio.AudioRecorder
import com.shahrafuking.kingassistant.voice.VoiceEnrollmentManager
import com.shahrafuking.kingassistant.voice.VoiceVerifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.Random

class VerificationActivity : AppCompatActivity() {
    private lateinit var infoTv: TextView
    private lateinit var passTv: TextView
    private lateinit var recBtn: Button

    private val verifier by lazy { VoiceVerifier(this) }
    private val enrollmentManager by lazy { VoiceEnrollmentManager(this) }
    private val audioRecorder by lazy { AudioRecorder(this) }

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    // current dynamic passphrase shown
    private var currentPassphrase: String = ""

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_LONG).show()
            finish()
        } else {
            startVerificationFlow()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification)
        infoTv = findViewById(R.id.verify_status)
        passTv = TextView(this).apply {
            textSize = 20f
            visibility = View.VISIBLE
            setPadding(10, 20, 10, 10)
        }
        val parent = findViewById<View>(R.id.verify_status).parent
        if (parent is android.view.ViewGroup) {
            val idx = parent.indexOfChild(infoTv)
            parent.addView(passTv, idx + 1)
        }

        recBtn = findViewById(R.id.verify_btn)

        recBtn.setOnClickListener {
            val perm = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            if (perm != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                return@setOnClickListener
            }
            startVerificationFlow()
        }

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        tts?.shutdown()
        audioRecorder.stop()
    }

    private fun generatePassphrase(): String {
        val r = Random()
        return if (r.nextBoolean()) {
            val n = 1000 + r.nextInt(9000)
            n.toString()
        } else {
            val choices = listOf(
                "open sesame",
                "king assistant",
                "hello world",
                "blue moon",
                "শাহ্ রাফু",
                "কিং অ্যাসিস্ট্যান্ট"
            )
            choices[r.nextInt(choices.size)]
        }
    }

    private fun startVerificationFlow() {
        currentPassphrase = generatePassphrase()
        passTv.text = "Passphrase: $currentPassphrase"
        infoTv.text = "Please say the passphrase shown below."

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            infoTv.text = "Speech recognizer unavailable. Cannot verify."
            return
        }
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { infoTv.text = "Listening for passphrase..." }
            override fun onBeginningOfSpeech() {
                tts?.stop()
                infoTv.text = "Detected speech — capturing..."
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                infoTv.text = "ASR error: $error — try again"
            }

            override fun onResults(results: Bundle?) {
                val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
                val transcript = texts.firstOrNull() ?: ""
                handleTranscript(transcript)
            }

            override fun onPartialResults(partial: Bundle?) {
                val parts = partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
                val p = parts.firstOrNull() ?: return
                if (p.isNotBlank()) {
                    tts?.stop()
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            speechRecognizer?.startListening(intent)
        } catch (t: Throwable) {
            infoTv.text = "ASR start failed"
            return
        }

        tts?.speak("Please repeat: $currentPassphrase", TextToSpeech.QUEUE_FLUSH, null, "PASS_PROMPT")
    }

    private fun normalizeForMatch(s: String): String {
        return s.lowercase(Locale.getDefault()).replace(Regex("[^\\p{L}\\p{N}\\s]"), "").trim()
    }

    private fun handleTranscript(transcript: String) {
        val low = normalizeForMatch(transcript)
        val expected = normalizeForMatch(currentPassphrase)
        infoTv.text = "Heard: $transcript"

        if (expected.isNotEmpty() && low.contains(expected)) {
            infoTv.text = "Passphrase matched. Verifying voice..."
            try { speechRecognizer?.stopListening() } catch (_: Throwable) {}
            CoroutineScope(Dispatchers.Main).launch {
                // record short sample (1.8s) using existing enrollment manager
                val featuresDouble = withContext(Dispatchers.Default) { enrollmentManager.recordAndExtract(1800) }
                if (featuresDouble == null) {
                    infoTv.text = "Recording failed during verification."
                    return@launch
                }
                // convert DoubleArray -> FloatArray for verifier.verify(FloatArray) compatibility
                val featuresFloat = FloatArray(featuresDouble.size) { i -> featuresDouble[i].toFloat() }
                val ok = verifier.verify(featuresFloat, threshold = VoiceVerifier.DEFAULT_THRESHOLD)
                if (ok) {
                    infoTv.text = "Verification successful — welcome Shah Rafu King."
                    Toast.makeText(this@VerificationActivity, "Verified", Toast.LENGTH_LONG).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    infoTv.text = "Verification failed — voice did not match."
                    Toast.makeText(this@VerificationActivity, "Verification failed", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            infoTv.text = "Passphrase did not match. Try again."
        }
    }
}
