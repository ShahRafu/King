package com.shahrafuking.kingassistant.location

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationSimulatorTest {
    @Test
    fun simulator_emits_configured_count() = runBlocking {
        val sim = LocationSimulator(centerLat = 23.8, centerLon = 90.4, pointCount = 5, intervalMs = 10L)
        val emitted = sim.start().toList()
        // should emit exactly 5 points
        assertEquals(5, emitted.size)
    }
}
