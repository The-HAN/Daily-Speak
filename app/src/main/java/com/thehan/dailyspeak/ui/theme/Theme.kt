package com.thehan.dailyspeak.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = DailySpeakGreen,
    onPrimary = LightSurface,
    primaryContainer = DailySpeakGreenContainer,
    onPrimaryContainer = LightOnSurface,
    secondary = DailySpeakWarm,
    secondaryContainer = DailySpeakWarmContainer,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = DailySpeakGreenDark,
    onPrimary = DarkBackground,
    primaryContainer = DailySpeakGreenContainerDark,
    onPrimaryContainer = DarkOnSurface,
    secondary = ColorTokens.DarkSecondary,
    secondaryContainer = ColorTokens.DarkSecondaryContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
)

private object ColorTokens {
    val DarkSecondary = androidx.compose.ui.graphics.Color(0xFFE6B8A5)
    val DarkSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF5A3F34)
}

@Composable
fun DailySpeakTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = DailySpeakTypography,
        content = content,
    )
}