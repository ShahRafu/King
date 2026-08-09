// OverlayService.kt
package com.shahrafuking.kingassistant.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
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
import com.shahrafuking.kingassistant.speech.LanguageTranslator
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * OverlayService (production-ready, robust)
 *
 * - Foreground service with notification channel
 * - HotwordManager hotword detection (Porcupine optional / SpeechRecognizer fallback)
 * - Conversation flow:
 *     1) Hotword detected -> capture incoming person's utterance (system ASR)
 *     2) Detect language & translate -> speak Bengali to owner
 *     3) Capture owner's Bengali reply -> translate back to detected language and speak
 *
 * Safety & lifecycle:
 * - Proper start/stop handling
 * - Panic intent handling: ACTION_PANIC_STOP broadcasts force immediate cleanup
 */
class OverlayService : Service() {
    private val TAG = "OverlayService"
    private val CHANNEL_ID = "king_overlay_channel"
    private val NOTIF_ID = 1337

    private var windowManager: WindowManager? = null
    private var overlayRoot: FrameLayout? = null

    private var hotwordManager: HotwordManager? = null
    private var commandRecognizer: CommandRecognizer? = null
    private var conversationRecognizer: SpeechRecognizer? = null
    private var languageTranslator: LanguageTranslator? = null

    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val commandInProgress = AtomicBoolean(false)
    private var isOverlayShown = AtomicBoolean(false)

