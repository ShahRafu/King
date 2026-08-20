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
 * - Persist App Language selection via SettingsRepository
 * - Cleaned header and removed unwanted visual bars
 * - Provides vertically scrollable content (LazyColumn)
 */

@Composable
fun SettingsDrawerContent(
    currentMode: RaghuPreviewMode,
    onModeSelected: (RaghuPreviewMode) -> Unit
) {
    val ctx = LocalContext.current
    val settingsRepo = remember { SettingsRepository(ctx) }
    val scope = rememberCoroutineScope()

    // collect states
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

    // persisted language selection
    val selectedLanguage by settingsRepo.appLanguageFlow.collectAsState(initial = "English")
    var languageExpanded by remember { mutableStateOf(false) }
    val languages = listOf("English", "বাংলা (Bangla)", "Español", "Français", "中文")

    // Image picker launcher (system)
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch { settingsRepo.setAppLogoUri(uri.toString()) }
            Toast.makeText(ctx, "Custom app icon selected (preview saved)", Toast.LENGTH_SHORT).show()
        }
    }

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
        "App Language"
    )

    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .padding(vertical = 8.dp)) {

        item { Spacer(modifier = Modifier.height(8.dp)) }

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

        itemsIndexed(options) { index, option ->
            when (option) {
                "App Language" -> {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(text = option)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box {
                            OutlinedButton(onClick = { languageExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(text = selectedLanguage)
                            }
                            DropdownMenu(expanded = languageExpanded, onDismissRequest = { languageExpanded = false }) {
                                languages.forEach { lang ->
                                    DropdownMenuItem(onClick = {
                                        scope.launch { settingsRepo.setAppLanguage(lang) }
                                        languageExpanded = false
                                        Toast.makeText(ctx, "Language set to $lang", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Text(text = lang)
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
                        Text(text = option)
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
                            Text(text = option)
                            Text(text = if (apiKey.isBlank()) "No key saved" else "Key saved", style = MaterialTheme.typography.caption)
                        }
                        OutlinedButton(onClick = { /* manage */ }) {
                            Text(text = "Manage")
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
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* navigate to option */ }
                        .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = option)
                    }
                }
            }

            Divider()
        }

        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "App Icon", style = MaterialTheme.typography.subtitle1)
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
                            Text(text = "Custom icon selected")
                            Text(text = appLogoUri, style = MaterialTheme.typography.caption, maxLines = 1)
                        }
                    } else {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "No custom icon selected")
                            Text(text = "Select from gallery", style = MaterialTheme.typography.caption)
                        }
                    }

                    Column {
                        OutlinedButton(onClick = { imagePicker.launch("image/*") }) { Text(text = "Select") }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(onClick = { scope.launch { settingsRepo.setAppLogoUri("") ; Toast.makeText(ctx, "Cleared", Toast.LENGTH_SHORT).show() } }) { Text(text = "Reset") }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        item {
            Card(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), shape = RoundedCornerShape(8.dp), elevation = 2.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Score", style = MaterialTheme.typography.subtitle1)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "1234", style = MaterialTheme.typography.h4)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Button(onClick = { Toast.makeText(ctx, "Settings saved", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Text(text = "Save & Close")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(title: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title)
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
        Text(text = mode.name, modifier = Modifier.padding(start = 8.dp))
    }
}
