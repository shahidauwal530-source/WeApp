package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WeDarkColorScheme = darkColorScheme(
    primary = WePurple,
    onPrimary = Color.White,
    primaryContainer = WePurpleDark,
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = WeBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = WeCyan,
    onTertiary = Color(0xFF042F2E),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = WeRose,
    onError = Color.White
)

private val WeLightColorScheme = lightColorScheme(
    primary = WePurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF3B0764),
    secondary = WeBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = WeCyan,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = WeRose,
    onError = Color.White
)

@Composable
fun WeAppTheme(
    themeMode: String = "dark",
    fontSize: String = "medium",
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "light" -> false
        "system" -> isSystemInDarkTheme()
        else -> true
    }
    val colorScheme = if (isDark) WeDarkColorScheme else WeLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = weAppTypography(fontSize),
        content = content
    )
}
