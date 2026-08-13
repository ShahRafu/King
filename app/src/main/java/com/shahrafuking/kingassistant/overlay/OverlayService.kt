package com.shahrafuking.kingassistant.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.shahrafuking.kingassistant.MainActivity
import com.shahrafuking.kingassistant.hotword.HotwordManager
import kotlinx.coroutines.*

/**
 * Foreground service that listens to microphone via HotwordManager.
 * On hotword detection it launches VerificationActivity (or notifies user).
 *
 * This service is intentionally minimal and safe: it does not perform any auto-clicking
 * or network action by itself. It only demonstrates background hotword-listening lifecycle.
 */
class OverlayService : Service() {
    companion object {
        const val CHANNEL_ID = "king_assistant_listen"
        const val NOTIF_ID = 1001
        private const val TAG = "OverlayService"
        const val ACTION_STOP_SERVICE = "com.shahrafuking.kingassistant.action.STOP"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var hotwordManager: HotwordManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        hotwordManager = HotwordManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notif = buildNotification()
        startForeground(NOTIF_ID, notif)

        hotwordManager.startListening { detected ->
            // Callback on main thread
            if (detected) {
                Log.i(TAG, "Hotword detected")
                // Bring verification or main UI to front (safe action)
                val i = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(i)
                // Optionally: post a notification or start VerificationActivity
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        hotwordManager.stopListening()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "King Assistant (listening)"
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pm = packageManager
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("King Assistant")
            .setContentText("Listening for hotword")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }
}
