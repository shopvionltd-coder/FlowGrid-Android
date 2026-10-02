package com.shopvion.flowgrid

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.shopvion.flowgrid.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Random

// ---------------------------------------------------------
// 1. APP ARCHITECTURE & CONFIGURATION CONSTANTS
// ---------------------------------------------------------
object GameConfig {
    const val PACKAGE_NAME = "com.shopvion.flowgrid"
    const val GAME_TITLE = "FlowGrid"
    const val GAME_SUBTITLE = "2D Pipe Puzzle"
    const val APP_VERSION = "2.0"

    // AdMob & Firebase Remote Config Parameter Keys
    const val REMOTE_KEY_ADMOB_APP_ID = "admob_app_id"
    const val REMOTE_KEY_BANNER_AD_ID = "banner_ad_id"
    const val REMOTE_KEY_INTERSTITIAL_AD_ID = "interstitial_ad_id"
    const val REMOTE_KEY_REWARDED_AD_ID = "rewarded_ad_id"

    // Default Fallback Values using official AdMob Test IDs
    const val DEFAULT_ADMOB_APP_ID = "ca-app-pub-3948256099942544~3347511713"
    const val DEFAULT_BANNER_AD_ID = "ca-app-pub-3940256099942544/6300978111"
    const val DEFAULT_INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"
    const val DEFAULT_REWARDED_AD_ID = "ca-app-pub-3940256099942544/5224354917"

    // Local aliases for backwards compatibility
    const val ADMOB_APP_ID = DEFAULT_ADMOB_APP_ID
    const val INLINE_ADAPTIVE_BANNER_ID = DEFAULT_BANNER_AD_ID
    const val INTERSTITIAL_AD_ID = DEFAULT_INTERSTITIAL_AD_ID
    const val REWARDED_AD_ID = DEFAULT_REWARDED_AD_ID

    // Default map for Firebase Remote Config fallback registration
    val REMOTE_CONFIG_DEFAULTS: Map<String, Any> = mapOf(
        REMOTE_KEY_ADMOB_APP_ID to DEFAULT_ADMOB_APP_ID,
        REMOTE_KEY_BANNER_AD_ID to DEFAULT_BANNER_AD_ID,
        REMOTE_KEY_INTERSTITIAL_AD_ID to DEFAULT_INTERSTITIAL_AD_ID,
        REMOTE_KEY_REWARDED_AD_ID to DEFAULT_REWARDED_AD_ID
    )

    const val INTERSTITIAL_INTERVAL_LEVELS = 3
    const val TOTAL_LEVELS = 100
}

// ---------------------------------------------------------
// 2. DATA MODELS FOR BOARD & FLOWS
// ---------------------------------------------------------
data class GridPoint(val x: Int, val y: Int)

data class ColorPair(
    val colorId: Int,
    val name: String,
    val color: Color,
    val darkColor: Color,
    val start: GridPoint,
    val end: GridPoint,
    val solutionPath: List<GridPoint> = emptyList()
)

val FLOW_PALETTE = listOf(
    Pair("Red", FlowRed to Color(0xFFB71C1C)),
    Pair("Blue", FlowBlue to Color(0xFF0D47A1)),
    Pair("Green", FlowGreen to Color(0xFF1B5E20)),
    Pair("Yellow", FlowYellow to Color(0xFFF57F17)),
    Pair("Orange", FlowOrange to Color(0xFFE65100)),
    Pair("Cyan", FlowCyan to Color(0xFF006064)),
    Pair("Purple", FlowPurple to Color(0xFF4A148C)),
    Pair("Magenta", FlowMagenta to Color(0xFF880E4F))
)

data class LevelData(
    val levelNumber: Int,
    val gridSize: Int,
    val colorPairs: List<ColorPair>
) {
    val totalCells: Int get() = gridSize * gridSize
    val minMoves: Int get() = colorPairs.size
}

