# Location module (feature/location-module)

This module implements a safe, in‑app Location simulation and a facade to run location sessions.

Design and guarantees
- Simulation only: the code does not modify system GPS or fake device location outside the app.
- Emits LocationPoint events every 15 seconds (configurable).
- Generates a random route of 5–15 waypoints by default (configurable per session).
- Provides a ProxyManager hook so that a later server/proxy provider integration can be attached; the client does NOT perform any automatic IP rotation or ban‑evasion logic.
- Session logs persistence is optional and disabled by default (privacy friendly).

Quick usage (Kotlin)

Start a simulated session:

```kotlin
val session = LocationSession(simulated = true, pointCount = 7, intervalMs = 15_000L)
LocationManager.startSession(context, session, centerLat = 23.8, centerLon = 90.4)

// collect updates
lifecycleScope.launchWhenStarted {
    LocationManager.locations.collect { point ->
        Log.d("LOC", "got: $point")
    }
}

// stop
LocationManager.stopSession()
```

Notes
- The Foreground LocationService is started automatically to keep the process alive while a session runs. The service shows an ongoing notification.
- If you later add a proxy provider, call ProxyManager inside startSession by providing the appropriate session/domain mapping. The client only stores the hook mapping — real proxy switching must be performed by a legal provider or server-side controller.
