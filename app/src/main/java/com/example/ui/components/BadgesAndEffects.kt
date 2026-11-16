package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.GiftSendOutcome
import com.example.ui.state.WeddingCelebration
import com.example.ui.theme.*
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class VipTierVisual(
    val tierName: String,
    val entryEffectTitle: String,
    val nameColorDark: Color,
    val nameColorLight: Color,
    val primaryColor: Color,
    val secondaryColor: Color,
    val badgeGradient: List<Color>,
    val darkBadgeText: Boolean
)

fun vipTierVisual(vipLevel: Int): VipTierVisual {
    val clamped = vipLevel.coerceIn(1, 14)
    return when {
        clamped >= 13 -> VipTierVisual(
            tierName = "Celestial Zenith",
            entryEffectTitle = "Celestial Sovereign Arrival",
            nameColorDark = Color(0xFFFFE57F),
            nameColorLight = Color(0xFF9A3412),
            primaryColor = Color(0xFFFFD166),
            secondaryColor = Color(0xFF22D3EE),
            badgeGradient = listOf(Color(0xFFD97706), Color(0xFFFDE047), Color(0xFF22D3EE)),
            darkBadgeText = true
        )
        clamped >= 10 -> VipTierVisual(
            tierName = "Imperial Crimson",
            entryEffectTitle = "Imperial Phoenix Surge",
            nameColorDark = Color(0xFFFF6B9E),
            nameColorLight = Color(0xFFBE123C),
            primaryColor = Color(0xFFFF4D8D),
            secondaryColor = Color(0xFFFFD166),
            badgeGradient = listOf(Color(0xFF881337), Color(0xFFE11D48), Color(0xFFFBBF24)),
            darkBadgeText = false
        )
        clamped >= 7 -> VipTierVisual(
            tierName = "Royal Amethyst",
            entryEffectTitle = "Royal Amethyst Vortex",
            nameColorDark = Color(0xFFD8B4FE),
            nameColorLight = Color(0xFF7E22CE),
            primaryColor = Color(0xFFC084FC),
            secondaryColor = Color(0xFFE879F9),
            badgeGradient = listOf(Color(0xFF5B21B6), Color(0xFFC026D3)),
            darkBadgeText = false
        )
        clamped >= 5 -> VipTierVisual(
            tierName = "Cyber Sapphire",
            entryEffectTitle = "Sapphire Comet Streak",
            nameColorDark = Color(0xFF38BDF8),
            nameColorLight = Color(0xFF0369A1),
            primaryColor = Color(0xFF00E5FF),
            secondaryColor = Color(0xFF3B82F6),
            badgeGradient = listOf(Color(0xFF0369A1), Color(0xFF22D3EE)),
            darkBadgeText = false
        )
        clamped >= 3 -> VipTierVisual(
            tierName = "Emerald Jade",
            entryEffectTitle = "Jade Aurora Sweep",
            nameColorDark = Color(0xFF4ADE80),
            nameColorLight = Color(0xFF047857),
            primaryColor = Color(0xFF10B981),
            secondaryColor = Color(0xFF34D399),
            badgeGradient = listOf(Color(0xFF065F46), Color(0xFF34D399)),
            darkBadgeText = false
        )
        else -> VipTierVisual(
            tierName = "Topaz Spark",
            entryEffectTitle = "Golden Spark Entry",
            nameColorDark = Color(0xFFFFCA64),
            nameColorLight = Color(0xFFB45309),
            primaryColor = Color(0xFFF59E0B),
            secondaryColor = Color(0xFFFFD166),
            badgeGradient = listOf(Color(0xFF92400E), Color(0xFFFBBF24)),
            darkBadgeText = true
        )
    }
}

@Composable
fun vipNameColor(
    isVip: Boolean,
    vipLevel: Int,
    onDarkSurface: Boolean = false,
    fallback: Color = MaterialTheme.colorScheme.onSurface
): Color {
    if (!isVip) return fallback
    val tier = vipTierVisual(vipLevel)
    val useDarkPalette = onDarkSurface || MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return if (useDarkPalette) tier.nameColorDark else tier.nameColorLight
}

@Composable
fun VipNameAndBadge(
    name: String,
    isVip: Boolean,
    vipLevel: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    fontWeight: FontWeight = FontWeight.Bold,
    onDarkSurface: Boolean = false,
    fallbackColor: Color = if (onDarkSurface) Color.White else MaterialTheme.colorScheme.onSurface,
    compactBadge: Boolean = false,
    maxLines: Int = 1
) {
    val resolvedColor = vipNameColor(
        isVip = isVip,
        vipLevel = vipLevel,
        onDarkSurface = onDarkSurface,
        fallback = fallbackColor
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = name,
            color = resolvedColor,
            style = style,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (isVip) {
            VipBadge(vipLevel = vipLevel, compact = compactBadge)
        }
    }
}

