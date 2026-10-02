package com.shopvion.flowgridgame

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.shopvion.flowgridgame.ui.theme.*

/**
 * 3D Cubist Win Modal Dialog
 * Instantly presented when all dot pairs are matched.
 */
@Composable
fun WinModal(
    levelNumber: Int,
    moves: Int,
    minMoves: Int,
    bestMoves: Int,
    stars: Int,
    pipeCoverage: Int = 100,
    onNextLevel: () -> Unit,
    onReplay: () -> Unit,
    onLevelSelect: () -> Unit
) {
    val palette = cubixPalette
    val scaleAnim = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    Dialog(onDismissRequest = { /* Non-dismissible by backdrop tap; requires button selection */ }) {
        Cubix3DCard(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .scale(scaleAnim.value)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            elevation = 16.dp,
            borderBevel = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 3D Trophy / Celebration Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(FlowGreen.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = "Victory Trophy",
                        tint = FlowYellow,
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Level $levelNumber Completed!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = palette.textPrimary,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )

                if (pipeCoverage == 100 && moves <= minMoves) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = if (palette.isDark) Color(0xFF452205) else Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "★ PERFECT 100% FLOW ★",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (palette.isDark) FlowYellowLight else Color(0xFFD97706),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Star Rating Display (Animated bouncy scale)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..3) {
                        val isFilled = i <= stars
                        val starScale by animateFloatAsState(
                            targetValue = if (isFilled) 1.25f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "starScale$i"
                        )
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Star $i",
                            tint = if (isFilled) FlowYellow else palette.surfaceShadow.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(34.dp)
                                .scale(starScale)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3D Score Details Card (Moves Taken vs Target vs Best Score vs Coverage)
                Cubix3DCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    elevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Moves",
                                fontSize = 11.sp,
                                color = palette.textSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$moves",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (moves <= minMoves) FlowGreen else palette.textPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(palette.border.copy(alpha = 0.5f))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Target",
                                fontSize = 11.sp,
                                color = palette.textSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$minMoves",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = palette.textSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(palette.border.copy(alpha = 0.5f))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Coverage",
                                fontSize = 11.sp,
                                color = palette.textSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$pipeCoverage%",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (pipeCoverage == 100) FlowGreen else FlowOrange
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(palette.border.copy(alpha = 0.5f))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Best",
                                fontSize = 11.sp,
                                color = palette.textSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            val displayBest = if (bestMoves > 0) bestMoves else moves
                            Text(
                                text = "$displayBest",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = FlowBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary & Secondary 3D Navigation Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Next Level Button (Advances to Level + 1)
                    if (levelNumber < GameConfig.TOTAL_LEVELS) {
                        Cubix3DButton(
                            onClick = onNextLevel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            backgroundColor = FlowGreen,
                            contentColor = Color.White,
                            depth = 6.dp
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NEXT LEVEL",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Replay Current Level
                        Cubix3DButton(
                            onClick = onReplay,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            backgroundColor = palette.surface,
                            contentColor = palette.textPrimary,
                            depth = 4.dp
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Replay", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        }

                        // Return to Level Select Menu
                        Cubix3DButton(
                            onClick = onLevelSelect,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            backgroundColor = palette.surface,
                            contentColor = palette.textPrimary,
                            depth = 4.dp
                        ) {
                            Icon(Icons.Default.GridView, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Levels", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        }
                    }
                }
            }
        }
    }
}
