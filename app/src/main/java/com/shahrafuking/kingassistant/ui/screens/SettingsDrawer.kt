package com.shahrafuking.kingassistant.ui.screens

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shahrafuking.kingassistant.system.BatteryHelper

@Composable
fun SettingsDrawerScreen(prefs: SharedPreferences, onClose: () -> Unit) {
    val ctx = LocalContext.current
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.padding(6.dp))

        SettingsToggleRow("Plugin Auto‑loader", "Enable plugin auto‑load from plugins folder", prefs, "plugin_autoload")
        Divider(modifier = Modifier.padding(vertical = 8.dp))

        SettingsToggleRow("Permanent Memory (Sync)", "Auto sync memory weekly", prefs, "memory_sync")
        SettingsToggleRow("Market Scan Priority", "Prioritize current server/market scans", prefs, "market_priority")
        SettingsToggleRow("Voice‑print Sensitivity", "Adjust voice biometric sensitivity (coarse)", prefs, "voice_sensitivity")
        SettingsToggleRow("Auto‑trade Mode", "Enable background auto‑trade (requires confirmation)", prefs, "autotrade_enabled")
        SettingsToggleRow("Panic Voice Enabled", "Allow panic voice commands", prefs, "panic_enabled")
        SettingsToggleRow("IP Rotation (proxy)", "Enable proxy management (requires server)", prefs, "ip_rotation")
        SettingsToggleRow("Data Backup", "Weekly cloud backup of settings & memory", prefs, "backup_enabled")
        SettingsToggleRow("Privacy Mode", "Minimal logs & local processing only", prefs, "privacy_mode")
        SettingsToggleRow("Accessibility Auto‑Click", "Allow auto-click for Quotex (use with caution)", prefs, "accessibility_autoclick")

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Location module settings
        Text("Location Module", style = MaterialTheme.typography.subtitle1)
        Spacer(modifier = Modifier.height(8.dp))

        SettingsToggleRow("Simulate Location", "Use in‑app simulated locations (no system spoofing)", prefs, "simulate_location")

        var pointCountText by remember { mutableStateOf(prefs.getInt("location_point_count", 7).toString()) }
        OutlinedTextField(value = pointCountText, onValueChange = {
            pointCountText = it
            val v = it.toIntOrNull() ?: 7
            prefs.edit().putInt("location_point_count", v.coerceIn(5,15)).apply()
        }, label = { Text("Points per session (5-15)") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(8.dp))

        var intervalText by remember { mutableStateOf(prefs.getLong("location_interval_ms", 15000L).toString()) }
        OutlinedTextField(value = intervalText, onValueChange = {
            intervalText = it
            val v = it.toLongOrNull() ?: 15000L
            prefs.edit().putLong("location_interval_ms", v.coerceAtLeast(1000L)).apply()
        }, label = { Text("Interval ms (>=1000)") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(8.dp))

        SettingsToggleRow("Persist Location Logs", "Store session logs locally (opt‑in)", prefs, "persist_location_logs")

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = {
                BatteryHelper.requestIgnoreBatteryOptimization(ctx as android.app.Activity)
            }) {
                Text("Request Battery Exemption")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onClose() }) {
                Text("Close")
            }
        }
    }
}

@Composable
fun SettingsToggleRow(title: String, subtitle: String, prefs: SharedPreferences, key: String) {
    var enabled by remember { mutableStateOf(prefs.getBoolean(key, false)) }
    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.caption)
        }
        Switch(checked = enabled, onCheckedChange = {
            enabled = it
            prefs.edit().putBoolean(key, it).apply()
        }, colors = SwitchDefaults.colors())
    }
}