// ---------------------------------------------------------
// 3. 100 SOLVABLE LEVELS GENERATOR (100% COVERAGE)
// ---------------------------------------------------------
object LevelRepository {
    /**
     * Dynamically pulls the specific grid size and dot coordinates corresponding to [currentLevelId] (1 to 100).
     * Strictly avoids reusing cached or stale board data.
     */
    fun getLevel(currentLevelId: Int): LevelData {
        val clampedId = currentLevelId.coerceIn(1, GameConfig.TOTAL_LEVELS)
        return generateSolvableLevel(clampedId)
    }

    /**
     * Dynamically resolves the grid size for the given [levelId].
     */
    fun getGridSize(levelId: Int): Int {
        val clamped = levelId.coerceIn(1, GameConfig.TOTAL_LEVELS)
        return when {
            clamped <= 20 -> 5
            clamped <= 40 -> 6
            clamped <= 60 -> 7
            clamped <= 80 -> 8
            clamped <= 90 -> 9
            else -> 10
        }
    }

    /**
     * Dynamically resolves the number of color pairs for the given [levelId].
     */
    fun getColorCount(levelId: Int): Int {
        val clamped = levelId.coerceIn(1, GameConfig.TOTAL_LEVELS)
        return when {
            clamped <= 20 -> 4
            clamped <= 40 -> 5
            clamped <= 60 -> 6
            clamped <= 80 -> 7
            else -> 8
        }
    }

    /**
     * Generates a unique, 100% solvable level configuration for [level].
     * Each level receives unique dot coordinates and grid dimension based on its level ID.
     */
    private fun generateSolvableLevel(level: Int): LevelData {
        val gridSize = getGridSize(level)
        val numColors = getColorCount(level)

        val totalCells = gridSize * gridSize
        val rng = Random(level * 8831L + 43L)

        // Find Hamiltonian path using Warnsdorff's heuristic for 100 unique solvable layouts
        var hPath: List<GridPoint>? = null
        for (attempt in 0 until 150) {
            val sx = rng.nextInt(gridSize)
            val sy = rng.nextInt(gridSize)
            val path = ArrayList<GridPoint>(totalCells)
            val visited = HashSet<GridPoint>(totalCells)
            val startPt = GridPoint(sx, sy)
            path.add(startPt)
            visited.add(startPt)

            while (path.size < totalCells) {
                val curr = path.last()
                var bestPoint: GridPoint? = null
                var minDegree = 999
                var bestTie = 999.0

                val dirs = arrayOf(0 to -1, 0 to 1, -1 to 0, 1 to 0)
                for ((dx, dy) in dirs) {
                    val nx = curr.x + dx
                    val ny = curr.y + dy
                    val pt = GridPoint(nx, ny)
                    if (nx in 0 until gridSize && ny in 0 until gridSize && !visited.contains(pt)) {
                        // Count unvisited neighbors of pt
                        var deg = 0
                        for ((ddx, ddy) in dirs) {
                            val nnx = nx + ddx
                            val nny = ny + ddy
                            if (nnx in 0 until gridSize && nny in 0 until gridSize && !visited.contains(GridPoint(nnx, nny))) {
                                deg++
                            }
                        }
                        val tie = rng.nextDouble()
                        if (deg < minDegree || (deg == minDegree && tie < bestTie)) {
                            minDegree = deg
                            bestTie = tie
                            bestPoint = pt
                        }
                    }
                }
                if (bestPoint == null) break
                path.add(bestPoint)
                visited.add(bestPoint)
            }

            if (path.size == totalCells) {
                hPath = path
                break
            }
        }

        // Deterministic serpentine fallback if heuristic limit reached
        val finalPath = hPath ?: run {
            val fallback = ArrayList<GridPoint>(totalCells)
            for (y in 0 until gridSize) {
                if (y % 2 == 0) {
                    for (x in 0 until gridSize) fallback.add(GridPoint(x, y))
                } else {
                    for (x in gridSize - 1 downTo 0) fallback.add(GridPoint(x, y))
                }
            }
            fallback
        }

        // Partition into numColors segments with randomized lengths (each >= 3)
        val minLen = 3
        val remCells = totalCells - (numColors * minLen)
        val weights = IntArray(numColors) { rng.nextInt(9) + 2 }
        val sumWeights = weights.sum().toDouble()
        val alloc = IntArray(numColors) { (weights[it] / sumWeights * remCells).toInt() }
        var diff = remCells - alloc.sum()
        var wIdx = 0
        while (diff > 0) {
            alloc[wIdx % numColors]++
            diff--
            wIdx++
        }

        val segments = mutableListOf<List<GridPoint>>()
        var currIdx = 0
        for (k in 0 until numColors) {
            val segLen = minLen + alloc[k]
            segments.add(finalPath.subList(currIdx, currIdx + segLen).toList())
            currIdx += segLen
        }

        val colorPairs = segments.mapIndexed { idx, seg ->
            val paletteEntry = FLOW_PALETTE[idx % FLOW_PALETTE.size]
            ColorPair(
                colorId = idx,
                name = paletteEntry.first,
                color = paletteEntry.second.first,
                darkColor = paletteEntry.second.second,
                start = seg.first(),
                end = seg.last(),
                solutionPath = seg
            )
        }

        return LevelData(
            levelNumber = level,
            gridSize = gridSize,
            colorPairs = colorPairs
        )
    }
}

