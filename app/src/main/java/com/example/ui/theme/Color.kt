package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Core WeApp Brand Palette: Purple #7C3AED -> Blue #2563EB
val WePurple = Color(0xFF7C3AED)
val WePurpleLight = Color(0xFFA78BFA)
val WePurpleDark = Color(0xFF5B21B6)

val WeBlue = Color(0xFF2563EB)
val WeBlueLight = Color(0xFF60A5FA)
val WeCyan = Color(0xFF06B6D4)

val WeGold = Color(0xFFF59E0B)
val WeEmerald = Color(0xFF10B981)
val WeRose = Color(0xFFF43F5E)

// Dark Theme Surfaces (Primary Aesthetic)
val DarkBackground = Color(0xFF090814)
val DarkSurface = Color(0xFF121024)
val DarkSurfaceVariant = Color(0xFF1B1834)
val DarkOutline = Color(0xFF2E294E)
val DarkOnBackground = Color(0xFFF5F3FF)
val DarkOnSurfaceVariant = Color(0xFFA5A1C2)

// Light Theme Surfaces (When Dark Mode toggled off)
val LightBackground = Color(0xFFF8F7FF)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEDE9FE)
val LightOutline = Color(0xFFD8D0F5)
val LightOnBackground = Color(0xFF141126)
val LightOnSurfaceVariant = Color(0xFF585378)

val WeBrandGradient = Brush.linearGradient(
    colors = listOf(WePurple, WeBlue)
)

val WeVibrantGradient = Brush.linearGradient(
    colors = listOf(WePurple, WeBlue, WeCyan)
)

val WeGoldGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
)
