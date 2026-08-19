package com.shahrafuking.kingassistant.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun VoiceOrb(probability: Int = 0, budgetText: String = "--", onClick: () -> Unit = {}, mode: RaghuPreviewMode = RaghuPreviewMode.EXTERNAL) {
    // simple pulsing animation to imply listening/processing
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(animation = tween(900, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse)
    )

    val outerSize = when(mode) {
        RaghuPreviewMode.COMPACT -> 120.dp
        RaghuPreviewMode.EXPANDED -> 220.dp
        RaghuPreviewMode.EXTERNAL -> 180.dp
    }

    val innerBase = when(mode) {
        RaghuPreviewMode.COMPACT -> 48f
        RaghuPreviewMode.EXPANDED -> 88f
        RaghuPreviewMode.EXTERNAL -> 64f
    }

    Box(modifier = Modifier
        .size(outerSize)
        .clip(CircleShape)
        .background(Color(0xFF314B8A))
        .padding(12.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("King Assistant", color = Color.White, style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier
                .size((innerBase * scale).dp)
                .clip(CircleShape)
                .background(Color(0xFFFF6B6B)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Mic, contentDescription = "Mic", tint = Color.White)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("Prob: $probability%", color = Color.White)
            Text("Budget: $budgetText", color = Color.White)

            if (mode == RaghuPreviewMode.EXPANDED) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Detailed status: listening, model ready", color = Color.White, maxLines = 2)
            }

            if (mode == RaghuPreviewMode.EXTERNAL) {
                Spacer(modifier = Modifier.height(8.dp))
                // External preview button is a placeholder; integrate with your external preview Intent if available
                androidx.compose.material.Button(onClick = onClick) {
                    androidx.compose.material.Text("Open External Preview")
                }
            }
        }
    }
}
