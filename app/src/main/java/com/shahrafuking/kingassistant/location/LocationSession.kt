package com.shahrafuking.kingassistant.location

import java.util.UUID

/**
 * Metadata for a Location session.
 */
data class LocationSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val domainContext: String? = null, // optional site / domain context
    val simulated: Boolean = true,
    val pointCount: Int = 7,
    val intervalMs: Long = 15_000L
)