// ---------------------------------------------------------
// 4. CUBISM 3D UI COMPONENTS & FACETED GEOMETRY
// ---------------------------------------------------------

/**
 * 3D Cubist Card with beveled geometric facets, multi-angle lighting, and ambient shadows.
 */
@Composable
fun Cubix3DCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 6.dp,
    backgroundColor: Color? = null,
    borderBevel: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = cubixPalette
    val baseBg = backgroundColor ?: palette.surface
    val highlightColor = palette.surfaceHighlight
    val shadowFacetColor = palette.surfaceShadow

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = palette.surfaceShadow,
                spotColor = palette.surfaceShadowDark
            )
            .background(baseBg, shape)
            .drawBehind {
                if (borderBevel) {
                    val strokeW = 1.5.dp.toPx()
                    // Top-left facet specular highlight
                    drawRoundRect(
                        color = highlightColor.copy(alpha = if (palette.isDark) 0.35f else 0.85f),
                        topLeft = Offset(0.5f, 0.5f),
                        size = Size(size.width - 1f, size.height - 1f),
                        cornerRadius = CornerRadius(20.dp.toPx()),
                        style = Stroke(width = strokeW)
                    )
                    // Bottom-right facet shading
                    drawRoundRect(
                        color = shadowFacetColor.copy(alpha = if (palette.isDark) 0.5f else 0.4f),
                        topLeft = Offset(1.5f, 1.5f),
                        size = Size(size.width - 3f, size.height - 3f),
                        cornerRadius = CornerRadius(20.dp.toPx()),
                        style = Stroke(width = strokeW * 0.75f)
                    )
                }
            }
            .clip(shape),
        content = content
    )
}

/**
 * Alias for backward compatibility that renders a high-fidelity 3D Cubist Card.
 */
@Composable
fun ClayCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Cubix3DCard(
        modifier = modifier,
        shape = shape,
        elevation = elevation,
        content = content
    )
}

/**
 * 3D Tactile Cubist Button that exhibits a physical 3D block extrusion:
 * - Highlights on the top face
 * - Solid 3D shaded extrusion on the bottom edge
 * - Visibly depresses into the surface when pressed!
 */
