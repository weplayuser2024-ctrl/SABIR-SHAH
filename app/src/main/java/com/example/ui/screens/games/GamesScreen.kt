package com.example.ui.screens.games

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.*
import com.example.ui.components.LevelBadge
import com.example.ui.components.OfficialAdminTag
import com.example.ui.components.UserAvatar
import com.example.ui.components.VipNameAndBadge
import com.example.ui.components.ZyroAuroraStageBackdrop
import com.example.ui.components.zyroShimmerSweep
import com.example.ui.components.zyroSpringScale
import com.example.ui.state.RankedUser
import com.example.ui.theme.*

@Composable
fun GamesScreen(
    profile: UserProfileEntity?,
    friends: List<FriendEntity>,
    rankings: List<RankedUser>,
    dailyTasks: List<DailyTaskEntity>,
    darkModeEnabled: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onOpenProfileTab: () -> Unit,
    onOpenTopUp: () -> Unit,
    onOpenVip: () -> Unit,
    onClaimDailyTask: (String) -> Unit,
    onInspectRankUser: (RankedUser) -> Unit,
    onInspectFriend: (FriendEntity) -> Unit,
    onOpenChatWithFriend: (String) -> Unit,
    onMatchReward: (gameName: String, charm: Long, coins: Long) -> Unit,
    onOpenOfficialRoom: () -> Unit,
    onShowEventToast: (String) -> Unit
) {
    var activeGame by remember { mutableStateOf<String?>(null) }
    var showRankingModal by remember { mutableStateOf(false) }
    var showDailyTasksModal by remember { mutableStateOf(false) }
    var showFriendsModal by remember { mutableStateOf(false) }

    if (activeGame == "CARROM") {
        CarromBoardGameScreen(
            onBack = { activeGame = null },
            onMatchWin = { charm, coins -> onMatchReward("Carrom Pro", charm, coins) }
        )
        return
    }
    if (activeGame == "UNO") {
        UnoTableGameScreen(
            onBack = { activeGame = null },
            onMatchWin = { charm, coins -> onMatchReward("Zyro Uno", charm, coins) }
        )
        return
    }

    val vipProg = ZyroProgression.computeVipLevel(profile?.vipGrowthPoints ?: 0)
    val previewFloat by rememberInfiniteTransition(label = "arcade_float").animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float_y"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Top Bar: Top-left Profile Icon + Coin Balance + '+' Top-Up; Top-right VIP Option & Theme Toggle
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top-Left: Profile Icon + Coin Balance + '+' Top-Up Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clickable { onOpenProfileTab() }
                            .testTag("home_profile_icon")
                    ) {
                        UserAvatar(
                            name = profile?.displayName ?: "Aarav",
                            colorHex = 0xFF00E5FF,
                            sizeDp = 42,
                            isVip = profile?.isVip == true,
                            vipLevel = vipProg.vipLevel,
                            isAdmin = profile?.isOfficialAdmin == true,
                            avatarImagePath = profile?.avatarImagePath
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ZyroGold.copy(alpha = 0.16f))
                            .border(1.dp, ZyroGold.copy(alpha = 0.45f), RoundedCornerShape(50))
                            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MonetizationOn,
                            contentDescription = "Coin Balance",
                            tint = ZyroGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = ZyroProgression.formatCompactCoins(profile?.coins ?: 0L),
                            color = ZyroGold,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(ZyroGold)
                                .clickable { onOpenTopUp() }
                                .testTag("home_topup_plus_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Top Up Coins",
                                tint = Color.Black,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                // Top-Right: VIP Button (1..14) + Dark/Light Mode Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF92400E), ZyroGold)))
                            .clickable { onOpenVip() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("home_vip_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WorkspacePremium,
                            contentDescription = "VIP",
                            tint = Color(0xFF1E1300),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "VIP ${vipProg.vipLevel}",
                            color = Color(0xFF1E1300),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    IconButton(
                        onClick = { onToggleDarkMode(!darkModeEnabled) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (darkModeEnabled) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Toggle Dark or Light Mode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 2. 'Events' Section (Weekly & Monthly Events)
        item {
            Column(modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Celebration, contentDescription = null, tint = ZyroGold, modifier = Modifier.size(20.dp))
                        Text("Events", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    }
                    Text(
                        "Weekly & Monthly",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(ZyroProgression.activeEvents, key = { it.id }) { ev ->
                        val accent = Color(ev.accentHex)
                        Card(
                            modifier = Modifier
                                .width(276.dp)
                                .zyroSpringScale()
                                .zyroShimmerSweep(accent, durationMs = 3400)
                                .clickable { onShowEventToast("Joined ${ev.title}! Earn ${ev.rewardText}") }
                                .testTag("event_card_${ev.id}"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(accent.copy(alpha = 0.22f), MaterialTheme.colorScheme.surfaceVariant)
                                        )
                                    )
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(color = accent.copy(alpha = 0.22f), shape = RoundedCornerShape(50)) {
                                            Text(
                                                text = ev.cadence,
                                                color = accent,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        Text(
                                            text = ev.timeLeft,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = ev.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = ev.subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                    Text(
                                        text = "Reward: ${ev.rewardText}",
                                        color = accent,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Three Quick Icons Row: Ranking, Daily Tasks, Friends
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionIconTile(
                    title = "Ranking",
                    icon = Icons.Filled.EmojiEvents,
                    accent = ZyroGold,
                    onClick = { showRankingModal = true },
                    modifier = Modifier.weight(1f).testTag("home_icon_ranking")
                )
                val unclaimedCount = dailyTasks.count { !it.isCompleted }
                QuickActionIconTile(
                    title = if (unclaimedCount > 0) "Daily Tasks ($unclaimedCount)" else "Daily Tasks",
                    icon = Icons.Filled.TaskAlt,
                    accent = ZyroEmerald,
                    onClick = { showDailyTasksModal = true },
                    modifier = Modifier.weight(1f).testTag("home_icon_daily_tasks")
                )
                QuickActionIconTile(
                    title = "Friends",
                    icon = Icons.Filled.Group,
                    accent = ZyroCyan,
                    onClick = { showFriendsModal = true },
                    modifier = Modifier.weight(1f).testTag("home_icon_friends")
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 4. High-Graphics Games Showcase (Hero + Uno & Carrom)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(152.dp)
                    .zyroShimmerSweep(ZyroCyan, durationMs = 3000),
                shape = RoundedCornerShape(22.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_lounge_hero),
                        contentDescription = "Zyro Tabletop Hero",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    ZyroAuroraStageBackdrop(primaryAccent = ZyroCyan, secondaryAccent = ZyroGold, audioEnergy = 0.25f)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF070A16).copy(alpha = 0.88f),
                                        Color(0xFF070A16).copy(alpha = 0.30f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(color = ZyroCyan.copy(alpha = 0.2f), shape = RoundedCornerShape(50)) {
                                Text(
                                    "ZYRO ARCADE",
                                    color = ZyroCyan,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            if (profile?.isOfficialAdmin == true) OfficialAdminTag()
                        }
                        Column {
                            Text(
                                "High-Graphics Tabletop Games",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Play Uno & Carrom to earn Charm & Coins",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Uno Card
        item {
            GameShowcaseCard(
                title = "Zyro Uno Tabletop",
                subtitle = "Casino felt • 3D glossy cards • Wild +4 & UNO! callouts",
                badgeText = "LIVE BOT MATCH",
                accent = ZyroEmerald,
                onPlay = { activeGame = "UNO" },
                testTag = "play_uno_card",
                previewContent = {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(UnoFeltGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 34.dp, height = 50.dp)
                                .offset(x = (-8).dp, y = previewFloat.dp)
                                .rotate(-6f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(UnoRed)
                                .border(2.dp, Color.White, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+4", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
                        }
                        Box(
                            modifier = Modifier
                                .size(width = 34.dp, height = 50.dp)
                                .offset(x = 10.dp, y = (4f - previewFloat).dp)
                                .rotate(8f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(UnoBlue)
                                .border(2.dp, Color.White, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("7", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Carrom Card
        item {
            GameShowcaseCard(
                title = "Carrom Pro",
                subtitle = "Birchwood surface • Ornate rose mandala • Elastic striker physics",
                badgeText = "REALISTIC 3D BOARD",
                accent = ZyroGold,
                onPlay = { activeGame = "CARROM" },
                testTag = "play_carrom_card",
                previewContent = {
                    Canvas(modifier = Modifier.size(76.dp)) {
                        drawRoundRect(
                            color = CarromFrameLight,
                            cornerRadius = CornerRadius(12f)
                        )
                        drawRoundRect(
                            color = CarromSurfaceWarm,
                            topLeft = Offset(8f, 8f),
                            size = Size(size.width - 16f, size.height - 16f),
                            cornerRadius = CornerRadius(8f)
                        )
                        drawCircle(CarromCrimson, radius = 10f, center = center)
                        drawCircle(Color(0xFF1F232E), radius = 7f, center = center + Offset(18f, -14f))
                        drawCircle(Color.White, radius = 7f, center = center + Offset(-18f, 14f))
                        drawCircle(ZyroGold, radius = 8f, center = center + Offset(previewFloat * 1.5f, 34f))
                    }
                }
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Official Admin Room Quick Entry
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { onOpenOfficialRoom() }
                    .testTag("quick_admin_room_banner"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(modifier = Modifier.height(104.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_admin_room_bg),
                        contentDescription = "Official Admin Voice Room",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.58f))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            OfficialAdminTag()
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Zyro Official Admin Voice Room",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Admin-controlled royal lounge • Speaker enabled",
                                color = ZyroGold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        FilledTonalButton(onClick = onOpenOfficialRoom) {
                            Text("Enter")
                        }
                    }
                }
            }
        }
    }

    // Ranking Modal (Charm-based)
    if (showRankingModal) {
        Dialog(onDismissRequest = { showRankingModal = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.78f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = ZyroGold)
                            Text("Charm Rankings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { showRankingModal = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                    Text(
                        "Ranked strictly by Charm Points earned",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(rankings, key = { _, u -> u.uid }) { index, user ->
                            val rank = index + 1
                            val rankColor = when (rank) {
                                1 -> ZyroGold
                                2 -> Color(0xFFD1D5DB)
                                3 -> Color(0xFFD97706)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showRankingModal = false
                                        onInspectRankUser(user)
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("#$rank", color = rankColor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                    UserAvatar(
                                        name = user.name,
                                        colorHex = user.avatarColorHex,
                                        sizeDp = 42,
                                        isVip = user.isVip,
                                        vipLevel = user.vipLevel,
                                        isAdmin = user.isOfficialAdmin,
                                        avatarImagePath = user.avatarImagePath
                                    )
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        VipNameAndBadge(
                                            name = user.name,
                                            isVip = user.isVip,
                                            vipLevel = user.vipLevel
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            LevelBadge(badgeLabel = user.charmBadge, tierLevel = user.level)
                                            if (user.isOfficialAdmin) OfficialAdminTag()
                                        }
                                    }
                                    Text("${ZyroProgression.formatNumber(user.charmPoints)} 💖", color = ZyroPink, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Daily Tasks Modal
    if (showDailyTasksModal) {
        Dialog(onDismissRequest = { showDailyTasksModal = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.TaskAlt, contentDescription = null, tint = ZyroEmerald)
                            Text("Daily Tasks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { showDailyTasksModal = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                    dailyTasks.forEach { task ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(task.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("+${task.rewardCoins} Coins • +${task.rewardExp} EXP", color = ZyroGold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onClaimDailyTask(task.taskId) },
                                    enabled = !task.isCompleted,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (task.isCompleted) Color.Gray else ZyroEmerald,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text(if (task.isCompleted) "Claimed" else "Claim", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Friends Modal
    if (showFriendsModal) {
        Dialog(onDismissRequest = { showFriendsModal = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.74f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Group, contentDescription = null, tint = ZyroCyan)
                            Text("Friends (${friends.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { showFriendsModal = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(friends, key = { it.uid }) { f ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showFriendsModal = false
                                        onInspectFriend(f)
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    UserAvatar(
                                        name = f.name,
                                        colorHex = f.avatarColorHex,
                                        sizeDp = 44,
                                        isVip = f.isVip,
                                        vipLevel = f.vipLevel,
                                        isAdmin = f.isOfficialAdmin
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        VipNameAndBadge(
                                            name = f.name,
                                            isVip = f.isVip,
                                            vipLevel = f.vipLevel
                                        )
                                        Text("ID: ${f.zyroId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = {
                                        showFriendsModal = false
                                        onOpenChatWithFriend(f.uid)
                                    }) {
                                        Icon(Icons.Filled.Chat, contentDescription = "Message ${f.name}", tint = ZyroCyan)
                                    }
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
private fun QuickActionIconTile(
    title: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .zyroSpringScale()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(24.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GameShowcaseCard(
    title: String,
    subtitle: String,
    badgeText: String,
    accent: Color,
    onPlay: () -> Unit,
    testTag: String,
    previewContent: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .zyroSpringScale()
            .zyroShimmerSweep(accent, durationMs = 3200)
            .clickable { onPlay() }
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            previewContent()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = badgeText,
                    color = accent,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledIconButton(
                onClick = onPlay,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = accent, contentColor = Color.Black)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play $title")
            }
        }
    }
}
