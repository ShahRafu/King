package com.shahrafuking.kingassistant.location

import android.util.Log

/**
 * ProxyManager: a safe hook API for the app to register proxy configuration for a session.
 * IMPORTANT: This implementation is a stub that records the desired domain→proxy mapping.
 * It does NOT perform any automatic IP rotation or network interception. Use a legal provider
 * and server-side component to realize real proxy switching.
 */
object ProxyManager {
    private const val TAG = "ProxyManager"

    private val sessions = mutableMapOf<String, String>() // sessionId -> domain

    fun setProxyForSession(sessionId: String, domain: String) {
        // store the mapping and log; real proxy application must be implemented outside the client
        sessions[sessionId] = domain
        Log.i(TAG, "set proxy hook for session=$sessionId domain=$domain")
    }

    fun clearProxyForSession(sessionId: String) {
        sessions.remove(sessionId)
        Log.i(TAG, "cleared proxy hook for session=$sessionId")
    }

    fun activeSessions(): Map<String, String> = sessions.toMap()
}
