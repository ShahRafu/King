package com.shahrafuking.kingassistant.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.shahrafuking.kingassistant.R
import com.shahrafuking.kingassistant.audio.AudioRecorder
import com.shahrafuking.kingassistant.voice.VoiceEnrollmentManager
import com.shahrafuking.kingassistant.voice.VoiceSecurityManager
import com.shahrafuking.kingassistant.voice.preproc.AntiSpoofPreprocessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VoiceTestActivity : AppCompatActivity() {
    private lateinit var enrollBtn: Button
    private lateinit var verifyBtn: Button
    private lateinit var statusTv: TextView
    private lateinit var progress: ProgressBar

    private val audioRecorder by lazy { AudioRecorder(this) }
    private val enrollmentManager by lazy { VoiceEnrollmentManager(this) }
    private lateinit var voiceSecurityManager: VoiceSecurityManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) Toast.makeText(this, "Microphone permission required", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_test)
        enrollBtn = findViewById(R.id.vt_enroll)
        verifyBtn = findViewById(R.id.vt_verify)
        statusTv = findViewById(R.id.vt_status)
        progress = findViewById(R.id.vt_progress)

        voiceSecurityManager = VoiceSecurityManager(applicationContext, modelAssetPath = "sample_speaker_model.tflite", modelFilePath = null)

        enrollBtn.setOnClickListener { startEnrollFlow() }
        verifyBtn.setOnClickListener { startVerifyFlow() }

        val perm = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        if (perm != PackageManager.PERMISSION_GRANTED) requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startEnrollFlow() {
        statusTv.text = "Recording 3 enrollment samples..."
        progress.visibility = View.VISIBLE
        progress.max = 3
        progress.progress = 0
        CoroutineScope(Dispatchers.Main).launch {
            val samples = mutableListOf<FloatArray>()
            for (i in 1..3) {
                statusTv.text = "Recording sample $i of 3..."
                val s = withContext(Dispatchers.Default) { enrollmentManager.recordAndExtract(1800) }
                if (s == null) {
                    statusTv.text = "Recording failed at sample $i"
                    progress.visibility = View.INVISIBLE
                    return@launch
                }
                // convert DoubleArray to FloatArray
                val fa = FloatArray(s.size); for (j in s.indices) fa[j] = s[j].toFloat()
                samples.add(fa)
                progress.progress = i
                // short pause
                Thread.sleep(400)
            }
            // average
            val len = samples[0].size
            val avg = FloatArray(len)
            for (s in samples) for (k in 0 until len) avg[k] += s[k] / samples.size
            statusTv.text = "Enrolling..."
            voiceSecurityManager.enroll(avg) { ok ->
                runOnUiThread {
                    progress.visibility = View.INVISIBLE
                    statusTv.text = if (ok) "Enrollment saved." else "Enrollment failed."
                    Toast.makeText(this@VoiceTestActivity, if (ok) "Enrolled" else "Enroll failed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun startVerifyFlow() {
        statusTv.text = "Recording verification sample..."
        progress.visibility = View.VISIBLE
        progress.max = 1
        progress.progress = 0
        CoroutineScope(Dispatchers.Main).launch {
            val s = withContext(Dispatchers.Default) { enrollmentManager.recordAndExtract(1800) }
            if (s == null) { statusTv.text = "Recording failed"; progress.visibility = View.INVISIBLE; return@launch }
            val fa = FloatArray(s.size); for (j in s.indices) fa[j] = s[j].toFloat()
            // For antispoof we need raw pcm; enrollmentManager doesn't return PCM here — instead record a short pcm via AudioRecorder quickly
            val pcmShorts = recordShortPcm(1200)
            val livenessFeatures = pcmShorts?.let { AntiSpoofPreprocessor.preprocessFromShorts(it, 16000) }
            statusTv.text = "Verifying..."
            voiceSecurityManager.verify(fa, livenessFeatures) { passed, sim, liveScore ->
                runOnUiThread {
                    progress.visibility = View.INVISIBLE
                    statusTv.text = "Result: passed=$passed sim=${"%.3f".format(sim)} live=${"%.3f".format(liveScore)}"
                    Toast.makeText(this@VoiceTestActivity, statusTv.text, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // helper to record raw PCM via AudioRecorder for given ms duration
    private suspend fun recordShortPcm(durationMs: Long, sampleRate: Int = 16000): ShortArray? = withContext(Dispatchers.Default) {
        try {
            val list = mutableListOf<Short>()
            val recorder = AudioRecorder(this@VoiceTestActivity)
            if (!recorder.hasRecordPermission()) return@withContext null
            val done = java.lang.Object()
            recorder.start({ pcmChunk, sr -> synchronized(list) { for (p in pcmChunk) list.add(p) } }, sampleRate)
            Thread.sleep(durationMs)
            recorder.stop()
            synchronized(list) {
                val arr = ShortArray(list.size)
                for (i in list.indices) arr[i] = list[i]
                return@withContext arr
            }
        } catch (t: Throwable) {
            null
        }
    }
}
