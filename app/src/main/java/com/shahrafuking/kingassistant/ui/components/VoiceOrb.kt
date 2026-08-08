package com.shahrafuking.kingassistant.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun VoiceOrb(isActive: Boolean, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition()
    val pulse by infinite.animateFloat(
        initialValue = 0.6f,
        targetValue = if (isActive) 1.2f else 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isActive) 800 else 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(modifier = modifier.size(140.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(140.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 3f
            // pulsing background rings
            drawCircle(color = Color(0xFF2E7D32).copy(alpha = 0.12f * pulse), radius = radius * 1.6f, center = center)
            drawCircle(color = Color(0xFF2E7D32).copy(alpha = 0.20f * pulse), radius = radius * 1.2f, center = center)
            drawCircle(color = Color(0xFF2E7D32), radius = radius, center = center)
        }

        // overlay mic icon
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Mic",
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )
    }
}
