package com.shahrafuking.kingassistant.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.shahrafuking.kingassistant.core.RobotEngine
import com.shahrafuking.kingassistant.security.VoiceAuthStub

class MainActivity : ComponentActivity() {
    private val robotEngine = RobotEngine(this)
    private lateinit var voiceAuth: VoiceAuthStub

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voiceAuth = VoiceAuthStub(this)

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            // handle permission result
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            MaterialTheme {
                val authenticated = remember { mutableStateOf(false) }

                Scaffold(
                    topBar = { TopAppBar(title = { Text("King Assistant") }) },
                    drawerContent = { com.shahrafuking.kingassistant.ui.screens.SettingsDrawer() }
                ) { padding ->
                    com.shahrafuking.kingassistant.ui.screens.HomeScreen(
                        authenticated = authenticated.value,
                        onStartAuth = {
                            // start voice auth
                            voiceAuth.startAuth { success ->
                                authenticated.value = success
                            }
                        },
                        onSendText = { input ->
                            val response = robotEngine.processTextInput(input)
                            // simple Toast-like response could be implemented; left as scaffold
                        }
                    )
                }
            }
        }
    }
}
