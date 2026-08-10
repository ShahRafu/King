package com.shahrafuking.kingassistant.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CommandHandlerTest {

    private val ctx: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        // ensure panic released and budget cleared after each test
        PanicManager.release(ctx)
        BudgetManager.clearBudget(ctx)
    }

    @Test
    fun testBudgetSetAndTradeCap() = runBlocking {
        // clear
        BudgetManager.clearBudget(ctx)
        val r1 = CommandHandler.handle(ctx, "বাজেট 20 ডলার")
        val b = BudgetManager.getBudget(ctx)
        assertNotNull("Budget should be set", b)
        assertEquals(20.0, b!!, 0.001)

        val r2 = CommandHandler.handle(ctx, "এখন ট্রেড নাও 50 ডলার")
        // Expect simulated response mentioning dry-run and amount
        assertTrue(r2.contains("DRY-RUN") || r2.contains("Simulated trade"))
        assertTrue(r2.contains("AmountUsd") || r2.contains("AmountUsd"))
    }

    @Test
    fun testPanicEngage() = runBlocking {
        val r = CommandHandler.handle(ctx, "সব ট্রেড বন্ধ করো")
        assertTrue("Panic should be engaged", PanicManager.isEngaged(ctx))
        assertTrue(r.contains("Panic command received"))
    }
}
