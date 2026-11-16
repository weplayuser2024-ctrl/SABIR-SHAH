package com.example.ui.screens.games

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.ZyroVictoryConfettiOverlay
import com.example.ui.components.zyroShimmerSweep
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private enum class CoinType { WHITE, BLACK, QUEEN, STRIKER }

private data class CarromPiece(
    val id: Int,
    val type: CoinType,
    var x: Float, // normalized 0f..1f inside inner playing surface
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val radius: Float = 0.032f,
    var pocketed: Boolean = false
)

@Composable
fun CarromBoardGameScreen(
    onBack: () -> Unit,
    onMatchWin: (charmGain: Long, coinGain: Long) -> Unit
) {
    BackHandler(onBack = onBack)

    val scope = rememberCoroutineScope()
    var pieces by remember { mutableStateOf(createInitialCarromPieces()) }
    var strikerBaseX by remember { mutableFloatStateOf(0.5f) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCurrent by remember { mutableStateOf<Offset?>(null) }
    var isSimulating by remember { mutableStateOf(false) }
    var playerScore by remember { mutableIntStateOf(0) }
    var shotsTaken by remember { mutableIntStateOf(0) }
    var queenCaptured by remember { mutableStateOf(false) }
    var statusBanner by remember { mutableStateOf("Slide striker below, then pull back on the striker to shoot!") }
    var matchRewarded by remember { mutableStateOf(false) }

    var pocketBurstPos by remember { mutableStateOf<Offset?>(null) }
    var pocketBurstColor by remember { mutableStateOf(ZyroGold) }
    val pocketBurstAnim = remember { Animatable(0f) }

    val animatedScore by animateIntAsState(
        targetValue = playerScore,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "carrom_score"
    )
    val strikerPulse by rememberInfiniteTransition(label = "carrom_aura").animateFloat(
        initialValue = 0.88f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(950, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "striker_pulse"
    )

    val remainingTargetCoins = pieces.count { !it.pocketed && it.type != CoinType.STRIKER }

    // Physics simulation loop
    LaunchedEffect(isSimulating) {
        if (!isSimulating) return@LaunchedEffect
        val pockets = listOf(
            Offset(0.055f, 0.055f),
            Offset(0.945f, 0.055f),
            Offset(0.055f, 0.945f),
            Offset(0.945f, 0.945f)
        )
        val pocketRadius = 0.055f
        val minBound = 0.038f
        val maxBound = 0.962f

        val simList = pieces.map { it.copy() }.toMutableList()
        var step = 0
        while (step < 260) {
            var anyMoving = false
            for (p in simList) {
                if (p.pocketed) continue
                p.x += p.vx
                p.y += p.vy
                p.vx *= 0.968f
                p.vy *= 0.968f
                if (hypot(p.vx, p.vy) < 0.00045f) {
                    p.vx = 0f
                    p.vy = 0f
                } else {
                    anyMoving = true
                }

                // Cushion bounces
                if (p.x - p.radius < minBound) {
                    p.x = minBound + p.radius
                    p.vx = -p.vx * 0.84f
                } else if (p.x + p.radius > maxBound) {
                    p.x = maxBound - p.radius
                    p.vx = -p.vx * 0.84f
                }
                if (p.y - p.radius < minBound) {
                    p.y = minBound + p.radius
                    p.vy = -p.vy * 0.84f
                } else if (p.y + p.radius > maxBound) {
                    p.y = maxBound - p.radius
                    p.vy = -p.vy * 0.84f
                }

                // Pocket check
                for (pk in pockets) {
                    if (hypot(p.x - pk.x, p.y - pk.y) < pocketRadius) {
                        p.pocketed = true
                        p.vx = 0f
                        p.vy = 0f
                        pocketBurstPos = pk
                        pocketBurstColor = when (p.type) {
                            CoinType.QUEEN -> ZyroGold
                            CoinType.WHITE -> ZyroCyan
                            CoinType.BLACK -> ZyroEmerald
                            CoinType.STRIKER -> ZyroCrimson
                        }
                        scope.launch {
                            pocketBurstAnim.snapTo(0f)
                            pocketBurstAnim.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
                        }
                        when (p.type) {
                            CoinType.QUEEN -> {
                                playerScore += 50
                                queenCaptured = true
                                statusBanner = "Queen Pocketed! +50 pts!"
                            }
                            CoinType.WHITE -> {
                                playerScore += 20
                                statusBanner = "White Coin Pocketed! +20 pts"
                            }
                            CoinType.BLACK -> {
                                playerScore += 10
                                statusBanner = "Black Coin Pocketed! +10 pts"
                            }
                            CoinType.STRIKER -> {
                                playerScore = (playerScore - 10).coerceAtLeast(0)
                                statusBanner = "Striker Foul! -10 pts"
                            }
                        }
                        break
                    }
                }
            }

            // Elastic piece-to-piece collisions
            for (i in 0 until simList.size) {
                val a = simList[i]
                if (a.pocketed) continue
                for (j in i + 1 until simList.size) {
                    val b = simList[j]
                    if (b.pocketed) continue
                    val dx = b.x - a.x
                    val dy = b.y - a.y
                    val dist = hypot(dx, dy)
                    val minDist = a.radius + b.radius
                    if (dist > 0.0001f && dist < minDist) {
                        val nx = dx / dist
                        val ny = dy / dist
                        val overlap = (minDist - dist) * 0.5f
                        a.x -= nx * overlap
                        a.y -= ny * overlap
                        b.x += nx * overlap
                        b.y += ny * overlap

                        val dvx = a.vx - b.vx
                        val dvy = a.vy - b.vy
                        val dot = dvx * nx + dvy * ny
                        if (dot > 0f) {
                            val impulse = dot * 0.92f
                            a.vx -= impulse * nx
                            a.vy -= impulse * ny
                            b.vx += impulse * nx
                            b.vy += impulse * ny
                        }
                    }
                }
            }

            pieces = simList.map { it.copy() }
            if (!anyMoving) break
            step++
            delay(16L)
        }

        // Reset striker on baseline
        val reset = simList.map {
            if (it.type == CoinType.STRIKER) {
                it.copy(x = strikerBaseX, y = 0.805f, vx = 0f, vy = 0f, pocketed = false)
            } else it
        }
        pieces = reset
        isSimulating = false

        if ((playerScore >= 100 || reset.count { !it.pocketed && it.type != CoinType.STRIKER } == 0) && !matchRewarded) {
            matchRewarded = true
            onMatchWin(140L, 180L)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top HUD
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("carrom_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Games")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ZYRO CARROM PRO", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Target: 100 pts to Win Reward",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = {
                    pieces = createInitialCarromPieces()
                    playerScore = 0
                    shotsTaken = 0
                    queenCaptured = false
                    matchRewarded = false
                    strikerBaseX = 0.5f
                    statusBanner = "Fresh Carrom rack! Pull back to strike."
                },
                modifier = Modifier.testTag("carrom_reset_button")
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Reset Board")
            }
        }

        // Score strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .zyroShimmerSweep(ZyroGold, durationMs = 3000)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("SCORE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$animatedScore pts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ZyroGold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("COINS LEFT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$remainingTargetCoins", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("QUEEN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (queenCaptured) "Captured 👑" else "On Board",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (queenCaptured) ZyroEmerald else ZyroCrimson
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = statusBanner,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Real-Life Carrom Board Canvas
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val boardSide = minOf(maxWidth, maxHeight)
            Box(
                modifier = Modifier
                    .size(boardSide)
                    .clip(RoundedCornerShape(22.dp))
                    .border(2.dp, Color(0xFF2D190A), RoundedCornerShape(22.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isSimulating, strikerBaseX) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    if (!isSimulating) {
                                        dragStart = offset
                                        dragCurrent = offset
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    if (!isSimulating) {
                                        dragCurrent = change.position
                                    }
                                },
                                onDragEnd = {
                                    val s = dragStart
                                    val c = dragCurrent
                                    if (!isSimulating && s != null && c != null) {
                                        val dx = s.x - c.x
                                        val dy = s.y - c.y
                                        val pull = hypot(dx, dy)
                                        if (pull > 18f) {
                                            val maxPull = size.width * 0.28f
                                            val power = (pull / maxPull).coerceIn(0.15f, 1f) * 0.045f
                                            val angle = atan2(dy, dx)
                                            val vx = cos(angle) * power
                                            val vy = sin(angle) * power
                                            pieces = pieces.map {
                                                if (it.type == CoinType.STRIKER) {
                                                    it.copy(x = strikerBaseX, y = 0.805f, vx = vx, vy = vy, pocketed = false)
                                                } else it
                                            }
                                            shotsTaken++
                                            isSimulating = true
                                        }
                                    }
                                    dragStart = null
                                    dragCurrent = null
                                },
                                onDragCancel = {
                                    dragStart = null
                                    dragCurrent = null
                                }
                            )
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val framePad = w * 0.068f
                    val playW = w - framePad * 2f
                    val playH = h - framePad * 2f

                    // Outer Rosewood Frame with bevel
                    drawRoundRect(
                        brush = Brush.linearGradient(listOf(CarromFrameDark, CarromFrameLight, CarromFrameDark)),
                        cornerRadius = CornerRadius(20.dp.toPx())
                    )
                    // Inner wooden bevel shadow
                    drawRoundRect(
                        color = Color(0xFF281408),
                        topLeft = Offset(framePad - 3.dp.toPx(), framePad - 3.dp.toPx()),
                        size = Size(playW + 6.dp.toPx(), playH + 6.dp.toPx()),
                        cornerRadius = CornerRadius(12.dp.toPx())
                    )

                    // Polished Birch Playing Surface
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(CarromSurfaceWarm, CarromSurfaceEdge),
                            center = Offset(w / 2f, h / 2f),
                            radius = playW * 0.72f
                        ),
                        topLeft = Offset(framePad, framePad),
                        size = Size(playW, playH),
                        cornerRadius = CornerRadius(10.dp.toPx())
                    )

                    // Helper to convert normalized coordinates to canvas coordinates
                    fun toPx(nx: Float, ny: Float) = Offset(framePad + nx * playW, framePad + ny * playH)

                    // 4 Corner Pockets with depth shadow
                    val pocketOffsets = listOf(
                        toPx(0.055f, 0.055f),
                        toPx(0.945f, 0.055f),
                        toPx(0.055f, 0.945f),
                        toPx(0.945f, 0.945f)
                    )
                    pocketOffsets.forEach { center ->
                        drawCircle(color = Color(0xFF9C7B56), radius = playW * 0.056f, center = center)
                        drawCircle(color = CarromPocketDark, radius = playW * 0.049f, center = center)
                    }

                    // Ornate Center Mandala / Rose Star
                    val cCenter = toPx(0.5f, 0.5f)
                    drawCircle(
                        color = Color(0xFF3A2518),
                        radius = playW * 0.145f,
                        center = cCenter,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = CarromCrimson.copy(alpha = 0.85f),
                        radius = playW * 0.125f,
                        center = cCenter,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    // 8-point star inside center circle
                    val starPath = Path()
                    for (i in 0 until 16) {
                        val r = if (i % 2 == 0) playW * 0.12f else playW * 0.045f
                        val ang = Math.toRadians(i * 22.5)
                        val px = cCenter.x + (cos(ang) * r).toFloat()
                        val py = cCenter.y + (sin(ang) * r).toFloat()
                        if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
                    }
                    starPath.close()
                    drawPath(starPath, color = CarromCrimson.copy(alpha = 0.35f))
                    drawCircle(color = CarromCrimson, radius = playW * 0.032f, center = cCenter)

                    // 4 Baselines with red circles
                    val lineStroke = 1.5.dp.toPx()
                    val baseLineColor = Color(0xFF3A2518)
                    val endCircleR = playW * 0.024f
                    val baselines = listOf(
                        Pair(toPx(0.21f, 0.195f), toPx(0.79f, 0.195f)),
                        Pair(toPx(0.21f, 0.805f), toPx(0.79f, 0.805f)),
                        Pair(toPx(0.195f, 0.21f), toPx(0.195f, 0.79f)),
                        Pair(toPx(0.805f, 0.21f), toPx(0.805f, 0.79f))
                    )
                    baselines.forEach { (start, end) ->
                        drawLine(baseLineColor, start, end, strokeWidth = lineStroke)
                        drawCircle(CarromCrimson, radius = endCircleR, center = start)
                        drawCircle(baseLineColor, radius = endCircleR, center = start, style = Stroke(1.2.dp.toPx()))
                        drawCircle(CarromCrimson, radius = endCircleR, center = end)
                        drawCircle(baseLineColor, radius = endCircleR, center = end, style = Stroke(1.2.dp.toPx()))
                    }

                    // Diagonal foul arrows toward pockets
                    val arrows = listOf(
                        Pair(toPx(0.28f, 0.28f), toPx(0.11f, 0.11f)),
                        Pair(toPx(0.72f, 0.28f), toPx(0.89f, 0.11f)),
                        Pair(toPx(0.28f, 0.72f), toPx(0.11f, 0.89f)),
                        Pair(toPx(0.72f, 0.72f), toPx(0.89f, 0.89f))
                    )
                    arrows.forEach { (aStart, aEnd) ->
                        drawLine(baseLineColor.copy(alpha = 0.75f), aStart, aEnd, strokeWidth = 1.3.dp.toPx())
                    }

                    // Draw Pieces with 3D drop shadows, motion trails & specular sheen
                    pieces.forEach { piece ->
                        if (piece.pocketed) return@forEach
                        val center = toPx(piece.x, piece.y)
                        val r = piece.radius * playW
                        val speed = hypot(piece.vx, piece.vy)

                        // High-speed velocity motion trail
                        if (speed > 0.002f) {
                            val trailOffset = Offset(-piece.vx * playW * 2.2f, -piece.vy * playH * 2.2f)
                            drawCircle(
                                color = (if (piece.type == CoinType.STRIKER) ZyroCyan else ZyroGold).copy(alpha = 0.28f),
                                radius = r * 0.9f,
                                center = center + trailOffset
                            )
                        }

                        // Drop shadow
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.30f),
                            radius = r,
                            center = center + Offset(2.dp.toPx(), 3.dp.toPx())
                        )

                        when (piece.type) {
                            CoinType.WHITE -> {
                                drawCircle(Color(0xFFFDF8EE), radius = r, center = center)
                                drawCircle(Color(0xFFD5C4A1), radius = r, center = center, style = Stroke(1.4.dp.toPx()))
                                drawCircle(Color(0xFFE9DEC7), radius = r * 0.62f, center = center, style = Stroke(1.2.dp.toPx()))
                                drawCircle(Color.White.copy(alpha = 0.65f), radius = r * 0.25f, center = center + Offset(-r * 0.28f, -r * 0.28f))
                            }
                            CoinType.BLACK -> {
                                drawCircle(Color(0xFF1F232E), radius = r, center = center)
                                drawCircle(Color(0xFF0A0C12), radius = r, center = center, style = Stroke(1.4.dp.toPx()))
                                drawCircle(Color(0xFF475066), radius = r * 0.62f, center = center, style = Stroke(1.2.dp.toPx()))
                                drawCircle(Color.White.copy(alpha = 0.22f), radius = r * 0.25f, center = center + Offset(-r * 0.28f, -r * 0.28f))
                            }
                            CoinType.QUEEN -> {
                                drawCircle(CarromCrimson, radius = r, center = center)
                                drawCircle(ZyroGold.copy(alpha = 0.28f), radius = r * (1.25f * strikerPulse), center = center)
                                drawCircle(ZyroGold, radius = r * 0.68f, center = center, style = Stroke(1.5.dp.toPx()))
                                drawCircle(ZyroGold, radius = r * 0.28f, center = center)
                            }
                            CoinType.STRIKER -> {
                                if (!isSimulating) {
                                    drawCircle(
                                        color = ZyroCyan.copy(alpha = 0.24f),
                                        radius = r * (1.35f * strikerPulse),
                                        center = center,
                                        style = Stroke(width = 1.6.dp.toPx())
                                    )
                                }
                                drawCircle(
                                    brush = Brush.radialGradient(listOf(Color.White, ZyroGold, Color(0xFFD97706))),
                                    radius = r * 1.12f,
                                    center = center
                                )
                                drawCircle(Color(0xFF78350F), radius = r * 1.12f, center = center, style = Stroke(1.8.dp.toPx()))
                                drawCircle(ZyroCyan, radius = r * 0.45f, center = center, style = Stroke(1.5.dp.toPx()))
                            }
                        }
                    }

                    // Pocket Shockwave & Star Burst VFX
                    val burstP = pocketBurstPos
                    if (burstP != null && pocketBurstAnim.value > 0f && pocketBurstAnim.value < 1f) {
                        val bp = pocketBurstAnim.value
                        val bCenter = toPx(burstP.x, burstP.y)
                        drawCircle(
                            color = pocketBurstColor.copy(alpha = (1f - bp) * 0.85f),
                            radius = playW * (0.04f + 0.12f * bp),
                            center = bCenter,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        for (i in 0 until 8) {
                            val ang = Math.toRadians(i * 45.0 + bp * 40.0)
                            val dist = playW * (0.04f + 0.09f * bp)
                            drawCircle(
                                color = pocketBurstColor.copy(alpha = 1f - bp),
                                radius = 3.dp.toPx(),
                                center = Offset(bCenter.x + (cos(ang) * dist).toFloat(), bCenter.y + (sin(ang) * dist).toFloat())
                            )
                        }
                    }

                    // Aiming Slingshot Trajectory Guide
                    val s = dragStart
                    val c = dragCurrent
                    if (!isSimulating && s != null && c != null) {
                        val dx = s.x - c.x
                        val dy = s.y - c.y
                        val dist = hypot(dx, dy)
                        if (dist > 14f) {
                            val strikerPos = toPx(strikerBaseX, 0.805f)
                            val aimTarget = strikerPos + Offset(dx * 1.4f, dy * 1.4f)
                            drawLine(
                                color = ZyroCyan,
                                start = strikerPos,
                                end = aimTarget,
                                strokeWidth = 3.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                            )
                            drawCircle(color = ZyroGold, radius = 6.dp.toPx(), center = aimTarget)
                        }
                    }
                }
            }
        }

        // Striker Baseline Position Slider
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Striker Pos", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.width(10.dp))
            Slider(
                value = strikerBaseX,
                onValueChange = { newX ->
                    if (!isSimulating) {
                        strikerBaseX = newX
                        pieces = pieces.map {
                            if (it.type == CoinType.STRIKER) it.copy(x = newX, y = 0.805f) else it
                        }
                    }
                },
                valueRange = 0.22f..0.78f,
                modifier = Modifier.weight(1f)
            )
        }

        if (matchRewarded) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = ZyroGold)
                Text(
                    "Victory! +140 Charm & +180 Coins credited!",
                    color = ZyroGold,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    ZyroVictoryConfettiOverlay(visible = matchRewarded)
    }
}

