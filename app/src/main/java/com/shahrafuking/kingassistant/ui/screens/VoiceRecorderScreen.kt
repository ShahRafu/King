package com.shahrafuking.kingassistant.ui.screens

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VoiceRecorderScreen(ctx: Context, onClose: () -> Unit = {}) {
    var recording by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var recorder: MediaRecorder? by remember { mutableStateOf(null) }

    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Text("Voice Recorder (samples saved locally)", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            if (!recording) {
                try {
                    val samplesDir = File(ctx.filesDir, "voice_samples")
                    if (!samplesDir.exists()) samplesDir.mkdirs()
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val outFile = File(samplesDir, "sample_$timestamp.3gp")
                    val mr = MediaRecorder().apply {
                        setAudioSource(MediaRecorder.AudioSource.MIC)
                        setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                        setOutputFile(outFile.absolutePath)
                        prepare()
                        start()
                    }
                    recorder = mr
                    recording = true
                    status = "Recording..."
                } catch (e: Throwable) {
                    Log.e("VoiceRecorder", "start failed", e)
                    status = "Record failed: ${e.message}"
                }
            } else {
                try {
                    recorder?.apply {
                        stop()
                        release()
                    }
                    recorder = null
                    recording = false
                    status = "Saved sample"
                } catch (e: Throwable) {
                    Log.e("VoiceRecorder", "stop failed", e)
                    status = "Stop failed: ${e.message}"
                }
            }
        }) { Text(if (!recording) "Start Recording" else "Stop Recording") }

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onClose) { Text("Close") }
        Spacer(modifier = Modifier.height(8.dp))
        Text(status)
    }
}