@Composable
fun Cubix3DButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color? = null,
    contentColor: Color? = null,
    depth: Dp = 4.5.dp,
    contentPadding: PaddingValues? = null,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val palette = cubixPalette
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val baseBg = backgroundColor ?: palette.surface
    val targetContentColor = contentColor ?: when {
        backgroundColor != null && backgroundColor != palette.surface -> Color.White
        else -> palette.textPrimary
    }

    val effectivePadding = contentPadding ?: if (shape == CircleShape) {
        PaddingValues(0.dp)
    } else {
        PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    }

    // Determine bottom 3D extrusion color based on the base color
    val extrusionColor = when {
        backgroundColor == FlowBlue -> FlowBlueDark
        backgroundColor == FlowGreen -> FlowGreenDark
        backgroundColor == FlowRed -> FlowRedDark
        backgroundColor == FlowYellow -> FlowYellowDark
        backgroundColor == FlowOrange -> FlowOrangeDark
        backgroundColor == FlowPurple -> FlowPurpleDark
        backgroundColor == FlowCyan -> FlowCyanDark
        backgroundColor == FlowMagenta -> FlowMagentaDark
        backgroundColor != null -> backgroundColor.copy(alpha = 0.8f)
        else -> palette.surfaceExtrusion
    }

    val topHighlightColor = when {
        backgroundColor == FlowBlue -> FlowBlueLight.copy(alpha = 0.45f)
        backgroundColor == FlowGreen -> FlowGreenLight.copy(alpha = 0.45f)
        backgroundColor == FlowRed -> FlowRedLight.copy(alpha = 0.45f)
        backgroundColor != null -> Color.White.copy(alpha = 0.35f)
        else -> palette.surfaceHighlight.copy(alpha = if (palette.isDark) 0.4f else 0.9f)
    }

    // 3D depression animation: top face translates down by (depth - 1.2.dp) when pressed
    val animatedPressOffset by animateFloatAsState(
        targetValue = if (isPressed && enabled) (depth.value - 1.2f) else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btn3dPress"
    )

    val currentElevation by animateFloatAsState(
        targetValue = if (isPressed && enabled) 1.5f else depth.value + 2f,
        label = "btn3dElev"
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = currentElevation.dp,
                shape = shape,
                ambientColor = palette.surfaceShadow,
                spotColor = palette.surfaceShadowDark
            )
            .background(extrusionColor, shape) // Bottom extrusion visible when top face is elevated
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        // Top Face of the 3D Cube / Prism
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = animatedPressOffset.dp)
                .background(baseBg, shape)
                .drawBehind {
                    // Beveled top facet highlight
                    val bevelW = 1.5.dp.toPx()
                    drawRoundRect(
                        color = topHighlightColor,
                        topLeft = Offset(0.5f, 0.5f),
                        size = Size(size.width - 1f, size.height - 1f),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                        style = Stroke(width = bevelW)
                    )
                }
                .padding(effectivePadding),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CompositionLocalProvider(LocalContentColor provides targetContentColor) {
                    content()
                }
            }
        }
    }
}

/**
 * Backward compatible alias for Cubix3DButton.
 */
@Composable
fun ClayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = ClayBg,
    contentColor: Color = ClayTextPrimary,
    elevation: Dp = 5.dp,
    content: @Composable RowScope.() -> Unit
) {
    val palette = cubixPalette
    val effectiveBg = if (backgroundColor == ClayBg) palette.surface else backgroundColor
    val effectiveColor = if (contentColor == ClayTextPrimary) palette.textPrimary else contentColor

    Cubix3DButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        backgroundColor = effectiveBg,
        contentColor = effectiveColor,
        depth = (elevation.value * 0.75f).coerceIn(3f, 6f).dp,
        content = content
    )
}

/**
 * Dynamic Cubist Background Canvas:
 * Draws multi-plane geometric facets, isometric angular lines, and floating 3D prisms.
 */
