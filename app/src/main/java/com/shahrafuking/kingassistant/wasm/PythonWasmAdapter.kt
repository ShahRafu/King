package com.shahrafuking.kingassistant.wasm

import android.content.Context
import android.util.Log

/**
 * PythonWasmAdapter
 *
 * Thin Kotlin wrapper around a native WASM runtime JNI bridge (wasm_bridge). The native layer
 * should execute Python compiled-to-WASM binaries or a Python WASM runtime (Pyodide/compiled)
 * securely in a sandbox. For now the JNI bridge is a mock stub returning deterministic output
 * so the app flow can be tested; later replace the native runtime with Wasmtime/Wasmer or a
 * direct Pyodide port.
 */
class PythonWasmAdapter(private val context: Context) {
    private val TAG = "PythonWasmAdapter"

    init {
        try {
            System.loadLibrary("llmbridge") // wasm stub compiled into same native lib
        } catch (t: Throwable) {
            Log.w(TAG, "could not load native llmbridge (wasm stub)", t)
        }
    }

    external fun nativeRunPythonWasm(code: String, timeoutMs: Int): String?

    suspend fun runPythonCode(code: String, timeoutMs: Int = 5000): String {
        // Call native runtime; fallback to a simple local validator if native fails
        return try {
            val res = nativeRunPythonWasm(code, timeoutMs)
            res ?: "(native wasm runtime returned null)"
        } catch (t: Throwable) {
            Log.w(TAG, "native wasm runtime failed, returning safe fallback", t)
            "(fallback) Python WASM runtime not available on this device."
        }
    }
}
