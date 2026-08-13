package com.shahrafuking.kingassistant.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.shahrafuking.kingassistant.location.LocationPoint

@Composable
fun LocationPreviewScreen(points: List<LocationPoint>) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Location Preview (latest first)")
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(points.reversed()) { p ->
                Text(Gson().toJson(p))
            }
        }
    }
}
