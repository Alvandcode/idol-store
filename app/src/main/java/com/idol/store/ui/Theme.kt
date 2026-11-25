package com.idol.store.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idol.store.R

/** فونت وزیرمتن — فونت استاندارد رابط فارسی */
val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

private val Indigo = Color(0xFF6366F1)
private val IndigoLight = Color(0xFF818CF8)
private val Cyan = Color(0xFF22D3EE)
private val CyanDeep = Color(0xFF06B6D4)

/* پالت تیره — سرمه‌ای عمیق هماهنگ با لوگوی نئونی */
private val DarkBg = Color(0xFF070A12)
private val DarkSurface = Color(0xFF0E1420)
private val DarkSurfaceHigh = Color(0xFF182233)
private val DarkOutline = Color(0xFF263349)

private val DarkColors = darkColorScheme(
    primary = Cyan,
    onPrimary = Color(0xFF04222B),
    primaryContainer = Color(0xFF0E4A5C),
    onPrimaryContainer = Color(0xFFB6F3FF),
    secondary = IndigoLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF31346B),
    onSecondaryContainer = Color(0xFFDDE0FF),
    tertiary = CyanDeep,
    background = DarkBg,
    onBackground = Color(0xFFE9EDF4),
    surface = DarkSurface,
    onSurface = Color(0xFFE9EDF4),
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = Color(0xFF96A2B8),
    outline = DarkOutline,
    outlineVariant = Color(0xFF1B2434),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF4A1520),
    onErrorContainer = Color(0xFFFDC5CD),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0E7490),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFF4FC),
    onPrimaryContainer = Color(0xFF06333F),
    secondary = Indigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3E5FF),
    onSecondaryContainer = Color(0xFF1D2080),
    tertiary = Color(0xFF0891B2),
    background = Color(0xFFF2F5FA),
    onBackground = Color(0xFF131A26),
    surface = Color.White,
    onSurface = Color(0xFF131A26),
    surfaceVariant = Color(0xFFE8EDF5),
    onSurfaceVariant = Color(0xFF5A6B84),
    outline = Color(0xFFC9D4E3),
    outlineVariant = Color(0xFFE2E9F2),
    error = Color(0xFFDC2626),
)

val IdolShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private fun idolTypography() = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.copy(fontFamily = Vazirmatn),
        displayMedium = t.displayMedium.copy(fontFamily = Vazirmatn),
        displaySmall = t.displaySmall.copy(fontFamily = Vazirmatn),
        headlineLarge = t.headlineLarge.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold),
        headlineMedium = t.headlineMedium.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold),
        headlineSmall = t.headlineSmall.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 34.sp),
        titleLarge = t.titleLarge.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
        titleMedium = t.titleMedium.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp),
        titleSmall = t.titleSmall.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = t.bodyLarge.copy(fontFamily = Vazirmatn, fontSize = 15.sp, lineHeight = 24.sp),
        bodyMedium = t.bodyMedium.copy(fontFamily = Vazirmatn, fontSize = 13.sp, lineHeight = 20.sp),
        bodySmall = t.bodySmall.copy(fontFamily = Vazirmatn, fontSize = 12.sp, lineHeight = 17.sp),
        labelLarge = t.labelLarge.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 14.sp),
        labelMedium = t.labelMedium.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Medium, fontSize = 12.sp),
        labelSmall = t.labelSmall.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Medium, fontSize = 11.sp),
    )
}

/** استایل عنوان بولد کوچک برای استفاده‌های متفرقه */
val SmallTitle = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 13.sp)

@Composable
fun IdolStoreTheme(content: @Composable () -> Unit) {
    val darkTheme = when (Prefs.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = IdolShapes,
        typography = idolTypography(),
        content = content,
    )
}
