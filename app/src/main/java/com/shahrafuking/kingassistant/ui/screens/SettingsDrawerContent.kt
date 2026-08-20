package com.shahrafuking.kingassistant.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import coil.compose.AsyncImage
import com.shahrafuking.kingassistant.settings.SecurePrefs
import kotlinx.coroutines.launch

@Composable
fun SettingsDrawerContent(ctx: Context, settingsRepo: SettingsRepository) {
    val scope = rememberCoroutineScope()
    val secure = remember { SecurePrefs(ctx) }
    val backendUrl by settingsRepo.backendUrlFlow.collectAsState(initial = "")
    val backendTokenFlow = secure.tokenFlow()
    val backendToken by backendTokenFlow.collectAsState(initial = "")
    val appLogoUri by settingsRepo.appLogoUriFlow.collectAsState(initial = "")

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            scope.launch { settingsRepo.setAppLogoUri(it.toString()) }
        }
    }

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

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { Toast.makeText(ctx, "Settings saved", Toast.LENGTH_SHORT).show() }) {
                Text("Save & Close")
            }
            Button(onClick = { launcher.launch("image/*") }) {
                Text("Pick App Logo")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (appLogoUri.isNotBlank()) {
            Text("Current App Logo:")
            Spacer(modifier = Modifier.height(8.dp))
            AsyncImage(model = appLogoUri, contentDescription = "App logo preview", modifier = Modifier.size(96.dp))
        }
    }
}
