package com.shopvion.flowgrid.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =========================================================
// CUBISM 3D COLOR PALETTES (LIGHT & DARK MODES)
// =========================================================

// --- Flow Colors (Vibrant, high-contrast, solid gem colors) ---
val FlowRed = Color(0xFFEF4444)
val FlowRedDark = Color(0xFF991B1B)
val FlowRedLight = Color(0xFFFCA5A5)

val FlowBlue = Color(0xFF2563EB)
val FlowBlueDark = Color(0xFF1E3A8A)
val FlowBlueLight = Color(0xFF93C5FD)

val FlowGreen = Color(0xFF10B981)
val FlowGreenDark = Color(0xFF065F46)
val FlowGreenLight = Color(0xFF6EE7B7)

val FlowYellow = Color(0xFFF59E0B)
val FlowYellowDark = Color(0xFF92400E)
val FlowYellowLight = Color(0xFFFDE68A)

val FlowOrange = Color(0xFFF97316)
val FlowOrangeDark = Color(0xFF9A3412)
val FlowOrangeLight = Color(0xFFFDBA74)

val FlowPurple = Color(0xFFA855F7)
val FlowPurpleDark = Color(0xFF581C87)
val FlowPurpleLight = Color(0xFFD8B4FE)

val FlowCyan = Color(0xFF06B6D4)
val FlowCyanDark = Color(0xFF164E63)
val FlowCyanLight = Color(0xFFA5F3FC)

val FlowMagenta = Color(0xFFEC4899)
val FlowMagentaDark = Color(0xFF831843)
val FlowMagentaLight = Color(0xFFF9A8D4)

// --- Accents ---
val ClaySuccess = FlowGreen
val ClayAccent = FlowBlue

// --- Legacy Clay Theme Support (mapped to light defaults) ---
val ClayBg = Color(0xFFE2E8F0)
val ClayLightShadow = Color(0xFFFFFFFF)
val ClayDarkShadow = Color(0xFFB4C2D6)
val ClayDarkShadowPressed = Color(0xFF94A3B8)
val ClayTextPrimary = Color(0xFF1E293B)
val ClayTextSecondary = Color(0xFF64748B)

// =========================================================
// CUBISM PALETTE DATA CLASS
// =========================================================
data class CubixPalette(
    val isDark: Boolean,
    // Backgrounds
    val background: Color,
    val backgroundSecondary: Color,
    // 3D Surfaces & Cubist Blocks
    val surface: Color,
    val surfaceHighlight: Color,      // Light source reflection facet (top-left)
    val surfaceShadow: Color,         // Shaded facet (bottom-right)
    val surfaceShadowDark: Color,     // Deep contact shadow
    val surfaceExtrusion: Color,      // 3D vertical thickness / bottom edge
    val surfaceFacetA: Color,         // Geometric plane A
    val surfaceFacetB: Color,         // Geometric plane B
    // Game Board Recessed Arena
    val boardBackground: Color,
    val boardBevelLight: Color,
    val boardBevelDark: Color,
    // Grid Cells
    val cellBackground: Color,
    val cellHighlight: Color,
    val cellShadow: Color,
    val cellCenterDot: Color,
    // Typography
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    // Accents & Lines
    val border: Color,
    val accent: Color
)

// --- Light Cubism Palette (Clean architectural stone & galleries) ---
val LightCubixPalette = CubixPalette(
    isDark = false,
    background = Color(0xFFE6ECF5),
    backgroundSecondary = Color(0xFFDCE4F0),
    surface = Color(0xFFF2F6FC),
    surfaceHighlight = Color(0xFFFFFFFF),
    surfaceShadow = Color(0xFFB8C6DA),
    surfaceShadowDark = Color(0xFF98A7BD),
    surfaceExtrusion = Color(0xFFA6B7CE),
    surfaceFacetA = Color(0xFFEDF3FA),
    surfaceFacetB = Color(0xFFE1EBF6),
    boardBackground = Color(0xFFD6E0EE),
    boardBevelLight = Color(0xFFFFFFFF),
    boardBevelDark = Color(0xFFA2B4CD),
    cellBackground = Color(0xFFEAF0F9),
    cellHighlight = Color(0xFFFFFFFF),
    cellShadow = Color(0xFFC4D2E5),
    cellCenterDot = Color(0xFFA9BCCC),
    textPrimary = Color(0xFF1E293B),
    textSecondary = Color(0xFF64748B),
    textMuted = Color(0xFF94A3B8),
    border = Color(0xFFCAD6E6),
    accent = FlowBlue
)

// --- Dark Cubism Palette (Deep futuristic obsidian & neon facets) ---
val DarkCubixPalette = CubixPalette(
    isDark = true,
    background = Color(0xFF0F131A),
    backgroundSecondary = Color(0xFF151B24),
    surface = Color(0xFF1D2432),
    surfaceHighlight = Color(0xFF333E54),
    surfaceShadow = Color(0xFF0A0D13),
    surfaceShadowDark = Color(0xFF05070A),
    surfaceExtrusion = Color(0xFF07090E),
    surfaceFacetA = Color(0xFF222B3B),
    surfaceFacetB = Color(0xFF181F2C),
    boardBackground = Color(0xFF131822),
    boardBevelLight = Color(0xFF293347),
    boardBevelDark = Color(0xFF080B10),
    cellBackground = Color(0xFF19202C),
    cellHighlight = Color(0xFF2B364A),
    cellShadow = Color(0xFF0B0F16),
    cellCenterDot = Color(0xFF36445D),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    border = Color(0xFF2A3447),
    accent = Color(0xFF38BDF8)
)

val LocalCubixPalette = staticCompositionLocalOf { LightCubixPalette }

val cubixPalette: CubixPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalCubixPalette.current
