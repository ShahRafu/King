package com.shahrafuking.kingassistant.lang

import android.content.Context
import com.shahrafuking.kingassistant.wasm.PythonWasmAdapter

/**
 * PythonAdapter
 *
 * Language adapter for Python code. Uses the PythonWasmAdapter to run code safely in a
 * WASM runtime (mocked for now). Linting is currently a placeholder that uses simple
 * heuristics — later integrate ruff or pylint as needed.
 */
class PythonAdapter(private val context: Context) : LanguageAdapter {
    private val wasm = PythonWasmAdapter(context)

    override suspend fun runCode(source: String, timeoutMs: Int): String {
        // Run in WASM (or fallback) and return output
        return wasm.runPythonCode(source, timeoutMs)
    }

    override suspend fun lintCode(source: String): String {
        // Simple heuristic checks as placeholder
        if (source.contains("import os") || source.contains("subprocess")) {
            return "Warning: code imports OS-level modules which may be unsafe to run in sandbox."
        }
        return "" // empty = no issues found
    }
}
