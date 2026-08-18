package com.shahrafuking.kingassistant.ui.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shahrafuking.kingassistant.system.BatteryHelper
import com.shahrafuking.kingassistant.ui.screens.LogoPickerScreen

@Composable
fun SettingsDrawerScreen(prefs: SharedPreferences, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var showSecrets by remember { mutableStateOf(false) }
    var showPermissions by remember { mutableStateOf(false) }
    var showPlugins by remember { mutableStateOf(false) }
    var showLogoPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(6.dp))

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

        // Quick action buttons that open dialogs
        OutlinedButton(onClick = { showPlugins = true }) {
            Text("Manage Plugins")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = { showSecrets = true }) {
            Text("Manage API Keys")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = { showPermissions = true }) {
            Text("Permission Check")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = { showLogoPicker = true }) {
            Text("App Logo & Theme")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row {
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

    // Dialogs (modal) wired to the new screens
    if (showSecrets) {
        AlertDialog(
            onDismissRequest = { showSecrets = false },
            title = { Text("Manage API Keys") },
            text = {
                // Compose your SecretsScreen content here or call a small composable
                Column { SecretsScreen(onClose = { showSecrets = false }) }
            },
            confirmButton = {
                Button(onClick = { showSecrets = false }) { Text("Done") }
            }
        )
    }

    if (showPermissions) {
        AlertDialog(
            onDismissRequest = { showPermissions = false },
            title = { Text("Permission Check") },
            text = {
                Column { PermissionCheckerScreen(onClose = { showPermissions = false }) }
            },
            confirmButton = {
                Button(onClick = { showPermissions = false }) { Text("Done") }
            }
        )
    }

    if (showPlugins) {
        AlertDialog(
            onDismissRequest = { showPlugins = false },
            title = { Text("Plugins (v1)") },
            text = {
                // Minimal plugin info UI: list file names and enabled flags
                val pm = com.shahrafuking.kingassistant.plugin.PluginManager(LocalContext.current)
                val list = pm.listPlugins()
                Column {
                    if (list.isEmpty()) {
                        Text("No plugins installed. Place plugin files into the app's filesDir/plugins.")
                    } else {
                        for (p in list) {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(p.id, modifier = Modifier.weight(1f))
                                Text(if (p.enabled) "ENABLED" else "DISABLED")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showPlugins = false }) { Text("Close") }
            }
        )
    }

    if (showLogoPicker) {
        AlertDialog(
            onDismissRequest = { showLogoPicker = false },
            title = { Text("App Logo & Theme") },
            text = {
                Column { LogoPickerScreen(LocalContext.current, onClose = { showLogoPicker = false }) }
            },
            confirmButton = {
                Button(onClick = { showLogoPicker = false }) { Text("Done") }
            }
        )
    }
}

@Composable
fun SettingsToggleRow(title: String, subtitle: String, prefs: SharedPreferences, key: String) {
    var enabled by remember { mutableStateOf(prefs.getBoolean(key, false)) }
    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth(0.75f)) {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.caption)
        }
        Switch(checked = enabled, onCheckedChange = {
            enabled = it
            prefs.edit().putBoolean(key, it).apply()
        }, colors = SwitchDefaults.colors())
    }
}

/**
 * Convenience no-arg drawer used by MainActivity scaffold.
 */
@Composable
fun SettingsDrawer() {
    val ctx = LocalContext.current
    val prefs: SharedPreferences = ctx.getSharedPreferences("king_prefs", Context.MODE_PRIVATE)
    SettingsDrawerScreen(prefs = prefs, onClose = { /* scaffold will handle closing */ })
}
