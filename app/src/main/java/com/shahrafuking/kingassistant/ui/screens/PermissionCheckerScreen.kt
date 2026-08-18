package com.shahrafuking.kingassistant.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun PermissionCheckerScreen(onClose: () -> Unit = {}) {
    val ctx = LocalContext.current
    val pkg = ctx.packageName

    // required permissions we care about here
    val required = listOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.INTERNET
    )

    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Text("Permissions", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))
        for (p in required) {
            val status = ContextCompat.checkSelfPermission(ctx, p)
            val label = when (p) {
                android.Manifest.permission.RECORD_AUDIO -> "Microphone"
                android.Manifest.permission.CAMERA -> "Camera"
                android.Manifest.permission.INTERNET -> "Internet"
                else -> p
            }
            Text("$label: ${if (status == android.content.pm.PackageManager.PERMISSION_GRANTED) \"GRANTED\" else \"MISSING\"}")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = {
            // open app settings so the user can grant permissions
            val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", pkg, null)
            }
            ctx.startActivity(i)
        }) {
            Text("Open App Settings")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onClose() }) {
            Text("Close")
        }
    }
}
