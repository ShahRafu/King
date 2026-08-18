package com.shahrafuking.kingassistant.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val LightPalette = lightColors(
    primary = androidx.compose.ui.graphics.Color(0xFF0D47A1),
    primaryVariant = androidx.compose.ui.graphics.Color(0xFF002171),
    secondary = androidx.compose.ui.graphics.Color(0xFF00ACC1)
)

private val DarkPalette = darkColors(
    primary = androidx.compose.ui.graphics.Color(0xFF90CAF9)
)

/**
 * AppTheme wrapper
 * - Use this to control app-wide colors and theming
 * - Also central place to add typography and shapes
 */
@Composable
fun AppTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (!darkTheme) LightPalette else DarkPalette
    MaterialTheme(colors = colors, content = content)
}
