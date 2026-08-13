package com.shahrafuking.kingassistant.location

import kotlin.math.round

/**
 * A simple immutable representation of a location update used in the Location module.
 */
data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 5f,
    val provider: String = "simulated",
    val timestampUtcMs: Long = System.currentTimeMillis()
) {
    override fun toString(): String = "LocationPoint(lat=$latitude, lon=$longitude, acc=$accuracyMeters, provider=$provider, ts=$timestampUtcMs)"
}
