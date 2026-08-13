package com.shahrafuking.kingassistant.llm

import android.app.Activity
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * LocalLLMManager
 *
 * High-level Kotlin API for on-device LLM runtime. This class is a thin wrapper around
 * a native library (loaded via JNI). All code-generation operations MUST be gated by
 * VoiceAuthGatekeeper to ensure owner authorization before any file writes.
 *
 * NOTE: The native library is expected to be provided separately (llama.cpp-based build
 * compiled for Android). This Kotlin class can operate with a mock native during testing.
 */
class LocalLLMManager(private val activity: Activity) {
    private val TAG = "LocalLLMManager"

    init {
        try {
            System.loadLibrary("llmbridge")
        } catch (t: Throwable) {
            Log.w(TAG, "Could not load native llmbridge library", t)
        }
    }

    external fun nativeLoadModel(path: String): Boolean
    external fun nativeGenerate(prompt: String, maxTokens: Int): String?
    external fun nativeUnload()

    private val gatekeeper = com.shahrafuking.kingassistant.selfheal.VoiceAuthGatekeeper(activity)

    suspend fun loadModel(path: String): Boolean = withContext(Dispatchers.IO) {
        // ensure model path is under allowed directories (owner-controlled)
        nativeLoadModel(path)
    }

    suspend fun generateCodeWithApproval(prompt: String, maxTokens: Int = 512): String? = withContext(Dispatchers.IO) {
        // require voice approval before generation that may lead to file write
        val approvalPrompt = "Approve code generation request. Repeat the challenge phrase to approve."
        val ok = gatekeeper.requestOwnerApproval(approvalPrompt)
        if (!ok) {
            Log.i(TAG, "Owner denied code generation request")
            return@withContext null
        }

        // Call native generate
        val result = nativeGenerate(prompt, maxTokens)
        return@withContext result
    }

    fun unload() {
        try {
            nativeUnload()
        } catch (t: Throwable) {
            Log.w(TAG, "unload failed", t)
        }
    }
}
