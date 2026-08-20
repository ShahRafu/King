package com.shahrafuking.kingassistant.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.input.TextFieldValue
import com.shahrafuking.kingassistant.data.model.SearchResult
import com.shahrafuking.kingassistant.data.repository.SearchRepository
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(repo: SearchRepository) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var provider by remember { mutableStateOf("wiki") }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.weight(1f), placeholder = { Text("Search") })
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                scope.launch {
                    // For simplicity, call backend directly from UI using SettingsRepository + SecurePrefs in real implementation
                    // Here we call repo.searchRemote (not fully implemented) as placeholder
                }
            }) {
                Text("Go")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Provider", fontSize = 12.sp)
        Row {
            listOf("wiki", "rss", "scrape").forEach { p ->
                Button(onClick = { provider = p }, modifier = Modifier.padding(end = 6.dp)) { Text(p) }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn {
            items(results) { r ->
                ResultItem(r)
                Divider()
            }
        }
    }
}

@Composable
fun ResultItem(r: SearchResult) {
    Column(modifier = Modifier.padding(12.dp)) {
        Text(text = r.title, style = MaterialTheme.typography.subtitle1)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = r.snippet, style = MaterialTheme.typography.body2)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = r.url, style = MaterialTheme.typography.caption)
    }
}
