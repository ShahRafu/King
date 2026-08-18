package com.shahrafuking.kingassistant.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.shahrafuking.kingassistant.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val ctx = this
            val prefs = ctx.getSharedPreferences("king_prefs", Context.MODE_PRIVATE)
            val darkPref = remember { mutableStateOf(prefs.getBoolean("dark_theme", false)) }

            AppTheme(darkTheme = darkPref.value) {
                MaterialTheme {
                    // reuse existing scaffold & home wiring from previous MainActivity UI file
                    val authenticated = remember { mutableStateOf(false) }
                    val scaffoldState = rememberScaffoldState()
                    val scope = rememberCoroutineScope()

                    Scaffold(
                        scaffoldState = scaffoldState,
                        topBar = { TopAppBar(title = { Text("King Assistant") }) },
                        drawerContent = { com.shahrafuking.kingassistant.ui.screens.SettingsDrawer() }
                    ) { padding ->
                        com.shahrafuking.kingassistant.ui.screens.HomeScreen(
                            authenticated = authenticated.value,
                            onStartAuth = {
                                authenticated.value = true
                                try {
                                    voiceAuth.startAuth { success -> }
                                } catch (_: Throwable) {}
                            },
                            onSendText = { input ->
                                val response = robotEngine.processTextInput(input)
                                android.widget.Toast.makeText(this@MainActivity, response, android.widget.Toast.LENGTH_LONG).show()
                            },
                            onOpenSettings = {
                                scope.launch { scaffoldState.drawerState.open() }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAppRoot() {
    AppTheme {
        Text("Preview King Assistant")
    }
}
