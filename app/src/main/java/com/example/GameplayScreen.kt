package com.shopvion.flowgrid

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.shopvion.flowgrid.ui.theme.*

/**
 * Validates if a single color pair is fully and legally connected.
 * Checks BOTH directions:
 * Path Start == Dot A & Path End == Dot B, OR Path Start == Dot B & Path End == Dot A.
 */
fun isColorPairConnected(pair: ColorPair, path: List<GridPoint>?): Boolean {
    if (path == null || path.size < 2) return false
    val first = path.first()
    val last = path.last()
    return (first == pair.start && last == pair.end) || (first == pair.end && last == pair.start)
}

/**
 * Calculates total count of completed and connected color flows.
 */
fun countConnectedFlows(
    levelData: LevelData,
    paths: Map<Int, List<GridPoint>>,
    currentActiveColorId: Int? = null,
    currentActivePath: List<GridPoint> = emptyList()
): Int {
    return levelData.colorPairs.count { pair ->
        val p = if (pair.colorId == currentActiveColorId) currentActivePath else paths[pair.colorId]
        isColorPairConnected(pair, p)
    }
}

/**
 * Collects all uniquely occupied grid cells across completed and active pipes.
 */
fun calculateOccupiedCells(
    paths: Map<Int, List<GridPoint>>,
    currentActiveColorId: Int? = null,
    currentActivePath: List<GridPoint> = emptyList()
): Set<GridPoint> {
    val occupied = HashSet<GridPoint>()
    paths.forEach { (cId, p) ->
        if (cId != currentActiveColorId) {
            occupied.addAll(p)
        }
    }
    if (currentActivePath.isNotEmpty()) {
        occupied.addAll(currentActivePath)
    }
    return occupied
}

/**
 * Generates orthogonal discrete grid steps between two points to handle fast touch drag without dropping cells.
 */
fun getOrthogonalSteps(from: GridPoint, to: GridPoint): List<GridPoint> {
    val steps = mutableListOf<GridPoint>()
    var cx = from.x
    var cy = from.y
    while (cx != to.x || cy != to.y) {
        if (cx < to.x) cx++
        else if (cx > to.x) cx--
        else if (cy < to.y) cy++
        else if (cy > to.y) cy--
        steps.add(GridPoint(cx, cy))
    }
    return steps
}

