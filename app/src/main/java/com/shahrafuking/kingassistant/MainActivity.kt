package com.shahrafuking.kingassistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shahrafuking.kingassistant.ui.HotwordTestActivity

/**
 * Simple launcher activity that opens HotwordTestActivity.
 * Keeps app manifest consistent with a MAIN/LAUNCHER activity.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                // Simple button to open HotwordTestActivity
                Button(modifier = Modifier.padding(24.dp), onClick = {
                    startActivity(Intent(this, HotwordTestActivity::class.java))
                }) {
                    Text("Open Hotword Test")
                }
            }
        }
    }
}