    private val panicReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_PANIC_STOP) {
                Log.i(TAG, "Panic stop received")
                performPanicStop()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForeground(NOTIF_ID, createNotification())

        languageTranslator = LanguageTranslator(applicationContext)

        hotwordManager = HotwordManager(applicationContext)
        hotwordManager?.setListener(object : HotwordManager.HotwordListener {
            override fun onHotwordDetected() {
                Log.i(TAG, "Hotword detected")
                mainScope.launch { onHotwordTriggered() }
            }
        })

        try {
            hotwordManager?.start()
        } catch (t: Throwable) {
            Log.w(TAG, "hotword start failed", t)
        }

        // register receiver for panic stop (and potential future actions)
        val filter = IntentFilter().apply {
            addAction(ACTION_PANIC_STOP)
        }
        registerReceiver(panicReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
        try { unregisterReceiver(panicReceiver) } catch (_: Exception) {}
        hideOverlay()
        try { hotwordManager?.stop() } catch (_: Throwable) {}
        try { commandRecognizer?.cancel() } catch (_: Throwable) {}
        try { conversationRecognizer?.destroy() } catch (_: Throwable) {}
        try { languageTranslator?.shutdown() } catch (_: Throwable) {}
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

    // ---------------- Overlay UI ----------------
    fun showOverlay() {
        if (isOverlayShown.get()) return
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission not granted")
            return
        }

        overlayRoot = FrameLayout(this).apply { setBackgroundColor(0x00000000) }

        val robotView = TextView(this).apply {
            text = "\uD83E\uDD16"
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
                        lastX = event.rawX; lastY = event.rawY
                        initialX = lp.x; initialY = lp.y
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - lastX).toInt()
                        val dy = (event.rawY - lastY).toInt()
                        lp.x = initialX + dx; lp.y = initialY + dy
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
        else WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100; y = 300
        }

        try {
            windowManager?.addView(overlayRoot, params)
            isOverlayShown.set(true)
            startWalkingAnimation(robotView, params)
            sendBroadcast(Intent(ACTION_OVERLAY_SHOW))
        } catch (t: Throwable) {
            Log.e(TAG, "addView failed", t)
        }
    }

    private fun hideOverlay() {
        try {
            if (isOverlayShown.get()) {
                windowManager?.removeView(overlayRoot)
                overlayRoot = null
                isOverlayShown.set(false)
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
                while (isOverlayShown.get() && isActive) {
                    delay(1400)
                    params.x = (0.05f * screenWidth).toInt() + (if (dir > 0) (0.6f * screenWidth).toInt() else 0)
                    try { windowManager?.updateViewLayout(robotView, params) } catch (_: Exception) {}
                    dir *= -1
                }
            } catch (_: CancellationException) { }
            catch (t: Throwable) { Log.w(TAG, "walk anim error", t) }
        }
    }

    // ---------------- Conversation flow ----------------
    private suspend fun onHotwordTriggered() {
        if (commandInProgress.get()) {
            Log.i(TAG, "Command in progress; ignoring hotword")
            return
        }

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "overlay permission missing; aborting hotword flow")
            return
        }

        showOverlay()
        try { hotwordManager?.stop() } catch (_: Throwable) {}

        commandInProgress.set(true)

        // 1) capture incoming person's utterance (auto language)
        captureOneShotIncomingSpeech { incomingText ->
            mainScope.launch {
                try {
                    if (incomingText.isNullOrBlank()) {
                        Log.i(TAG, "No incoming speech; prompt owner reply directly")
                        promptOwnerReplyAndRelay("und")
                    } else {
                        // detect & translate to Bengali
                        val pair = try {
                            // detect language then translate to Bengali via translator
                            runCatching { runBlocking { languageTranslator?.detectAndTranslateTo(incomingText, "bn") } }.getOrNull()
                        } catch (t: Throwable) {
                            Log.w(TAG, "detect/translate error", t); null
                        }
                        val detectedLang = pair?.first ?: "und"
                        val bengaliText = pair?.second ?: incomingText
                        Log.i(TAG, "Detected: $detectedLang -> Bengali: $bengaliText")

                        // speak Bengali to owner
                        if (!bengaliText.isNullOrBlank()) {
                            languageTranslator?.speakSafely(bengaliText, "bn-BD")
                        }
                        // then prompt owner to reply and relay
                        promptOwnerReplyAndRelay(detectedLang)
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "conversation error", t)
                    cleanupCommandSession()
                }
            }
        }
    }

    /**
     * Capture one incoming utterance (no forced language).
     * Returns recognized text or null via callback.
     */
    private fun captureOneShotIncomingSpeech(cb: (String?) -> Unit) {
        try { conversationRecognizer?.destroy() } catch (_: Throwable) {}
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.w(TAG, "SpeechRecognizer unavailable for incoming capture")
            cb(null); return
        }
        conversationRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = RecognizerIntent().apply {
            action = RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            // DO NOT force language -> let system return best text
        }
        conversationRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Log.w(TAG, "incoming recognizer error: $error")
                try { conversationRecognizer?.destroy() } catch (_: Throwable) {}
                cb(null)
            }
            override fun onResults(results: Bundle?) {
                val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val result = texts?.firstOrNull()
                try { conversationRecognizer?.destroy() } catch (_: Throwable) {}
                cb(result)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        try {
            conversationRecognizer?.startListening(intent)
            // safety fallback timeout
            mainScope.launch {
                delay(7000)
                try {
                    conversationRecognizer?.cancel()
                    conversationRecognizer?.destroy()
                } catch (_: Throwable) {}
                cb(null)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "startListening incoming failed", t)
            try { conversationRecognizer?.destroy() } catch (_: Throwable) {}
            cb(null)
        }
    }

    /**
     * Prompt owner to reply (captured in Bengali) and relay back to targetLang.
     */
    private fun promptOwnerReplyAndRelay(targetLang: String) {
        try { commandRecognizer?.cancel() } catch (_: Throwable) {}
        commandRecognizer = CommandRecognizer(applicationContext)
        commandRecognizer?.listenOnce(object : CommandRecognizer.CommandListener {
            override fun onCommandResult(text: String) {
                Log.i(TAG, "Owner replied (bn): $text")
                mainScope.launch {
                    try {
                        if (targetLang == "bn" || targetLang.startsWith("bn") || targetLang == "und") {
                            Log.i(TAG, "No back-translation (target=$targetLang)")
                            // Optionally speak owner's reply back or log
                        } else {
                            // translate owner reply BN -> targetLang and speak
                            val translated = try { languageTranslator?.translateOwnerReplyAndSpeak(text, targetLang) } catch (t: Throwable) { "" }
                            Log.i(TAG, "Relayed owner reply -> $targetLang: $translated")
                        }
                    } catch (t: Throwable) {
                        Log.w(TAG, "reply relay error", t)
                    } finally {
                        cleanupCommandSession()
                    }
                }
            }

            override fun onCommandError(reason: String) {
                Log.w(TAG, "Owner command error: $reason")
                cleanupCommandSession()
            }
        })
    }

    private fun cleanupCommandSession() {
        try { commandRecognizer?.cancel() } catch (_: Throwable) {}
        try { conversationRecognizer?.destroy() } catch (_: Throwable) {}
        commandRecognizer = null
        conversationRecognizer = null
        commandInProgress.set(false)
        // restart hotword listening after brief delay
        mainScope.launch {
            delay(500)
            try { hotwordManager?.start() } catch (_: Throwable) {}
        }
    }

    private fun performPanicStop() {
        // Immediate aggressive cleanup: stop hotword, cancel recognizers, stop speaking, hide overlay
        try { hotwordManager?.stop() } catch (_: Throwable) {}
        try { conversationRecognizer?.cancel(); conversationRecognizer?.destroy() } catch (_: Throwable) {}
        try { commandRecognizer?.cancel() } catch (_: Throwable) {}
        try { languageTranslator?.shutdown() } catch (_: Throwable) {}
        try { hideOverlay() } catch (_: Throwable) {}
        commandInProgress.set(false)
    }

    companion object {
        const val ACTION_STOP_SERVICE = "king.overlay.STOP"
        const val EXTRA_SHOW_OVERLAY = "extra_show_overlay"
        const val ACTION_OVERLAY_SHOW = "king.overlay.SHOWED"
        const val ACTION_OVERLAY_HIDE = "king.overlay.HIDDEN"
        const val ACTION_PANIC_STOP = "king.overlay.PANIC_STOP"
    }
}
