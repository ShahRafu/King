package com.shahrafuking.kingassistant.location

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * LocationManager: facade to start/stop simulated or (future) real location sessions.
 * Exposes a SharedFlow of LocationPoint updates that app components can collect.
 */
object LocationManager {
    private const val TAG = "LocationManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var sessionJob: Job? = null
    private var currentSession: LocationSession? = null

    private val _locations = MutableSharedFlow<LocationPoint>(replay = 2)
    val locations: SharedFlow<LocationPoint> = _locations

    /** Start a session. If simulated==true, use LocationSimulator. */
    fun startSession(context: Context, session: LocationSession, centerLat: Double? = null, centerLon: Double? = null) {
        stopSession()
        currentSession = session
        Log.i(TAG, "starting session ${session.sessionId} simulated=${session.simulated}")

        // If there is a domain context and a ProxyManager is available, call the proxy hook.
        session.domainContext?.let { domain ->
            try {
                ProxyManager.setProxyForSession(session.sessionId, domain)
            } catch (t: Throwable) {
                Log.w(TAG, "ProxyManager.setProxyForSession failed: ${t.message}")
            }
        }

        sessionJob = scope.launch {
            if (session.simulated) {
                val lat = centerLat ?: 23.8103
                val lon = centerLon ?: 90.4125
                val simulator = LocationSimulator(centerLat = lat, centerLon = lon, pointCount = session.pointCount, intervalMs = session.intervalMs)
                simulator.start().collect { p ->
                    _locations.emit(p)
                }
            } else {
                // Placeholder for real fused location client implementation in a later change.
                Log.i(TAG, "real location provider not implemented — toggling to simulator fallback")
                val simulator = LocationSimulator(pointCount = session.pointCount, intervalMs = session.intervalMs)
                simulator.start().collect { p -> _locations.emit(p) }
            }

            // when flow completes (simulation ended), clear proxy
            session.domainContext?.let { domain ->
                try { ProxyManager.clearProxyForSession(session.sessionId) } catch (_: Throwable) {}
            }
        }

        // start a foreground service to keep process alive while session runs (best effort)
        try {
            val i = Intent(context, LocationService::class.java)
            i.putExtra("sessionId", session.sessionId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i) else context.startService(i)
        } catch (t: Throwable) {
            Log.w(TAG, "failed to start LocationService: ${t.message}")
        }
    }

    fun stopSession() {
        try {
            val sid = currentSession?.sessionId
            currentSession = null
            sessionJob?.cancel()
            sessionJob = null
            sid?.let { ProxyManager.clearProxyForSession(it) }
            Log.i(TAG, "session stopped")
        } catch (t: Throwable) {
            Log.w(TAG, "stopSession error: ${t.message}")
        }
    }

    fun isRunning(): Boolean = sessionJob?.isActive == true
}
