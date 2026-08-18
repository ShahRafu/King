package com.shahrafuking.kingassistant.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * PermissionCheckerScreen
 * - Shows key permissions and allows quick actions: request or open app settings
 */
@Composable
fun PermissionCheckerScreen(onClose: () -> Unit = {}) {
    val ctx = LocalContext.current
    val pkg = ctx.packageName

    val permissions = listOf(
        android.Manifest.permission.RECORD_AUDIO to "Microphone",
        android.Manifest.permission.CAMERA to "Camera",
        android.Manifest.permission.INTERNET to "Internet",
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) android.Manifest.permission.READ_MEDIA_IMAGES to "Media" else android.Manifest.permission.READ_EXTERNAL_STORAGE to "Media"
    )

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        // no-op; UI reads current state when needed
    }

    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Text("Permissions", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))

        for ((perm, labelAny) in permissions) {
            val permGranted = androidx.core.content.ContextCompat.checkSelfPermission(ctx, perm) == android.content.pm.PackageManager.PERMISSION_GRANTED
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(labelAny)
                    Text(if (permGranted) "GRANTED" else "MISSING", style = MaterialTheme.typography.caption)
                }
                if (!permGranted) {
                    Button(onClick = { launcher.launch(arrayOf(perm)) }) { Text("Request") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", pkg, null)
                        }
                        ctx.startActivity(i)
                    }) { Text("Open Settings") }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onClose) { Text("Close") }
    }
}
