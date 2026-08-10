package com.shahrafuking.kingassistant.ui.screens

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shahrafuking.kingassistant.system.BatteryHelper
import com.shahrafuking.kingassistant.core.BudgetManager
import com.shahrafuking.kingassistant.core.PanicManager

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

        // Budget row added for Part-3
        BudgetRow(prefs)

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Panic controls
        PanicRow()

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Spacer(modifier = Modifier.padding(6.dp))
        Row {
            Button(onClick = {
                BatteryHelper.requestIgnoreBatteryOptimization(ctx as android.app.Activity)
            }) {
                Text("Request Battery Exemption")
            }
            Spacer(modifier = Modifier.padding(8.dp))
            Button(onClick = { onClose() }) {
                Text("Close")
            }
        }
    }
}

@Composable
fun BudgetRow(prefs: SharedPreferences) {
    val ctx = LocalContext.current
    val existing = BudgetManager.getBudget(ctx)?.toString() ?: ""
    var value by remember { mutableStateOf(existing) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Voice Budget (USD)")
        Spacer(modifier = Modifier.padding(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.weight(1f),
                label = { Text("Enter amount e.g. 20") }
            )
            Spacer(modifier = Modifier.padding(6.dp))
            Button(onClick = {
                val v = value.replace(",", "").trim().toDoubleOrNull()
                if (v != null) {
                    BudgetManager.setBudget(ctx, v)
                    prefs.edit().putString("voice_budget_ui", v.toString()).apply()
                    value = v.toString()
                }
            }) {
                Text("Set")
            }
            Spacer(modifier = Modifier.padding(4.dp))
            Button(onClick = {
                BudgetManager.clearBudget(ctx)
                prefs.edit().remove("voice_budget_ui").apply()
                value = ""
            }) {
                Text("Clear")
            }
        }
    }
}

@Composable
fun PanicRow() {
    val ctx = LocalContext.current
    var engaged by remember { mutableStateOf(PanicManager.isEngaged(ctx)) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Panic Mode")
        Spacer(modifier = Modifier.padding(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(if (engaged) "Panic ENGAGED — trading disabled" else "Panic not engaged")
            Spacer(modifier = Modifier.weight(1f))
            if (!engaged) {
                Button(onClick = {
                    PanicManager.engage(ctx)
                    engaged = true
                }) {
                    Text("Engage")
                }
            } else {
                Button(onClick = {
                    PanicManager.release(ctx)
                    engaged = false
                }) {
                    Text("Release")
                }
            }
        }
    }
}

@Composable
fun SettingsToggleRow(title: String, subtitle: String, prefs: SharedPreferences, key: String) {
    var enabled by remember { mutableStateOf(prefs.getBoolean(key, false)) }
    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.caption)
        }
        Switch(checked = enabled, onCheckedChange = {
            enabled = it
            prefs.edit().putBoolean(key, it).apply()
        }, colors = SwitchDefaults.colors())
    }
}
