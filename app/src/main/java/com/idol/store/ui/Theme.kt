package com.idol.store.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Indigo = Color(0xFF6366F1)
private val IndigoLight = Color(0xFF818CF8)
private val DarkBg = Color(0xFF0F1115)
private val DarkSurface = Color(0xFF171A21)
private val DarkSurfaceHigh = Color(0xFF222733)

private val DarkColors = darkColorScheme(
    primary = IndigoLight,
    onPrimary = Color.White,
    secondary = Color(0xFF22D3EE),
    background = DarkBg,
    onBackground = Color(0xFFE7E9EE),
    surface = DarkSurface,
    onSurface = Color(0xFFE7E9EE),
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF3A4150),
)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
)

@Composable
fun IdolStoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
