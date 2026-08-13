package com.shahrafuking.kingassistant.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * LocationService: Foreground service that keeps the location session alive while running.
 * It subscribes to LocationManager.locations and posts a lightweight ongoing notification.
 */
class LocationService : Service() {
    private val TAG = "LocationService"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var collectJob: Job? = null

    companion object {
        const val CHAN_ID = "king_location_channel"
        const val NOTIF_ID = 1402
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notif = NotificationCompat.Builder(this, CHAN_ID)
            .setContentTitle("King Assistant — Location")
            .setContentText("Location session active")
            .setSmallIcon(com.shahrafuking.kingassistant.R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(NOTIF_ID, notif as Notification)

        collectJob = scope.launch {
            try {
                LocationManager.locations.collect { lp ->
                    // lightweight handling: log points; other components can separately collect the flow
                    Log.d(TAG, "loc: ${lp.latitude}, ${lp.longitude} @ ${lp.timestampUtcMs}")
                }
            } catch (t: Throwable) {
                Log.w(TAG, "collector failed: ${t.message}")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // keep running until explicitly stopped
        return Service.START_STICKY
    }

    override fun onDestroy() {
        collectJob?.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(CHAN_ID, "King Location", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(ch)
        }
    }
}
