package com.shahrafuking.kingassistant.util

/**
 * NumberParser utility object exposing normalization and parsing helpers.
 */
object NumberParser {
    private val bengaliToAscii = mapOf(
        '০' to '0','১' to '1','২' to '2','৩' to '3','৪' to '4','৫' to '5','৬' to '6','৭' to '7','৮' to '8','৯' to '9'
    )

    fun normalizeDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (c in input) {
            sb.append(bengaliToAscii[c] ?: c)
        }
        return sb.toString()
    }

    /**
     * Try to parse a number out of the given text.
     * Returns Double? (null if no parsable number).
     */
    fun parseNumber(text: String?): Double? {
        if (text == null) return null
        val norm = normalizeDigits(text).trim().lowercase()

        // Remove currency words/symbols and letters; keep digits, dot, comma, minus
        val cleaned = norm
            .replace(Regex("(usd|dollar|ডলার|টাকা|৳)"), "")
            .replace(Regex("[^0-9\\.,\\-]"), "")

        if (cleaned.isBlank()) return null

        // If both comma and dot present, remove commas (assume comma thousands separator)
        val normalizedDecimal = when {
            cleaned.contains('.') && cleaned.contains(',') -> cleaned.replace(",", "")
            cleaned.contains(',') && !cleaned.contains('.') -> cleaned.replace(',', '.')
            else -> cleaned
        }

        return try {
            normalizedDecimal.toDouble()
        } catch (e: NumberFormatException) {
            null
        }
    }
}
