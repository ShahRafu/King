package com.shahrafuking.kingassistant.ml

/**
 * PCM utilities: conversion helpers between PCM 16-bit (ShortArray) and FloatArray normalized [-1..1]
 */
object PcmUtils {
    fun shortsToFloats(pcm: ShortArray): FloatArray {
        val out = FloatArray(pcm.size)
        for (i in pcm.indices) {
            out[i] = pcm[i].toFloat() / Short.MAX_VALUE
        }
        return out
    }

    fun bytesToShortsLE(bytes: ByteArray): ShortArray {
        val n = bytes.size / 2
        val s = ShortArray(n)
        var idx = 0
        var i = 0
        while (i + 1 < bytes.size) {
            val low = bytes[i].toInt() and 0xFF
            val high = bytes[i + 1].toInt() and 0xFF
            s[idx++] = ((high shl 8) or low).toShort()
            i += 2
        }
        return s
    }
}
