package com.shahrafuking.kingassistant.core

/**
 * Central configuration for King Assistant.
 * Put global constants here so the Owner/Assistant names and other global
 * settings can be changed from a single place.
 */
object Config {
    // Owner full name (display, logs, voice prompts)
    const val OWNER_FULL_NAME: String = "Shah Rafu King"

    // Assistant name used in UI, wakeword hints, and spoken prompts
    const val ASSISTANT_NAME: String = "King Assistant"

    // Display variants (you can use these where a localized or stylized form is required)
    const val OWNER_DISPLAY_SHORT: String = "Shah Rafu"
    const val ASSISTANT_DISPLAY_SHORT: String = "King"
}
