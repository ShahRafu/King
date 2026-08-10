package com.shahrafuking.kingassistant.speech

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CommandRecognizerTest {
    @Test
    fun testBudgetExtraction() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val cr = CommandRecognizer(ctx)
        val b1 = cr.extractBudgetFrom("Take trade with 20 dollars")
        assertNotNull(b1)
        assertEquals(20.0, b1!!, 0.001)

        val b2 = cr.extractBudgetFrom("budget ৫০০")
        // depending on numerals, may be null; just ensure method returns safely
    }
}
