package com.shahrafuking.kingassistant.location

/**
 * Compile‑safe no-op ProxyManager.
 * If your app wants to route network calls per-session, replace these stubs with a real implementation.
 */
object ProxyManager {
    fun setProxyForSession(sessionId: String, domain: String) {
        // No-op safe default
    }

    fun clearProxyForSession(sessionId: String) {
        // No-op safe default
    }
}
