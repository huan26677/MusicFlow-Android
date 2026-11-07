package com.example.musicflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = MusicFlowBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBFF3E9),
    onPrimaryContainer = Color(0xFF063B37),
    secondary = Color(0xFF925900),
    onSecondary = Color.White,
    background = MusicFlowLightBackground,
    surface = Color.White,
    onSurface = Color(0xFF142521),
    onBackground = Color(0xFF142521),
    onSurfaceVariant = Color(0xFF47625D),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F7F5),
    surfaceContainer = Color(0xFFE6F2EF),
    surfaceContainerHigh = Color(0xFFDDEDE9),
    outlineVariant = Color(0xFFC3DAD4),
    tertiary = Color(0xFF285D8A),
    onTertiary = Color.White
)

private val DarkColors = darkColorScheme(
    primary = MusicFlowBlueDark,
    onPrimary = Color(0xFF0B1220),
    primaryContainer = Color(0xFF174D48),
    onPrimaryContainer = Color(0xFFBFF3E9),
    secondary = Color(0xFFFFC46A),
    onSecondary = Color(0xFF3B2500),
    background = MusicFlowDarkBackground,
    surface = OceanNightSurface,
    onSurface = Color(0xFFE5F1F0),
    onBackground = Color(0xFFE5F1F0),
    onSurfaceVariant = Color(0xFFB0C9C7),
    surfaceContainerLowest = Color(0xFF0C1422),
    surfaceContainerLow = Color(0xFF19293A),
    surfaceContainer = Color(0xFF1D3040),
    surfaceContainerHigh = Color(0xFF23394A),
    outlineVariant = Color(0xFF3A5560),
    tertiary = OceanSky,
    onTertiary = OceanNavy
)

private val OceanShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun MusicFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = OceanShapes,
        content = content
    )
}
