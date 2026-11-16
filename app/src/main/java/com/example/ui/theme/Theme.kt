package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ZyroDarkColorScheme = darkColorScheme(
    primary = ZyroCyan,
    onPrimary = Color(0xFF04141F),
    primaryContainer = Color(0xFF0C3550),
    onPrimaryContainer = Color(0xFFB8F5FF),
    secondary = ZyroGold,
    onSecondary = Color(0xFF281A00),
    secondaryContainer = Color(0xFF473208),
    onSecondaryContainer = Color(0xFFFFE6A8),
    tertiary = ZyroViolet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF2D1B5E),
    onTertiaryContainer = Color(0xFFE3D7FF),
    background = ZyroMidnightBg,
    onBackground = Color(0xFFF0F4FF),
    surface = ZyroMidnightSurface,
    onSurface = Color(0xFFF0F4FF),
    surfaceVariant = ZyroMidnightCard,
    onSurfaceVariant = Color(0xFFA7B2D6),
    surfaceContainerHigh = ZyroMidnightElevated,
    outline = Color(0xFF2F3A61),
    error = ZyroCrimson,
    onError = Color.White
)

private val ZyroLightColorScheme = lightColorScheme(
    primary = ZyroRoyalBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF0F295E),
    secondary = ZyroLightGold,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF451A03),
    tertiary = ZyroLightViolet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF2E1065),
    background = ZyroLightBg,
    onBackground = ZyroLightInk,
    surface = ZyroLightSurface,
    onSurface = ZyroLightInk,
    surfaceVariant = ZyroLightCard,
    onSurfaceVariant = ZyroLightMuted,
    surfaceContainerHigh = Color(0xFFE2E8F5),
    outline = Color(0xFFCBD5E1),
    error = ZyroCrimson,
    onError = Color.White
)

val ZyroShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun ZyroTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ZyroDarkColorScheme else ZyroLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ZyroTypography,
        shapes = ZyroShapes,
        content = content
    )
}
