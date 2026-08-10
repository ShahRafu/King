package com.shahrafuking.kingassistant.core

import org.junit.Assert.*
import org.junit.Test

class SchedulerTest {

    @Test
    fun testParseRelativeTimeMinutesEnglish() {
        val t = "schedule trade in 5 minutes"
        val ms = Scheduler.parseRelativeTime(t)
        assertNotNull(ms)
        assertEquals(5 * 60 * 1000L, ms)
    }

    @Test
    fun testParseRelativeTimeMinutesBengaliDigits() {
        val t = "৫ মিনিট পরে ট্রেড নাও"
        val ms = Scheduler.parseRelativeTime(t)
        assertNotNull(ms)
        assertEquals(5 * 60 * 1000L, ms)
    }

    @Test
    fun testParseRelativeTimeHoursBengali() {
        val t = "২ ঘণ্টা পরে ট্রেড করো"
        val ms = Scheduler.parseRelativeTime(t)
        assertNotNull(ms)
        assertEquals(2 * 60 * 60 * 1000L, ms)
    }
}
