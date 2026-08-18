package com.shahrafuking.kingassistant.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CloudRecoveryScreen(onClose: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Text("Data Recovery & Cloud Sync", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Export local settings & memory into a zip and share or upload to cloud.")
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { /* TODO: implement export to zip and show share sheet */ }) { Text("Export Data (1‑Click)") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { /* TODO: start cloud sync job */ }) { Text("Start Cloud Sync") }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onClose) { Text("Close") }
    }
}
