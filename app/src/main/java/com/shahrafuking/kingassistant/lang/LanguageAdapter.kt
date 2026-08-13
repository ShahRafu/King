package com.shahrafuking.kingassistant.lang

/**
 * LanguageAdapter: interface for running or validating generated code in a specific language.
 * Implementations should run code in a sandboxed environment and return stdout/stderr or
 * structured results for the orchestrator to consume.
 */
interface LanguageAdapter {
    /**
     * Run the provided source code and return captured output. Implementations MUST sandbox
     * execution and enforce timeouts and resource limits.
     */
    suspend fun runCode(source: String, timeoutMs: Int = 5000): String

    /**
     * Optionally run static analysis / linters and return a report (empty string if clean).
     */
    suspend fun lintCode(source: String): String
}
