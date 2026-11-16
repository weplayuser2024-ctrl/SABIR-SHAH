package com.example.ui.screens.games

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ZyroAuroraStageBackdrop
import com.example.ui.components.ZyroVictoryConfettiOverlay
import com.example.ui.components.zyroShimmerSweep
import com.example.ui.components.zyroSpringScale
import com.example.ui.theme.*
import kotlinx.coroutines.delay

enum class UnoColor(val color: Color, val label: String) {
    RED(UnoRed, "Red"),
    BLUE(UnoBlue, "Blue"),
    GREEN(UnoGreen, "Green"),
    YELLOW(UnoYellow, "Yellow"),
    WILD(UnoWildDark, "Wild")
}

data class UnoCard(
    val id: Int,
    val color: UnoColor,
    val value: String // "0".."9", "SKIP", "REV", "+2", "WILD", "+4"
)

@Composable
fun UnoTableGameScreen(
    onBack: () -> Unit,
    onMatchWin: (charmGain: Long, coinGain: Long) -> Unit
) {
    BackHandler(onBack = onBack)

    var deck by remember { mutableStateOf(buildUnoDeck().shuffled()) }
    var playerHand by remember { mutableStateOf(listOf<UnoCard>()) }
    var botHand by remember { mutableStateOf(listOf<UnoCard>()) }
    var discardPile by remember { mutableStateOf(listOf<UnoCard>()) }
    var activeColor by remember { mutableStateOf(UnoColor.RED) }
    var isPlayerTurn by remember { mutableStateOf(true) }
    var unoShouted by remember { mutableStateOf(false) }
    var showColorPickerForCard by remember { mutableStateOf<UnoCard?>(null) }
    var statusText by remember { mutableStateOf("Your turn! Match color or symbol.") }
    var gameWinner by remember { mutableStateOf<String?>(null) }

    fun dealFreshGame() {
        val full = buildUnoDeck().shuffled()
        playerHand = full.take(7)
        botHand = full.drop(7).take(7)
        val firstNonWild = full.drop(14).firstOrNull { it.color != UnoColor.WILD } ?: full[14]
        val remaining = full.drop(14).filterNot { it.id == firstNonWild.id }
        discardPile = listOf(firstNonWild)
        deck = remaining
        activeColor = firstNonWild.color
        isPlayerTurn = true
        unoShouted = false
        gameWinner = null
        statusText = "Your turn! Match ${firstNonWild.color.label} or ${firstNonWild.value}."
    }

    LaunchedEffect(Unit) {
        dealFreshGame()
    }

    fun drawCardsFromDeck(count: Int): List<UnoCard> {
        if (deck.size < count && discardPile.size > 1) {
            val top = discardPile.last()
            val recycled = discardPile.dropLast(1).shuffled()
            deck = deck + recycled
            discardPile = listOf(top)
        }
        val drawn = deck.take(count)
        deck = deck.drop(count)
        return drawn
    }

    fun canPlayCard(card: UnoCard): Boolean {
        val top = discardPile.lastOrNull() ?: return true
        return card.color == UnoColor.WILD || card.color == activeColor || card.value == top.value
    }

    fun executePlayerPlay(card: UnoCard, chosenWildColor: UnoColor? = null) {
        playerHand = playerHand.filterNot { it.id == card.id }
        discardPile = discardPile + card
        activeColor = if (card.color == UnoColor.WILD) (chosenWildColor ?: UnoColor.RED) else card.color

        if (playerHand.isEmpty()) {
            gameWinner = "You Won!"
            statusText = "ZYRO UNO CHAMPION! +160 Charm & +220 Coins!"
            onMatchWin(160L, 220L)
            return
        }

        when (card.value) {
            "+2" -> {
                botHand = botHand + drawCardsFromDeck(2)
                statusText = "Bot drew +2! Play again."
                isPlayerTurn = true
            }
            "+4" -> {
                botHand = botHand + drawCardsFromDeck(4)
                statusText = "Wild +4 (${activeColor.label})! Bot drew 4."
                isPlayerTurn = true
            }
            "SKIP", "REV" -> {
                statusText = "${card.value}! Your turn again."
                isPlayerTurn = true
            }
            else -> {
                statusText = "Bot is thinking..."
                isPlayerTurn = false
            }
        }
    }

    // Bot AI Turn
    LaunchedEffect(isPlayerTurn, gameWinner) {
        if (isPlayerTurn || gameWinner != null) return@LaunchedEffect
        delay(950)
        val playable = botHand.firstOrNull { canPlayCard(it) }
        if (playable != null) {
            botHand = botHand.filterNot { it.id == playable.id }
            discardPile = discardPile + playable
            val nextColor = if (playable.color == UnoColor.WILD) {
                listOf(UnoColor.RED, UnoColor.BLUE, UnoColor.GREEN, UnoColor.YELLOW).random()
            } else playable.color
            activeColor = nextColor

            if (botHand.isEmpty()) {
                gameWinner = "Bot Won"
                statusText = "Bot cleared its hand! Tap Reset for a rematch."
                return@LaunchedEffect
            }

            when (playable.value) {
                "+2" -> {
                    playerHand = playerHand + drawCardsFromDeck(2)
                    statusText = "Bot played +2! You drew 2 cards. Bot plays again."
                    isPlayerTurn = false
                }
                "+4" -> {
                    playerHand = playerHand + drawCardsFromDeck(4)
                    statusText = "Bot played Wild +4 (${nextColor.label})!"
                    isPlayerTurn = false
                }
                "SKIP", "REV" -> {
                    statusText = "Bot played ${playable.value}! Bot goes again."
                    isPlayerTurn = false
                }
                else -> {
                    statusText = "Bot played ${playable.color.label} ${playable.value}. Your turn!"
                    isPlayerTurn = true
                }
            }
        } else {
            val drawn = drawCardsFromDeck(1)
            botHand = botHand + drawn
            statusText = "Bot drew a card. Your turn!"
            isPlayerTurn = true
        }
    }

    val animatedActiveColor by animateColorAsState(
        targetValue = activeColor.color,
        animationSpec = tween(380),
        label = "uno_active_color"
    )
    val discardPulse = remember { Animatable(0f) }
    LaunchedEffect(discardPile.size) {
        discardPulse.snapTo(0f)
        discardPulse.animateTo(1f, animationSpec = tween(550, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(UnoFeltGreen, UnoFeltDark),
                    radius = 1200f
                )
            )
    ) {
        ZyroAuroraStageBackdrop(
            primaryAccent = animatedActiveColor,
            secondaryAccent = ZyroGold,
            audioEnergy = if (isPlayerTurn) 0.2f else 0.1f
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("uno_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "ZYRO UNO TABLE",
                    color = ZyroGold,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { dealFreshGame() }, modifier = Modifier.testTag("uno_reset_button")) {
                    Icon(Icons.Filled.Refresh, contentDescription = "New Match", tint = Color.White)
                }
            }

            // Bot Hand
            Text(
                text = "Opponent (${botHand.size} cards)",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy((-18).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                botHand.take(9).forEachIndexed { idx, _ ->
                    UnoCardBack(
                        modifier = Modifier
                            .size(width = 46.dp, height = 68.dp)
                            .rotate((idx - (botHand.size.coerceAtMost(9) / 2)) * 3f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Center Table: Draw Deck + Active Discard Card + Color Glow
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    UnoCardBack(
                        modifier = Modifier
                            .size(width = 86.dp, height = 126.dp)
                            .clickable(enabled = isPlayerTurn && gameWinner == null) {
                                val drawn = drawCardsFromDeck(1)
                                playerHand = playerHand + drawn
                                statusText = "Drew a card. Bot's turn."
                                isPlayerTurn = false
                            }
                            .testTag("uno_draw_pile")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("DRAW (${deck.size})", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val topCard = discardPile.lastOrNull()
                    Box(contentAlignment = Alignment.Center) {
                        if (discardPulse.value < 1f) {
                            Canvas(modifier = Modifier.size(136.dp)) {
                                val p = discardPulse.value
                                drawCircle(
                                    color = animatedActiveColor.copy(alpha = (1f - p) * 0.7f),
                                    radius = size.minDimension * (0.34f + 0.16f * p),
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .border(3.dp, animatedActiveColor, RoundedCornerShape(16.dp))
                                .padding(4.dp)
                        ) {
                            if (topCard != null) {
                                RealUnoCardView(
                                    card = topCard,
                                    activeOverrideColor = if (topCard.color == UnoColor.WILD) activeColor else null,
                                    playable = true,
                                    modifier = Modifier.size(width = 92.dp, height = 132.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(activeColor.color)
                        )
                        Text(
                            text = "Active: ${activeColor.label}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Status & UNO! Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusText,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                if (playerHand.size <= 2 && gameWinner == null) {
                    Button(
                        onClick = {
                            unoShouted = true
                            statusText = "UNO SHOUTED! Drop your final card!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ZyroGold, contentColor = Color.Black),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (unoShouted) "UNO!" else "Call UNO", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Player Hand
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(playerHand, key = { it.id }) { card ->
                    val playable = isPlayerTurn && gameWinner == null && canPlayCard(card)
                    val liftDp by animateDpAsState(
                        targetValue = if (playable) (-10).dp else 0.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "card_lift"
                    )
                    val cardScale by animateFloatAsState(
                        targetValue = if (playable) 1.04f else 0.96f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "card_scale"
                    )
                    RealUnoCardView(
                        card = card,
                        playable = playable,
                        modifier = Modifier
                            .size(width = 76.dp, height = 112.dp)
                            .offset(y = liftDp)
                            .graphicsLayer {
                                cameraDistance = 12f * density
                                scaleX = cardScale
                                scaleY = cardScale
                                rotationX = if (playable) 4f else 0f
                            }
                            .zyroSpringScale()
                            .clickable(enabled = playable) {
                                if (card.color == UnoColor.WILD) {
                                    showColorPickerForCard = card
                                } else {
                                    executePlayerPlay(card)
                                }
                            }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Wild Color Picker Dialog
        if (showColorPickerForCard != null) {
            AlertDialog(
                onDismissRequest = { showColorPickerForCard = null },
                title = { Text("Choose Wild Color") },
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(UnoColor.RED, UnoColor.BLUE, UnoColor.GREEN, UnoColor.YELLOW).forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .zyroSpringScale()
                                    .clip(CircleShape)
                                    .background(c.color)
                                    .border(2.dp, Color.White, CircleShape)
                                    .clickable {
                                        val pending = showColorPickerForCard
                                        showColorPickerForCard = null
                                        if (pending != null) executePlayerPlay(pending, c)
                                    }
                            )
                        }
                    }
                },
                confirmButton = {}
            )
        }

        ZyroVictoryConfettiOverlay(visible = gameWinner == "You Won!")
    }
}

@Composable
private fun RealUnoCardView(
    card: UnoCard,
    playable: Boolean,
    activeOverrideColor: UnoColor? = null,
    modifier: Modifier = Modifier
) {
    val bgColor = (activeOverrideColor ?: card.color).color
    Box(
        modifier = modifier
            .shadow(if (playable) 12.dp else 3.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (card.color == UnoColor.WILD && activeOverrideColor == null) {
                    Brush.linearGradient(listOf(UnoRed, UnoBlue, UnoGreen, UnoYellow))
                } else {
                    Brush.verticalGradient(listOf(bgColor, bgColor.copy(alpha = 0.82f)))
                }
            )
            .then(if (playable) Modifier.zyroShimmerSweep(Color.White, durationMs = 2400) else Modifier)
            .then(
                if (!playable) Modifier.background(Color.Black.copy(alpha = 0.35f)) else Modifier
            )
    ) {
        // White tilted center oval
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawOval(
                color = Color.White.copy(alpha = 0.25f),
                topLeft = Offset(size.width * 0.12f, size.height * 0.2f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.76f, size.height * 0.6f)
            )
        }
        Text(
            text = card.value,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(5.dp)
        )
        Text(
            text = card.value,
            color = Color.White,
            fontSize = if (card.value.length > 2) 17.sp else 26.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.align(Alignment.Center)
        )
        Text(
            text = card.value,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(5.dp)
        )
    }
}

@Composable
private fun UnoCardBack(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(3.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0xFF14182B)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.75f)
                .clip(RoundedCornerShape(50))
                .background(UnoRed),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ZYRO",
                color = ZyroGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.rotate(-18f)
            )
        }
    }
}

private fun buildUnoDeck(): List<UnoCard> {
    val list = mutableListOf<UnoCard>()
    var id = 1
    val colors = listOf(UnoColor.RED, UnoColor.BLUE, UnoColor.GREEN, UnoColor.YELLOW)
    colors.forEach { c ->
        for (n in 1..9) {
            list.add(UnoCard(id++, c, n.toString()))
        }
        list.add(UnoCard(id++, c, "SKIP"))
        list.add(UnoCard(id++, c, "REV"))
        list.add(UnoCard(id++, c, "+2"))
    }
    repeat(3) {
        list.add(UnoCard(id++, UnoColor.WILD, "WILD"))
        list.add(UnoCard(id++, UnoColor.WILD, "+4"))
    }
    return list
}
