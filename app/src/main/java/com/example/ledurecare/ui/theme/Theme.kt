package com.example.ledurecare.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Green500,
    onPrimary = Color.White,
    error = ErrorRed,
)

private val LightColorScheme = lightColorScheme(
    primary = Green500,
    onPrimary = Color.White,
    secondary = Green600,
    onBackground = Ink,
    onSurface = Ink,
    error = ErrorRed,
)

@Composable
fun LedureCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
