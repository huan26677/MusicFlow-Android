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
    primaryContainer = Color(0xFFBFF3E9),
    onPrimaryContainer = Color(0xFF063B37),
    secondary = Color(0xFF925900),
    onSecondary = Color.White,
    background = MusicFlowLightBackground,
    surface = Color.White,
    onSurface = Color(0xFF142521)
)

private val DarkColors = darkColorScheme(
    primary = MusicFlowBlueDark,
    onPrimary = Color(0xFF0B1220),
    primaryContainer = Color(0xFF174D48),
    onPrimaryContainer = Color(0xFFBFF3E9),
    secondary = Color(0xFFFFC46A),
    onSecondary = Color(0xFF3B2500),
    background = MusicFlowDarkBackground,
    surface = Color(0xFF162334),
    onSurface = Color(0xFFE5F1F0)
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
