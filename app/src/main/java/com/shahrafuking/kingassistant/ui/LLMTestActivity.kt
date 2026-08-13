package com.shahrafuking.kingassistant.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.shahrafuking.kingassistant.llm.LocalLLMManager
import com.shahrafuking.kingassistant.selfheal.FileIsolator
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * LLMTestActivity
 *
 * Simple Compose-based UI to validate the voice‑gated generation and staging flow using the
 * LocalLLMManager (which currently uses the mock native bridge). This lets you test the
 * VoiceAuthGatekeeper, staged file storage, and review workflow on-device before adding a
 * real native LLM backend.
 */
class LLMTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val llm = LocalLLMManager(this)
        val isolator = FileIsolator(this)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.padding(16.dp)) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("Local LLM Test (mock runtime)", style = MaterialTheme.typography.h6)
                        Spacer(modifier = Modifier.height(12.dp))

                        var modelPath by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = modelPath,
                            onValueChange = { modelPath = it },
                            label = { Text("Model path (for record only)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        var promptText by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = promptText,
                            onValueChange = { promptText = it },
                            label = { Text("Enter natural language prompt (e.g., 'Create a Kotlin function to reverse a list')") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row {
                            Button(onClick = {
                                // Load model (mock) — just record the path for now
                                lifecycleScope.launch {
                                    val ok = llm.loadModel(modelPath)
                                    // mock returns false (stub), but UI will still allow generate to test voice gating
                                }
                            }) {
                                Text("Load Model (mock)")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(onClick = {
                                // Generate with voice approval and show result
                                lifecycleScope.launch {
                                    val result = llm.generateCodeWithApproval(promptText)
                                    // Update local state via a side-effect — use a simple mutable var
                                    // We can't update Compose state from here directly; use a temporary approach below
                                }
                            }) {
                                Text("Generate (voice-approved)")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // We will keep a simple local state holder for lastResult
                        var lastResult by remember { mutableStateOf("") }

                        // Hook: launch a coroutine that updates lastResult when a generation completes
                        // (We'll implement the generation call inline to get the result into lastResult.)

                        Button(onClick = {
                            lifecycleScope.launch {
                                val res = llm.generateCodeWithApproval(promptText)
                                lastResult = res ?: "(no result or owner denied)"
                            }
                        }) {
                            Text("Generate and preview")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Preview / Generated Output:")
                        OutlinedTextField(
                            value = lastResult,
                            onValueChange = { lastResult = it },
                            label = { Text("Generated output") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        var filename by remember { mutableStateOf("") }
                        if (filename.isBlank()) {
                            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            filename = "generated_$ts.kt"
                        }

                        OutlinedTextField(
                            value = filename,
                            onValueChange = { filename = it },
                            label = { Text("Staged filename (will be saved into app internal staged dir)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(onClick = {
                            // Stage the current preview into internal staged dir
                            lifecycleScope.launch {
                                if (lastResult.isBlank()) return@launch
                                val stagedPath = isolator.stageFileContents(filename, lastResult)
                                // simple feedback: overwrite lastResult to show path
                                lastResult = "Staged to: $stagedPath"
                            }
                        }) {
                            Text("Stage Preview (save to staged dir)")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Staged files:")
                        val stagedDir = File(filesDir, "selfheal_staged")
                        val stagedList = stagedDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
                        for (f in stagedList) {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(f.name, modifier = Modifier.weight(1f))
                                Button(onClick = {
                                    lifecycleScope.launch {
                                        val content = isolator.readStaged(f.name) ?: "(could not read)"
                                        // show content in preview
                                        // update lastResult
                                        // use runOnUiThread via lifecycleScope
                                        lastResult = content
                                    }
                                }) {
                                    Text("View")
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(onClick = {
                                    lifecycleScope.launch {
                                        if (f.exists()) f.delete()
                                    }
                                }) {
                                    Text("Delete")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text("Notes:")
                        Text("- This UI uses the mock native bridge.\n- Voice authorization is enforced before generation.\n- Staged files are saved to app internal storage (selfheal_staged).\n- Promotion to actual source tree or commit is a separate owner action.")

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
