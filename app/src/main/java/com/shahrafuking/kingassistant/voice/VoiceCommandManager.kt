package com.shahrafuking.kingassistant.voice

import java.util.Locale

/**
 * Very small voice command parser that recognizes simple Bengali/English patterns.
 * This is intentionally conservative and meant for demo/testing.
 */
object VoiceCommandManager {
    private val TAG = "VoiceCommandManager"

    private val wakeWords = listOf("king assistant", "king", "কিং অ্যাসিস্ট্যান্ট", "কিং")

    private fun normalizeBengaliDigits(input: String): String {
        // Map Bengali digits to ASCII digits
        val map = mapOf(
            '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4', '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
        )
        val sb = StringBuilder()
        input.forEach { c -> sb.append(map[c] ?: c) }
        return sb.toString()
    }

    fun parse(text: String): Command {
        if (text.isBlank()) return Command.Unknown(text)
        var t = text.trim().lowercase(Locale.getDefault())
        // Remove wake words if present
        for (w in wakeWords) {
            if (t.startsWith(w)) {
                t = t.removePrefix(w).trim()
                break
            }
        }
        t = normalizeBengaliDigits(t)

        // Panic stop phrases (Bengali/English)
        val panicPatterns = listOf("সব ট্রেড বন্ধ", "সব ট্রেড বন্ধ করো", "সব বন্ধ করো", "stop all trades", "stop all", "stop")
        for (p in panicPatterns) if (t.contains(p)) return Command.PanicStop

        // Set budget (e.g., "বাজেট সেট করো 50 ডলার" or "set budget 50 dollars")
        val setBudgetKeywords = listOf("বাজেট", "set budget", "budget")
        for (k in setBudgetKeywords) {
            if (t.contains(k)) {
                val num = extractNumber(t)
                return if (num != null) Command.SetBudget(num) else Command.Unknown(text)
            }
        }

        // Trade commands (e.g., "এখন ২০ ডলারের ট্রেড নাও", "take trade 20 dollars")
        val tradeKeywords = listOf("ট্রেড", "trade", "নাও", "take trade", "buy", "sell")
        for (k in tradeKeywords) {
            if (t.contains(k)) {
                val num = extractNumber(t)
                return if (num != null) Command.Trade(num) else Command.Unknown(text)
            }
        }

        // Query status
        if (t.contains("স্ট্যাটাস") || t.contains("status")) {
            return Command.QueryStatus()
        }

        return Command.Unknown(text)
    }

    private fun extractNumber(s: String): Double? {
        // Basic regex to find numbers like 12, 12.5 or $12
        val regex = Regex("(\\d+(?:\\.\\d+)?)")
        val m = regex.find(s)
        return m?.value?.toDoubleOrNull()
    }
}
