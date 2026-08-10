package com.shahrafuking.kingassistant.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
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
import com.shahrafuking.kingassistant.speech.ChallengeGenerator
import com.shahrafuking.kingassistant.speech.AndroidTtsHelper
import com.shahrafuking.kingassistant.voice.VoiceEnrollmentManager
import com.shahrafuking.kingassistant.voice.VoiceSecurityManager
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * OverlayService — updated to implement strict owner unlock flow:
 * 1) Hotword detected -> show overlay and fetch challenge text
 * 2) Display challenge on overlay and prompt Owner to read
 * 3) Capture Owner speech, run ASR -> ensure transcript matches challenge
 * 4) Run biometric verification + liveness -> only on success dismiss overlay and unlock
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
    private var ttsHelper: AndroidTtsHelper? = null

    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val commandInProgress = AtomicBoolean(false)
    private var isOverlayShown = AtomicBoolean(false)

    private var voiceSecurityManager: VoiceSecurityManager? = null
    private var enrollmentManager: VoiceEnrollmentManager? = null

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

        ttsHelper = AndroidTtsHelper(applicationContext)
        voiceSecurityManager = VoiceSecurityManager(applicationContext, modelAssetPath = "sample_speaker_model.tflite")
        enrollmentManager = VoiceEnrollmentManager(applicationContext)

        hotwordManager = HotwordManager(applicationContext)
        hotwordManager?.setListener(object : HotwordManager.HotwordListener {
            override fun onHotwordDetected() {
                Log.i(TAG, "Hotword detected — entering strict unlock flow")
                mainScope.launch { startStrictUnlockFlow() }
            }
        })

        try { hotwordManager?.start() } catch (t: Throwable) { Log.w(TAG, "hotword start failed", t) }

        val filter = IntentFilter().apply { addAction(ACTION_PANIC_STOP) }
        registerReceiver(panicReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_SERVICE) { stopSelf(); return START_NOT_STICKY }
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
        try { ttsHelper?.shutdown() } catch (_: Throwable) {}
        mainScope.cancel()
        try { voiceSecurityManager?.close() } catch (_: Throwable) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "King Overlay", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(channel)
        }
        val stopIntent = Intent(this, OverlayService::class.java).apply { action = ACTION_STOP_SERVICE }
        val pendingStop = PendingIntent.getService(this, 0, stopIntent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("King Assistant")
            .setContentText("Overlay service running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pendingStop)
            .build()
    }

    private suspend fun startStrictUnlockFlow() {
        if (commandInProgress.get()) return
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "overlay permission not granted; cannot show unlock overlay")
            return
        }
        commandInProgress.set(true)
        try {
            showOverlay()
            // 1) fetch challenge text
            val challenge = withContext(Dispatchers.IO) { ChallengeGenerator.fetchChallenge(applicationContext) }
            displayChallengeOnOverlay(challenge)

            // 2) prompt owner to read and capture ASR + feature embedding + raw PCM
            val asrResult = captureOwnerSpeechAsr(5000)
            if (asrResult == null) {
                ttsHelper?.speak("শোনাতে পারিনি, আবার বলুন")
                cleanupCommandSession(); return
            }
            // extract features via enrollmentManager.recordAndExtract would require a separate recording; instead we capture raw PCM for both features and antispoof
            val pcmShorts = recordShortPcmBlocking(1800)
            val featuresDouble = enrollmentManager?.let { withContext(Dispatchers.Default) { it.recordAndExtract(1800) } }
            val featuresFloat = featuresDouble?.let { arr -> FloatArray(arr.size) { i -> arr[i].toFloat() } } ?: FloatArray(0)

            // 3) verify both text match & biometric & liveness
            voiceSecurityManager?.verifyChallenge(challenge, asrResult, featuresFloat, pcmShorts) { passed, sim, liveScore, textMatch ->
                if (passed && textMatch) {
                    // success: dismiss overlay and unlock
                    ttsHelper?.speak("অভিনন্দন, সিস্টেম আনলক করা হলো")
                    hideOverlay()
                    // optionally send broadcast that system unlocked
                    sendBroadcast(Intent(ACTION_OVERLAY_HIDE))
                } else {
                    ttsHelper?.speak("ভুল বা পরিচয় মিলেনি — আনলক ব্যর্থ")
                    // keep overlay visible but maybe regenerate challenge after short delay
                    mainScope.launch { delay(1200); displayChallengeOnOverlay(ChallengeGenerator.fetchChallenge(applicationContext)) }
                }
                cleanupCommandSession()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "strict unlock flow failed", t)
            cleanupCommandSession()
        }
    }

    private fun displayChallengeOnOverlay(text: String) {
        try {
            // simple TextView update on overlay root (we created robotView earlier). Replace root contents with challenge
            val tv = TextView(this).apply {
                this.text = text
                textSize = 20f
                setPadding(24, 24, 24, 24)
                setBackgroundResource(android.R.drawable.alert_light_frame)
            }
            overlayRoot?.removeAllViews()
            overlayRoot?.addView(tv)
        } catch (t: Throwable) { Log.w(TAG, "displayChallenge failed", t) }
    }

    private fun recordShortPcmBlocking(durationMs: Long, sampleRate: Int = 16000): ShortArray? {
        // Blocking wrapper around AudioRecorder similar to earlier helper
        try {
            val recorder = com.shahrafuking.kingassistant.audio.AudioRecorder(this)
            if (!recorder.hasRecordPermission()) return null
            val list = mutableListOf<Short>()
            recorder.start({ pcmChunk, sr -> synchronized(list) { for (s in pcmChunk) list.add(s) } }, sampleRate)
            Thread.sleep(durationMs)
            recorder.stop()
            synchronized(list) {
                val arr = ShortArray(list.size)
                for (i in list.indices) arr[i] = list[i]
                return arr
            }
        } catch (t: Throwable) {
            Log.w(TAG, "recordShortPcmBlocking failed", t)
            return null
        }
    }

    private suspend fun captureOwnerSpeechAsr(timeoutMs: Long = 4000): String? = withContext(Dispatchers.Main) {
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(this@OverlayService)) return@withContext null
            val sr = SpeechRecognizer.createSpeechRecognizer(this@OverlayService)
            val intent = RecognizerIntent().apply {
                action = RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            var result: String? = null
            val latch = java.lang.Object()
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    synchronized(latch) { latch.notify() }
                }
                override fun onResults(results: Bundle?) {
                    val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    result = texts?.firstOrNull()
                    synchronized(latch) { latch.notify() }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            sr.startListening(intent)
            // wait for result or timeout
            synchronized(latch) { latch.wait(timeoutMs) }
            try { sr.cancel(); sr.destroy() } catch (_: Throwable) {}
            return@withContext result
        } catch (t: Throwable) {
            Log.w(TAG, "captureOwnerSpeechAsr failed", t); return@withContext null
        }
    }

    private fun cleanupCommandSession() {
        try { commandRecognizer?.cancel() } catch (_: Throwable) {}
        try { conversationRecognizer?.destroy() } catch (_: Throwable) {}
        commandRecognizer = null
        conversationRecognizer = null
        commandInProgress.set(false)
        // resume hotword listening
        mainScope.launch { delay(500); try { hotwordManager?.start() } catch (_: Throwable) {} }
    }

    private fun showOverlay() {
        if (isOverlayShown.get()) return
        overlayRoot = FrameLayout(this).apply { setBackgroundColor(0x00000000) }
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, layoutFlag, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = 100; y = 300 }
        try { windowManager?.addView(overlayRoot, params); isOverlayShown.set(true); sendBroadcast(Intent(ACTION_OVERLAY_SHOW)) } catch (t: Throwable) { Log.e(TAG, "addView failed", t) }
    }

    private fun hideOverlay() {
        try {
            if (isOverlayShown.get()) {
                windowManager?.removeView(overlayRoot)
                overlayRoot = null
                isOverlayShown.set(false)
                sendBroadcast(Intent(ACTION_OVERLAY_HIDE))
            }
        } catch (t: Throwable) { Log.w(TAG, "hide error", t) }
    }

    private fun performPanicStop() {
        try { hotwordManager?.stop() } catch (_: Throwable) {}
        try { conversationRecognizer?.cancel(); conversationRecognizer?.destroy() } catch (_: Throwable) {}
        try { commandRecognizer?.cancel() } catch (_: Throwable) {}
        try { ttsHelper?.shutdown() } catch (_: Throwable) {}
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
