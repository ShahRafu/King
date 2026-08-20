package com.shahrafuking.kingassistant.ui.screens

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "king_settings")

enum class RaghuPreviewMode(val value: String) {
    EXTERNAL("external"),
    COMPACT("compact"),
    EXPANDED("expanded");

    companion object {
        fun fromValue(v: String?) = values().firstOrNull { it.value == v } ?: EXTERNAL
    }
}

object SettingsKeys {
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
    val APP_LOGO_URI = stringPreferencesKey("app_logo_uri")
    val ADV_BIO_EYE = booleanPreferencesKey("adv_bio_eye")
    val ADV_BIO_VOICE = booleanPreferencesKey("adv_bio_voice")
    val ADV_BIO_VIBRATION = booleanPreferencesKey("adv_bio_vibration")

    // new persistent language key
    val APP_LANGUAGE = stringPreferencesKey("app_language")
}

class SettingsRepository(private val ctx: Context) {
    val raghuPreviewModeFlow: Flow<RaghuPreviewMode> = ctx.dataStore.data
        .map { prefs -> RaghuPreviewMode.fromValue(prefs[SettingsKeys.RAGHU_PREVIEW_MODE]) }

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

    // NEW: persistent flow for app language
    val appLanguageFlow: Flow<String> = ctx.dataStore.data.map { it[SettingsKeys.APP_LANGUAGE] ?: "English" }

    suspend fun setRaghuPreviewMode(mode: RaghuPreviewMode) = ctx.dataStore.edit { it[SettingsKeys.RAGHU_PREVIEW_MODE] = mode.value }
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

    // NEW: setter for app language
    suspend fun setAppLanguage(v: String) = ctx.dataStore.edit { it[SettingsKeys.APP_LANGUAGE] = v }

    suspend fun setAdvBioEye(v: Boolean) = ctx.dataStore.edit { it[SettingsKeys.ADV_BIO_EYE] = v }
    suspend fun setAdvBioVoice(v: Boolean) = ctx.dataStore.edit { it[SettingsKeys.ADV_BIO_VOICE] = v }
    suspend fun setAdvBioVibration(v: Boolean) = ctx.dataStore.edit { it[SettingsKeys.ADV_BIO_VIBRATION] = v }
}
