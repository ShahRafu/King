package com.shahrafuking.kingassistant.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shahrafuking.kingassistant.logo.LogoManager

/**
 * LogoPickerScreen
 * - Allows picking an external logo (for marketing/launcher) and an internal logo (in-app)
 * - For now this is a simple picker that stores the chosen URI strings in LogoManager.
 * - Image previewing is intentionally minimal (shows URI) — integrate your image loader (Coil/Glide)
 *   in future if you want visual previews.
 */
@Composable
fun LogoPickerScreen(ctx: Context, onClose: () -> Unit = {}) {
    val lm = remember { LogoManager(ctx) }
    var externalUri by remember { mutableStateOf(lm.getExternalLogo()) }
    var internalUri by remember { mutableStateOf(lm.getInternalLogo()) }

    // pick single image
    val pickExternal = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        externalUri = uri?.toString()
        lm.setExternalLogo(externalUri)
    }
    val pickInternal = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        internalUri = uri?.toString()
        lm.setInternalLogo(internalUri)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Text("App Logo & Theme", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("External logo (used for marketing / launcher):")
                Spacer(modifier = Modifier.height(6.dp))
                Text(externalUri ?: "No external logo set")
                Spacer(modifier = Modifier.height(6.dp))
                Row {
                    Button(onClick = { pickExternal.launch("image/*") }) { Text("Pick External") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { lm.clearExternal(); externalUri = null }) { Text("Clear") }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Internal logo (shown inside the app):")
                Spacer(modifier = Modifier.height(6.dp))
                Text(internalUri ?: "No internal logo set")
                Spacer(modifier = Modifier.height(6.dp))
                Row {
                    Button(onClick = { pickInternal.launch("image/*") }) { Text("Pick Internal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { lm.clearInternal(); internalUri = null }) { Text("Clear") }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = onClose) { Text("Done") }
    }
}
