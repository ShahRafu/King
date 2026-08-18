package com.shahrafuking.kingassistant.ui.screens

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

/**
 * MediaBackupScreen - lists images from app external files dir and allows selecting for backup (stub).
 * Note: real external storage access on modern Android requires scoped storage handling.
 */
@Composable
fun MediaBackupScreen(ctx: Context, onClose: () -> Unit = {}) {
    var files by remember { mutableStateOf(listOf<File>()) }
    var selected by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        val pics = ctx.getExternalFilesDir(null)?.let { File(it, "Pictures") }
        val list = mutableListOf<File>()
        if (pics != null && pics.exists()) list.addAll(pics.listFiles()?.toList() ?: emptyList())
        files = list
    }

    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Text("Media Backup", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))

        if (files.isEmpty()) {
            Text("No media files found in app files/Pictures.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(files) { f ->
                    Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            AsyncImage(model = f.path, contentDescription = f.name, modifier = Modifier.size(64.dp))
                            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(f.name)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Size: ${f.length()} bytes", style = MaterialTheme.typography.caption)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                selected = if (selected.contains(f.path)) selected - f.path else selected + f.path
                            }) {
                                Text(if (selected.contains(f.path)) "Deselect" else "Select")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Button(onClick = { /* TODO: upload selected files to cloud - stub */ }) { Text("Backup Selected") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onClose) { Text("Close") }
        }
    }
}
