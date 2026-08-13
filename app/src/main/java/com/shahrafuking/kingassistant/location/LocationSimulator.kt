package com.shahrafuking.kingassistant.location

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

/**
 * LocationSimulator
 * - Generates a configurable list of waypoints around a center coordinate and emits them at a fixed interval.
 * - This is an in-app simulation only; it does not change system GPS or device location providers.
 */
class LocationSimulator(
    private val centerLat: Double = 23.8103, // default Dhaka center if none provided
    private val centerLon: Double = 90.4125,
    private val pointCount: Int = 7,
    private val intervalMs: Long = 15_000L
) {
    /** Create a simple route of pointCount waypoints around center */
    private fun generateRoute(): List<LocationPoint> {
        val pts = mutableListOf<LocationPoint>()
        val rnd = Random(System.currentTimeMillis())
        val maxOffset = 0.02 // degrees (~2km)
        for (i in 0 until pointCount) {
            val latOff = (rnd.nextDouble() - 0.5) * maxOffset
            val lonOff = (rnd.nextDouble() - 0.5) * maxOffset
            val lat = centerLat + latOff
            val lon = centerLon + lonOff
            pts.add(LocationPoint(latitude = lat, longitude = lon, accuracyMeters = (5f + rnd.nextFloat() * 10f), provider = "simulated", timestampUtcMs = System.currentTimeMillis()))
        }
        return pts
    }

    /**
     * Returns a Flow that emits a LocationPoint every intervalMs for the generated route.
     */
    fun start(): Flow<LocationPoint> = flow {
        val route = generateRoute()
        for (p in route) {
            emit(p.copy(timestampUtcMs = System.currentTimeMillis()))
            delay(intervalMs)
        }
    }
}
