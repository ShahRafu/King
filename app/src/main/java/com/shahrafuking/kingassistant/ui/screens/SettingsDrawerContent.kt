package com.shahrafuking.kingassistant.ui.screens

import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Refactored SettingsDrawerContent:
 * - Cleaned header and removed unwanted visual bars
 * - Removed redundant theme/language/about blocks from the main list
 * - Provides vertically scrollable content (LazyColumn) so all options and Score view are reachable
 * - Adds a new 'App Language' selector as the 13th option (UI-only selector; persist as needed)
 */

@Composable
fun SettingsDrawerContent(
    currentMode: RaghuPreviewMode,
    onModeSelected: (RaghuPreviewMode) -> Unit
) {
    val ctx = LocalContext.current
    val settingsRepo = remember { SettingsRepository(ctx) }
    val scope = rememberCoroutineScope()

    // collect a few states used elsewhere
    val newFileStatus by settingsRepo.newFileStatusFlow.collectAsState(initial = true)
    val archiveAutoSave by settingsRepo.archiveAutoSaveFlow.collectAsState(initial = false)
    val apiKey by settingsRepo.apiKeyFlow.collectAsState(initial = "")
    val permissionAutoFix by settingsRepo.permissionAutoFixFlow.collectAsState(initial = false)
    val recoverySync by settingsRepo.recoverySyncFlow.collectAsState(initial = false)
    val appLogoUri by settingsRepo.appLogoUriFlow.collectAsState(initial = "")

    // advanced biometric states
    val advBioEye by settingsRepo.advBioEyeFlow.collectAsState(initial = false)
    val advBioVoice by settingsRepo.advBioVoiceFlow.collectAsState(initial = false)
    val advBioVibration by settingsRepo.advBioVibrationFlow.collectAsState(initial = false)

    // Image picker launcher (system)
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch { settingsRepo.setAppLogoUri(uri.toString()) }
            Toast.makeText(ctx, "Custom app icon selected (preview saved)", Toast.LENGTH_SHORT).show()
        }
    }

    // Local UI state for the new App Language selector (persist via SettingsRepository if you add a key)
    var selectedLanguage by remember { mutableStateOf("English") }
    var languageExpanded by remember { mutableStateOf(false) }
    val languages = listOf("English", "বাংলা (Bangla)", "Español", "Français", "中文")

    // Compact list of settings options (cleaned)
    val options = listOf(
        "Profile",
        "Account",
        "Notifications",
        "Privacy",
        "Shortcuts",
        "Storage",
        "Help & Feedback",
        "API Keys & Secrets",
        "Network & Rotation",
        "Permissions & System Health",
        "Emergency Data Recovery",
        "Advanced Biometric Security",
        "App Language" // 13th option
    )

    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .padding(vertical = 8.dp)) {

        item {
            // Minimal top spacing (no large header label)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Raghu preview mode radios (kept but compact)
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text(text = "Appearance", style = MaterialTheme.typography.subtitle1)
                Spacer(modifier = Modifier.height(6.dp))
                for (mode in RaghuPreviewMode.values()) {
                    RowOption(mode = mode, selected = mode == currentMode, onSelect = { onModeSelected(mode) })
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }

        // Main options list
        itemsIndexed(options) { index, option ->
            // Special UI for some options
            when (option) {
                "App Language" -> {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(text = option)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box {
                            OutlinedButton(onClick = { languageExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(selectedLanguage)
                            }
                            DropdownMenu(expanded = languageExpanded, onDismissRequest = { languageExpanded = false }) {
                                languages.forEach { lang ->
                                    DropdownMenuItem(onClick = {
                                        selectedLanguage = lang
                                        languageExpanded = false
                                        // TODO: persist selection to SettingsRepository (add key + setter in SettingsModel)
                                        Toast.makeText(ctx, "Language set to $lang (UI-only)", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Text(lang)
                                    }
                                }
                            }
                        }
                    }
                }

                "Advanced Biometric Security" -> {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(option)
                        Spacer(modifier = Modifier.height(6.dp))
                        SettingSwitchRow(
                            title = "Eye Scan (Iris)",
                            checked = advBioEye,
                            onToggle = { v -> scope.launch { settingsRepo.setAdvBioEye(v) } }
                        )
                        SettingSwitchRow(
                            title = "Voice Recognition",
                            checked = advBioVoice,
                            onToggle = { v -> scope.launch { settingsRepo.setAdvBioVoice(v) } }
                        )
                        SettingSwitchRow(
                            title = "Voice Vibration / Frequency",
                            checked = advBioVibration,
                            onToggle = { v -> scope.launch { settingsRepo.setAdvBioVibration(v) } }
                        )
                    }
                }

                "API Keys & Secrets" -> {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* open manager */ }
                        .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(option)
                            Text(if (apiKey.isBlank()) "No key saved" else "Key saved", style = MaterialTheme.typography.caption)
                        }
                        OutlinedButton(onClick = { /* manage */ }) {
                            Text("Manage")
                        }
                    }
                }

                "Permissions & System Health" -> {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)) {
                        SettingSwitchRow(title = "Auto-fix permissions & health", checked = permissionAutoFix, onToggle = { v -> scope.launch { settingsRepo.setPermissionAutoFix(v) } })
                    }
                }

                "Emergency Data Recovery" -> {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)) {
                        SettingSwitchRow(title = "Cloud sync & recovery", checked = recoverySync, onToggle = { v -> scope.launch { settingsRepo.setRecoverySync(v) } })
                    }
                }

                else -> {
                    // generic clickable row for remaining options
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* navigate to option */ }
                        .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(option)
                    }
                }
            }

            Divider()
        }

        // App logo customizer snippet (kept compact)
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("App Icon", style = MaterialTheme.typography.subtitle1)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (appLogoUri.isNotBlank()) {
                        AndroidView(factory = { ctxInner ->
                            ImageView(ctxInner).apply {
                                scaleType = ImageView.ScaleType.CENTER_CROP
                                setImageURI(Uri.parse(appLogoUri))
                            }
                        }, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Custom icon selected")
                            Text(appLogoUri, style = MaterialTheme.typography.caption, maxLines = 1)
                        }
                    } else {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("No custom icon selected")
                            Text("Select from gallery", style = MaterialTheme.typography.caption)
                        }
                    }

                    Column {
                        OutlinedButton(onClick = { imagePicker.launch("image/*") }) { Text("Select") }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(onClick = { scope.launch { settingsRepo.setAppLogoUri("") ; Toast.makeText(ctx, "Cleared", Toast.LENGTH_SHORT).show() } }) { Text("Reset") }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Score view at bottom so it's reachable after scrolling
        item {
            Card(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), shape = RoundedCornerShape(8.dp), elevation = 2.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Score", style = MaterialTheme.typography.subtitle1)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1234", style = MaterialTheme.typography.h4)
                }
            }
        }

        // Save & close button at the very bottom
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Button(onClick = { Toast.makeText(ctx, "Settings saved", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Text("Save & Close")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// Keeping helper composables unchanged

@Composable
private fun SettingSwitchRow(title: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title)
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
private fun RowOption(mode: RaghuPreviewMode, selected: Boolean, onSelect: () -> Unit) {
    Row(modifier = Modifier
        .padding(vertical = 8.dp)
        .clickable { onSelect() }) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(modifier = Modifier.width(8.dp))
        Text(mode.name, modifier = Modifier.padding(start = 8.dp))
    }
}
