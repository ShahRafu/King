package com.shahrafuking.kingassistant.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shahrafuking.kingassistant.logo.LogoManager
import com.shahrafuking.kingassistant.ui.components.VoiceOrb
import androidx.compose.ui.platform.LocalContext

@Composable
fun HomeScreen(
    authenticated: Boolean,
    onStartAuth: () -> Unit,
    onSendText: (String) -> Unit,
    onOpenSettings: () -> Unit, // new callback to open drawer
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    val lm = remember { LogoManager(ctx) }
    val internalLogo = remember { lm.getInternalLogo() }
    val prefs = ctx.getSharedPreferences("king_prefs", Context.MODE_PRIVATE)
    val ipEnabled = prefs.getBoolean("ip_rotation", false)

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onOpenSettings() }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
                if (!internalLogo.isNullOrBlank()) {
                    AsyncImage(model = internalLogo, contentDescription = "Internal Logo", modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = "King Assistant", style = MaterialTheme.typography.h6)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // future slot (+)
                IconButton(onClick = { /* future slot action */ }) {
                    Icon(Icons.Default.Add, contentDescription = "Future Slot")
                }
                // ip/security status
                Text(text = if (ipEnabled) "IP: ON" else "IP: OFF", color = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { /* security placeholder */ }) {
                    Icon(Icons.Default.Security, contentDescription = "Security")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                VoiceOrb(isActive = authenticated)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = if (authenticated) "স্বাগতম, শাহ্ রাফু কিং" else "ভয়েস লগইন করুন")
                Spacer(modifier = Modifier.height(8.dp))
                // live badge under orb
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Card(shape = RoundedCornerShape(8.dp), elevation = 2.dp) {
                        Text("বাজেট: নির্ধারিত নেই", modifier = Modifier.padding(6.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Card(shape = RoundedCornerShape(8.dp), elevation = 2.dp) {
                        Text("প্রবাবিলিটি: -", modifier = Modifier.padding(6.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Card(shape = RoundedCornerShape(12.dp), elevation = 4.dp) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("লিখে পাঠান") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Button(onClick = {
                                if (input.isNotBlank()) {
                                    onSendText(input)
                                }
                            }) { Text("পাঠান") }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = onStartAuth) { Text("ভয়েস‑লগইন") }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // bottom mini status / budget badge (duplicate removed)
    }
}
