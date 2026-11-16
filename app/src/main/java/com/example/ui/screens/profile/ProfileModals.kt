package com.example.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.*
import com.example.ui.components.LevelBadge
import com.example.ui.components.UserAvatar
import com.example.ui.components.VipBadge
import com.example.ui.components.VipNameAndBadge
import com.example.ui.components.vipTierVisual
import com.example.ui.theme.*
import java.io.File

@Composable
fun EditProfileDialog(
    profile: UserProfileEntity,
    onDismiss: () -> Unit,
    onSave: (newName: String, newPfpUri: Uri?) -> Unit
) {
    var nameDraft by remember { mutableStateOf(profile.displayName) }
    var pickedPfpUri by remember { mutableStateOf<Uri?>(null) }

    val pfpPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pickedPfpUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile (PFP & Name)") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.clickable {
                        pfpPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                ) {
                    if (pickedPfpUri != null) {
                        AsyncImage(
                            model = pickedPfpUri,
                            contentDescription = "New PFP Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(82.dp)
                                .clip(RoundedCornerShape(50))
                                .border(2.dp, ZyroCyan, RoundedCornerShape(50))
                        )
                    } else {
                        UserAvatar(
                            name = nameDraft.ifBlank { profile.displayName },
                            colorHex = 0xFF00E5FF,
                            sizeDp = 82,
                            isVip = profile.isVip,
                            vipLevel = ZyroProgression.computeVipLevel(profile.vipGrowthPoints).vipLevel,
                            isAdmin = profile.isOfficialAdmin,
                            avatarImagePath = profile.avatarImagePath
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        pfpPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.testTag("pick_pfp_button")
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Change Profile Picture")
                }
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_display_name_field")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(nameDraft, pickedPfpUri)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_profile_edit_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun VipDialog(
    profile: UserProfileEntity,
    onDismiss: () -> Unit,
    onActivateVipPlan: (months: Int, priceInr: Int) -> Unit,
    onSpendVipCoinsForGrowth: (Long) -> Unit,
    onOpenInrTopUp: () -> Unit
) {
    val lvlInfo = ZyroProgression.computeUserLevel(profile.charmPoints)
    val vipProg = ZyroProgression.computeVipLevel(profile.vipGrowthPoints)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VipBadge(vipLevel = vipProg.vipLevel)
                        Text("VIP 1–14 Lounge", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close VIP")
                    }
                }

                // VIP Level 1-14 Growth Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF422006), Color(0xFF92400E), Color(0xFF422006))))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "VIP Level ${vipProg.vipLevel} / 14",
                                color = ZyroGold,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${ZyroProgression.formatCompactCoins(profile.coins)} VIP Coins 🪙",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Text(
                            text = if (vipProg.vipLevel >= 14) {
                                "Max VIP 14 Reached • ${vipProg.growthPoints} Growth Points"
                            } else {
                                "Growth: ${vipProg.growthPoints} / ${vipProg.nextLevelPoints} pts (${vipProg.pointsToNext} pts to VIP ${vipProg.vipLevel + 1})"
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        LinearProgressIndicator(
                            progress = { vipProg.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ZyroGold
                        )
                    }
                }

                // Spend VIP Coins to Boost Account Growth (1 Growth pt per 100 coins spent)
                Text("Spend VIP Coins for Account Growth", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Every 100 VIP Coins spent adds +1 Growth Point toward VIP 1–14!",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { onSpendVipCoinsForGrowth(200L) },
                        modifier = Modifier.weight(1f).testTag("spend_vip_200")
                    ) {
                        Text("200 🪙 (+2)")
                    }
                    OutlinedButton(
                        onClick = { onSpendVipCoinsForGrowth(500L) },
                        modifier = Modifier.weight(1f).testTag("spend_vip_500")
                    ) {
                        Text("500 🪙 (+5)")
                    }
                    OutlinedButton(
                        onClick = { onSpendVipCoinsForGrowth(1000L) },
                        modifier = Modifier.weight(1f).testTag("spend_vip_1000")
                    ) {
                        Text("1000 🪙 (+10)")
                    }
                }

                // VIP Level 1..14 Ladder Chips (Color-coded by VIP Tier)
                Text("VIP Tiers (1 to 14) & Name Colors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items((1..14).toList()) { tier ->
                        val unlocked = profile.isVip && vipProg.vipLevel >= tier
                        val tierStyle = vipTierVisual(tier)
                        Surface(
                            color = if (unlocked) tierStyle.primaryColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.border(
                                1.dp,
                                if (unlocked) tierStyle.primaryColor else tierStyle.primaryColor.copy(alpha = 0.35f),
                                RoundedCornerShape(10.dp)
                            )
                        ) {
                            Text(
                                text = "VIP $tier",
                                color = if (unlocked) tierStyle.primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // VIP Room Entry Style Preview
                val myTier = vipTierVisual(vipProg.vipLevel)
                Text("Voice Room Entry Effect Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF0B1024),
                                    myTier.primaryColor.copy(alpha = 0.35f),
                                    Color(0xFF0B1024)
                                )
                            )
                        )
                        .border(1.5.dp, Brush.horizontalGradient(myTier.badgeGradient), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = myTier.primaryColor, modifier = Modifier.size(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        VipNameAndBadge(
                            name = profile.displayName,
                            isVip = true,
                            vipLevel = vipProg.vipLevel,
                            onDarkSurface = true
                        )
                        Text(
                            text = "${myTier.tierName} • ${myTier.entryEffectTitle}",
                            color = Color.White.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    LevelBadge(badgeLabel = lvlInfo.badgeLabel, tierLevel = lvlInfo.level)
                }

                Text("VIP Subscription Plans", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "Status: ${profile.vipExpiryText}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onActivateVipPlan(1, 200) },
                        colors = ButtonDefaults.buttonColors(containerColor = ZyroGold, contentColor = Color.Black),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("subscribe_vip_1m_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("1 Month VIP", fontWeight = FontWeight.Bold)
                            Text("₹200 INR", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Button(
                        onClick = { onActivateVipPlan(3, 500) },
                        colors = ButtonDefaults.buttonColors(containerColor = ZyroViolet, contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("subscribe_vip_3m_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("3 Months VIP", fontWeight = FontWeight.Bold)
                            Text("₹500 INR (Save ₹100)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenInrTopUp()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.QrCode2, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("INR Top-Up & Custom Payment QR")
                }
            }
        }
    }
}

@Composable
fun ShopDialog(
    profile: UserProfileEntity,
    items: List<ShopInventoryEntity>,
    onDismiss: () -> Unit,
    onBuyOrEquip: (ShopInventoryEntity) -> Unit
) {
    val categories = listOf(
        "RING" to "Rings 💍",
        "BUBBLE" to "Chat Bubbles 💬",
        "FRAME" to "Seat Frames 🪩",
        "ENTRY" to "Entry Effects ✨",
        "THEME" to "Room Themes 🎨"
    )
    var selectedCategory by remember { mutableStateOf("RING") }
    val filtered = items.filter { it.category == selectedCategory }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
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
                    Column {
                        Text("Zyro Boutique Shop", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Balance: ${ZyroProgression.formatCompactCoins(profile.coins)} Coins 🪙",
                            color = ZyroGold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close Shop")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { (key, label) ->
                        FilterChip(
                            selected = selectedCategory == key,
                            onClick = { selectedCategory = key },
                            label = { Text(label) },
                            modifier = Modifier.testTag("shop_category_$key")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filtered, key = { it.itemId }) { item ->
                        val accent = Color(item.accentHex)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(accent.copy(alpha = 0.2f))
                                        .border(1.5.dp, accent, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val emoji = when (item.category) {
                                        "RING" -> "💍"
                                        "BUBBLE" -> "💬"
                                        "FRAME" -> "🪩"
                                        "THEME" -> "🌌"
                                        else -> "✨"
                                    }
                                    Text(emoji, style = MaterialTheme.typography.titleLarge)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { onBuyOrEquip(item) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (item.isEquipped) ZyroEmerald else accent,
                                        contentColor = Color.Black
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("shop_item_button_${item.itemId}")
                                ) {
                                    Text(
                                        text = when {
                                            item.isEquipped -> "Equipped"
                                            item.isOwned -> "Equip"
                                            else -> "${ZyroProgression.formatCompactCoins(item.priceCoins)} 🪙"
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
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
fun PrivacyDialog(
    privacy: PrivacySettingsEntity,
    onDismiss: () -> Unit,
    onUpdate: ((PrivacySettingsEntity) -> PrivacySettingsEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Privacy & Display Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrivacyToggleRow(
                    title = "Dark Mode",
                    subtitle = "Switch between Zyro Midnight & Light Mode",
                    checked = privacy.darkModeEnabled,
                    onCheckedChange = { v -> onUpdate { it.copy(darkModeEnabled = v) } }
                )
                PrivacyToggleRow(
                    title = "Hide Online Status",
                    subtitle = "Appear offline to non-friends",
                    checked = privacy.hideOnlineStatus,
                    onCheckedChange = { v -> onUpdate { it.copy(hideOnlineStatus = v) } }
                )
                PrivacyToggleRow(
                    title = "Hide Voice Room in Chat",
                    subtitle = "Do not broadcast active room pill",
                    checked = privacy.hideVoiceRoomPresence,
                    onCheckedChange = { v -> onUpdate { it.copy(hideVoiceRoomPresence = v) } }
                )
                PrivacyToggleRow(
                    title = "Show Gift Wall Publicly",
                    subtitle = "Let visitors view your received gifts",
                    checked = privacy.showGiftWallPublicly,
                    onCheckedChange = { v -> onUpdate { it.copy(showGiftWallPublicly = v) } }
                )
                PrivacyToggleRow(
                    title = "VIP Voice Room Entry Effects",
                    subtitle = "Play VIP arrival burst & show tier badge on room join",
                    checked = privacy.showVipEntryEffect,
                    onCheckedChange = { v -> onUpdate { it.copy(showVipEntryEffect = v) } }
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Save") }
        }
    )
}

@Composable
private fun PrivacyToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun CpAndWeddingDialog(
    profile: UserProfileEntity,
    friends: List<FriendEntity>,
    rings: List<ShopInventoryEntity>,
    onDismiss: () -> Unit,
    onBuyRing: (ShopInventoryEntity) -> Unit,
    onAcquireCp: (costCoins: Long, cpGain: Long) -> Unit,
    onHostWedding: (partnerName: String, ring: ShopInventoryEntity) -> Unit
) {
    val ownedRings = rings.filter { it.isOwned }
    var selectedPartner by remember { mutableStateOf(friends.firstOrNull()?.name ?: "Meera Nair") }
    var selectedRingId by remember(ownedRings) { mutableStateOf(ownedRings.firstOrNull()?.itemId) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CP & In-Game Weddings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_wedding_chapel),
                        contentDescription = "Wedding Chapel",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(14.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            Text(
                                text = if (profile.isMarried) "Married to ${profile.cpPartnerName} 💍" else "CP Balance: ${profile.cpPoints} CP",
                                color = ZyroGold,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Purchase a ring, acquire CP & host an official Zyro ceremony",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Text("1. Acquire CP (Couple Points)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { onAcquireCp(200L, 250L) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+250 CP (200 🪙)")
                    }
                    OutlinedButton(
                        onClick = { onAcquireCp(500L, 700L) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+700 CP (500 🪙)")
                    }
                }

                Text("2. Purchase Wedding Ring", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                rings.forEach { ring ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                if (ring.isOwned) selectedRingId = ring.itemId else onBuyRing(ring)
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💍 ${ring.name}", fontWeight = FontWeight.SemiBold)
                        if (ring.isOwned) {
                            Text(
                                text = if (selectedRingId == ring.itemId) "Selected ✓" else "Owned",
                                color = ZyroEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text("Buy ${ring.priceCoins} 🪙", color = ZyroGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text("3. Choose Partner & Host Wedding (300 🪙)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(friends) { f ->
                        FilterChip(
                            selected = selectedPartner == f.name,
                            onClick = { selectedPartner = f.name },
                            label = { Text(f.name) }
                        )
                    }
                }

                val chosenRing = ownedRings.firstOrNull { it.itemId == selectedRingId } ?: ownedRings.firstOrNull()
                Button(
                    onClick = {
                        if (chosenRing != null) {
                            onDismiss()
                            onHostWedding(selectedPartner, chosenRing)
                        }
                    },
                    enabled = chosenRing != null,
                    colors = ButtonDefaults.buttonColors(containerColor = ZyroPink, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("host_wedding_button")
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (chosenRing != null) "Host Wedding with $selectedPartner" else "Buy a Ring above first",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun InrTopUpAndQrDialog(
    profile: UserProfileEntity,
    onDismiss: () -> Unit,
    onTopUpInr: (amountInr: Int, coins: Long, cp: Long) -> Unit,
    onSaveCustomQr: (Uri) -> Unit
) {
    val qrPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onSaveCustomQr(uri)
    }

    val packages = listOf(
        Triple(100, 1_000_000L, "1M Coins"),
        Triple(200, 2_200_000L, "2.2M Coins (Bonus Included!)"),
        Triple(500, 8_000_000L, "8M Coins"),
        Triple(1000, 12_000_000L, "12M Coins"),
        Triple(2000, 30_000_000L, "30M Coins")
    )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("INR Top-Up & Custom QR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val qrFile = profile.customQrPath?.let { File(it) }
                        if (qrFile != null && qrFile.exists()) {
                            AsyncImage(
                                model = qrFile,
                                contentDescription = "Custom Payment QR Code",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(168.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(6.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QrCode2,
                                    contentDescription = "QR Placeholder",
                                    tint = Color.Black,
                                    modifier = Modifier.size(96.dp)
                                )
                            }
                            Text(
                                "Upload your own UPI / Payment QR code for INR top-ups",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                qrPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.testTag("upload_custom_qr_button")
                        ) {
                            Icon(Icons.Filled.Upload, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (profile.customQrPath != null) "Replace My QR Code" else "Add My Custom QR Code")
                        }
                    }
                }

                Text("Select INR Top-Up Pack", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                packages.forEach { (inr, coins, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("$label 🪙", fontWeight = FontWeight.Bold)
                            Text(
                                "${ZyroProgression.formatNumber(coins)} Coins • Instant INR QR credit",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onTopUpInr(inr, coins, 0L) },
                            colors = ButtonDefaults.buttonColors(containerColor = ZyroEmerald, contentColor = Color.Black),
                            modifier = Modifier.testTag("topup_inr_$inr")
                        ) {
                            Text("₹$inr INR", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
