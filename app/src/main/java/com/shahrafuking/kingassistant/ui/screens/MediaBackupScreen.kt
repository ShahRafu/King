package com.shahrafuking.kingassistant.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shahrafuking.kingassistant.net.GitHubUploader
import com.shahrafuking.kingassistant.util.ZipUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MediaBackupScreen(ctx: Context, onClose: () -> Unit = {}) {
    var files by remember { mutableStateOf(listOf<File>()) }
    var selected by remember { mutableStateOf(mutableSetOf<String>()) }
    var status by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val prefs = ctx.getSharedPreferences("king_prefs", Context.MODE_PRIVATE)

    // permission launcher
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) loadFiles()
        else status = "Permission denied. Open app settings to allow access."
    }

    fun loadFiles() {
        val pics = ctx.getExternalFilesDir(null)?.let { File(it, "Pictures") }
        val list = mutableListOf<File>()
        if (pics != null && pics.exists()) list.addAll(pics.listFiles()?.toList() ?: emptyList())
        files = list
    }

    LaunchedEffect(Unit) {
        if (ctx.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) loadFiles() else permissionLauncher.launch(permission)
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
                                if (selected.contains(f.path)) selected.remove(f.path) else selected.add(f.path)
                                // trigger recomposition
                                selected = selected
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
            Button(onClick = {
                // manual backup: zip selected and upload
                if (selected.isEmpty()) {
                    status = "No files selected"
                    return@Button
                }
                scope.launch {
                    status = "Zipping..."
                    val inputs = selected.map { File(it) }
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val out = File(ctx.filesDir, "memory_backup_$timestamp.zip")
                    withContext(Dispatchers.IO) {
                        ZipUtil.zipFiles(out, inputs)
                    }
                    status = "Zip created: ${out.name}"

                    // upload if configured
                    val owner = prefs.getString("backup_remote_owner", null)
                    val repo = prefs.getString("backup_remote_repo", null)
                    val remotePathBase = prefs.getString("backup_remote_path", "backups") ?: "backups"
                    if (!owner.isNullOrBlank() && !repo.isNullOrBlank()) {
                        status = "Uploading to GitHub..."
                        val uploader = GitHubUploader(ctx)
                        val remotePath = "$remotePathBase/${out.name}"
                        val ok = withContext(Dispatchers.IO) { uploader.uploadFile(owner, repo, remotePath, out, "king: manual media backup ${out.name}") }
                        status = if (ok) "Uploaded to $owner/$repo:$remotePath" else "Upload failed"
                    } else {
                        status = "Remote repo not configured; zip saved locally: ${out.absolutePath}"
                    }
                }
            }) { Text("Backup Selected") }

            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                // open app settings for permission
                val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", ctx.packageName, null)
                }
                ctx.startActivity(i)
            }) { Text("Open App Settings") }

            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onClose) { Text("Close") }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(status)
    }
}
