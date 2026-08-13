package com.shahrafuking.kingassistant.voice

import org.junit.Assert.*
import org.junit.Test

class VoiceCommandManagerTest {

    @Test
    fun parse_trade_bengali() {
        val cmd = VoiceCommandManager.parse("King Assistant, এখন ২০ ডলারের ট্রেড নাও")
        assertTrue(cmd is Command.Trade)
        val t = cmd as Command.Trade
        assertEquals(20.0, t.amount, 0.001)
    }

    @Test
    fun parse_set_budget_english() {
        val cmd = VoiceCommandManager.parse("King Assistant, set budget 50 dollars")
        assertTrue(cmd is Command.SetBudget)
        val s = cmd as Command.SetBudget
        assertEquals(50.0, s.amount, 0.001)
    }

    @Test
    fun parse_panic_bengali() {
        val cmd = VoiceCommandManager.parse("King Assistant, সব ট্র��ড বন্ধ করো")
        assertTrue(cmd is Command.PanicStop)
    }

    @Test
    fun parse_unknown() {
        val cmd = VoiceCommandManager.parse("Hello there")
        assertTrue(cmd is Command.Unknown)
    }
}
