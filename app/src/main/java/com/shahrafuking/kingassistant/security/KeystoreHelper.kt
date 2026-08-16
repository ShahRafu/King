package com.shahrafuking.kingassistant.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeystoreHelper {
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val PREFS_NAME = "king_keystore_prefs"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getOrCreateKey(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        keyStore.load(null)

        val existing = keyStore.getEntry(alias, null)
        if (existing is KeyStore.SecretKeyEntry) {
            return existing.secretKey
        }

        // Generate AES key in AndroidKeyStore
        val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEY_STORE)
        val builder = android.security.keystore.KeyGenParameterSpec.Builder(
            alias,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)

        // Do not require user authentication here; for higher security you may require it.
        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    /**
     * Primary encrypt/store API used by EnrollmentActivity and other code.
     * Stores base64(iv||ciphertext) under the provided alias in SharedPreferences.
     */
    fun encryptAndStoreString(context: Context, keyAlias: String, plaintext: String) {
        val key = getOrCreateKey(keyAlias)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv // 12 bytes recommended for GCM
        val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))

        // store (iv + ciphertext) as base64 in SharedPreferences
        val out = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, out, 0, iv.size)
        System.arraycopy(ciphertext, 0, out, iv.size, ciphertext.size)
        val b64 = Base64.encodeToString(out, Base64.NO_WRAP)
        prefs(context).edit().putString(keyAlias, b64).apply()
    }

    /**
     * Backwards-compatible wrapper expected by older callers/tests
     * Returns true on success, false on failure.
     */
    fun encryptString(context: Context, plaintext: String, keyAlias: String): Boolean {
        return try {
            encryptAndStoreString(context, keyAlias, plaintext)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun decryptString(context: Context, keyAlias: String): String? {
        val stored = prefs(context).getString(keyAlias, null) ?: return null
        val raw = Base64.decode(stored, Base64.NO_WRAP)
        if (raw.size < 12) return null
        val iv = raw.copyOfRange(0, 12)
        val ct = raw.copyOfRange(12, raw.size)

        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        keyStore.load(null)
        val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry ?: return null
        val secretKey = entry.secretKey

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val plain = cipher.doFinal(ct)
        return String(plain, StandardCharsets.UTF_8)
    }

    fun clearStoredValue(context: Context, keyAlias: String) {
        prefs(context).edit().remove(keyAlias).apply()
    }

    /**
     * Backwards-compatible clear API
     */
    fun clear(context: Context, keyAlias: String): Boolean {
        return try {
            clearStoredValue(context, keyAlias)
            true
        } catch (_: Throwable) {
            false
        }
    }
}
