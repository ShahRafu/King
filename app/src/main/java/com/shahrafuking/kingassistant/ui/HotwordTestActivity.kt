package com.shahrafuking.kingassistant.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shahrafuking.kingassistant.speech.HotwordEngineAdapter

/**
 * Minimal Compose test UI for Hotword POC
 */
class HotwordTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val engine = HotwordEngineAdapter.getInstance(this)
        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                val logs = remember { mutableStateListOf<String>() }
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(onClick = {
                        logs.add("Init hotword engine")
                        engine.init()
                    }) {
                        Text("Init Engine")
                    }
                    Button(onClick = {
                        logs.add("Simulate detection (placeholder)")
                        engine.onHotwordDetected()
                    }) {
                        Text("Simulate Hotword")
                    }
                    logs.takeLast(20).forEach { l -> Text(text = l) }
                }
            }
        }
    }
}
