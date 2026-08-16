package com.shahrafuking.kingassistant.storage.room

import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.util.Base64

object MemoryUtils {
    fun floatArrayToBase64(arr: FloatArray): String {
        val byteBuffer = ByteBuffer.allocate(arr.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        arr.forEach { byteBuffer.putFloat(it) }
        return Base64.encodeToString(byteBuffer.array(), Base64.NO_WRAP)
    }

    fun base64ToFloatArray(s: String?): FloatArray? {
        if (s == null) return null
        val bytes = Base64.decode(s, Base64.NO_WRAP)
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val fa = FloatArray(bytes.size / 4)
        for (i in fa.indices) fa[i] = bb.getFloat()
        return fa
    }
}
