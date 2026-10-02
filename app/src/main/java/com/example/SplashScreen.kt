package com.shopvion.flowgridgame

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopvion.flowgridgame.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit
) {
    val palette = cubixPalette
    val scaleAnim = remember { Animatable(0.75f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
        }
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350)
            )
        }
        delay(750)
        onNavigateToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onNavigateToHome() },
        contentAlignment = Alignment.Center
    ) {
        CubistBackground(modifier = Modifier.fillMaxSize())

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scaleAnim.value)
        ) {
            // 3D Cubist App Icon Centerpiece
            Cubix3DCard(
                modifier = Modifier.size(136.dp),
                shape = RoundedCornerShape(34.dp),
                elevation = 14.dp,
                borderBevel = true
            ) {
                // 2x2 Grid of 3D Cubist Prisms
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CubistMiniCube(FlowRed, FlowRedDark, FlowRedLight)
                        CubistMiniCube(FlowBlue, FlowBlueDark, FlowBlueLight)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CubistMiniCube(FlowGreen, FlowGreenDark, FlowGreenLight)
                        CubistMiniCube(FlowYellow, FlowYellowDark, FlowYellowLight)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Game Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Flow",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = FlowBlue,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Grid",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = FlowGreen,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tagline
            Text(
                text = GameConfig.GAME_SUBTITLE,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // 3D Loading indicator dots
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(FlowRed, FlowBlue, FlowGreen, FlowYellow).forEachIndexed { i, color ->
                    val infiniteTransition = rememberInfiniteTransition(label = "splashDot$i")
                    val dotScale by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1.25f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = i * 150),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale$i"
                    )
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .scale(dotScale)
                            .shadow(3.dp, RoundedCornerShape(4.dp), ambientColor = color, spotColor = color)
                            .background(color, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun CubistMiniCube(
    color: Color,
    darkShade: Color,
    lightShade: Color
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .shadow(4.dp, RoundedCornerShape(10.dp), ambientColor = darkShade, spotColor = darkShade)
            .background(darkShade, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 3.dp)
                .background(color, RoundedCornerShape(10.dp))
                .drawBehind {
                    val bevelW = 1.2.dp.toPx()
                    drawRoundRect(
                        color = lightShade.copy(alpha = 0.6f),
                        topLeft = Offset(0.5f, 0.5f),
                        size = Size(size.width - 1f, size.height - 1f),
                        cornerRadius = CornerRadius(10.dp.toPx()),
                        style = Stroke(width = bevelW)
                    )
                }
                .clip(RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color.White.copy(alpha = 0.5f), CircleShape)
            )
        }
    }
}
