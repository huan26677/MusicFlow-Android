package com.example.musicflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = MusicFlowBlue,
    onPrimary = Color.White,
    secondary = Color(0xFF1D4ED8),
    background = MusicFlowLightBackground,
    surface = Color.White,
    onSurface = Color(0xFF0F172A)
)

private val DarkColors = darkColorScheme(
    primary = MusicFlowBlueDark,
    onPrimary = Color(0xFF0B1220),
    secondary = Color(0xFF60A5FA),
    background = MusicFlowDarkBackground,
    surface = Color(0xFF111827),
    onSurface = Color(0xFFE5E7EB)
)

@Composable
fun MusicFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}