package com.shahrafuking.kingassistant.data.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_results")
data class SearchResultEntity(
    @PrimaryKey val url: String,
    val title: String,
    val snippet: String,
    val source: String,
    val fetchedAt: Long
)
