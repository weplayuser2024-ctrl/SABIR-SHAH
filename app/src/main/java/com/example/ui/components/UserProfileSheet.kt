package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.GiftCatalogItem
import com.example.data.ZyroProgression
import com.example.ui.state.InspectedUserProfile
import com.example.ui.theme.ZyroCyan
import com.example.ui.theme.ZyroEmerald
import com.example.ui.theme.ZyroGold
import com.example.ui.theme.ZyroPink

@Composable
fun UserProfileDialog(
    profile: InspectedUserProfile,
    onDismiss: () -> Unit,
    onSendGift: (GiftCatalogItem) -> Unit,
    onOpenChat: (() -> Unit)?
) {
    val levelProgress = ZyroProgression.computeUserLevel(profile.charmPoints)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Player Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close Profile")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    UserAvatar(
                        name = profile.name,
                        colorHex = profile.avatarColorHex,
                        sizeDp = 64,
                        isVip = profile.isVip,
                        vipLevel = profile.vipLevel,
                        isAdmin = profile.isOfficialAdmin,
                        avatarImagePath = profile.avatarImagePath
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        VipNameAndBadge(
                            name = profile.name,
                            isVip = profile.isVip,
                            vipLevel = profile.vipLevel,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "ID: ${profile.zyroId}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            LevelBadge(badgeLabel = levelProgress.badgeLabel, tierLevel = levelProgress.level)
                            if (profile.isOfficialAdmin) OfficialAdminTag()
                        }
                    }
                }

                if (profile.currentVoiceRoomName != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ZyroEmerald.copy(alpha = 0.14f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = ZyroEmerald, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Currently in Voice Room: ${profile.currentVoiceRoomName}",
                            color = ZyroEmerald,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = profile.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMiniCard(
                        label = "Charm Points",
                        value = "${profile.charmPoints}",
                        accent = ZyroPink,
                        modifier = Modifier.weight(1f)
                    )
                    StatMiniCard(
                        label = "EXP",
                        value = "${profile.exp}",
                        accent = ZyroCyan,
                        modifier = Modifier.weight(1f)
                    )
                    StatMiniCard(
                        label = "CP Status",
                        value = profile.cpStatus ?: "Single",
                        accent = ZyroGold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = if (levelProgress.level >= 10) "Max Charm Tier (${levelProgress.badgeLabel}) Reached!"
                    else "Next Tier (${levelProgress.nextBadgeLabel}): ${ZyroProgression.formatNumber(levelProgress.charmToNext)} Charm needed",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { levelProgress.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = ZyroPink
                )

                if (profile.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Tags", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(profile.tags) { tag ->
                            AssistChip(onClick = {}, label = { Text(tag) })
                        }
                    }
                }

                if (!profile.isMe) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text("Send a Quick Gift (+Random Coins & EXP)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ZyroProgression.giftCatalog) { gift ->
                            OutlinedButton(
                                onClick = { onSendGift(gift) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text("${gift.emoji} ${gift.name} (${gift.coinCost})")
                            }
                        }
                    }

                    if (onOpenChat != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                onDismiss()
                                onOpenChat()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Chat, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Message ${profile.name}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMiniCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
    }
}
