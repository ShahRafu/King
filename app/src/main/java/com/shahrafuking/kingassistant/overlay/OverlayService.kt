package com.shahrafuking.kingassistant.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.shahrafuking.kingassistant.speech.CommandRecognizer
import com.shahrafuking.kingassistant.speech.HotwordManager
import kotlinx.coroutines.*

/**
 * OverlayService (updated)
 *
 * - Replaces previous continuous SpeechRecognizer loop with HotwordManager + CommandRecognizer.
 * - HotwordManager tries Porcupine (if enabled) otherwise uses Android SpeechRecognizer fallback,
 *   and both are configured to match Bangla (bn-BD) via the HotwordManager/CommandRecognizer implementations.
 * - When hotword is detected, this service starts a one-shot Bengali command recognition session
 *   (CommandRecognizer) and handles the returned Bangla text in handleVoiceCommand().
 *
 * Important:
 * - Activity must grant RECORD_AUDIO at runtime before starting this service.
 * - Activity must guide user to enable SYSTEM_ALERT_WINDOW (draw over other apps) before showing overlay.
 */
class OverlayService : Service() {
    private val TAG = "OverlayService"
    private val CHANNEL_ID = "king_overlay_channel"
    private val NOTIF_ID = 1337

    private var windowManager: WindowManager? = null
    private var overlayRoot: FrameLayout? = null

    // Hotword + command pieces
    private var hotwordManager: HotwordManager? = null
    private var commandRecognizer: CommandRecognizer? = null
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var isOverlayShown = false
    private var commandInProgress = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForeground(NOTIF_ID, createNotification())