@Composable
fun CubistBackground(
    modifier: Modifier = Modifier
) {
    val palette = cubixPalette
    val infiniteTransition = rememberInfiniteTransition(label = "cubistAnim")

    val drift1 by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift1"
    )

    val drift2 by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift2"
    )

    val facetColor1 = palette.surfaceFacetA.copy(alpha = if (palette.isDark) 0.5f else 0.6f)
    val facetColor2 = palette.surfaceFacetB.copy(alpha = if (palette.isDark) 0.4f else 0.5f)
    val lineAccent = palette.border.copy(alpha = if (palette.isDark) 0.25f else 0.4f)

    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Angular Cubist Facet Planes
        val path1 = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, 0f)
            lineTo(w * 0.85f, 0f)
            lineTo(w * 0.4f, h * 0.38f + drift1)
            lineTo(0f, h * 0.22f)
            close()
        }
        drawPath(path1, facetColor1)

        val path2 = androidx.compose.ui.graphics.Path().apply {
            moveTo(w, h * 0.15f)
            lineTo(w, h * 0.72f)
            lineTo(w * 0.55f, h * 0.58f + drift2)
            lineTo(w * 0.75f, h * 0.25f)
            close()
        }
        drawPath(path2, facetColor2)

        val path3 = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, h * 0.65f)
            lineTo(w * 0.45f, h * 0.8f + drift1 * 0.7f)
            lineTo(w * 0.2f, h)
            lineTo(0f, h)
            close()
        }
        drawPath(path3, facetColor1)

        // 2. Subtle geometric facet lines
        drawLine(
            color = lineAccent,
            start = Offset(0f, h * 0.22f),
            end = Offset(w * 0.85f, 0f),
            strokeWidth = 1.2.dp.toPx()
        )
        drawLine(
            color = lineAccent,
            start = Offset(w * 0.4f, h * 0.38f + drift1),
            end = Offset(w, h * 0.72f),
            strokeWidth = 1.2.dp.toPx()
        )
        drawLine(
            color = lineAccent,
            start = Offset(w * 0.45f, h * 0.8f + drift1 * 0.7f),
            end = Offset(w * 0.2f, h),
            strokeWidth = 1.2.dp.toPx()
        )
    }
}

// ---------------------------------------------------------
// 5. AUDIO & HAPTICS ENGINE
// ---------------------------------------------------------
// SoundManager & AudioEngine implementation is located in SoundManager.kt


// ---------------------------------------------------------
// 6. LOCAL PROGRESS & PREFERENCES MANAGER
// ---------------------------------------------------------
class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cubixflow_prefs", Context.MODE_PRIVATE)

    fun getUnlockedLevel(): Int = prefs.getInt("unlocked_level", 1)

    fun unlockLevel(level: Int) {
        val current = getUnlockedLevel()
        if (level > current) {
            prefs.edit().putInt("unlocked_level", level.coerceAtMost(GameConfig.TOTAL_LEVELS)).apply()
        }
    }

    fun getLevelStars(level: Int): Int = prefs.getInt("level_${level}_stars", 0)

    fun getLevelBestMoves(level: Int): Int = prefs.getInt("level_${level}_best_moves", 0)

    fun saveLevelScore(level: Int, moves: Int, stars: Int) {
        val prevBest = getLevelBestMoves(level)
        val best = if (prevBest == 0 || moves < prevBest) moves else prevBest
        val prevStars = getLevelStars(level)
        val bestStars = maxOf(stars, prevStars)
        prefs.edit()
            .putInt("level_${level}_best_moves", best)
            .putInt("level_${level}_stars", bestStars)
            .apply()
        unlockLevel(level + 1)
    }

    fun getHints(): Int = prefs.getInt("hints_count", 3)

    fun setHints(count: Int) {
        prefs.edit().putInt("hints_count", count.coerceAtLeast(0)).apply()
    }

    fun useHint(): Boolean {
        val current = getHints()
        if (current > 0) {
            prefs.edit().putInt("hints_count", current - 1).apply()
            return true
        }
        return false
    }

    fun addHints(count: Int) {
        val current = getHints()
        prefs.edit().putInt("hints_count", current + count).apply()
    }

    fun isSoundFxEnabled(): Boolean = prefs.getBoolean("sound_fx_enabled", true)
    fun setSoundFxEnabled(enabled: Boolean) = prefs.edit().putBoolean("sound_fx_enabled", enabled).apply()

    fun getSoundFxVolume(): Float = prefs.getFloat("sound_fx_volume", 0.8f)
    fun setSoundFxVolume(volume: Float) = prefs.edit().putFloat("sound_fx_volume", volume.coerceIn(0f, 1f)).apply()

    fun isMusicEnabled(): Boolean = prefs.getBoolean("music_enabled", true)
    fun setMusicEnabled(enabled: Boolean) = prefs.edit().putBoolean("music_enabled", enabled).apply()

    fun getMusicVolume(): Float = prefs.getFloat("music_volume", 0.6f)
    fun setMusicVolume(volume: Float) = prefs.edit().putFloat("music_volume", volume.coerceIn(0f, 1f)).apply()

    // --- Theme Mode Support (System / Light / Dark) ---
    fun getThemeMode(): String = prefs.getString("theme_mode", THEME_SYSTEM) ?: THEME_SYSTEM
    fun setThemeMode(mode: String) = prefs.edit().putString("theme_mode", mode).apply()

    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
    }

    fun resetAllProgress() {
        prefs.edit().clear().apply()
        prefs.edit().putInt("hints_count", 3).apply()
    }
}

