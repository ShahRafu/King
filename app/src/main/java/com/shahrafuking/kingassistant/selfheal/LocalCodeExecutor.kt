package com.shahrafuking.kingassistant.selfheal

import android.util.Log
import com.squareup.duktape.Duktape

/**
 * LocalCodeExecutor
 *
 * Minimal, intentionally-restricted JavaScript runtime for running small validation plugins
 * or augmentation logic embedded in the app. Uses Duktape (no network or File APIs are
 * exposed by default). The host binds only the inputs explicitly provided via the `args` map.
 */
class LocalCodeExecutor {
    private val TAG = "LocalCodeExecutor"

    fun runValidator(script: String, args: Map<String, String>): Boolean {
        var d: Duktape? = null
        return try {
            d = Duktape.create()
            // Bind provided args as a global `content` variable if present
            args["content"]?.let { content ->
                // Escape content and create a small wrapper that injects `content` variable
                val wrapper = "var content = " + org.json.JSONObject.quote(content) + ";\n" + script
                val res = d.evaluate(wrapper)
                if (res is Boolean) return res
                return res != null
            }
            val res = d.evaluate(script)
            if (res is Boolean) res else res != null
        } catch (t: Throwable) {
            Log.w(TAG, "validator error", t)
            false
        } finally {
            d?.close()
        }
    }

    /**
     * generateCode: run a JS generator script that uses `prompt` and `existing` globals and
     * returns a generated source string. This is a safe wrapper around Duktape for tests.
     */
    fun generateCode(script: String, prompt: String, existing: String): String? {
        var d: Duktape? = null
        return try {
            d = Duktape.create()
            val wrapper = "var prompt = " + org.json.JSONObject.quote(prompt) +
                    ";\nvar existing = " + org.json.JSONObject.quote(existing) + ";\n" + script
            val res = d.evaluate(wrapper)
            when (res) {
                is String -> res
                null -> null
                else -> res.toString()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "generateCode error", t)
            null
        } finally {
            d?.close()
        }
    }
}