@Composable
fun GameplayScreen(
    levelNumber: Int,
    audioEngine: AudioEngine,
    prefsManager: PreferencesManager,
    onNavigateBack: () -> Unit,
    onNextLevel: (Int) -> Unit
) {
    val palette = cubixPalette

    // -------------------------------------------------------------
    // 1. GAMEPLAY CORE LEVEL RESOLUTION
    // -------------------------------------------------------------
    var currentLevelId by remember(levelNumber) { mutableIntStateOf(levelNumber) }

    LaunchedEffect(levelNumber) {
        if (currentLevelId != levelNumber) {
            currentLevelId = levelNumber
        }
    }

    // Wrap the entire level game state in key(currentLevelId).
    // Guarantees all state resets cleanly when navigating to a new level.
    key(currentLevelId) {
        val levelData = remember(currentLevelId) { LevelRepository.getLevel(currentLevelId) }
        val gridSize = levelData.gridSize
        val totalCells = levelData.totalCells
        val totalColorPairs = levelData.colorPairs.size

        val endpointsMap = remember(levelData) {
            val map = mutableMapOf<GridPoint, ColorPair>()
            levelData.colorPairs.forEach {
                map[it.start] = it
                map[it.end] = it
            }
            map
        }

        // Board State
        val context = LocalContext.current
        val activity = context as? Activity

        var colorPaths by remember(currentLevelId) { mutableStateOf<Map<Int, List<GridPoint>>>(emptyMap()) }
        var activeColorId by remember(currentLevelId) { mutableStateOf<Int?>(null) }
        var activePath by remember(currentLevelId) { mutableStateOf<List<GridPoint>>(emptyList()) }

        var movesCount by remember(currentLevelId) { mutableIntStateOf(0) }
        var isInputEnabled by remember(currentLevelId) { mutableStateOf(true) }
        var isLevelWon by remember(currentLevelId) { mutableStateOf(false) }

        // Modals & Overlays
        var showWinModal by remember(currentLevelId) { mutableStateOf(false) }
        var showRewardedAd by remember(currentLevelId) { mutableStateOf(false) }

        var remainingHints by remember(currentLevelId) { mutableIntStateOf(prefsManager.getHints()) }
        var soundFxOn by remember { mutableStateOf(prefsManager.isSoundFxEnabled()) }

        // Lazily pre-load rewarded ads in background when entering gameplay
        LaunchedEffect(Unit) {
            AdManager.loadRewarded(context.applicationContext)
        }

        // -------------------------------------------------------------
        // 2. LIVE TELEMETRY CALCULATIONS
        // -------------------------------------------------------------
        val occupiedCells = remember(colorPaths, activePath, activeColorId) {
            calculateOccupiedCells(colorPaths, activeColorId, activePath)
        }

        val pipeCoveragePercent = remember(occupiedCells.size, totalCells) {
            if (totalCells > 0) {
                ((occupiedCells.size.toFloat() / totalCells.toFloat()) * 100).toInt().coerceAtMost(100)
            } else 0
        }

        val connectedFlowsCount = remember(colorPaths, activePath, activeColorId, levelData) {
            countConnectedFlows(levelData, colorPaths, activeColorId, activePath)
        }

        // -------------------------------------------------------------
        // 3. WIN EVALUATION ENGINE
        // -------------------------------------------------------------
        fun evaluateWinState(pathsToCheck: Map<Int, List<GridPoint>>) {
            if (isLevelWon) return

            val connectedCount = countConnectedFlows(levelData, pathsToCheck)
            val isAllConnected = connectedCount == totalColorPairs

            if (isAllConnected) {
                isLevelWon = true
                isInputEnabled = false

                audioEngine.playLevelWin(soundFxOn)

                val occupiedSet = calculateOccupiedCells(pathsToCheck)
                val isFullCoverage = occupiedSet.size == totalCells
                val isTargetMoves = movesCount <= levelData.minMoves

                val stars = when {
                    isFullCoverage && isTargetMoves -> 3
                    isFullCoverage || isTargetMoves -> 2
                    else -> 1
                }

                prefsManager.saveLevelScore(currentLevelId, movesCount, stars)
                showWinModal = true
            }
        }

        LaunchedEffect(colorPaths, activePath, activeColorId, isLevelWon, levelData) {
            if (!isLevelWon) {
                val pathsToCheck = if (activeColorId != null && activePath.size >= 2) {
                    colorPaths + (activeColorId!! to activePath)
                } else {
                    colorPaths
                }
                val connected = countConnectedFlows(levelData, pathsToCheck)
                if (connected == totalColorPairs) {
                    evaluateWinState(pathsToCheck)
                }
            }
        }

        // -------------------------------------------------------------
        // 4. LEVEL ADVANCEMENT & BOARD MANAGEMENT
        // -------------------------------------------------------------
        fun loadLevel(targetLevelId: Int) {
            val safeTarget = targetLevelId.coerceIn(1, GameConfig.TOTAL_LEVELS)
            showWinModal = false

            colorPaths = emptyMap()
            activeColorId = null
            activePath = emptyList()
            movesCount = 0
            isLevelWon = false
            isInputEnabled = true
            remainingHints = prefsManager.getHints()

            currentLevelId = safeTarget
            onNextLevel(safeTarget)
        }

        fun replayLevel() {
            showWinModal = false
            colorPaths = emptyMap()
            activeColorId = null
            activePath = emptyList()
            movesCount = 0
            isLevelWon = false
            isInputEnabled = true
            remainingHints = prefsManager.getHints()
            audioEngine.playLineCut(soundFxOn)
        }

        fun advanceToNextLevel() {
            val nextLevelId = currentLevelId + 1
            if (nextLevelId > GameConfig.TOTAL_LEVELS) {
                showWinModal = false
                onNavigateBack()
                return
            }

            showWinModal = false
            // Show real AdMob Interstitial Ad every 3 levels
            if (currentLevelId % GameConfig.INTERSTITIAL_INTERVAL_LEVELS == 0 && activity != null) {
                AdManager.showInterstitial(activity) {
                    loadLevel(nextLevelId)
                }
            } else {
                loadLevel(nextLevelId)
            }
        }

        fun resetBoard() {
            colorPaths = emptyMap()
            activePath = emptyList()
            activeColorId = null
            movesCount = 0
            isLevelWon = false
            isInputEnabled = true
            audioEngine.playLineCut(soundFxOn)
        }

        fun applyHint() {
            if (!isInputEnabled || isLevelWon) return
            if (remainingHints <= 0) {
                showRewardedAd = true
                return
            }

            val unconnected = levelData.colorPairs.firstOrNull { pair ->
                val p = colorPaths[pair.colorId]
                !isColorPairConnected(pair, p)
            }

            if (unconnected != null && unconnected.solutionPath.isNotEmpty()) {
                if (prefsManager.useHint()) {
                    remainingHints = prefsManager.getHints()

                    val newPaths = colorPaths.toMutableMap()
                    val hintPath = unconnected.solutionPath

                    newPaths.forEach { (otherCid, path) ->
                        if (otherCid != unconnected.colorId) {
                            val filtered = path.filter { it !in hintPath }
                            if (filtered.size >= 2) {
                                newPaths[otherCid] = filtered
                            } else {
                                newPaths.remove(otherCid)
                            }
                        }
                    }

                    newPaths[unconnected.colorId] = hintPath
                    colorPaths = newPaths
                    movesCount++
                    audioEngine.playPopConnect(soundFxOn)

                    evaluateWinState(newPaths)
                }
            }
        }

        fun showRewardedAdForHints() {
            if (activity != null) {
                AdManager.showRewarded(
                    activity = activity,
                    onRewardEarned = {
                        prefsManager.addHints(2)
                        remainingHints = prefsManager.getHints()
                        applyHint()
                    },
                    onDismissed = {
                        showRewardedAd = false
                    }
                )
            } else {
                prefsManager.addHints(2)
                remainingHints = prefsManager.getHints()
                showRewardedAd = false
                applyHint()
            }
        }

        // -------------------------------------------------------------
        // 5. 3D CUBISM USER INTERFACE
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Dynamic Cubist multi-plane background
            CubistBackground(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top App Bar: Back Button, Level Info, Hint Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Cubix3DButton(
                        onClick = onNavigateBack,
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

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Level $currentLevelId",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = palette.textPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${gridSize}x${gridSize} Cubist Grid",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textSecondary
                        )
                    }

                    // 3D Harmonious Hint Button with Jewel Counter Badge
                    HintButton3D(
                        hintsCount = remainingHints,
                        onClick = { applyHint() }
                    )
                }

                // Stats Bar (Moves, Flows, Pipe %) in a 3D Cubist Card
                Cubix3DCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    elevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatItem(
                            label = "MOVES",
                            value = "$movesCount",
                            subText = "Target: ${levelData.minMoves}",
                            color = if (movesCount <= levelData.minMoves) FlowGreen else palette.textPrimary
                        )

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(26.dp)
                                .background(palette.border.copy(alpha = 0.5f))
                        )

                        StatItem(
                            label = "FLOWS",
                            value = "$connectedFlowsCount / $totalColorPairs",
                            subText = if (connectedFlowsCount == totalColorPairs) "Done" else "Active",
                            color = if (connectedFlowsCount == totalColorPairs) FlowGreen else FlowBlue
                        )

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(26.dp)
                                .background(palette.border.copy(alpha = 0.5f))
                        )

                        StatItem(
                            label = "PIPE",
                            value = "$pipeCoveragePercent%",
                            subText = "${occupiedCells.size}/$totalCells cells",
                            color = if (pipeCoveragePercent == 100) FlowGreen else FlowOrange
                        )
                    }
                }

                // Interactive 3D Cubist Board Arena
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .aspectRatio(1f)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer 3D Recessed Cubist Tray
                    Cubix3DCard(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(26.dp),
                        elevation = 10.dp,
                        backgroundColor = palette.boardBackground,
                        borderBevel = true
                    ) {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            val pulseAnim = rememberInfiniteTransition(label = "pulse")
                            val pulseScale by pulseAnim.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.15f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(900, easing = EaseInOutSine),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "dotPulse"
                            )

                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(currentLevelId, isInputEnabled, isLevelWon) {
                                        if (!isInputEnabled || isLevelWon) return@pointerInput

                                        val cSize = size.width / gridSize.toFloat()

                                        fun offsetToGrid(offset: Offset): GridPoint? {
                                            val gx = (offset.x / cSize).toInt()
                                            val gy = (offset.y / cSize).toInt()
                                            return if (gx in 0 until gridSize && gy in 0 until gridSize) {
                                                GridPoint(gx, gy)
                                            } else null
                                        }

                                        detectDragGestures(
                                            onDragStart = { startOffset ->
                                                if (!isInputEnabled || isLevelWon) return@detectDragGestures
                                                val pt = offsetToGrid(startOffset) ?: return@detectDragGestures
                                                val ep = endpointsMap[pt]
                                                if (ep != null) {
                                                    activeColorId = ep.colorId
                                                    activePath = listOf(pt)
                                                    colorPaths = colorPaths - ep.colorId
                                                    audioEngine.playPopConnect(soundFxOn)
                                                } else {
                                                    for ((cId, path) in colorPaths) {
                                                        val idx = path.indexOf(pt)
                                                        if (idx != -1) {
                                                            activeColorId = cId
                                                            activePath = path.subList(0, idx + 1)
                                                            colorPaths = colorPaths - cId
                                                            break
                                                        }
                                                    }
                                                }
                                            },
                                            onDrag = { change, _ ->
                                                if (!isInputEnabled || isLevelWon) return@detectDragGestures
                                                change.consume()
                                                val cId = activeColorId ?: return@detectDragGestures
                                                val pt = offsetToGrid(change.position) ?: return@detectDragGestures
                                                if (activePath.isEmpty()) return@detectDragGestures
                                                val last = activePath.last()

                                                if (pt == last) return@detectDragGestures

                                                val colorPair = levelData.colorPairs.first { it.colorId == cId }
                                                val steps = getOrthogonalSteps(last, pt)

                                                for (step in steps) {
                                                    val curPath = activePath
                                                    if (curPath.isEmpty()) break

                                                    // 1. Retract
                                                    if (curPath.size >= 2 && step == curPath[curPath.size - 2]) {
                                                        activePath = curPath.dropLast(1)
                                                        continue
                                                    }

                                                    // 2. Truncate loop
                                                    if (step in curPath) {
                                                        val idx = curPath.indexOf(step)
                                                        activePath = curPath.subList(0, idx + 1)
                                                        continue
                                                    }

                                                    val endpointAtStep = endpointsMap[step]

                                                    // 3. Block cross endpoint
                                                    if (endpointAtStep != null && endpointAtStep.colorId != cId) {
                                                        break
                                                    }

                                                    // 4. Overlap & Cut
                                                    val newPaths = colorPaths.toMutableMap()
                                                    var lineWasCut = false
                                                    newPaths.forEach { (otherCid, path) ->
                                                        if (otherCid != cId && step in path) {
                                                            val cutIdx = path.indexOf(step)
                                                            newPaths[otherCid] = path.subList(0, cutIdx)
                                                            lineWasCut = true
                                                        }
                                                    }
                                                    if (lineWasCut) {
                                                        colorPaths = newPaths
                                                        audioEngine.playLineCut(soundFxOn)
                                                    }

                                                    // 5. Connect target
                                                    val targetEndpoint = if (curPath.first() == colorPair.start) colorPair.end else colorPair.start
                                                    if (step == targetEndpoint) {
                                                        val completedPath = curPath + step
                                                        val updatedPaths = colorPaths + (cId to completedPath)
                                                        colorPaths = updatedPaths
                                                        activeColorId = null
                                                        activePath = emptyList()
                                                        movesCount++
                                                        audioEngine.playPopConnect(soundFxOn)

                                                        evaluateWinState(updatedPaths)
                                                        break
                                                    } else {
                                                        activePath = curPath + step
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                if (!isInputEnabled || isLevelWon) return@detectDragGestures
                                                val cId = activeColorId
                                                val currentPath = activePath
                                                var updatedPaths = colorPaths

                                                if (cId != null && currentPath.size >= 2) {
                                                    val colorPair = levelData.colorPairs.firstOrNull { it.colorId == cId }
                                                    if (colorPair != null) {
                                                        val last = currentPath.last()
                                                        val target = if (currentPath.first() == colorPair.start) colorPair.end else colorPair.start
                                                        if (last == target) {
                                                            audioEngine.playPopConnect(soundFxOn)
                                                        }
                                                    }
                                                    updatedPaths = colorPaths + (cId to currentPath)
                                                    colorPaths = updatedPaths
                                                    movesCount++
                                                }
                                                activeColorId = null
                                                activePath = emptyList()

                                                evaluateWinState(updatedPaths)
                                            },
                                            onDragCancel = {
                                                if (!isInputEnabled || isLevelWon) return@detectDragGestures
                                                val cId = activeColorId
                                                val currentPath = activePath
                                                var updatedPaths = colorPaths

                                                if (cId != null && currentPath.size >= 2) {
                                                    updatedPaths = colorPaths + (cId to currentPath)
                                                    colorPaths = updatedPaths
                                                    movesCount++
                                                }
                                                activeColorId = null
                                                activePath = emptyList()

                                                evaluateWinState(updatedPaths)
                                            }
                                        )
                                    }
                            ) {
                                val cSize = size.width / gridSize.toFloat()

                                // -----------------------------------------------------
                                // A. DRAW 3D CUBIST ISOMETRIC RECESSED GRID CELLS
                                // -----------------------------------------------------
                                for (y in 0 until gridSize) {
                                    for (x in 0 until gridSize) {
                                        val cellLeft = x * cSize
                                        val cellTop = y * cSize
                                        val padding = cSize * 0.07f
                                        val cellW = cSize - padding * 2
                                        val cornerR = cSize * 0.2f

                                        // 1. Recessed bottom/right shadow facet
                                        drawRoundRect(
                                            color = palette.cellShadow,
                                            topLeft = Offset(cellLeft + padding + 1.5f, cellTop + padding + 1.5f),
                                            size = Size(cellW, cellW),
                                            cornerRadius = CornerRadius(cornerR)
                                        )

                                        // 2. Main cell plane
                                        drawRoundRect(
                                            color = palette.cellBackground,
                                            topLeft = Offset(cellLeft + padding, cellTop + padding),
                                            size = Size(cellW, cellW),
                                            cornerRadius = CornerRadius(cornerR)
                                        )

                                        // 3. Top-left beveled highlight facet
                                        drawRoundRect(
                                            color = palette.cellHighlight.copy(alpha = if (palette.isDark) 0.25f else 0.8f),
                                            topLeft = Offset(cellLeft + padding, cellTop + padding),
                                            size = Size(cellW, cellW),
                                            cornerRadius = CornerRadius(cornerR),
                                            style = Stroke(width = 1.dp.toPx())
                                        )

                                        // 4. Subtle central cubist diamond indentation
                                        val centerPt = Offset(cellLeft + cSize / 2f, cellTop + cSize / 2f)
                                        val diamondRadius = cSize * 0.08f
                                        val diamondPath = Path().apply {
                                            moveTo(centerPt.x, centerPt.y - diamondRadius)
                                            lineTo(centerPt.x + diamondRadius, centerPt.y)
                                            lineTo(centerPt.x, centerPt.y + diamondRadius)
                                            lineTo(centerPt.x - diamondRadius, centerPt.y)
                                            close()
                                        }
                                        drawPath(diamondPath, palette.cellCenterDot.copy(alpha = 0.45f))
                                    }
                                }

                                // -----------------------------------------------------
                                // B. DRAW 3D EXTRUDED CYLINDRICAL/PRISM PIPES
                                // -----------------------------------------------------
                                fun draw3DPipe(pathPoints: List<GridPoint>, color: Color, darkShade: Color) {
                                    if (pathPoints.size < 2) return
                                    val p = Path()
                                    pathPoints.forEachIndexed { idx, pt ->
                                        val cx = (pt.x + 0.5f) * cSize
                                        val cy = (pt.y + 0.5f) * cSize
                                        if (idx == 0) p.moveTo(cx, cy) else p.lineTo(cx, cy)
                                    }
                                    val pipeWidth = cSize * 0.46f

                                    // 1. Ambient Contact Drop Shadow (cast on recessed tiles)
                                    drawPath(
                                        path = p,
                                        color = Color.Black.copy(alpha = if (palette.isDark) 0.5f else 0.22f),
                                        style = Stroke(
                                            width = pipeWidth + 4.dp.toPx(),
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )

                                    // 2. Bottom 3D Extrusion Rim (dark shade of flow)
                                    drawPath(
                                        path = p,
                                        color = darkShade,
                                        style = Stroke(
                                            width = pipeWidth,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )

                                    // 3. Vibrant Core Flow Color
                                    drawPath(
                                        path = p,
                                        color = color,
                                        style = Stroke(
                                            width = pipeWidth * 0.88f,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )

                                    // 4. Elevated 3D Specular Highlight Ridge
                                    drawPath(
                                        path = p,
                                        color = Color.White.copy(alpha = 0.5f),
                                        style = Stroke(
                                            width = pipeWidth * 0.26f,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }

                                // Render completed pipes
                                colorPaths.forEach { (cId, pathPts) ->
                                    if (cId != activeColorId) {
                                        val colorPair = levelData.colorPairs.first { it.colorId == cId }
                                        draw3DPipe(pathPts, colorPair.color, colorPair.darkColor)
                                    }
                                }

                                // Render active dragging pipe
                                if (activeColorId != null && activePath.isNotEmpty()) {
                                    val colorPair = levelData.colorPairs.first { it.colorId == activeColorId }
                                    draw3DPipe(activePath, colorPair.color, colorPair.darkColor)
                                }

                                // -----------------------------------------------------
                                // C. DRAW 3D CUBIST GEM / NODE ENDPOINTS
                                // -----------------------------------------------------
                                levelData.colorPairs.forEach { pair ->
                                    listOf(pair.start, pair.end).forEach { ep ->
                                        val cx = (ep.x + 0.5f) * cSize
                                        val cy = (ep.y + 0.5f) * cSize
                                        val isConnected = run {
                                            val p = if (pair.colorId == activeColorId) activePath else colorPaths[pair.colorId]
                                            isColorPairConnected(pair, p)
                                        }

                                        val dotRadius = cSize * 0.34f
                                        val cornerRad = dotRadius * 0.45f

                                        // 1. Cast Drop Shadow
                                        drawRoundRect(
                                            color = Color.Black.copy(alpha = if (palette.isDark) 0.55f else 0.28f),
                                            topLeft = Offset(cx - dotRadius + 2.5.dp.toPx(), cy - dotRadius + 3.5.dp.toPx()),
                                            size = Size(dotRadius * 2, dotRadius * 2),
                                            cornerRadius = CornerRadius(cornerRad)
                                        )

                                        // 2. 3D Bottom Extrusion Block
                                        drawRoundRect(
                                            color = pair.darkColor,
                                            topLeft = Offset(cx - dotRadius, cy - dotRadius + 2.5.dp.toPx()),
                                            size = Size(dotRadius * 2, dotRadius * 2),
                                            cornerRadius = CornerRadius(cornerRad)
                                        )

                                        // 3. Main Gem Body Face
                                        drawRoundRect(
                                            color = pair.color,
                                            topLeft = Offset(cx - dotRadius, cy - dotRadius),
                                            size = Size(dotRadius * 2, dotRadius * 2),
                                            cornerRadius = CornerRadius(cornerRad)
                                        )

                                        // 4. Facet Bevel Highlight (top & left edges)
                                        val bevelW = 1.6.dp.toPx()
                                        drawRoundRect(
                                            color = Color.White.copy(alpha = 0.55f),
                                            topLeft = Offset(cx - dotRadius, cy - dotRadius),
                                            size = Size(dotRadius * 2, dotRadius * 2),
                                            cornerRadius = CornerRadius(cornerRad),
                                            style = Stroke(width = bevelW)
                                        )

                                        // 5. Specular Corner Point
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.8f),
                                            radius = dotRadius * 0.22f,
                                            center = Offset(cx - dotRadius * 0.38f, cy - dotRadius * 0.38f)
                                        )

                                        // 6. Connected State: Pulsing Energetic 3D Core Ring
                                        if (isConnected) {
                                            drawCircle(
                                                color = Color.White,
                                                radius = dotRadius * 0.32f,
                                                center = Offset(cx, cy)
                                            )
                                            drawCircle(
                                                color = pair.color,
                                                radius = dotRadius * 0.18f,
                                                center = Offset(cx, cy)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Bar: Reset, Previous, Sound, Next Level
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset Board Button
                    Cubix3DButton(
                        onClick = { resetBoard() },
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        depth = 4.dp
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset Level",
                            tint = palette.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Previous Level
                    Cubix3DButton(
                        onClick = {
                            if (currentLevelId > 1) {
                                loadLevel(currentLevelId - 1)
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        enabled = currentLevelId > 1,
                        depth = 4.dp
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Previous Level",
                            tint = if (currentLevelId > 1) palette.textPrimary else palette.textMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Sound FX Toggle
                    Cubix3DButton(
                        onClick = {
                            soundFxOn = !soundFxOn
                            prefsManager.setSoundFxEnabled(soundFxOn)
                            if (soundFxOn) audioEngine.playPopConnect(true)
                        },
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        depth = 4.dp
                    ) {
                        Icon(
                            if (soundFxOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Sound",
                            tint = if (soundFxOn) FlowBlue else palette.textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Next Level Button (manual skip if already unlocked)
                    val unlocked = prefsManager.getUnlockedLevel()
                    val canNext = currentLevelId < GameConfig.TOTAL_LEVELS && currentLevelId < unlocked
                    Cubix3DButton(
                        onClick = {
                            if (canNext) {
                                loadLevel(currentLevelId + 1)
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        enabled = canNext,
                        depth = 4.dp
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next Level",
                            tint = if (canNext) palette.textPrimary else palette.textMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Inline Adaptive Banner
                AdBannerView()
            }

            // ---------------------------------------------------------
            // OVERLAYS: 3D WIN MODAL & REWARDED AD DIALOG
            // ---------------------------------------------------------
            if (showWinModal) {
                WinModal(
                    levelNumber = currentLevelId,
                    moves = movesCount,
                    minMoves = levelData.minMoves,
                    bestMoves = prefsManager.getLevelBestMoves(currentLevelId),
                    stars = prefsManager.getLevelStars(currentLevelId),
                    pipeCoverage = pipeCoveragePercent,
                    onNextLevel = { advanceToNextLevel() },
                    onReplay = { replayLevel() },
                    onLevelSelect = {
                        showWinModal = false
                        onNavigateBack()
                    }
                )
            }

            if (showRewardedAd) {
                Dialog(onDismissRequest = { showRewardedAd = false }) {
                    Cubix3DCard(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        elevation = 12.dp,
                        borderBevel = true
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                color = FlowGreen.copy(alpha = 0.15f),
                                shape = CircleShape,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = FlowYellow,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Need More Hints?",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = palette.textPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Watch a short video ad to unlock +2 additional puzzle hints!",
                                fontSize = 13.sp,
                                color = palette.textSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Cubix3DButton(
                                    onClick = {
                                        showRewardedAd = false
                                        showRewardedAdForHints()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    backgroundColor = FlowGreen,
                                    contentColor = Color.White,
                                    depth = 4.dp
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Watch Video Ad", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                }

                                Cubix3DButton(
                                    onClick = { showRewardedAd = false },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    backgroundColor = palette.surface,
                                    contentColor = palette.textSecondary,
                                    depth = 3.dp
                                ) {
                                    Text(text = "Not Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    subText: String,
    color: Color
) {
    val palette = cubixPalette
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = palette.textSecondary
        )
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = subText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = palette.textMuted
        )
    }
}

/**
 * Beautiful 3D Cubist Hint Button:
 * - Perfectly sized (44.dp) to mirror the 44.dp Back button for visual harmony
 * - Warm golden amber lighting with subtle breathing glow when hints are active
 * - Cute 3D elevated jewel badge at top-right indicating remaining hint count (or '+' when empty)
 */
@Composable
private fun HintButton3D(
    hintsCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = cubixPalette
    val hasHints = hintsCount > 0

    val infiniteTransition = rememberInfiniteTransition(label = "hintGlow")
    val bulbAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bulbAlpha"
    )

    val buttonBg = when {
        hasHints -> if (palette.isDark) Color(0xFF282012) else Color(0xFFFEF3C7)
        else -> palette.surface
    }

    val iconTint = when {
        hasHints -> FlowYellow.copy(alpha = bulbAlpha)
        else -> palette.textSecondary.copy(alpha = 0.55f)
    }

    Box(
        modifier = modifier.size(46.dp),
        contentAlignment = Alignment.Center
    ) {
        // 3D Circular Button (42.dp)
        Cubix3DButton(
            onClick = onClick,
            modifier = Modifier.size(42.dp),
            shape = CircleShape,
            backgroundColor = buttonBg,
            depth = 3.5.dp
        ) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = "Hint",
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        // Jewel Counter Badge (Top-Right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 1.dp, y = (-1).dp)
                .shadow(
                    elevation = 3.dp,
                    shape = CircleShape,
                    ambientColor = if (hasHints) FlowOrangeDark else palette.surfaceShadow,
                    spotColor = if (hasHints) FlowOrangeDark else palette.surfaceShadowDark
                )
                .background(
                    if (hasHints) FlowOrange else FlowGreen,
                    CircleShape
                )
                .border(
                    width = 1.5.dp,
                    color = palette.background,
                    shape = CircleShape
                )
                .size(if (hasHints && hintsCount >= 10) 20.dp else 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (hasHints) "$hintsCount" else "+",
                fontSize = if (hasHints && hintsCount >= 10) 8.5.sp else 9.5.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

