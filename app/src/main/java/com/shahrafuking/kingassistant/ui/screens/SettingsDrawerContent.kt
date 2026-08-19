package com.shahrafuking.kingassistant.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Simple settings storage using DataStore Preferences
private val Context.dataStore by preferencesDataStore(name = "king_settings")

object SettingsKeys {
    val RAGHU_PREVIEW_MODE = stringPreferencesKey("raghu_preview_mode")
}

enum class RaghuPreviewMode(val value: String) {
    EXTERNAL("external"),
    COMPACT("compact"),
    EXPANDED("expanded");

    companion object {
        fun fromValue(v: String?) = values().firstOrNull { it.value == v } ?: EXTERNAL
    }
}

class SettingsRepository(private val ctx: Context) {
    val raghuPreviewModeFlow: Flow<RaghuPreviewMode> = ctx.dataStore.data
        .map { prefs -> RaghuPreviewMode.fromValue(prefs[SettingsKeys.RAGHU_PREVIEW_MODE]) }

    suspend fun setRaghuPreviewMode(mode: RaghuPreviewMode) {
        ctx.dataStore.edit { prefs ->
            prefs[SettingsKeys.RAGHU_PREVIEW_MODE] = mode.value
        }
    }
}

@Composable
fun SettingsDrawerContent(currentMode: RaghuPreviewMode, onModeSelected: (RaghuPreviewMode) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Appearance / রঘু প্রদর্শন", style = MaterialTheme.typography.h6)
        SpacerSmall()
        RaghuPreviewMode.values().forEach { mode ->
            RowOption(mode = mode, selected = mode == currentMode, onSelect = { onModeSelected(mode) })
        }
    }
}

@Composable
private fun RowOption(mode: RaghuPreviewMode, selected: Boolean, onSelect: () -> Unit) {
    Row(modifier = Modifier
        .padding(vertical = 8.dp)
        .clickable { onSelect() }) {
        RadioButton(selected = selected, onClick = onSelect)
        SpacerSmall()
        Text(mode.name, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun SpacerSmall() = androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(4.dp))