        // Initialize HotwordManager and listen for hotword events
        hotwordManager = HotwordManager(applicationContext)
        hotwordManager?.setListener(object : HotwordManager.HotwordListener {
            override fun onHotwordDetected() {
                Log.i(TAG, "Hotword detected (service)")
                onHotwordTriggered()
            }
        })
        // HotwordManager will try Porcupine if enabled; otherwise falls back to bn-BD speech recognizer.
        hotwordManager?.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle stop action
        val action = intent?.action
        if (action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        val show = intent?.getBooleanExtra(EXTRA_SHOW_OVERLAY, false) ?: false
        if (show) showOverlay()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        try {
            hotwordManager?.stop()
        } catch (_: Throwable) {}
        commandRecognizer?.cancel()
        mainScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "King Overlay", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, OverlayService::class.java).apply { action = ACTION_STOP_SERVICE }
        val pendingStop = PendingIntent.getService(this, 0, stopIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("King Assistant")
            .setContentText("Overlay service running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pendingStop)
            .build()
    }

    // --------------------------
    // Overlay view handling
    // --------------------------
    fun showOverlay() {
        if (isOverlayShown) return
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission not granted")
            return
        }

        overlayRoot = FrameLayout(this).apply {
            setBackgroundColor(0x00000000) // transparent container
        }

        val robotView = TextView(this).apply {
            text = "\uD83E\uDD16" // robot emoji placeholder; replace with animated view if desired
            textSize = 56f
            setPadding(24, 24, 24, 24)
            setBackgroundResource(android.R.drawable.alert_light_frame)
        }

        robotView.setOnTouchListener(object : View.OnTouchListener {
            var lastX = 0f
            var lastY = 0f
            var initialX = 0
            var initialY = 0
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val lp = v.layoutParams as WindowManager.LayoutParams
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastX = event.rawX
                        lastY = event.rawY
                        initialX = lp.x
                        initialY = lp.y
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - lastX).toInt()
                        val dy = (event.rawY - lastY).toInt()
                        lp.x = initialX + dx
                        lp.y = initialY + dy
                        try { windowManager?.updateViewLayout(v, lp) } catch (_: Exception) {}
                        return true
                    }
                }
                return false
            }
        })

        overlayRoot?.addView(robotView)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        try {
            windowManager?.addView(overlayRoot, params)
            isOverlayShown = true
            startWalkingAnimation(robotView, params)
            sendBroadcast(Intent(ACTION_OVERLAY_SHOW))
        } catch (t: Throwable) {
            Log.e(TAG, "addView failed", t)
        }
    }

    private fun hideOverlay() {
        try {
            if (isOverlayShown) {
                windowManager?.removeView(overlayRoot)
                overlayRoot = null
                isOverlayShown = false
                sendBroadcast(Intent(ACTION_OVERLAY_HIDE))
            }
        } catch (t: Throwable) {
            Log.w(TAG, "hide error", t)
        }
    }

    private fun startWalkingAnimation(robotView: View, params: WindowManager.LayoutParams) {
        mainScope.launch {
            try {
                val screenWidth = resources.displayMetrics.widthPixels
                var dir = 1
                while (isOverlayShown && isActive) {
                    delay(1400)
                    params.x = (0.05f * screenWidth).toInt() + (if (dir > 0) (0.6f * screenWidth).toInt() else 0)
                    try { windowManager?.updateViewLayout(robotView, params) } catch (_: Exception) {}
                    dir *= -1
                }
            } catch (_: CancellationException) { }
            catch (t: Throwable) { Log.w(TAG, "walk anim error", t) }
        }
    }

    // --------------------------
    // Hotword -> Command flow
    // --------------------------
    private fun onHotwordTriggered() {
        // prevent overlapping command sessions
        if (commandInProgress.get()) {
            Log.i(TAG, "command already in progress, ignoring hotword")
            return
        }

        // Optionally show overlay when hotword detected
        showOverlay()

        // Temporarily pause hotword manager to avoid double triggers while recognizing command
        try { hotwordManager?.stop() } catch (_: Throwable) {}

        commandInProgress.set(true)

        commandRecognizer = CommandRecognizer(applicationContext)
        commandRecognizer?.listenOnce(object : CommandRecognizer.CommandListener {
            override fun onCommandResult(text: String) {
                Log.i(TAG, "Command recognized (bn-BD): $text")
                handleVoiceCommand(text)
                cleanupCommandSession()
            }

            override fun onCommandError(reason: String) {
                Log.w(TAG, "Command error: $reason")
                cleanupCommandSession()
            }
        })
    }

    private fun cleanupCommandSession() {
        try { commandRecognizer?.cancel() } catch (_: Throwable) {}
        commandRecognizer = null
        commandInProgress.set(false)
        // restart hotword listening after brief delay
        mainScope.launch {
            delay(500)
            try { hotwordManager?.start() } catch (_: Throwable) {}
        }
    }

    /**
     * Parse Bangla (bn-BD) commands and execute actions.
     * Keep parsing focused and conservative — expand as you add more commands.
     */
    private fun handleVoiceCommand(text: String) {
        val low = text.lowercase().trim()
        // Deactivation commands: "King Assistant এখন জিরিয়ে নাও" / "King Assistant ঘুমিয়ে যাও" / "এক হও"
        if (low.contains("এক হও") || low.contains("ek ho") || low.contains("একহো") ||
            low.contains("কিং অ্যাসিস্ট্যান্ট ঘুমিয়ে") || low.contains("কিং অ্যাসিস্ট্যান্ট এখন জিরিয়ে")) {
            Log.i(TAG, "Voice command -> deactivate/stop service")
            // Stop overlay and optionally stop service
            hideOverlay()
            stopSelf()
            return
        }

        // Overlay hide command (Bangla variants)
        if (low.contains("বন্ধ কর") || low.contains("থামো") || low.contains("বন্ধ")) {
            Log.i(TAG, "Voice command -> hide overlay")
            hideOverlay()
            return
        }

        // Example panic stop (trades) placeholder
        if (low.contains("সব ট্রেড বন্ধ কর") || low.contains("সব ট্রেড থামাও") || low.contains("panic stop")) {
            Log.i(TAG, "Voice command -> PANIC STOP (placeholder)")
            // TODO: Hook into trading module to stop active trades
            // For now, broadcast or log
            sendBroadcast(Intent(ACTION_PANIC_STOP))
            return
        }

        // Example: request status
        if (low.contains("তোমার অবস্থা") || low.contains("কি করছ") || low.contains("স্ট্যাটাস")) {
            Log.i(TAG, "Voice command -> status request")
            // You can broadcast or update notification
            // For demo: just log
            return
        }

        // If command not recognized, you can show/voice a prompt or ignore
        Log.i(TAG, "Voice command not matched: $text")
    }

    companion object {
        const val ACTION_STOP_SERVICE = "king.overlay.STOP"
        const val EXTRA_SHOW_OVERLAY = "extra_show_overlay"

        const val ACTION_OVERLAY_SHOW = "king.overlay.SHOWED"
        const val ACTION_OVERLAY_HIDE = "king.overlay.HIDDEN"

        // Example broadcast for panic stop hook
        const val ACTION_PANIC_STOP = "king.overlay.PANIC_STOP"
    }
}
