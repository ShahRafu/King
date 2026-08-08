package com.shahrafuking.kingassistant.core

import java.util.Locale
import kotlin.math.abs
import kotlin.text.Regex

/**
 * IntentParser
 * - আপনার দেয়া ماس্টার লজিকের কমান্ড গুলো থেকে structured Intent তৈরি করে।
 * - সহজ নিয়মভিত্তিক parsing ব্যবহার করে (পরবর্তীতে LLM/ML ব্যবহার করে উন্নত করা যাবে)।
 *
 * Important: এই parser কেবলই intent বের করবে; কোনো কার্যকরী ট্রেড/নেটওয়ার্ক অ্যাকশন এটি এক্সিকিউট করবে না।
 */

sealed class Intent {
    data class AuthRequest(val phrase: String = "King Assistant") : Intent()
    data class TradeRequest(val side: TradeSide, val budgetUsd: Double?, val market: String?) : Intent()
    data class SetBudget(val budgetUsd: Double) : Intent()
    object PanicStop : Intent()
    object QueryStatus : Intent()
    data class AddMemory(val title: String, val text: String) : Intent()
    data class ScanMarket(val scope: String?) : Intent()
    object Shutdown : Intent()
    object Unknown : Intent()
}

enum class TradeSide { BUY, SELL, UP, DOWN }

object IntentParser {

    private val numberRegex = Regex("""(\d+(?:[.,]\d+)?)""")

    fun parse(textRaw: String): Intent {
        val text = textRaw.trim().lowercase(Locale.getDefault())

        // Auth requests (voice phrase invocation)
        if (text.contains("king assistant") || text.contains("কিং অ্যাসিস্টেন্ট")) {
            // If user also says stop/shutdown after phrase, handle accordingly upstream
            return Intent.AuthRequest()
        }

        // Panic / stop commands
        if (containsAny(text, listOf("সব ট্রেড বন্ধ", "পেনিক", "panic", "বন্ধ করো", "সব বন্ধ করো", "stop trades", "stop"))) {
            return Intent.PanicStop
        }

        // Shutdown / sleep
        if (containsAny(text, listOf("ঘুমে", "জিরিয়ে", "শাটডাউন", "shutdown", "sleep"))) {
            return Intent.Shutdown
        }

        // Query status
        if (containsAny(text, listOf("স্ট্যাটাস", "কি অবস্থা", "status", "what's up", "স্ট্যাটাস বল")) ) {
            return Intent.QueryStatus
        }

        // Set budget e.g. "২৫ ডলারের মধ্যে ট্রেড নাও" or "set 20 USD"
        if (containsAny(text, listOf("ডলার", "usd", "টাকা", "বাজেট"))) {
            val num = extractFirstNumber(text)
            if (num != null) {
                // If sentence also mentions trade/buy/sell -> TradeRequest, else SetBudget
                if (containsAny(text, listOf("ট্রেড", "নেও", "নাও", "buy", "sell", "up", "down", "বাই", "সেল"))) {
                    val side = detectTradeSide(text)
                    return Intent.TradeRequest(side = side, budgetUsd = num, market = detectMarket(text))
                } else {
                    return Intent.SetBudget(budgetUsd = num)
                }
            }
        }

        // Trade request without explicit budget: "এখন ট্রেড নাও" or "কটেক্সে DOWN নাও"
        if (containsAny(text, listOf("ট্রেড", "কটেক্স", "কটেক্সে", "buy", "sell", "up", "down", "বাই", "সেল"))) {
            val side = detectTradeSide(text)
            val budget = extractFirstNumber(text) // may be null
            return Intent.TradeRequest(side = side, budgetUsd = budget, market = detectMarket(text))
        }

        // Scan market: "১০০০ কয়েন স্ক্যান কর"
        if (text.contains("স্ক্যান") || text.contains("scan")) {
            return Intent.ScanMarket(scope = detectScanScope(text))
        }

        // Add memory: "মনে রাখো যে ..." or "নোট কর"
        if (containsAny(text, listOf("মনে রাখো", "নোট কর", "remember", "note"))) {
            val title = "user_note"
            return Intent.AddMemory(title = title, text = textRaw)
        }

        // default fallback
        return Intent.Unknown
    }

    private fun containsAny(text: String, keys: List<String>): Boolean {
        return keys.any { text.contains(it) }
    }

    private fun extractFirstNumber(text: String): Double? {
        val match = numberRegex.find(text.replace(",", "."))
        return match?.groups?.get(1)?.value?.toDoubleOrNull()
    }

    private fun detectTradeSide(text: String): TradeSide {
        return when {
            containsAny(text, listOf("বাই", "buy", "up", "উপর")) -> TradeSide.BUY
            containsAny(text, listOf("সেল", "sell", "down", "ডাউন")) -> TradeSide.SELL
            containsAny(text, listOf("up")) -> TradeSide.UP
            containsAny(text, listOf("down")) -> TradeSide.DOWN
            else -> TradeSide.BUY
        }
    }

    private fun detectMarket(text: String): String? {
        if (text.contains("কটেক্স")) return "quotex"
        if (text.contains("ক্রিপ্টো") || text.contains("btc") || text.contains("eth")) return "crypto"
        if (text.contains("ফরেক্স") || text.contains("forex")) return "forex"
        return null
    }

    private fun detectScanScope(text: String): String? {
        // naive: look for numbers or words like '১০০০' or 'কয়েন'
        if (text.contains("কয়েন") || text.contains("কয়েন") || text.contains("coin") ) return "coins"
        return null
    }
}
