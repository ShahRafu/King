package com.shahrafuking.kingassistant.util

import java.util.Locale

/**
 * NumberParser: robust parsing for numeric amounts embedded in natural language.
 * - Converts Bengali digits to ASCII
 * - Extracts numeric digit sequences (including unicode digits)
 * - Falls back to English & Bengali word->number parsing for spelled-out numbers
 */
object NumberParser {
    private val bengaliDigits = mapOf(
        '০' to '0','১' to '1','২' to '2','৩' to '3','৪' to '4','৫' to '5','৬' to '6','৭' to '7','৮' to '8','৯' to '9'
    )

    fun normalizeDigits(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            if (bengaliDigits.containsKey(ch)) sb.append(bengaliDigits[ch])
            else sb.append(ch)
        }
        return sb.toString()
    }

    fun parseNumber(input: String): Double? {
        if (input.isBlank()) return null
        val norm = normalizeDigits(input).lowercase(Locale.getDefault())

        // 1) try to find an explicit digit sequence (supports unicode digits after normalization)
        val digitRegex = Regex("[-+]?[0-9][0-9,\\.]*")
        val m = digitRegex.find(norm)
        if (m != null) {
            val cleaned = m.value.replace(",", "")
            return cleaned.toDoubleOrNull()
        }

        // 2) try English words
        val eng = parseEnglishWordsToNumber(norm)
        if (eng != null) return eng

        // 3) try Bengali words
        val bn = parseBengaliWordsToNumber(norm)
        if (bn != null) return bn

        return null
    }

    // English word->number (supports up to billions)
    private fun parseEnglishWordsToNumber(text: String): Double? {
        val units = mapOf(
            "zero" to 0L, "one" to 1L, "two" to 2L, "three" to 3L, "four" to 4L, "five" to 5L,
            "six" to 6L, "seven" to 7L, "eight" to 8L, "nine" to 9L, "ten" to 10L,
            "eleven" to 11L, "twelve" to 12L, "thirteen" to 13L, "fourteen" to 14L, "fifteen" to 15L,
            "sixteen" to 16L, "seventeen" to 17L, "eighteen" to 18L, "nineteen" to 19L
        )
        val tens = mapOf(
            "twenty" to 20L, "thirty" to 30L, "forty" to 40L, "fifty" to 50L, "sixty" to 60L,
            "seventy" to 70L, "eighty" to 80L, "ninety" to 90L
        )
        val scales = mapOf("hundred" to 100L, "thousand" to 1_000L, "million" to 1_000_000L, "billion" to 1_000_000_000L)

        // tokenization: keep alphabetic tokens
        val tokens = text.replace(Regex("[^a-zA-Z\\s-]"), " ").replace('-', ' ').split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null

        var total = 0L
        var current = 0L
        var any = false
        for (t0 in tokens) {
            val t = t0.trim()
            if (units.containsKey(t)) {
                current += units[t]!!
                any = true
            } else if (tens.containsKey(t)) {
                current += tens[t]!!
                any = true
            } else if (t == "and") {
                // ignore
            } else if (scales.containsKey(t)) {
                val scale = scales[t]!!
                if (scale == 100L) {
                    if (current == 0L) current = 1L
                    current *= scale
                } else {
                    if (current == 0L) current = 1L
                    total += current * scale
                    current = 0L
                }
                any = true
            } else {
                // not a number word
            }
        }
        val value = total + current
        return if (any) value.toDouble() else null
    }

    // Bengali word->number (supports basic words + lakh/crore)
    private fun parseBengaliWordsToNumber(text: String): Double? {
        // Simple mapping for common Bengali number words
        val units = mapOf(
            "শূন্য" to 0L, "এক" to 1L, "দুই" to 2L, "তিন" to 3L, "চার" to 4L, "পাঁচ" to 5L,
            "পাঁচ" to 5L, "ছয়" to 6L, "ছয়" to 6L, "সাত" to 7L, "আট" to 8L, "নয়" to 9L, "নয়" to 9L,
            "দশ" to 10L, "এগারো" to 11L, "বারো" to 12L, "তেরো" to 13L, "চৌদ্দ" to 14L, "পনেরো" to 15L,
            "ষোল" to 16L, "সতেরো" to 17L, "আঠারো" to 18L, "উনিশ" to 19L
        )
        val tens = mapOf(
            "বিশ" to 20L, "ত্রিশ" to 30L, "তেরিশ" to 30L, "চল্লিশ" to 40L, "চল্লিশ" to 40L,
            "পঞ্চাশ" to 50L, "ষাট" to 60L, "সত্তর" to 70L, "আশি" to 80L, "নব্বই" to 90L
        )
        val scales = mapOf("শত" to 100L, "হাজার" to 1_000L, "লক্ষ" to 100_000L, "কোটি" to 10_000_000L)

        // tokens (split by non-letter, keep bengali letters)
        val tokens = text.replace(Regex("[^\u0980-\u09FF\\s-]"), " ").replace('-', ' ').split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null

        var total = 0L
        var current = 0L
        var any = false
        for (t0 in tokens) {
            val t = t0.trim()
            if (units.containsKey(t)) {
                current += units[t]!!
                any = true
            } else if (tens.containsKey(t)) {
                current += tens[t]!!
                any = true
            } else if (t == "এবং" || t == "অথবা") {
                // ignore
            } else if (scales.containsKey(t)) {
                val scale = scales[t]!!
                if (scale == 100L) {
                    if (current == 0L) current = 1L
                    current *= scale
                } else {
                    if (current == 0L) current = 1L
                    total += current * scale
                    current = 0L
                }
                any = true
            } else {
                // not recognized
            }
        }
        val value = total + current
        return if (any) value.toDouble() else null
    }
}
