package com.shahrafuking.kingassistant.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * VoiceEnrollScreen
 * - Simple UI to record N samples and enroll a voice profile.
 * - Uses VoiceEnrollmentManager (suspend functions). This is a scaffold UI.
 */
@Composable
fun VoiceEnrollScreen(onEnrollComplete: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var ownerName by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var samples by remember { mutableStateOf(listOf<String>()) }
    var status by remember { mutableStateOf("শুরু করুন") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("আপনার নাম") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        Text("নমুনা সংখ্যা: ${samples.size}")
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Button(onClick = {
                if (ownerName.isBlank()) { status = "প্রথমে নাম দিন"; return@Button }
                isRecording = true
                status = "রেকর্ডিং শুরু..."
                scope.launch {
                    try {
                        val mgr = com.shahrafuking.kingassistant.security.VoiceEnrollmentManager(ctx.applicationContext)
                        val path = mgr.recordSample("enroll_${samples.size + 1}", durationMs = 2000)
                        samples = samples + path
                        status = "নমুনা রেকর্ড হয়েছে"
                    } catch (ex: Exception) {
                        status = "রেকর্ডিং ব্যর্থ: ${ex.message}"
                    } finally {
                        isRecording = false
                    }
                }
            }) { Text("নমুনা রেকর্ড করুন (2s)") }

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = {
                if (samples.isEmpty()) { status = "কোনো নমুনা নেই"; return@Button }
                status = "এনরোল করা হচ্ছে..."
                scope.launch {
                    try {
                        val mgr = com.shahrafuking.kingassistant.security.VoiceEnrollmentManager(ctx.applicationContext)
                        val profileId = mgr.enrollProfile(ownerName, samples)
                        status = "এনরোল সম্পন্ন: $profileId"
                        onEnrollComplete(profileId)
                    } catch (ex: Exception) {
                        status = "এনরোল ব্যর্থ: ${ex.message}"
                    }
                }
            }) { Text("এনরোল সমাপ্ত করুন") }
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (isRecording) CircularProgressIndicator()
        Spacer(modifier = Modifier.height(8.dp))
        Text(status)
    }
}
