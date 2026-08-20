package com.shahrafuking.kingassistant.data.model

data class SearchResult(
    val id: String,
    val title: String,
    val snippet: String = "",
    val url: String,
    val source: String = "backend",
    val fetchedAt: Long = System.currentTimeMillis()
)
