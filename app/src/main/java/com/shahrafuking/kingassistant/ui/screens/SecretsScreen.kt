package com.shahrafuking.kingassistant.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shahrafuking.kingassistant.security.SecretsManager

@Composable
fun SecretsScreen(onClose: () -> Unit = {}) {
    val ctx = LocalContext.current
    val sm = remember { SecretsManager(ctx) }
    var apiKey by remember { mutableStateOf(sm.getSecret("AI_API_KEY") ?: "") }
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Text("API Keys", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("AI API Key") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Button(onClick = {
                sm.putSecret("AI_API_KEY", apiKey)
                Toast.makeText(ctx, "API key saved (encrypted).", Toast.LENGTH_SHORT).show()
            }) {
                Text("Save")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                sm.removeSecret("AI_API_KEY")
                apiKey = ""
                Toast.makeText(ctx, "Removed", Toast.LENGTH_SHORT).show()
            }) {
                Text("Remove")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onClose() }) {
                Text("Close")
            }
        }
    }
}