private fun createInitialCarromPieces(): List<CarromPiece> {
    val list = mutableListOf<CarromPiece>()
    var id = 1
    // Queen at center
    list.add(CarromPiece(id++, CoinType.QUEEN, 0.5f, 0.5f))
    // Inner ring of 6 alternating white/black
    val r1 = 0.068f
    for (i in 0 until 6) {
        val ang = Math.toRadians(i * 60.0)
        val t = if (i % 2 == 0) CoinType.WHITE else CoinType.BLACK
        list.add(
            CarromPiece(
                id++,
                t,
                0.5f + (cos(ang) * r1).toFloat(),
                0.5f + (sin(ang) * r1).toFloat()
            )
        )
    }
    // Outer ring of 12
    val r2 = 0.132f
    for (i in 0 until 12) {
        val ang = Math.toRadians(i * 30.0 + 15.0)
        val t = if (i % 2 == 0) CoinType.WHITE else CoinType.BLACK
        list.add(
            CarromPiece(
                id++,
                t,
                0.5f + (cos(ang) * r2).toFloat(),
                0.5f + (sin(ang) * r2).toFloat()
            )
        )
    }
    // Striker
    list.add(CarromPiece(id, CoinType.STRIKER, 0.5f, 0.805f, radius = 0.036f))
    return list
}
