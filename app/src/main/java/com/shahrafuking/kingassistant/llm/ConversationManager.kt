package com.shahrafuking.kingassistant.llm

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ConversationManager
 * - Maintains a short in-memory session buffer
 * - Calls ApiClientOpenAI for assistant responses
 * - Applies a simple PolicyFilter before returning results
 */
class ConversationManager(private val context: Context) {
    companion object { private const val TAG = "ConversationManager" }

    private val client = ApiClientOpenAI(context)
    private val sessions = mutableMapOf<String, MutableList<Pair<String, String>>>()

    fun startSession(sessionId: String) {
        sessions[sessionId] = mutableListOf()
    }

    fun endSession(sessionId: String) {
        sessions.remove(sessionId)
    }

    fun pushUserMessage(sessionId: String, text: String) {
        val buf = sessions.getOrPut(sessionId) { mutableListOf() }
        buf.add("user" to text)
        // trim older messages if too long
        if (buf.size > 12) {
            buf.removeAt(0)
        }
    }

    suspend fun getAssistantReply(sessionId: String): String? {
        return withContext(Dispatchers.IO) {
            val buf = sessions[sessionId] ?: return@withContext null
            val systemPrompt = buildSystemPrompt()
            val messages = buf.toList()
            val msgPairs = messages.map { it.first to it.second }
            try {
                val resp = client.chatCompletion(systemPrompt, msgPairs)
                if (resp == null) return@withContext null
                // basic policy filter
                val allowed = PolicyFilter.isAllowed(resp)
                if (!allowed.first) {
                    Log.w(TAG, "PolicyFilter blocked assistant output: ${allowed.second}")
                    return@withContext "আমি এই অনুরোধটি সম্পাদন করতে পারছি না।"
                }
                // push assistant message to session history
                sessions[sessionId]?.add("assistant" to resp)
                return@withContext resp
            } catch (t: Throwable) {
                Log.w(TAG, "getAssistantReply failed", t)
                null
            }
        }
    }

    private fun buildSystemPrompt(): String {
        return "You are King Assistant, a helpful assistant for Shah Rafu King. Always respond in Bengali to the owner. Follow safety rules: do not execute financial trades without explicit confirmation. Keep responses concise."
    }
}
