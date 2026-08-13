package com.shahrafuking.kingassistant.security

import android.util.Base64
import java.security.SecureRandom

object SecurityUtils {
    private val secureRandom = SecureRandom()

    fun randomBase64(bytes: Int = 32): String {
        val b = ByteArray(bytes)
        secureRandom.nextBytes(b)
        return Base64.encodeToString(b, Base64.NO_WRAP)
    }

    fun toBase64(input: ByteArray): String = Base64.encodeToString(input, Base64.NO_WRAP)
    fun fromBase64(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)
}
