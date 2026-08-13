package com.shahrafuking.kingassistant.location

import org.junit.Assert.assertTrue
import org.junit.Test

class ProxyManagerTest {
    @Test
    fun proxyManager_records_and_clears_sessions() {
        val sid = "test-session-1"
        ProxyManager.setProxyForSession(sid, "example.com")
        val active = ProxyManager.activeSessions()
        assertTrue(active.containsKey(sid))
        ProxyManager.clearProxyForSession(sid)
        val after = ProxyManager.activeSessions()
        assertTrue(!after.containsKey(sid))
    }
}