@Composable
fun UserAvatar(
    name: String,
    colorHex: Long,
    sizeDp: Int = 46,
    isVip: Boolean = false,
    vipLevel: Int = 1,
    isAdmin: Boolean = false,
    avatarImagePath: String? = null
) {
    val baseColor = Color(colorHex)
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "Z"
    val file = avatarImagePath?.let { File(it) }
    val vipTier = remember(vipLevel) { vipTierVisual(vipLevel) }
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(baseColor, baseColor.copy(alpha = 0.55f))
                )
            )
            .border(
                width = if (isAdmin || isVip) 2.dp else 1.dp,
                brush = Brush.linearGradient(
                    when {
                        isAdmin && isVip -> listOf(vipTier.primaryColor, ZyroCrimson, ZyroGold)
                        isAdmin -> listOf(ZyroCrimson, ZyroGold)
                        isVip -> listOf(vipTier.primaryColor, vipTier.secondaryColor)
                        else -> listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.1f))
                    }
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (file != null && file.exists()) {
            AsyncImage(
                model = file,
                contentDescription = "$name profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (sizeDp * 0.42f).sp
            )
        }
    }
}

@Composable
fun LevelBadge(badgeLabel: String, tierLevel: Int = 1) {
    val bg = when {
        tierLevel >= 7 -> Brush.horizontalGradient(listOf(ZyroGoldDeep, ZyroPink))
        tierLevel >= 5 -> Brush.horizontalGradient(listOf(ZyroViolet, ZyroCyan))
        tierLevel >= 1 -> Brush.horizontalGradient(listOf(Color(0xFF1D4ED8), ZyroCyanDim))
        else -> Brush.horizontalGradient(listOf(Color(0xFF334155), Color(0xFF475569)))
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = badgeLabel,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun VipBadge(vipLevel: Int = 1, compact: Boolean = false) {
    val clamped = vipLevel.coerceIn(1, 14)
    val tier = vipTierVisual(clamped)
    val textColor = if (tier.darkBadgeText) Color(0xFF1C1000) else Color.White
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(tier.badgeGradient))
            .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(
                horizontal = if (compact) 5.dp else 8.dp,
                vertical = if (compact) 1.dp else 2.dp
            )
            .testTag("vip_badge_$clamped"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 3.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.WorkspacePremium,
            contentDescription = "VIP Level $clamped Badge",
            tint = textColor,
            modifier = Modifier.size(if (compact) 10.dp else 12.dp)
        )
        Text(
            text = if (compact) "V$clamped" else "VIP $clamped",
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontSize = if (compact) 9.sp else 11.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}

@Composable
fun OfficialAdminTag() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(ZyroCrimson, Color(0xFF991B1B))))
            .border(1.dp, ZyroGold.copy(alpha = 0.8f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = "Official Admin Tag",
            tint = ZyroGold,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = "ADMIN",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Non-blocking VIP-specific voice room arrival animation scaled by VIP level (1..14)
 * and the user's equipped shop entry effect style.
 */
@Composable
fun VipRoomEntryEffectOverlay(
    visible: Boolean,
    displayName: String,
    vipLevel: Int,
    equippedEntryEffectId: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260)) + slideInVertically(
            initialOffsetY = { -it / 2 },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        ) + scaleIn(initialScale = 0.84f),
        exit = fadeOut(tween(340)) + slideOutVertically(targetOffsetY = { -it / 2 }),
        modifier = modifier
    ) {
        val tier = vipTierVisual(vipLevel)
        val effectLabel = when (equippedEntryEffectId) {
            "entry_phoenix" -> "Golden Phoenix Flare"
            "entry_prism" -> "Prism Starlight Wave"
            else -> "Cyan Comet Trail"
        }
        val infinite = rememberInfiniteTransition(label = "vip_entry_fx")
        val sweep by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart),
            label = "entry_sweep"
        )
        val orbit by infinite.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
            label = "entry_orbit"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 56.dp, start = 14.dp, end = 14.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h * 0.42f

                // Radial tier aura
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            tier.primaryColor.copy(alpha = 0.34f * (1f - sweep * 0.35f)),
                            tier.secondaryColor.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = w * 0.46f
                    ),
                    radius = w * 0.46f,
                    center = Offset(cx, cy)
                )

                // Expanding shockwave rings (more rings for higher VIP levels)
                val ringCount = when {
                    vipLevel >= 10 -> 3
                    vipLevel >= 5 -> 2
                    else -> 1
                }
                for (rIdx in 0 until ringCount) {
                    val p = (sweep + rIdx * (1f / ringCount)) % 1f
                    drawCircle(
                        color = if (rIdx % 2 == 0) tier.primaryColor.copy(alpha = (1f - p) * 0.7f)
                        else tier.secondaryColor.copy(alpha = (1f - p) * 0.6f),
                        radius = (26.dp.toPx() + p * w * 0.36f),
                        center = Offset(cx, cy),
                        style = Stroke(width = (2.6f - p * 1.4f).coerceAtLeast(0.8f).dp.toPx())
                    )
                }

                // Style-specific vector geometry
                when (equippedEntryEffectId) {
                    "entry_phoenix" -> {
                        // Symmetrical ascending phoenix wing arcs
                        for (side in listOf(-1f, 1f)) {
                            val wing = Path().apply {
                                moveTo(cx + side * 24.dp.toPx(), cy)
                                quadraticTo(
                                    cx + side * (95.dp.toPx() + sweep * 26.dp.toPx()),
                                    cy - 44.dp.toPx(),
                                    cx + side * (145.dp.toPx() * sweep.coerceAtLeast(0.35f)),
                                    cy - 18.dp.toPx() - sweep * 28.dp.toPx()
                                )
                            }
                            drawPath(
                                path = wing,
                                color = tier.primaryColor.copy(alpha = 0.85f * (1f - sweep * 0.4f)),
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                    "entry_prism" -> {
                        // Prismatic rotating star rays
                        val rays = 8 + (vipLevel / 3)
                        for (i in 0 until rays) {
                            val ang = orbit + i * (2f * PI.toFloat() / rays)
                            val len = 42.dp.toPx() + sin(orbit * 2f + i) * 14.dp.toPx()
                            drawLine(
                                color = if (i % 2 == 0) tier.primaryColor.copy(alpha = 0.7f) else tier.secondaryColor.copy(alpha = 0.65f),
                                start = Offset(cx + cos(ang) * 18.dp.toPx(), cy + sin(ang) * 18.dp.toPx()),
                                end = Offset(cx + cos(ang) * len, cy + sin(ang) * len),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    else -> {
                        // High-speed diagonal comet streaks
                        for (k in 0..3) {
                            val sx = (sweep * 1.35f - k * 0.14f) * w
                            val sy = 14.dp.toPx() + k * 24.dp.toPx()
                            drawLine(
                                brush = Brush.linearGradient(
                                    listOf(Color.Transparent, tier.primaryColor, Color.White),
                                    start = Offset(sx - 95.dp.toPx(), sy - 10.dp.toPx()),
                                    end = Offset(sx, sy)
                                ),
                                start = Offset(sx - 95.dp.toPx(), sy - 10.dp.toPx()),
                                end = Offset(sx, sy),
                                strokeWidth = 2.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // Crisp vector diamond sparkles orbiting banner
                val particleCount = 12 + vipLevel.coerceIn(1, 14)
                for (i in 0 until particleCount) {
                    val ang = orbit * (if (i % 2 == 0) 1f else -1f) + i * (2f * PI.toFloat() / particleCount)
                    val rx = 74.dp.toPx() + (i % 4) * 22.dp.toPx()
                    val ry = 26.dp.toPx() + (i % 3) * 12.dp.toPx()
                    val px = cx + cos(ang) * rx
                    val py = cy + sin(ang) * ry
                    val d = (2.5f + (i % 3) * 1.1f).dp.toPx()
                    val star = Path().apply {
                        moveTo(px, py - d)
                        lineTo(px + d * 0.65f, py)
                        lineTo(px, py + d)
                        lineTo(px - d * 0.65f, py)
                        close()
                    }
                    drawPath(
                        star,
                        color = if (i % 2 == 0) tier.primaryColor else tier.secondaryColor
                    )
                }
            }

            // Sleek VIP Arrival Ribbon
            Row(
                modifier = Modifier
                    .padding(top = 28.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF0A0F24).copy(alpha = 0.94f),
                                tier.primaryColor.copy(alpha = 0.30f),
                                Color(0xFF0A0F24).copy(alpha = 0.94f)
                            )
                        )
                    )
                    .zyroShimmerSweep(tier.primaryColor, durationMs = 1400)
                    .border(
                        width = 1.8.dp,
                        brush = Brush.horizontalGradient(tier.badgeGradient),
                        shape = RoundedCornerShape(50)
                    )
                    .clickable { onDismiss() }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
                    .testTag("vip_room_entry_effect_banner"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(tier.badgeGradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = if (tier.darkBadgeText) Color(0xFF1C1000) else Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    VipNameAndBadge(
                        name = displayName,
                        isVip = true,
                        vipLevel = vipLevel,
                        style = MaterialTheme.typography.titleSmall,
                        onDarkSurface = true
                    )
                    Text(
                        text = "${tier.entryEffectTitle} • $effectLabel",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun GiftAnimationOverlay(
    outcome: GiftSendOutcome?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = outcome != null,
        enter = fadeIn(tween(240)) + scaleIn(
            initialScale = 0.65f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        ),
        exit = fadeOut(tween(200))
    ) {
        if (outcome != null) {
            val infiniteTransition = rememberInfiniteTransition(label = "gift_pulse_4k")
            val pulse by infiniteTransition.animateFloat(
                initialValue = 0.92f,
                targetValue = 1.14f,
                animationSpec = infiniteRepeatable(
                    animation = tween(550, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )
            val orbitAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2600, easing = LinearEasing)
                ),
                label = "orbit"
            )
            val shockwave by infiniteTransition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "shockwave"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.70f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(340.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(ZyroGold.copy(alpha = 0.45f), ZyroCyan.copy(alpha = 0.18f), Color.Transparent)
                        ),
                        radius = size.minDimension * 0.50f * pulse
                    )
                    // Expanding shockwave ring
                    drawCircle(
                        color = ZyroCyan.copy(alpha = (1f - shockwave) * 0.65f),
                        radius = size.minDimension * 0.48f * shockwave,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                    // Dual counter-rotating orbital rings
                    drawCircle(
                        color = ZyroGold.copy(alpha = 0.35f),
                        radius = size.minDimension * 0.36f,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                    drawCircle(
                        color = ZyroPink.copy(alpha = 0.28f),
                        radius = size.minDimension * 0.43f,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    for (i in 0 until 24) {
                        val dir = if (i % 2 == 0) 1f else -1f
                        val angleRad = Math.toRadians((orbitAngle * dir + i * (360.0 / 24.0)))
                        val radius = size.minDimension * (0.26f + (i % 4) * 0.06f)
                        val px = cx + (cos(angleRad) * radius).toFloat()
                        val py = cy + (sin(angleRad) * radius).toFloat()
                        val pColor = when (i % 4) {
                            0 -> ZyroGold
                            1 -> ZyroCyan
                            2 -> ZyroPink
                            else -> Color.White
                        }
                        val d = (3.5f + (i % 3) * 1.5f).dp.toPx()
                        val star = Path().apply {
                            moveTo(px, py - d)
                            lineTo(px + d * 0.7f, py)
                            lineTo(px, py + d)
                            lineTo(px - d * 0.7f, py)
                            close()
                        }
                        drawPath(star, color = pColor)
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(24.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF1A1F3D), Color(0xFF0E1228))))
                        .zyroShimmerSweep(ZyroGold)
                        .border(2.dp, Brush.linearGradient(listOf(ZyroGold, ZyroCyan)), RoundedCornerShape(28.dp))
                        .padding(horizontal = 28.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = outcome.gift.emoji,
                        fontSize = 68.sp,
                        modifier = Modifier.scale(pulse)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${outcome.gift.name} Sent!",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "to ${outcome.recipientName}",
                        color = ZyroCyan,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = ZyroGold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "+${outcome.randomBonusCoins} Random Coins!",
                                color = ZyroGold,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        Surface(
                            color = ZyroEmerald.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "+${outcome.expEarned} EXP",
                                color = ZyroEmerald,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    if (outcome.earnedTag != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = ZyroCyan.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "Tag Unlocked: ${outcome.earnedTag}",
                                color = ZyroCyan,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                    if (outcome.newRoomLevel != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Room Leveled Up to Lv.${outcome.newRoomLevel}! (${outcome.newAdminSlots} Admin Slots)",
                            color = ZyroPink,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tap anywhere to continue",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun WeddingCelebrationOverlay(
    wedding: WeddingCelebration?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = wedding != null,
        enter = fadeIn(tween(260)) + scaleIn(
            initialScale = 0.82f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ),
        exit = fadeOut(tween(200))
    ) {
        if (wedding != null) {
            val heartPulse by rememberInfiniteTransition(label = "wedding_heart").animateFloat(
                initialValue = 0.94f,
                targetValue = 1.16f,
                animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "heart_scale"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                ZyroVictoryConfettiOverlay(visible = true)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .zyroShimmerSweep(ZyroGold),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_wedding_chapel),
                            contentDescription = "Zyro Wedding Chapel",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(390.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(390.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.2f),
                                            Color(0xFF140A1E).copy(alpha = 0.88f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(390.dp)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "CP Wedding",
                                tint = ZyroPink,
                                modifier = Modifier.size(44.dp).scale(heartPulse)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ZYRO ROYAL WEDDING",
                                color = ZyroGold,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${wedding.groomOrHostName}  💍  ${wedding.partnerName}",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Bonded with ${wedding.ringName}\n+500 CP Points • +400 Charm Awarded",
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = ZyroGold, contentColor = Color.Black)
                            ) {
                                Text("Celebrate!", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
