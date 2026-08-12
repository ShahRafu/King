package com.shahrafuking.kingassistant.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shahrafuking.kingassistant.ui.components.VoiceOrb

@Composable
fun HomeScreen(
    authenticated: Boolean,
    onStartAuth: () -> Unit,
    onSendText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* open settings */ }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
            Text(text = "King Assistant", style = MaterialTheme.typography.h6)
            IconButton(onClick = { /* security */ }) {
                Icon(Icons.Default.Lock, contentDescription = "Security")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                VoiceOrb(isActive = authenticated)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = if (authenticated) "স্বাগতম, শাহ্ রাফু কিং" else "ভয়েস লগইন করুন")
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
                            Button(onClick = { onSendText(input) }) { Text("পাঠান") }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = onStartAuth) { Text("ভয়েস‑লগইন") }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // bottom mini status / budget badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("বাজেট: নির্ধারিত নেই", color = Color.Gray)
            Text("প্রবাবিলিটি: -", color = Color.Gray)
        }
    }
}
