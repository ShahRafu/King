package com.shahrafuking.kingassistant.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONArray

/**
 * TradeNotepadScreen - simple list of notes persisted to SharedPreferences as JSON array (MVP).
 */
@Composable
fun TradeNotepadScreen(ctx: Context, onClose: () -> Unit = {}) {
    val prefs = ctx.getSharedPreferences("king_prefs", Context.MODE_PRIVATE)
    var notes by remember { mutableStateOf(listOf<String>()) }
    var input by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val raw = prefs.getString("trade_notepad", "[]") ?: "[]"
        try {
            val arr = JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) list.add(arr.getString(i))
            notes = list
        } catch (e: Throwable) { notes = emptyList() }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Text("Trade Notepad", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("New note") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            if (input.isNotBlank()) {
                notes = notes + input
                input = ""
                val arr = JSONArray()
                notes.forEach { arr.put(it) }
                prefs.edit().putString("trade_notepad", arr.toString()).apply()
            }
        }) { Text("Add") }

        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(notes) { n ->
                Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(n, modifier = Modifier.weight(1f))
                        Button(onClick = {
                            notes = notes - n
                            val arr = JSONArray()
                            notes.forEach { arr.put(it) }
                            prefs.edit().putString("trade_notepad", arr.toString()).apply()
                        }) { Text("Delete") }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onClose) { Text("Done") }
    }
}
