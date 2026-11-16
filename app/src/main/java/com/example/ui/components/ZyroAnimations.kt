package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ZyroCyan
import com.example.ui.theme.ZyroGold
import com.example.ui.theme.ZyroPink
import com.example.ui.theme.ZyroViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-precision, resolution-independent vector aurora + bokeh particle field
 * rendered cleanly at native display density (including 4K panels).
 */
@Composable
fun ZyroAuroraStageBackdrop(
    primaryAccent: Color = ZyroCyan,
    secondaryAccent: Color = ZyroViolet,
    audioEnergy: Float = 0f,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "zyro_4k_aurora")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aurora_phase"
    )
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_drift"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val boost = 1f + audioEnergy * 0.45f

        // Dual volumetric radial light blooms
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryAccent.copy(alpha = (0.20f * boost).coerceAtMost(0.45f)),
                    Color.Transparent
                ),
                center = Offset(w * (0.25f + 0.12f * cos(phase)), h * (0.22f + 0.06f * sin(phase))),
                radius = w * 0.72f
            ),
            radius = w * 0.72f,
            center = Offset(w * (0.25f + 0.12f * cos(phase)), h * (0.22f + 0.06f * sin(phase)))
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondaryAccent.copy(alpha = (0.18f * boost).coerceAtMost(0.42f)),
                    Color.Transparent
                ),
                center = Offset(w * (0.78f - 0.10f * sin(phase)), h * (0.48f + 0.08f * cos(phase))),
                radius = w * 0.65f
            ),
            radius = w * 0.65f,
            center = Offset(w * (0.78f - 0.10f * sin(phase)), h * (0.48f + 0.08f * cos(phase)))
        )

        // Smooth vector aurora wave ribbons
        for (ribbon in 0..2) {
            val baseY = h * (0.24f + ribbon * 0.16f)
            val amp = (18f + ribbon * 10f + audioEnergy * 26f).dp.toPx()
            val path = Path()
            val steps = 28
            for (s in 0..steps) {
                val t = s.toFloat() / steps
                val px = t * w
                val py = baseY + sin(t * 4.5f + phase + ribbon * 1.4f) * amp
                if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            val ribbonColor = when (ribbon) {
                0 -> primaryAccent.copy(alpha = 0.22f)
                1 -> secondaryAccent.copy(alpha = 0.18f)
                else -> ZyroGold.copy(alpha = 0.14f)
            }
            drawPath(
                path = path,
                color = ribbonColor,
                style = Stroke(width = (1.6f + audioEnergy * 1.8f).dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Crisp 4K vector starlight & bokeh motes
        for (i in 0 until 26) {
            val seedX = ((i * 137) % 100) / 100f
            val seedY = ((i * 223) % 100) / 100f
            val px = ((seedX + drift * 0.05f * if (i % 2 == 0) 1f else -1f).coerceIn(0.02f, 0.98f)) * w
            val py = ((seedY + sin(phase + i * 0.7f) * 0.025f).coerceIn(0.02f, 0.98f)) * h
            val r = (1.6f + (i % 4) * 1.1f).dp.toPx()
            val alpha = (0.24f + 0.36f * ((sin(phase * 1.5f + i) + 1f) * 0.5f)).coerceIn(0.1f, 0.7f)
            val moteColor = when (i % 4) {
                0 -> primaryAccent
                1 -> ZyroGold
                2 -> ZyroPink
                else -> Color.White
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(moteColor.copy(alpha = alpha), Color.Transparent),
                    center = Offset(px, py),
                    radius = r * 2.6f
                ),
                radius = r * 2.6f,
                center = Offset(px, py)
            )
            drawCircle(color = moteColor.copy(alpha = alpha), radius = r * 0.65f, center = Offset(px, py))
        }
    }
}

/**
 * Diagonal high-gloss light sweep across premium surfaces and cards.
 */
fun Modifier.zyroShimmerSweep(
    accent: Color = Color.White,
    durationMs: Int = 2600
): Modifier = composed {
    val infinite = rememberInfiniteTransition(label = "shimmer_sweep")
    val progress by infinite.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_x"
    )

    this.drawWithContent {
        drawContent()
        val startX = size.width * progress
        val band = size.width * 0.36f
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    accent.copy(alpha = 0.14f),
                    Color.Transparent
                ),
                start = Offset(startX - band, 0f),
                end = Offset(startX + band, size.height)
            )
        )
    }
}

/**
 * Spring physics micro-interaction scale on press for tactile 60-120fps feedback
 * without consuming pointer events needed by downstream clickable modifiers.
 */
fun Modifier.zyroSpringScale(pressedScale: Float = 0.965f): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "spring_scale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    pressed = event.changes.any { it.pressed }
                }
            }
        }
}

/**
 * High-resolution celebration confetti + sparkle shower rendered with crisp rotated diamonds and stars.
 */
@Composable
fun ZyroVictoryConfettiOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    val infinite = rememberInfiniteTransition(label = "victory_confetti")
    val fall by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_fall"
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val palette = listOf(ZyroGold, ZyroCyan, ZyroPink, ZyroViolet, Color.White)
        for (i in 0 until 32) {
            val baseX = ((i * 97) % 100) / 100f * w
            val speed = 0.7f + (i % 4) * 0.25f
            val yFrac = ((fall * speed + (i * 0.08f)) % 1f)
            val py = yFrac * h
            val px = baseX + sin(yFrac * 10f + i) * 18.dp.toPx()
            val sizePx = (4f + (i % 3) * 2.2f).dp.toPx()
            val c = palette[i % palette.size].copy(alpha = (1f - yFrac * 0.55f).coerceIn(0.2f, 0.9f))
            val diamond = Path().apply {
                moveTo(px, py - sizePx)
                lineTo(px + sizePx * 0.7f, py)
                lineTo(px, py + sizePx)
                lineTo(px - sizePx * 0.7f, py)
                close()
            }
            drawPath(diamond, color = c)
        }
    }
}
