package com.shahrafuking.kingassistant.speech

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LanguageTranslatorTest {
    @Test
    fun testFallbackTranslatorDetectTranslate() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val tts = AndroidTtsHelper(ctx)
        val translator = SimpleFallbackTranslator(ctx, tts)

        val pair = translator.detectAndTranslateTo("Hello world", "bn")
        assertNotNull(pair)
        assertEquals("Hello world", pair!!.second)
    }
}
