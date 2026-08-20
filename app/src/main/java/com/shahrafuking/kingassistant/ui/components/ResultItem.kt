package com.shahrafuking.kingassistant.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shahrafuking.kingassistant.data.model.SearchResult

@Composable
fun ResultItem(result: SearchResult) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable { /* TODO: open link */ }
        .padding(12.dp)) {
        Text(text = result.title)
        Text(text = result.snippet, modifier = Modifier.padding(top = 6.dp))
    }
}
