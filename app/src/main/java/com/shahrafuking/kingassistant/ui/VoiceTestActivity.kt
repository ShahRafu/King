package com.shahrafuking.kingassistant.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shahrafuking.kingassistant.voice.tflite.SpeakerVerifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VoiceTestActivity : ComponentActivity() {
    private val TAG = "VoiceTestActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create verifier with asset names placed at app/src/main/assets/
        val verifier = try {
            SpeakerVerifier(
                applicationContext,
                modelAssetPath = "speaker_model.tflite",
                antiSpoofAssetPath = "antispoof_model.tflite"
            ).also { Log.i(TAG, "SpeakerVerifier constructed (attempting asset load)") }
        } catch (t: Throwable) {
            Log.w(TAG, "SpeakerVerifier ctor failed: ${t.message}")
            null
        }

        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                val status = remember { mutableStateOf("Ready") }
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Voice model test", modifier = Modifier.padding(bottom = 8.dp))
                    Text(text = "Status: ${status.value}", modifier = Modifier.padding(bottom = 12.dp))
                    Button(onClick = {
                        status.value = "Running load & quick check..."
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                // quick smoke: computeEmbedding on a dummy vector
                                val dummy = FloatArray(512) { i -> (i % 10) / 10f }
                                val emb = verifier?.computeEmbedding(dummy)
                                val anti = verifier?.runAntiSpoofModel(FloatArray(40) { 0.1f })
                                Log.i(TAG, "computeEmbedding -> ${emb?.size ?: "null"}; antispoof -> $anti")
                                status.value = "OK: emb=${emb?.size ?: "null"}, anti=$anti"
                            } catch (t: Throwable) {
                                Log.w(TAG, "smoke test failed: ${t.message}")
                                status.value = "Smoke test failed: ${t.message}"
                            }
                        }
                    }) {
                        Text("Run smoke test")
                    }
                    Button(onClick = {
                        try { verifier?.close(); status.value = "Verifier closed" } catch (t: Throwable) { status.value = "Close failed" }
                    }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Close verifier")
                    }
                }
            }
        }
    }
}
