package com.shahrafuking.kingassistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign

/**
 * KingHomeScreen - Final home UI skeleton as specified by the product owner.
 * Shows: top-left settings, top-right future slot + IP status, center Voice Orb with live badge,
 * bottom typing chatbox. All actions currently wired to placeholders or local screens in the branch.
 */

@Composable
fun KingHomeScreen(onOpenSettings: () -> Unit = {}, onOpenVoiceSamples: () -> Unit = {}) {
    val ctx = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("King Assistant") },
                navigationIcon = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Menu, contentDescription = "Settings")
                    }
                },
                actions = {
                    // Future slot (+)
                    IconButton(onClick = { /* TODO: future slot action */ }) {
                        Icon(Icons.Default.Add, contentDescription = "Future Slot")
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
                    onClick = onOpenVoiceSamples
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

                // Typing chatbox at bottom
                var text by remember { mutableStateOf("") }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { /* send message */ }) { Text("Send") }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    )
}
