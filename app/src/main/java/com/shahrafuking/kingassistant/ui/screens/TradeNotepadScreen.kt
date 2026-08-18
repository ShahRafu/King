package com.shahrafuking.kingassistant.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shahrafuking.kingassistant.net.GitHubUploader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File

@Composable
fun TradeNotepadScreen(ctx: Context, onClose: () -> Unit = {}) {
    val prefs = ctx.getSharedPreferences("king_prefs", Context.MODE_PRIVATE)
    var notes = prefs.getString("trade_notepad", "[]") ?: "[]"

    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Text("Trade Notepad", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))
        // existing UI omitted for brevity (assumes earlier version exists)
        Button(onClick = {
            // export notes to file and upload
            val filename = "trade_notepad_export_${System.currentTimeMillis()}.json"
            val out = File(ctx.filesDir, filename)
            out.writeText(notes)
            // upload if configured
            val owner = prefs.getString("backup_remote_owner", null)
            val repo = prefs.getString("backup_remote_repo", null)
            val remotePathBase = prefs.getString("backup_remote_path", "backups") ?: "backups"
            if (!owner.isNullOrBlank() && !repo.isNullOrBlank()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val uploader = GitHubUploader(ctx)
                    uploader.uploadFile(owner, repo, "$remotePathBase/$filename", out, "king: trade notepad export $filename")
                }
            }
        }) { Text("Export & Upload") }

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onClose) { Text("Done") }
    }
}
