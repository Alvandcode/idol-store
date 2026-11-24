package com.idol.store.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Indigo = Color(0xFF6366F1)
private val IndigoLight = Color(0xFF818CF8)
private val Cyan = Color(0xFF22D3EE)
private val DarkBg = Color(0xFF0F1115)
private val DarkSurface = Color(0xFF171A21)
private val DarkSurfaceHigh = Color(0xFF222733)

private val DarkColors = darkColorScheme(
    primary = IndigoLight,
    onPrimary = Color.White,
    secondary = Cyan,
    background = DarkBg,
    onBackground = Color(0xFFE7E9EE),
    surface = DarkSurface,
    onSurface = Color(0xFFE7E9EE),
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF3A4150),
    error = Color(0xFFF87171),
)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    secondary = Color(0xFF0891B2),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEEF2F7),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
)

@Composable
fun IdolStoreTheme(
    content: @Composable () -> Unit,
) {
    val darkTheme = when (Prefs.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
