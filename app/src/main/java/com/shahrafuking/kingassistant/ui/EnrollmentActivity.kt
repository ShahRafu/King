package com.shahrafuking.kingassistant.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.shahrafuking.kingassistant.security.KeystoreHelper
import com.shahrafuking.kingassistant.security.SecurityUtils
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import android.util.Base64
import kotlinx.coroutines.*

/**
 * Simple EnrollmentActivity that records a short audio sample (PCM) and
 * produces a simple "template" (SHA-256 of the raw PCM) which is stored
 * encrypted using KeystoreHelper.
 *
 * This is intentionally minimal and safe: it does NOT compute ML embeddings here.
 * Replace the placeholder "template" generation with your model-based embedder.
 *
 * The Activity returns RESULT_OK when enrollment completed successfully.
 */
class EnrollmentActivity : AppCompatActivity() {
    private val SAMPLE_RATE = 16000
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val RECORD_SECONDS = 4 // record 4 seconds for enrollment
    private val TEMPLATE_KEY = "voice_enrollment_template_v1"

    private lateinit var statusTv: TextView
    private lateinit var btnRecord: Button
    private lateinit var progressBar: ProgressBar

    private var recordingJob: Job? = null

    private val requestMicrophone = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            Toast.makeText(this, "Microphone permission is required", Toast.LENGTH_LONG).show()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        statusTv = TextView(this).apply { text = "Enrollment: ready" }
        btnRecord = Button(this).apply { text = "Start Enrollment (record ~4s)"; setOnClickListener { startEnrollment() } }
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; progress = 0 }

        layout.addView(statusTv)
        layout.addView(btnRecord)
        layout.addView(progressBar)
        setContentView(layout)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestMicrophone.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startEnrollment() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission missing", Toast.LENGTH_SHORT).show()
            return
        }

        btnRecord.isEnabled = false
        statusTv.text = "Recording..."
        progressBar.progress = 0

        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
                val bufferSize = maxOf(minBuf, SAMPLE_RATE * 2)
                val recorder = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize)
                val audioFile = File(filesDir, "enrollment_raw.pcm")
                if (audioFile.exists()) audioFile.delete()
                val fos = FileOutputStream(audioFile)

                val shortBuf = ShortArray(bufferSize / 2)
                recorder.startRecording()
                val totalFrames = SAMPLE_RATE * RECORD_SECONDS
                var framesRead = 0
                while (framesRead < totalFrames && isActive) {
                    val toRead = minOf(shortBuf.size, totalFrames - framesRead)
                    val r = recorder.read(shortBuf, 0, toRead)
                    if (r > 0) {
                        // write raw PCM little-endian
                        val byteBuf = ShortArrayToByteArray(shortBuf, r)
                        fos.write(byteBuf)
                        framesRead += r
                        val p = (framesRead.toFloat() / totalFrames.toFloat() * 100).toInt()
                        withContext(Dispatchers.Main) { progressBar.progress = p }
                    } else {
                        delay(10)
                    }
                }
                recorder.stop()
                recorder.release()
                fos.flush()
                fos.close()

                // Simple placeholder "template": SHA-256 of the raw PCM file
                val pcmBytes = audioFile.readBytes()
                val sha = MessageDigest.getInstance("SHA-256").digest(pcmBytes)
                val templateB64 = Base64.encodeToString(sha, Base64.NO_WRAP)

                // Store encrypted via KeystoreHelper
                KeystoreHelper.encryptAndStoreString(this@EnrollmentActivity, TEMPLATE_KEY, templateB64)

                withContext(Dispatchers.Main) {
                    statusTv.text = "Enrollment saved."
                    Toast.makeText(this@EnrollmentActivity, "Enrollment completed", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EnrollmentActivity, "Enrollment failed: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                    statusTv.text = "Enrollment failed"
                    btnRecord.isEnabled = true
                }
            }
        }
    }

    override fun onDestroy() {
        recordingJob?.cancel()
        super.onDestroy()
    }

    private fun ShortArrayToByteArray(src: ShortArray, length: Int): ByteArray {
        val out = ByteArray(length * 2)
        var i = 0
        var o = 0
        while (i < length) {
            val s = src[i].toInt()
            out[o++] = (s and 0x00FF).toByte()
            out[o++] = ((s shr 8) and 0x00FF).toByte()
            i++
        }
        return out
    }
}
