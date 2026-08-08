package com.shahrafuking.kingassistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    authenticated: Boolean,
    onStartAuth: () -> Unit,
    onSendText: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Voice Orb placeholder
        Box(
            modifier = Modifier
                .size(180.dp)
                .background(color = androidx.compose.ui.graphics.Color(0xFF2E7D32)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = if (authenticated) "শাহ্ রাফু কিং — লগ ইন" else "King Assistant", color = androidx.compose.ui.graphics.Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        var input by remember { mutableStateOf("") }
        OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(), label = { Text("লিখে পাঠান") })

        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Button(onClick = { onSendText(input) }) { Text("পাঠান") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onStartAuth) { Text("ভয়েস‑লগইন") }
        }
    }
}
