package com.shopvion.flowgrid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = FlowBlue,
    onPrimary = Color.White,
    primaryContainer = LightCubixPalette.surface,
    onPrimaryContainer = LightCubixPalette.textPrimary,
    secondary = FlowGreen,
    onSecondary = Color.White,
    background = LightCubixPalette.background,
    onBackground = LightCubixPalette.textPrimary,
    surface = LightCubixPalette.surface,
    onSurface = LightCubixPalette.textPrimary,
    surfaceVariant = LightCubixPalette.surfaceFacetB,
    onSurfaceVariant = LightCubixPalette.textSecondary
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkCubixPalette.accent,
    onPrimary = Color.White,
    primaryContainer = DarkCubixPalette.surface,
    onPrimaryContainer = DarkCubixPalette.textPrimary,
    secondary = FlowGreen,
    onSecondary = Color.White,
    background = DarkCubixPalette.background,
    onBackground = DarkCubixPalette.textPrimary,
    surface = DarkCubixPalette.surface,
    onSurface = DarkCubixPalette.textPrimary,
    surfaceVariant = DarkCubixPalette.surfaceFacetB,
    onSurfaceVariant = DarkCubixPalette.textSecondary
)

@Composable
fun FlowGridTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkCubixPalette else LightCubixPalette
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalCubixPalette provides palette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Backward compatibility alias
@Composable
fun CubixFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) = FlowGridTheme(darkTheme = darkTheme, content = content)
