package com.example.qahelper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = BrandPurple,
    onPrimary = Color.White,
    primaryContainer = BrandLavender,
    onPrimaryContainer = Color(0xFF23136E),
    secondary = BrandBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2ECFF),
    onSecondaryContainer = Color(0xFF0D2E63),
    tertiary = BrandCyan,
    background = LightBackground,
    onBackground = TextPrimary,
    surface = LightSurface,
    onSurface = TextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = LightOutline,
    error = ErrorRed,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = BrandViolet,
    onPrimary = Color(0xFF1B0B66),
    primaryContainer = Color(0xFF3B2E94),
    onPrimaryContainer = Color(0xFFE8E3FF),
    secondary = Color(0xFF8CB4FF),
    onSecondary = Color(0xFF0D2E63),
    secondaryContainer = Color(0xFF263C66),
    onSecondaryContainer = Color(0xFFD9E7FF),
    tertiary = Color(0xFF6ED4EF),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    error = Color(0xFFFF8B95),
    onError = Color(0xFF5A0610)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun QAHelperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
