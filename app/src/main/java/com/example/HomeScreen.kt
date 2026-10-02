package com.shopvion.flowgrid

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopvion.flowgrid.ui.theme.*

@Composable
fun HomeScreen(
    audioEngine: AudioEngine,
    prefsManager: PreferencesManager,
    themeMode: String = PreferencesManager.THEME_SYSTEM,
    onToggleTheme: () -> Unit = {},
    onPlayClick: () -> Unit,
    onLevelSelectClick: () -> Unit = {},
    onSettingsClick: () -> Unit
) {
    val palette = cubixPalette
    val unlockedLevel = remember { prefsManager.getUnlockedLevel() }
    var isAudioOn by remember { mutableStateOf(prefsManager.isMusicEnabled()) }

    LaunchedEffect(Unit) {
        isAudioOn = prefsManager.isMusicEnabled()
    }

    // Calculate total stars
    val totalStars = remember {
        (1..GameConfig.TOTAL_LEVELS).sumOf { prefsManager.getLevelStars(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Dynamic multi-plane Cubist geometry background
        CubistBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with Stars Badge, Theme Quick Toggle, Sound & Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 3D Stars Badge
                Cubix3DCard(
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Stars",
                            tint = FlowYellow,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "$totalStars",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textPrimary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Quick Theme Toggle (Light / Dark)
                    Cubix3DButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.size(46.dp),
                        shape = CircleShape,
                        depth = 4.dp
                    ) {
                        Icon(
                            if (palette.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = if (palette.isDark) FlowYellow else palette.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Audio (Music & Sound) quick toggle
                    Cubix3DButton(
                        onClick = {
                            val nextState = !isAudioOn
                            isAudioOn = nextState
                            prefsManager.setMusicEnabled(nextState)
                            prefsManager.setSoundFxEnabled(nextState)
                            if (nextState) {
                                audioEngine.playMusic(true)
                                audioEngine.playPopConnect(true)
                            } else {
                                audioEngine.pauseMusic()
                            }
                        },
                        modifier = Modifier.size(46.dp),
                        shape = CircleShape,
                        depth = 4.dp
                    ) {
                        Icon(
                            if (isAudioOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (isAudioOn) "Mute Audio" else "Unmute Audio",
                            tint = if (isAudioOn) FlowBlue else palette.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Settings Button
                    Cubix3DButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.size(46.dp),
                        shape = CircleShape,
                        depth = 4.dp
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = palette.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Central Game Branding & 3D Cubist Interactive Hero Sculpture
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 3D Cubist Hero Composition
                HeroCubistSculpture()

                Spacer(modifier = Modifier.height(24.dp))

                // Title with modern dual-color pipe branding: "Flow" (Electric Blue) + "Grid" (Neon Green)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Flow",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        color = FlowBlue,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Grid",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        color = FlowGreen,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    color = palette.surfaceFacetB.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "2D PIPE PUZZLE • 100 LEVELS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textSecondary,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }

            // Bottom Actions & 3D Tactile CTA Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Large 3D Primary Play Button
                Cubix3DButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp),
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = FlowBlue,
                    contentColor = Color.White,
                    depth = 6.dp
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (unlockedLevel > 1) "CONTINUE (LV $unlockedLevel)" else "PLAY GAME",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                }

                // 3D Secondary Level Select Button
                Cubix3DButton(
                    onClick = onLevelSelectClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    backgroundColor = palette.surface,
                    contentColor = palette.textPrimary,
                    depth = 5.dp
                ) {
                    Icon(
                        Icons.Default.GridView,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = FlowOrange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SELECT LEVEL",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = palette.textPrimary
                    )
                }

                // Inline Adaptive Banner View
                AdBannerView()
            }
        }
    }
}

/**
 * 3D Cubist Hero Sculpture:
 * Depicts multi-perspective isometric cubes with beveled facet lighting
 * and connecting 3D glowing pipes bridging the blocks.
 */
@Composable
private fun HeroCubistSculpture() {
    val palette = cubixPalette
    val infiniteTransition = rememberInfiniteTransition(label = "heroFloat")

    val bobOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heroBob"
    )

    Cubix3DCard(
        modifier = Modifier
            .size(175.dp)
            .offset(y = bobOffset.dp),
        shape = RoundedCornerShape(38.dp),
        elevation = 14.dp,
        borderBevel = true
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(18.dp)) {
            val w = size.width
            val h = size.height

            // 4 Isometric 3D Cubist Nodes:
            // Top-Left (Red), Top-Right (Blue), Bottom-Right (Yellow), Bottom-Left (Green)
            val pTL = Offset(w * 0.24f, h * 0.24f)
            val pTR = Offset(w * 0.76f, h * 0.24f)
            val pBR = Offset(w * 0.76f, h * 0.76f)
            val pBL = Offset(w * 0.24f, h * 0.76f)

            val pipeStrokeW = 12.dp.toPx()

            // 1. Draw 3D Connecting Pipes between cubes
            // Horizontal Top: Red -> Blue
            draw3DPipeSegment(pTL, pTR, FlowRed, FlowBlue, pipeStrokeW)
            // Vertical Right: Blue -> Yellow
            draw3DPipeSegment(pTR, pBR, FlowBlue, FlowYellow, pipeStrokeW)
            // Horizontal Bottom: Yellow -> Green
            draw3DPipeSegment(pBR, pBL, FlowYellow, FlowGreen, pipeStrokeW)

            // 2. Draw 4 3D Cubist Gem Cubes with bevels
            draw3DCubeNode(pTL, FlowRed, FlowRedDark, FlowRedLight)
            draw3DCubeNode(pTR, FlowBlue, FlowBlueDark, FlowBlueLight)
            draw3DCubeNode(pBR, FlowYellow, FlowYellowDark, FlowYellowLight)
            draw3DCubeNode(pBL, FlowGreen, FlowGreenDark, FlowGreenLight)
        }
    }
}

/**
 * Draws a 3D isometric cubist node with multi-faceted depth:
 * - Ambient drop shadow
 * - Bottom extrusion facet
 * - Main vibrant face
 * - Top-left specular highlight facet
 * - Center jewel ring
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.draw3DCubeNode(
    center: Offset,
    mainColor: Color,
    darkShade: Color,
    lightHighlight: Color
) {
    val radius = 22.dp.toPx()

    // 1. Ambient Drop Shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.25f),
        radius = radius * 1.15f,
        center = Offset(center.x + 3.dp.toPx(), center.y + 4.dp.toPx())
    )

    // 2. 3D Bottom Extrusion Block
    drawRoundRect(
        color = darkShade,
        topLeft = Offset(center.x - radius, center.y - radius + 3.dp.toPx()),
        size = Size(radius * 2, radius * 2),
        cornerRadius = CornerRadius(8.dp.toPx())
    )

    // 3. Top Face of Cube
    drawRoundRect(
        color = mainColor,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        cornerRadius = CornerRadius(8.dp.toPx())
    )

    // 4. Facet Bevel Highlight (top & left edges)
    val facetPath = Path().apply {
        moveTo(center.x - radius + 8.dp.toPx(), center.y - radius)
        lineTo(center.x + radius - 8.dp.toPx(), center.y - radius)
        lineTo(center.x + radius - 11.dp.toPx(), center.y - radius + 5.dp.toPx())
        lineTo(center.x - radius + 5.dp.toPx(), center.y - radius + 5.dp.toPx())
        lineTo(center.x - radius + 5.dp.toPx(), center.y + radius - 11.dp.toPx())
        lineTo(center.x - radius, center.y + radius - 8.dp.toPx())
        close()
    }
    drawPath(facetPath, lightHighlight.copy(alpha = 0.65f))

    // 5. Specular Corner Point
    drawCircle(
        color = Color.White.copy(alpha = 0.7f),
        radius = radius * 0.22f,
        center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f)
    )
}

/**
 * Draws a 3D cylindrical pipe between two points with specular highlight ridge.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.draw3DPipeSegment(
    start: Offset,
    end: Offset,
    startColor: Color,
    endColor: Color,
    width: Float
) {
    // Drop shadow
    drawLine(
        color = Color.Black.copy(alpha = 0.2f),
        start = Offset(start.x + 2.dp.toPx(), start.y + 3.dp.toPx()),
        end = Offset(end.x + 2.dp.toPx(), end.y + 3.dp.toPx()),
        strokeWidth = width + 2.dp.toPx()
    )

    // Pipe body
    drawLine(
        color = startColor,
        start = start,
        end = end,
        strokeWidth = width
    )

    // 3D Top Specular Highlight Ridge
    drawLine(
        color = Color.White.copy(alpha = 0.45f),
        start = Offset(start.x, start.y - width * 0.2f),
        end = Offset(end.x, end.y - width * 0.2f),
        strokeWidth = width * 0.3f
    )
}
