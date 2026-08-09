package com.shahrafuking.kingassistant.core

import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * OfflineIntentParser
 * - Lightweight rule-based parser for local (on-device) voice commands after authentication.
 * - Produces concrete actions (OpenApp, ToggleSetting, PanicStop, SetBudget, StartTradingStub).
 */
object OfflineIntentParser {
    private const val TAG = "OfflineIntentParser"

    sealed class Action {
        object OpenApp : Action()
        data class ToggleSetting(val key: String, val enabled: Boolean?) : Action()
        data class LaunchSettings(val section: String?) : Action()
        object PanicStop : Action()
        data class SetBudget(val usd: Double) : Action()
        data class Unknown(val raw: String) : Action()
    }

    fun parse(textRaw: String): Action {
        val text = textRaw.trim().lowercase()
        try {
            if (text.contains("open app") || text.contains("open king") || text.contains("অ্যাপ খুল")) {
                return Action.OpenApp
            }
            if (containsAny(text, listOf("প্যানিক", "সব ট্রেড বন্ধ", "panic", "stop trades"))) {
                return Action.PanicStop
            }
            // Toggle sample setting patterns e.g. "turn on notifications", "নোটিফিকেশন বন্ধ কর"
            if (containsAny(text, listOf("নোটিফিকেশন", "notifications"))) {
                val on = text.contains("on") || containsAny(text, listOf("চালু", "on", "enable"))
                val off = text.contains("off") || containsAny(text, listOf("বন্ধ", "off", "disable"))
                return Action.ToggleSetting(key = "notifications", enabled = if (on) true else if (off) false else null)
            }
            // Settings pages
            if (containsAny(text, listOf("settings", "সেটিং", "ইনস্টল"))) {
                return Action.LaunchSettings(section = null)
            }
            // Budget
            val numberRe = Regex("(\\d+(?:[.,]\\d+)?)")
            val match = numberRe.find(text)
            if (match != null && (text.contains("ডলার") || text.contains("usd") || text.contains("budget"))) {
                val num = match.groupValues[1].replace(",", ".").toDoubleOrNull()
                if (num != null) return Action.SetBudget(usd = num)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "parse error: ${t.message}")
            return Action.Unknown(textRaw)
        }
        return Action.Unknown(textRaw)
    }

    private fun containsAny(text: String, keys: List<String>) = keys.any { text.contains(it) }
}
