package com.shahrafuking.kingassistant.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// DataStore instance
private val Context.dataStore by preferencesDataStore(name = "king_settings")

object SettingsKeys {
    // Raghu preview mode (used by KingHomeScreen and VoiceOrb)
    val RAGHU_PREVIEW_MODE = stringPreferencesKey("raghu_preview_mode")

    val NEW_FILE_STATUS = booleanPreferencesKey("new_file_status")
    val ARCHIVE_AUTOSAVE = booleanPreferencesKey("archive_autosave")
    val MARKETING_NOTEPAD = booleanPreferencesKey("marketing_notepad")
    val PERSONAL_NOTEPAD = booleanPreferencesKey("personal_notepad")
    val APP_BRAND_THEME = stringPreferencesKey("app_brand_theme")
    val VOICE_CALIB_LEVEL = intPreferencesKey("voice_calib_level")
    val API_KEY_STORE = stringPreferencesKey("api_key_store")
    val NETWORK_ROTATION = stringPreferencesKey("network_rotation")
    val PERMISSION_AUTOFIX = booleanPreferencesKey("permission_autofix")
    val RECOVERY_SYNC = booleanPreferencesKey("recovery_sync")
    // 11. App Logo Uri key
    val APP_LOGO_URI = stringPreferencesKey("app_logo_uri")
    // 12. Advanced Biometric Security keys
    val ADV_BIO_EYE = booleanPreferencesKey("adv_bio_eye")
    val ADV_BIO_VOICE = booleanPreferencesKey("adv_bio_voice")
    val ADV_BIO_VIBRATION = booleanPreferencesKey("adv_bio_vibration")
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
    // Raghu preview flow
    val raghuPreviewModeFlow: Flow<RaghuPreviewMode> = ctx.dataStore.data.map { prefs ->
        RaghuPreviewMode.fromValue(prefs[SettingsKeys.RAGHU_PREVIEW_MODE])
    }

    val newFileStatusFlow: Flow<Boolean> = ctx.dataStore.data.map { it[SettingsKeys.NEW_FILE_STATUS] ?: true }
    val archiveAutoSaveFlow: Flow<Boolean> = ctx.dataStore.data.map { it[SettingsKeys.ARCHIVE_AUTOSAVE] ?: false }
    val marketingNotepadFlow: Flow<Boolean> = ctx.dataStore.data.map { it[SettingsKeys.MARKETING_NOTEPAD] ?: false }
    val personalNotepadFlow: Flow<Boolean> = ctx.dataStore.data.map { it[SettingsKeys.PERSONAL_NOTEPAD] ?: false }
    val appBrandThemeFlow: Flow<String> = ctx.dataStore.data.map { it[SettingsKeys.APP_BRAND_THEME] ?: "Default" }
    val voiceCalibFlow: Flow<Int> = ctx.dataStore.data.map { it[SettingsKeys.VOICE_CALIB_LEVEL] ?: 50 }
    val apiKeyFlow: Flow<String> = ctx.dataStore.data.map { it[SettingsKeys.API_KEY_STORE] ?: "" }
    val networkRotationFlow: Flow<String> = ctx.dataStore.data.map { it[SettingsKeys.NETWORK_ROTATION] ?: "Off" }
    val permissionAutoFixFlow: Flow<Boolean> = ctx.dataStore.data.map { it[SettingsKeys.PERMISSION_AUTOFIX] ?: false }
    val recoverySyncFlow: Flow<Boolean> = ctx.dataStore.data.map { it[SettingsKeys.RECOVERY_SYNC] ?: false }
    val appLogoUriFlow: Flow<String> = ctx.dataStore.data.map { it[SettingsKeys.APP_LOGO_URI] ?: "" }

    // Advanced biometric flows
    val advBioEyeFlow: Flow<Boolean> = ctx.dataStore.data.map { it[SettingsKeys.ADV_BIO_EYE] ?: false }
    val advBioVoiceFlow: Flow<Boolean> = ctx.dataStore.data.map { it[SettingsKeys.ADV_BIO_VOICE] ?: false }
    val advBioVibrationFlow: Flow<Boolean> = ctx.dataStore.data.map { it[SettingsKeys.ADV_BIO_VIBRATION] ?: false }