// ---------------------------------------------------------
// 7. ADMOB WRAPPER LOGIC & OVERLAYS
// ---------------------------------------------------------
@Composable
fun AdBannerView(
    modifier: Modifier = Modifier
) {
    AdMobBannerView(modifier = modifier)
}

@Composable
fun InterstitialAdDialog(
    onDismiss: () -> Unit
) {
    val palette = cubixPalette
    var countdown by remember { mutableIntStateOf(3) }
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    Dialog(onDismissRequest = { if (countdown == 0) onDismiss() }) {
        Cubix3DCard(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = Color(0xFFFBBF24), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            "AdMob Interstitial",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        )
                    }
                    if (countdown == 0) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close Ad", tint = palette.textPrimary)
                        }
                    } else {
                        Text(
                            "Skip in ${countdown}s",
                            fontSize = 12.sp,
                            color = palette.textSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .shadow(8.dp, CircleShape, ambientColor = FlowBlueDark, spotColor = FlowBlueDark)
                        .background(FlowBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Enjoying FlowGrid?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ad ID: ${GameConfig.INTERSTITIAL_AD_ID.take(15)}...",
                    fontSize = 11.sp,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))
                Cubix3DButton(
                    onClick = { if (countdown == 0) onDismiss() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = if (countdown == 0) FlowBlue else palette.surface,
                    contentColor = if (countdown == 0) Color.White else palette.textSecondary
                ) {
                    Text(
                        text = if (countdown == 0) "Continue Game" else "Please wait ${countdown}s...",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RewardedAdDialog(
    onRewardEarned: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = cubixPalette
    var countdown by remember { mutableIntStateOf(4) }
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    Dialog(onDismissRequest = { if (countdown == 0) onDismiss() }) {
        Cubix3DCard(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(color = Color(0xFF10B981), shape = RoundedCornerShape(6.dp)) {
                    Text(
                        "Rewarded Video Ad",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Watch Ad to Earn +2 Free Hints",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (countdown > 0) "Reward unlocked in ${countdown}s..." else "🎉 Reward Ready!",
                    fontSize = 14.sp,
                    color = if (countdown > 0) palette.textSecondary else Color(0xFF10B981),
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(24.dp))
                Cubix3DButton(
                    onClick = {
                        if (countdown == 0) {
                            onRewardEarned()
                        } else {
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = if (countdown == 0) Color(0xFF10B981) else palette.surface,
                    contentColor = if (countdown == 0) Color.White else palette.textSecondary
                ) {
                    Text(
                        text = if (countdown == 0) "Claim +2 Hints" else "Cancel",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
