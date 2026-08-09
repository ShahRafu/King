package com.shahrafuking.kingassistant.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.shahrafuking.kingassistant.R
import com.shahrafuking.kingassistant.voice.SpeechRecognizerHelper
import com.shahrafuking.kingassistant.voice.TextToSpeechHelper
import com.shahrafuking.kingassistant.voice.HotwordDetector

class VoiceMonitorService : Service() {
    private val TAG = "VoiceMonitorService"
    private var recognizerHelper: SpeechRecognizerHelper? = null
    private var ttsHelper: TextToSpeechHelper? = null
    private var hotword: HotwordDetector? = null

    override fun onCreate() {
        super.onCreate()
        recognizerHelper = SpeechRecognizerHelper(applicationContext)
        ttsHelper = TextToSpeechHelper(applicationContext)
        hotword = HotwordDetector { detected -> onHotwordDetected() }
        startForegroundCompat()
    }

    private fun startForegroundCompat() {
        val notif = NotificationCompat.Builder(this, "king_assistant_channel")
            .setContentTitle("King Assistant")
            .setContentText("Voice monitoring active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(1001, notif as Notification)
    }

    private fun onHotwordDetected() {
        recognizerHelper?.startListening(object : SpeechRecognizerHelper.SpeechCallback {
            override fun onResult(text: String) {
                Log.d(TAG, "User said: $text")
                com.shahrafuking.kingassistant.commands.CommandDispatcher.dispatch(applicationContext, text)
            }
            override fun onError(error: String) { Log.w(TAG, "Speech error: $error") }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onDestroy() { recognizerHelper?.destroy(); ttsHelper?.shutdown(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
