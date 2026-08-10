package com.shahrafuking.kingassistant.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PanicManagerTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        PanicManager.release(ctx)
    }

    @Test
    fun testPanicEngageAndRelease() {
        PanicManager.release(ctx)
        assertFalse(PanicManager.isEngaged(ctx))
        PanicManager.engage(ctx)
        assertTrue(PanicManager.isEngaged(ctx))
        PanicManager.release(ctx)
        assertFalse(PanicManager.isEngaged(ctx))
    }
}
