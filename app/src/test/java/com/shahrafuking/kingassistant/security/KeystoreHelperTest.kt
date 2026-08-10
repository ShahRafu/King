package com.shahrafuking.kingassistant.security

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test

class KeystoreHelperTest {
    @Test
    fun encryptDecryptClear() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val key = "unittest_key"
        val value = "hello_world_test"
        val ok = KeystoreHelper.encryptString(ctx, value, key)
        assertTrue(ok)
        val got = KeystoreHelper.decryptString(ctx, key)
        assertEquals(value, got)
        val cleared = KeystoreHelper.clear(ctx, key)
        assertTrue(cleared)
        val after = KeystoreHelper.decryptString(ctx, key)
        assertNull(after)
    }
}
