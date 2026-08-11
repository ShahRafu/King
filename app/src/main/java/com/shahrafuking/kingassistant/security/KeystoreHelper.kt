package com.shahrafuking.kingassistant.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Base64
import android.util.Log
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeystoreHelper {
    private const val TAG = "KeystoreHelper"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "king_assistant_aes_key_v1"
    private const val PREFS_NAME = "king_keystore_prefs"
    private const val PREF_ENC_PREFIX = "enc_"

    private fun getSharedPrefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun ensureKeyExists(): SecretKey? {
        try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
            ks.load(null)
            if (ks.containsAlias(KEY_ALIAS)) {
                val entry = ks.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
                return entry.secretKey
            }
            // generate
            val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
            val specBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.security.keystore.KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
            } else {
                null
            }
            specBuilder?.let {
                keyGenerator.init(it.build())
                return keyGenerator.generateKey()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "ensureKeyExists error", t)
        }
        return null
    }

    fun encryptString(ctx: Context, plain: String, keyNameSuffix: String = ""): Boolean {
        try {
            val secretKey = ensureKeyExists() ?: return false
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv // 12 bytes GCM IV
            val ciphertext = cipher.doFinal(plain.toByteArray(StandardCharsets.UTF_8))
            // store iv + ciphertext Base64
            val combined = ByteArray(iv.size + ciphertext.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
            val b64 = Base64.encodeToString(combined, Base64.NO_WRAP)
            getSharedPrefs(ctx).edit().putString(PREF_ENC_PREFIX + keyNameSuffix, b64).apply()
            return true
        } catch (t: Throwable) {
            Log.w(TAG, "encryptString error", t)
            return false
        }
    }

    fun decryptString(ctx: Context, keyNameSuffix: String = ""): String? {
        try {
            val b64 = getSharedPrefs(ctx).getString(PREF_ENC_PREFIX + keyNameSuffix, null) ?: return null
            val combined = Base64.decode(b64, Base64.NO_WRAP)
            if (combined.size < 12) return null
            val iv = combined.copyOfRange(0, 12)
            val ciphertext = combined.copyOfRange(12, combined.size)
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
            ks.load(null)
            if (!ks.containsAlias(KEY_ALIAS)) return null
            val entry = ks.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            val secretKey = entry.secretKey
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val plain = cipher.doFinal(ciphertext)
            return String(plain, StandardCharsets.UTF_8)
        } catch (t: Throwable) {
            Log.w(TAG, "decryptString error", t)
            return null
        }
    }

    fun clear(ctx: Context, keyNameSuffix: String = ""): Boolean {
        return try {
            getSharedPrefs(ctx).edit().remove(PREF_ENC_PREFIX + keyNameSuffix).apply()
            true
        } catch (t: Throwable) {
            Log.w(TAG, "clear error", t)
            false
        }
    }
}
