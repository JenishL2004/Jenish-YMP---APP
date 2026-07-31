package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = YamahaBlueLight,
    onPrimary = Color.White,
    primaryContainer = YamahaBlueDark,
    onPrimaryContainer = Color.White,
    secondary = YamahaRed,
    onSecondary = Color.White,
    tertiary = IndustrialSteel,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = YamahaRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = YamahaBlue,
    onPrimary = Color.White,
    primaryContainer = YamahaBlueLight,
    onPrimaryContainer = Color.White,
    secondary = YamahaRed,
    onSecondary = Color.White,
    tertiary = IndustrialSteel,
    background = LightBackground,
    surface = LightSurface,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF334155),
    error = YamahaRed,
    onError = Color.White
)

@Composable
fun YamahaPatrolTheme(
    darkTheme: Boolean = false, // Strictly Light Theme Only
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

