package com.shahrafuking.kingassistant.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import com.shahrafuking.kingassistant.settings.SecurePrefs

@Composable
fun SettingsDrawerContent(ctx: Context, settingsRepo: SettingsRepository) {
    val scope = rememberCoroutineScope()
    val secure = remember { SecurePrefs(ctx) }
    val backendUrl by settingsRepo.backendUrlFlow.collectAsState(initial = "")
    val backendTokenFlow = secure.tokenFlow()
    val backendToken by backendTokenFlow.collectAsState(initial = "")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = backendUrl,
            onValueChange = { v -> scope.launch { settingsRepo.setBackendUrl(v) } },
            label = { Text("Backend URL") },
            placeholder = { Text("http://192.168.0.5:8080") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = backendToken,
            onValueChange = { v -> scope.launch { secure.setToken(v) } },
            label = { Text("Backend Token") },
            placeholder = { Text("set secure token") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        Spacer(Modifier.height(16.dp))

        Button(onClick = { Toast.makeText(ctx, "Settings saved", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth()) {
            Text("Save & Close")
        }
    }
}
