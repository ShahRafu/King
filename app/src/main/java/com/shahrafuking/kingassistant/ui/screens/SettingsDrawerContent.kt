package com.shahrafuking.kingassistant.ui.screens

import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch

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
    val marketingNotepad by settingsRepo.marketingNotepadFlow.collectAsState(initial = false)
    val personalNotepad by settingsRepo.personalNotepadFlow.collectAsState(initial = false)
    val appBrandTheme by settingsRepo.appBrandThemeFlow.collectAsState(initial = "Default")
    val voiceCalibLevel by settingsRepo.voiceCalibFlow.collectAsState(initial = 50)
    val apiKey by settingsRepo.apiKeyFlow.collectAsState(initial = "")
    val networkRotation by settingsRepo.networkRotationFlow.collectAsState(initial = "Off")
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

    Column(modifier = Modifier
        .padding(16.dp)
        .fillMaxWidth()) {

        Text("Settings", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))

        // Raghu preview modes (existing)
        Text("Appearance / রঘু প্রদর্শন", style = MaterialTheme.typography.subtitle1)
        Spacer(modifier = Modifier.height(4.dp))
        for (mode in RaghuPreviewMode.values()) {
            RowOption(mode = mode, selected = mode == currentMode, onSelect = { onModeSelected(mode) })
        }
        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // 1. New file / plugin status
        SettingSwitchRow(
            title = "New file & plugin status (আপডেট চেক)",
            checked = newFileStatus,
            onToggle = { v -> scope.launch { settingsRepo.setNewFileStatus(v) } }
        )

        // 2. Archive autosave
        SettingSwitchRow(
            title = "স্থায়ী স্মৃতি & সাপ্তাহিক আর্কাইভ (Auto-save)",
            checked = archiveAutoSave,
            onToggle = { v -> scope.launch { settingsRepo.setArchiveAutoSave(v) } }
        )

        // 3. Marketing & trade notepad
        SettingSwitchRow(
            title = "Marketing / Trade Memory notepad",
            checked = marketingNotepad,
            onToggle = { v -> scope.launch { settingsRepo.setMarketingNotepad(v) } }
        )

        // 4. Personal all-media notepad
        SettingSwitchRow(
            title = "Personal all-media notepad (Photos/Videos)",
            checked = personalNotepad,
            onToggle = { v -> scope.launch { settingsRepo.setPersonalNotepad(v) } }
        )

        // 5. App logo & theme (existing button kept)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("App logo & Brand theme")
                Text(appBrandTheme, style = MaterialTheme.typography.caption)
            }
            OutlinedButton(onClick = {
                Toast.makeText(ctx, "Open theme / icon picker (placeholder)", Toast.LENGTH_SHORT).show()
            }) {
                Text("Customize")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // 6. Voice-print & biometric calibration
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Voice-print & Biometric calibration")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Calibration: $voiceCalibLevel", modifier = Modifier.weight(1f))
                Button(onClick = {
                    val next = (voiceCalibLevel + 10).coerceAtMost(100)
                    scope.launch { settingsRepo.setVoiceCalibLevel(next) }
                }) { Text("Calibrate") }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // 7. API key & secret manager
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("API key & Secret manager")
                Text(if (apiKey.isBlank()) "No key saved" else "Key saved", style = MaterialTheme.typography.caption)
            }
            OutlinedButton(onClick = {
                Toast.makeText(ctx, "Open API Key manager (placeholder)", Toast.LENGTH_SHORT).show()
            }) { Text("Manage") }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // 8. Network & IP rotation control
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Network & IP rotation")
                Text(networkRotation, style = MaterialTheme.typography.caption)
            }
            OutlinedButton(onClick = {
                val next = if (networkRotation == "Off") "VPN Rotation" else "Off"
                scope.launch { settingsRepo.setNetworkRotation(next) }
            }) { Text("Configure") }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // 9. Permissions & system health controller
        SettingSwitchRow(
            title = "Permissions & System Health (Auto-fix)",
            checked = permissionAutoFix,
            onToggle = { v -> scope.launch { settingsRepo.setPermissionAutoFix(v) } }
        )

        // 10. Emergency data recovery & cloud sync
        SettingSwitchRow(
            title = "Emergency Data Recovery & Cloud Sync",
            checked = recoverySync,
            onToggle = { v -> scope.launch { settingsRepo.setRecoverySync(v) } }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 11. App Logo & Launcher Icon Customizer
        Text("App Logo & Launcher Icon Customizer", style = MaterialTheme.typography.subtitle1)
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (appLogoUri.isNotBlank()) {
                AndroidView(factory = { ctxInner ->
                    ImageView(ctxInner).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setImageURI(Uri.parse(appLogoUri))
                    }
                }, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Custom icon selected")
                    Text(appLogoUri, style = MaterialTheme.typography.caption, maxLines = 1)
                }
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text("No custom icon selected")
                    Text("Select from gallery or file manager", style = MaterialTheme.typography.caption)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                OutlinedButton(onClick = { imagePicker.launch("image/*") }) {
                    Text("Select Image")
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(onClick = {
                    scope.launch {
                        settingsRepo.setAppLogoUri("")
                        Toast.makeText(ctx, "Custom icon cleared", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Reset")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Divider()
        Spacer(modifier = Modifier.height(12.dp))

        // 12. Advanced Biometric Security
        Text("Advanced Biometric Security", style = MaterialTheme.typography.subtitle1)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Manage advanced biometrics: Eye (Iris) scan, Voice biometric, and Voice vibration/frequency checks.",
            style = MaterialTheme.typography.caption
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 12.1 Eye Scan (Iris) setup & calibration
        SettingSwitchRow(
            title = "Eye Scan (Iris) — setup & calibration",
            checked = advBioEye,
            onToggle = { v -> scope.launch { settingsRepo.setAdvBioEye(v) } }
        )
        if (advBioEye) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Iris calibration", modifier = Modifier.weight(1f))
                OutlinedButton(onClick = {
                    Toast.makeText(ctx, "Start Iris setup (placeholder)", Toast.LENGTH_SHORT).show()
                }) { Text("Setup") }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 12.2 Voice Recognition (biometric)
        SettingSwitchRow(
            title = "Voice Recognition (biometric)",
            checked = advBioVoice,
            onToggle = { v -> scope.launch { settingsRepo.setAdvBioVoice(v) } }
        )
        if (advBioVoice) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Voice enrollment", modifier = Modifier.weight(1f))
                OutlinedButton(onClick = {
                    Toast.makeText(ctx, "Start Voice enrollment (placeholder)", Toast.LENGTH_SHORT).show()
                }) { Text("Enroll") }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 12.3 Voice Vibration / Frequency verification
        SettingSwitchRow(
            title = "Voice Vibration / Frequency verification",
            checked = advBioVibration,
            onToggle = { v -> scope.launch { settingsRepo.setAdvBioVibration(v) } }
        )
        if (advBioVibration) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Vibration/frequency scan", modifier = Modifier.weight(1f))
                OutlinedButton(onClick = {
                    Toast.makeText(ctx, "Start frequency test (placeholder)", Toast.LENGTH_SHORT).show()
                }) { Text("Test") }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // small CTA
        Button(
            onClick = {
                Toast.makeText(ctx, "Settings saved locally", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Save & Close")
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
