package com.shahrafuking.kingassistant.model

/**
 * ApiClientOpenAI: centralised access to the OpenAI API key.
 * Preferred: inject OPENAI_API_KEY into BuildConfig in your module Gradle config.
 * Fallback: environment variable OPENAI_API_KEY.
 */
object ApiClientOpenAI {
    val API_KEY: String
        get() {
            // Try BuildConfig if present
            try {
                val pkg = this::class.java.`package`?.name ?: "com.shahrafuking.kingassistant"
                val cls = Class.forName("$pkg.BuildConfig")
                val field = cls.getField("OPENAI_API_KEY")
                val value = field.get(null) as? String
                if (!value.isNullOrBlank()) return value
            } catch (_: Throwable) { /* ignore if BuildConfig not present or field missing */ }

            // Fallback to environment
            val env = System.getenv("OPENAI_API_KEY")
            if (!env.isNullOrBlank()) return env

            throw IllegalStateException("OpenAI API key not found. Set BuildConfig.OPENAI_API_KEY or env OPENAI_API_KEY")
        }
}
