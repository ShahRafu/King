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
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

/**
 * OverlayService: Foreground Service that creates a system overlay robot view
 * and runs a SpeechRecognizer to toggle the overlay using voice commands.
 *
 * Notes:
 * - Requires user to grant SYSTEM_ALERT_WINDOW (draw over apps) via Settings.
 * - Requires RECORD_AUDIO runtime permission before starting speech recognition.
 * - Use this for POC only; Play Store policies require explicit UX & disclosure.
 */
class OverlayService : Service() {
    private val TAG = "OverlayService"
    private val CHANNEL_ID = "king_overlay_channel"
    private val NOTIF_ID = 1337

    private var windowManager: WindowManager? = null
    private var overlayRoot: FrameLayout? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isOverlayShown = false
    private var listenJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForeground(NOTIF_ID, createNotification())

        // Start speech recognition loop (only if audio permission granted)
        startSpeechRecognitionLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Optionally start overlay immediately if intent instructs
        val show = intent?.getBooleanExtra(EXTRA_SHOW_OVERLAY, false) ?: false
        if (show) showOverlay()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        stopSpeechRecognitionLoop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "King Overlay", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(channel)
        }
        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val pending = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("King Assistant")
            .setContentText("Overlay service running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pending)
            .build()
    }

    private fun showOverlay() {
        if (isOverlayShown) return
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission not granted")
            return
        }

        overlayRoot = FrameLayout(this).apply {
            alpha = 0.99f
            // Transparent background
            setBackgroundColor(0x00000000)
        }

        // Simple robot view: a TextView with emoji; replace with animated view or custom layout
        val robotView = TextView(this).apply {
            text = "\uD83E\uDD16" // robot emoji
            textSize = 48f
            setPadding(16, 16, 16, 16)
            // optional background circle
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
                        windowManager?.updateViewLayout(v, lp)
                        return true
                    }
                }
                return false
            }
        })

        // Container for robot (to animate or add more elements)
        overlayRoot?.addView(robotView)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            // allow touches to this view, but let underlying windows receive events outside region
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        try {
            windowManager?.addView(overlayRoot, params)
            isOverlayShown = true
            // Optional: start a simple walking animation coroutine
            startWalkingAnimation(robotView, params)
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
            }
        } catch (t: Throwable) {
            Log.w(TAG, "hide error", t)
        }
    }

    // Simple periodic movement (walk) across screen margins
    private fun startWalkingAnimation(robotView: View, params: WindowManager.LayoutParams) {
        // Run a coroutine to slowly nudge robot left-right
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val screenWidth = resources.displayMetrics.widthPixels
                var dir = 1
                while (isOverlayShown) {
                    delay(1500)
                    params.x = (0.1f * screenWidth).toInt() + (dir * (0.6f * screenWidth).toInt())
                    try {
                        windowManager?.updateViewLayout(robotView, params)
                    } catch (_: Exception) { }
                    dir *= -1
                }
            } catch (t: CancellationException) {
                // ignore
            } catch (t: Throwable) {
                Log.w(TAG, "walk anim error", t)
            }
        }
    }

    // Speech recognition loop: continuously listen and restart when needed.
    private fun startSpeechRecognitionLoop() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.w(TAG, "Speech recognition not available")
            return
        }
        stopSpeechRecognitionLoop()
        listenJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                try {
                    startSingleRecognition()
                    // wait a bit between sessions to avoid brief errors
                    delay(500)
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
        // ensure audio permission granted by host Activity before invoking this method
        val recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer = recognizer
        val intent = RecognizerIntent().apply {
            action = RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { }
            override fun onBeginningOfSpeech() { }
            override fun onRmsChanged(rmsdB: Float) { }
            override fun onBufferReceived(buffer: ByteArray?) { }
            override fun onEndOfSpeech() { }
            override fun onError(error: Int) {
                Log.w(TAG, "recognizer error: $error")
                recognizer.cancel()
                recognizer.destroy()
            }
            override fun onResults(results: Bundle?) {
                val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
                handleRecognizedTexts(texts)
                recognizer.destroy()
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val texts = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
                handleRecognizedTexts(texts)
            }
            override fun onEvent(eventType: Int, params: Bundle?) { }
        })
        recognizer.startListening(intent)
    }

    private fun handleRecognizedTexts(texts: ArrayList<String>) {
        for (t in texts) {
            val low = t.lowercase()
            if (low.contains("king assistant go to your form") || low.contains("king assistant go to your form".lowercase())) {
                // show overlay and optionally close/hide main UI (activity)
                showOverlay()
                // optional broadcast so Activity can hide itself
                sendBroadcast(Intent(ACTION_OVERLAY_SHOW))
                return
            }
            if (low.contains("ek ho") || low.contains("এক হও") || low.contains("ekho")) {
                // hide overlay and notify UI
                hideOverlay()
                sendBroadcast(Intent(ACTION_OVERLAY_HIDE))
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