    suspend fun setRaghuPreviewMode(mode: RaghuPreviewMode) {
        ctx.dataStore.edit { prefs -> prefs[SettingsKeys.RAGHU_PREVIEW_MODE] = mode.value }
    }

    suspend fun setNewFileStatus(v: Boolean) = ctx.dataStore.edit { it[SettingsKeys.NEW_FILE_STATUS] = v }
    suspend fun setArchiveAutoSave(v: Boolean) = ctx.dataStore.edit { it[SettingsKeys.ARCHIVE_AUTOSAVE] = v }
    suspend fun setMarketingNotepad(v: Boolean) = ctx.dataStore.edit { it[SettingsKeys.MARKETING_NOTEPAD] = v }
    suspend fun setPersonalNotepad(v: Boolean) = ctx.dataStore.edit { it[SettingsKeys.PERSONAL_NOTEPAD] = v }
    suspend fun setAppBrandTheme(v: String) = ctx.dataStore.edit { it[SettingsKeys.APP_BRAND_THEME] = v }
    suspend fun setVoiceCalibLevel(v: Int) = ctx.dataStore.edit { it[SettingsKeys.VOICE_CALIB_LEVEL] = v }
    suspend fun setApiKey(v: String) = ctx.dataStore.edit { it[SettingsKeys.API_KEY_STORE] = v }
    suspend fun setNetworkRotation(v: String) = ctx.dataStore.edit { it[SettingsKeys.NETWORK_ROTATION] = v }
    suspend fun setPermissionAutoFix(v: Boolean) = ctx.dataStore.edit { it[SettingsKeys.PERMISSION_AUTOFIX] = v }
    suspend fun setRecoverySync(v: Boolean) = ctx.dataStore.edit { it[SettingsKeys.RECOVERY_SYNC] = v }
    suspend fun setAppLogoUri(v: String) = ctx.dataStore.edit { it[SettingsKeys.APP_LOGO_URI] = v }

    // Advanced biometric setters
    suspend fun setAdvBioEye(v: Boolean) = ctx.dataStore.edit { it[SettingsKeys.ADV_BIO_EYE] = v }
    suspend fun setAdvBioVoice(v: Boolean) = ctx.dataStore.edit { it[SettingsKeys.ADV_BIO_VOICE] = v }
    suspend fun setAdvBioVibration(v: Boolean) = ctx.dataStore.edit { it[SettingsKeys.ADV_BIO_VIBRATION] = v }
}

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

    Column(modifier = Modifier
        .padding(16.dp)
        .fillMaxWidth()) {

        Text("Settings", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))

        // Raghu preview modes (existing)
        Text("Appearance / রঘু প্রদর্শন", style = MaterialTheme.typography.subtitle1)
        Spacer(modifier = Modifier.height(4.dp))
        RaghuPreviewMode.values().forEach { mode ->
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

        // 5. App logo & theme (button to change / choose)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("App logo & Brand theme")
                Text(appBrandTheme, style = MaterialTheme.typography.caption)
            }
            OutlinedButton(onClick = {
                // placeholder: show toast or open theme picker screen
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
                    // placeholder: increment calibration for demo
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
                // placeholder: open API key manager screen
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
                // placeholder: toggle simple rotation option for demo
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

        // 11. App Logo & Launcher Icon Customizer (existing)
        Text("App Logo & Launcher Icon Customizer", style = MaterialTheme.typography.subtitle1)
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (appLogoUri.isNotBlank()) {
                Text("Custom icon selected", modifier = Modifier.weight(1f))
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text("No custom icon selected")
                    Text("Select from gallery or file manager", style = MaterialTheme.typography.caption)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                OutlinedButton(onClick = { /* image picker handled elsewhere */ }) {
                    Text("Select Image")
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(onClick = {
                    scope.launch { settingsRepo.setAppLogoUri("") }
                    Toast.makeText(ctx, "Custom icon cleared", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Reset")
                }
            }
        }

... (truncated on display)