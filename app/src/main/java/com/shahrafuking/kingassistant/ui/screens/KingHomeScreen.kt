package com.shahrafuking.kingassistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.getValue
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import android.widget.Toast

/**
 * KingHomeScreen - Final home UI skeleton as specified by the product owner.
 * Shows: top-left settings, top-right future slot + IP status, center Voice Orb with live badge,
 * bottom typing chatbox with integrated voice mic button like Gemini search bar.
 */

@Composable
fun KingHomeScreen(onOpenSettings: () -> Unit = {}, onOpenVoiceSamples: () -> Unit = {}) {
    val ctx = LocalContext.current

    // Drawer state for settings
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Settings repository & current mode
    val settingsRepo = remember { SettingsRepository(ctx) }
    val currentMode by settingsRepo.raghuPreviewModeFlow.collectAsState(initial = RaghuPreviewMode.EXTERNAL)

    ModalDrawer(
        drawerState = drawerState,
        drawerContent = {
            SettingsDrawerContent(currentMode = currentMode, onModeSelected = { mode ->
                scope.launch {
                    settingsRepo.setRaghuPreviewMode(mode)
                    drawerState.close()
                }
            })
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("King Assistant") },
                    navigationIcon = {
                        IconButton(onClick = {
                            onOpenSettings()
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Settings")
                        }
                    },
                    actions = {
                        // Future slot (+)
                        IconButton(onClick = { /* TODO: future slot action */ }) {
                            Icon(Icons.Filled.Add, contentDescription = "Future Slot")
                        }
                        // IP / Security status (simple badge)
                        Box(modifier = Modifier.padding(end = 8.dp), contentAlignment = Alignment.Center) {
                            Text("IP: OK", fontSize = 12.sp, color = Color.White)
                        }
                    }
                )
            },
            content = { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Voice Orb and live badge
                    VoiceOrb(
                        probability = 0,
                        budgetText = "--",
                        onClick = onOpenVoiceSamples,
                        mode = currentMode
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live badge showing budget/probability
                    Card(backgroundColor = Color(0xFF1F8A70), modifier = Modifier.padding(8.dp)) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Budget: --", color = Color.White, modifier = Modifier.weight(1f))
                            Text("Prob: --%", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Typing chatbox at bottom with integrated voice mic (like Gemini search bar)
                    var text by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            placeholder = { Text("Type a message or tap mic") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    // Voice mic pressed - placeholder action
                                    Toast.makeText(ctx, "Voice input (placeholder)", Toast.LENGTH_SHORT).show()
                                    // Here you can start actual voice recognition or recorder
                                }) {
                                    Icon(Icons.Filled.Mic, contentDescription = "Voice")
                                }
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(onClick = { /* send message */ Toast.makeText(ctx, "Send (placeholder)", Toast.LENGTH_SHORT).show() }) {
                            Text("Send")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        )
    }
}
