/**
 * Replace Bengali digits with ASCII digits and return the normalized string.
 * Leaves other characters untouched.
 */
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
 *
 * Behaviour:
 *  - normalize Bengali digits to ASCII
 *  - remove common currency words/symbols and non numeric characters except dot and comma
 *  - accept either '.' or ',' as decimal separator (prefers '.')
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
