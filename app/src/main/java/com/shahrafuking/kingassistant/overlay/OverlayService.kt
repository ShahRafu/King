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
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

/**
 * OverlayService: Foreground Service that creates a system overlay robot view
 * and runs a SpeechRecognizer to toggle the overlay using voice commands.
 *
 * Usage:
 * - Ensure RECORD_AUDIO runtime permission granted and SYSTEM_ALERT_WINDOW enabled in Settings.
 * - Start service via startForegroundService(intent) with EXTRA_SHOW_OVERLAY=true to show immediately.
 *
 * Voice commands recognized (examples):
 * - "king assistant go to your form"  => show overlay
 * - "ek ho" (or variants)             => hide overlay
 *
 * IMPORTANT:
 * - This uses Android SpeechRecognizer which may be OEM/battery-sensitive for continuous listening.
 * - For robust always-on hotword, prefer Porcupine + offline small recognizer.
 */
class OverlayService : Service() {
    private val TAG = "OverlayService"
    private val CHANNEL_ID = "king_overlay_channel"
    private val NOTIF_ID = 1337

    private var windowManager: WindowManager? = null
    private var overlayRoot: FrameLayout? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var listenJob: Job? = null
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var isOverlayShown = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForeground(NOTIF_ID, createNotification())
        startSpeechRecognitionLoop()
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
        stopSpeechRecognitionLoop()
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

    fun showOverlay() {
        if (isOverlayShown) return
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission not granted")
            return
        }

        overlayRoot = FrameLayout(this).apply {
            // Transparent container
            setBackgroundColor(0x00000000)
        }

        val robotView = TextView(this).apply {
            text = "\uD83E\uDD16" // robot emoji - replace with animated view if desired
            textSize = 56f
            setPadding(24, 24, 24, 24)
            setBackgroundResource(android.R.drawable.alert_light_frame)
        }

        // Make robot draggable
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

    // Speech recognition loop
    private fun startSpeechRecognitionLoop() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.w(TAG, "Speech recognition not available")
            return
        }
        stopSpeechRecognitionLoop()
        listenJob = mainScope.launch {
            while (isActive) {
                try {
                    startSingleRecognition()
                    delay(500) // brief pause between sessions
                } catch (t: Throwable) {
                    Log.w(TAG, "speech loop error", t)
                    delay(1000)
                }
            }
        }
    }

    private fun stopSpeechRecognitionLoop() {
        listenJob?.cancel()
        listenJob = null
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun startSingleRecognition() {
        // Caller (Activity) must ensure audio permission already granted
        speechRecognizer?.destroy()
        val recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer = recognizer
        val intent = RecognizerIntent().apply {
            action = RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Log.w(TAG, "recognizer error: $error")
                try { recognizer.cancel(); recognizer.destroy() } catch (_: Exception) {}
            }
            override fun onResults(results: Bundle?) {
                val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
                handleRecognizedTexts(texts)
                try { recognizer.destroy() } catch (_: Exception) {}
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val texts = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
                handleRecognizedTexts(texts)
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer.startListening(intent)
    }

    private fun handleRecognizedTexts(texts: ArrayList<String>) {
        for (t in texts) {
            val low = t.lowercase()
            // exact phrase matching is brittle; consider fuzzy or wakeword + command recognizer later
            if (low.contains("king assistant go to your form") || low.contains("king assistant go to your form".lowercase())) {
                showOverlay()
                return
            }
            if (low.contains("ek ho") || low.contains("এক হও") || low.contains("ekho")) {
                hideOverlay()
                return
            }
        }
    }

    companion object {
        const val ACTION_STOP_SERVICE = "king.overlay.STOP"
        const val EXTRA_SHOW_OVERLAY = "extra_show_overlay"

        const val ACTION_OVERLAY_SHOW = "king.overlay.SHOWED"
        const val ACTION_OVERLAY_HIDE = "king.overlay.HIDDEN"
    }
}
