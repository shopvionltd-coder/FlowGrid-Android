package com.shopvion.flowgrid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopvion.flowgrid.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LevelSelectScreen(
    prefsManager: PreferencesManager,
    onLevelSelected: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    val palette = cubixPalette
    val unlockedLevel = remember { prefsManager.getUnlockedLevel() }
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    // Packs navigation
    val packTabs = listOf(
        "All" to (1..100),
        "5x5" to (1..20),
        "6x6" to (21..40),
        "7x7" to (41..60),
        "8x8" to (61..80),
        "9x9+" to (81..100)
    )
    var selectedPackIndex by remember { mutableIntStateOf(0) }
    val currentRange = packTabs[selectedPackIndex].second

    // Automatically scroll to highest unlocked level when first opening
    LaunchedEffect(Unit) {
        if (unlockedLevel > 4) {
            val targetIndex = (unlockedLevel - 1).coerceAtMost(99)
            gridState.animateScrollToItem(targetIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Multi-plane Cubist geometry background
        CubistBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Cubix3DButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    depth = 4.dp
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Select Level",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = palette.textPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Unlocked: $unlockedLevel / ${GameConfig.TOTAL_LEVELS}",
                        fontSize = 12.sp,
                        color = palette.textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Pack Filter Tabs (3D Segmented Pills)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                packTabs.forEachIndexed { index, (label, _) ->
                    val isSelected = selectedPackIndex == index
                    Cubix3DButton(
                        onClick = {
                            selectedPackIndex = index
                            coroutineScope.launch {
                                gridState.scrollToItem(0)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(19.dp),
                        backgroundColor = if (isSelected) FlowBlue else palette.surface,
                        contentColor = if (isSelected) Color.White else palette.textPrimary,
                        depth = if (isSelected) 4.5.dp else 2.5.dp
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                        )
                    }
                }
            }

            // 100 Levels Grid (4 columns of 3D Cubist Tiles)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                state = gridState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(currentRange.toList()) { levelNum ->
                    val isUnlocked = levelNum <= unlockedLevel
                    val stars = if (isUnlocked) prefsManager.getLevelStars(levelNum) else 0
                    val bestMoves = if (isUnlocked) prefsManager.getLevelBestMoves(levelNum) else 0

                    LevelTile(
                        levelNumber = levelNum,
                        isUnlocked = isUnlocked,
                        stars = stars,
                        bestMoves = bestMoves,
                        onClick = {
                            if (isUnlocked) {
                                onLevelSelected(levelNum)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Inline Adaptive Banner
            AdBannerView()
        }
    }
}

/**
 * 3D Cubist Level Tile:
 * - Unlocked & Completed: Faceted vibrant gem block with golden stars
 * - Unlocked & In-progress: Elevated 3D prism with crisp facet highlights
 * - Locked: Sunken textured block with shaded keyhole
 */
@Composable
private fun LevelTile(
    levelNumber: Int,
    isUnlocked: Boolean,
    stars: Int,
    bestMoves: Int,
    onClick: () -> Unit
) {
    val palette = cubixPalette

    val baseBg = when {
        !isUnlocked -> palette.cellBackground.copy(alpha = 0.6f)
        stars == 3 -> if (palette.isDark) Color(0xFF143324) else Color(0xFFE8F5E9)
        stars > 0 -> if (palette.isDark) Color(0xFF192A3E) else Color(0xFFEBF3FC)
        else -> palette.surface
    }

    val extrusionColor = when {
        !isUnlocked -> palette.cellShadow
        stars == 3 -> if (palette.isDark) Color(0xFF0A1F15) else Color(0xFFC8E6C9)
        stars > 0 -> if (palette.isDark) Color(0xFF0F1B2A) else Color(0xFFC7DCF5)
        else -> palette.surfaceExtrusion
    }

    val highlightColor = when {
        !isUnlocked -> Color.Transparent
        stars == 3 -> if (palette.isDark) FlowGreenLight.copy(alpha = 0.4f) else Color.White
        else -> palette.surfaceHighlight.copy(alpha = if (palette.isDark) 0.35f else 0.85f)
    }

    val depth = if (isUnlocked) 4.dp else 1.5.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.95f)
            .shadow(
                elevation = if (isUnlocked) 4.dp else 1.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = palette.surfaceShadow,
                spotColor = palette.surfaceShadowDark
            )
            .background(extrusionColor, RoundedCornerShape(16.dp))
            .clickable(enabled = isUnlocked, onClick = onClick),
        contentAlignment = Alignment.TopCenter
    ) {
        // Top Face of the 3D Tile
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isUnlocked) 3.dp else 1.dp)
                .background(baseBg, RoundedCornerShape(16.dp))
                .drawBehind {
                    // Beveled top facet specular line
                    drawRoundRect(
                        color = highlightColor,
                        topLeft = Offset(0.5f, 0.5f),
                        size = Size(size.width - 1f, size.height - 1f),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                if (!isUnlocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = palette.textMuted.copy(alpha = 0.55f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    // Level Number
                    Text(
                        text = "$levelNumber",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = when {
                            stars == 3 -> FlowGreen
                            stars > 0 -> FlowBlue
                            else -> palette.textPrimary
                        },
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    // Best Moves or Grid Size
                    if (bestMoves > 0) {
                        Text(
                            text = "$bestMoves moves",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textSecondary
                        )
                    } else {
                        val gridSize = when {
                            levelNumber <= 20 -> "5x5"
                            levelNumber <= 40 -> "6x6"
                            levelNumber <= 60 -> "7x7"
                            levelNumber <= 80 -> "8x8"
                            else -> "9x9+"
                        }
                        Text(
                            text = gridSize,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textSecondary
                        )
                    }

                    // 3 Stars display with 3D facet stars
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        for (i in 1..3) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = if (i <= stars) FlowYellow else palette.surfaceShadow.copy(alpha = 0.6f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
