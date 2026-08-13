name=app/src/main/java/com/shahrafuking/kingassistant/hotword/WakewordService.kt url=https://github.com/ShahRafu/King/blob/main/app/src/main/java/com/shahrafuking/kingassistant/hotword/WakewordService.kt
package com.shahrafuking.kingassistant.hotword

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.shahrafuking.kingassistant.audio.AudioRecorder
import com.shahrafuking.kingassistant.speech.HotwordEngineAdapter
import kotlinx.coroutines.*

/**
 * Foreground service that runs the audio recorder and hotword engine.
 * - Starts AudioRecorder
 * - Pipes PCM frames to HotwordEngineAdapter
 *
 * NOTE: This is a skeleton for POC. Add real Porcupine (Picovoice) dependency
 * and PORCOVINE_KEY via local.properties or GitHub Secret before enabling.
 */
class WakewordService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var recorder: AudioRecorder
    private lateinit var engine: HotwordEngineAdapter

    override fun onCreate() {
        super.onCreate()
        recorder = AudioRecorder(applicationContext)
        engine = HotwordEngineAdapter.getInstance(applicationContext)
        startForegroundServiceNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            // explicitly annotate lambda parameter types so compiler can infer correctly
            recorder.start({ pcmBuffer: ShortArray, sampleRate: Int ->
                // Pass audio buffer to hotword engine
                val detected = engine.process(pcmBuffer, sampleRate)
                if (detected) {
                    // TODO: trigger authentication flow or notify UI
                    engine.onHotwordDetected()
                }
            }, AudioRecorder.DEFAULT_SAMPLE_RATE)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        recorder.stop()
        engine.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundServiceNotification() {
        val channelId = "king_wakeword_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "King Wakeword Service", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(channel)
        }
        val notif: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("King Assistant")
            .setContentText("Wakeword listening")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
        startForeground(1, notif)
    }
}
