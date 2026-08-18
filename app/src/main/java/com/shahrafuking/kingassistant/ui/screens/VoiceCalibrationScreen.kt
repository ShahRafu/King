package com.shahrafuking.kingassistant.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * VoiceCalibrationScreen - slider to adjust voice sensitivity stored in preferences.
 */
@Composable
fun VoiceCalibrationScreen(ctx: Context, onClose: () -> Unit = {}) {
    val prefs = ctx.getSharedPreferences("king_prefs", Context.MODE_PRIVATE)
    var sensitivity by remember { mutableStateOf(prefs.getFloat("voice_sensitivity_value", 0.5f)) }

    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Text("Voice Calibration", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Sensitivity: ${(sensitivity * 100).toInt()} %")
        Slider(value = sensitivity, onValueChange = { sensitivity = it }, valueRange = 0f..1f)
        Spacer(modifier = Modifier.height(12.dp))
        Row {
            Button(onClick = {
                prefs.edit().putFloat("voice_sensitivity_value", sensitivity).apply()
            }) { Text("Save") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onClose) { Text("Close") }
        }
    }
}
