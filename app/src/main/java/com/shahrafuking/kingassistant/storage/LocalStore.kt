package com.shahrafuking.kingassistant.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class LocalStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPref = EncryptedSharedPreferences.create(
        context,
        "king_assistant_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveApiKey(keyName: String, keyValue: String) {
        sharedPref.edit().putString(keyName, keyValue).apply()
    }

    fun getApiKey(keyName: String): String? {
        return sharedPref.getString(keyName, null)
    }

    fun saveUserPreference(key: String, value: String) {
        sharedPref.edit().putString(key, value).apply()
    }

    fun getUserPreference(key: String): String? {
        return sharedPref.getString(key, null)
    }

    fun saveMemory(title: String, text: String) {
        // simple memory save: prefix 'mem_' to key with timestamp
        val key = "mem_" + System.currentTimeMillis().toString()
        sharedPref.edit().putString(key, "$title|$text").apply()
    }
}
