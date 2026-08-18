package com.shahrafuking.kingassistant.ui.screens

import android.content.Context
import android.media.MediaPlayer
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
import java.io.File

@Composable
fun VoiceSamplesList(ctx: Context, onClose: () -> Unit = {}) {
    var samples by remember { mutableStateOf(listOf<File>()) }
    var player: MediaPlayer? by remember { mutableStateOf(null) }

    fun loadSamples() {
        val dir = File(ctx.filesDir, "voice_samples")
        samples = if (dir.exists()) dir.listFiles()?.toList() ?: emptyList() else emptyList()
    }

    LaunchedEffect(Unit) { loadSamples() }

    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Text("Voice Samples", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))

        if (samples.isEmpty()) {
            Text("No voice samples found.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(samples) { f ->
                    Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(f.name)
                                Text("Size: ${f.length()} bytes", style = MaterialTheme.typography.caption)
                            }
                            Row {
                                Button(onClick = {
                                    try {
                                        player?.release()
                                        player = MediaPlayer().apply {
                                            setDataSource(f.absolutePath)
                                            prepare()
                                            start()
                                        }
                                    } catch (_: Throwable) {}
                                }) { Text("Play") }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = {
                                    if (player?.isPlaying == true) player?.stop()
                                    f.delete()
                                    loadSamples()
                                }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Button(onClick = { onClose() }) { Text("Close") }
        }
    }
}
