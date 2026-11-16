package com.example.ui.screens.profile

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.components.LevelBadge
import com.example.ui.components.OfficialAdminTag
import com.example.ui.components.UserAvatar
import com.example.ui.components.VipNameAndBadge
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    profile: UserProfileEntity?,
    friends: List<FriendEntity>,
    myMoments: List<MomentEntity>,
    giftWall: List<GiftRecordEntity>,
    rooms: List<RoomEntity>,
    shopItems: List<ShopInventoryEntity>,
    privacy: PrivacySettingsEntity,
    onUpdateProfileDetails: (newName: String, newPfpUri: Uri?) -> Unit,
    onToggleOfficialAdmin: (Boolean) -> Unit,
    onInspectMyProfile: () -> Unit,
    onRejoinRoom: (String) -> Unit,
    onActivateVipPlan: (months: Int, priceInr: Int) -> Unit,
    onSpendVipCoinsForGrowth: (Long) -> Unit,
    onTopUpInr: (Int, Long, Long) -> Unit,
    onSaveCustomQr: (Uri) -> Unit,
    onBuyOrEquipShopItem: (ShopInventoryEntity) -> Unit,
    onAcquireCp: (Long, Long) -> Unit,
    onHostWedding: (String, ShopInventoryEntity) -> Unit,
    onUpdatePrivacy: ((PrivacySettingsEntity) -> PrivacySettingsEntity) -> Unit,
    onShareFeedback: (String) -> Unit = {},
    onRedeemInviteLink: (String) -> Unit = {},
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val me = profile ?: return
    val levelInfo = ZyroProgression.computeUserLevel(me.charmPoints)
    val vipProg = ZyroProgression.computeVipLevel(me.vipGrowthPoints)
    val joinedRooms = rooms.filter { it.isJoined }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showVipDialog by remember { mutableStateOf(false) }
    var showShopDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showCpWeddingDialog by remember { mutableStateOf(false) }
    var showInrTopUpDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Bar: Fixed User ID + Admin Chip + Shop Icon + Edit Profile + Compact Full-Profile Icon
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("fixed_user_id_badge"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "ID: ${me.zyroId}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Fixed User ID",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = me.isOfficialAdmin,
                        onClick = { onToggleOfficialAdmin(!me.isOfficialAdmin) },
                        label = { Text("Admin", style = MaterialTheme.typography.labelSmall) }
                    )
                    // Shop Icon Button in Me header
                    IconButton(
                        onClick = { showShopDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ZyroGold.copy(alpha = 0.20f))
                            .testTag("me_shop_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storefront,
                            contentDescription = "Open Shop",
                            tint = ZyroGold
                        )
                    }
                    // Compact Edit Profile Button (PFP + Display Name)
                    IconButton(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("edit_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DriveFileRenameOutline,
                            contentDescription = "Edit Profile Picture & Name",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Compact Icon for Full-Profile View
                    IconButton(
                        onClick = onInspectMyProfile,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .testTag("compact_full_profile_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Badge,
                            contentDescription = "View Full Profile",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // 2. Profile Summary Card: PFP, Name, Charm Points, Level, VIP 1..14 Badge, Next Level Progress
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(modifier = Modifier.clickable { showEditProfileDialog = true }) {
                            UserAvatar(
                                name = me.displayName,
                                colorHex = 0xFF00E5FF,
                                sizeDp = 66,
                                isVip = me.isVip,
                                vipLevel = vipProg.vipLevel,
                                isAdmin = me.isOfficialAdmin,
                                avatarImagePath = me.avatarImagePath
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                VipNameAndBadge(
                                    name = me.displayName,
                                    isVip = me.isVip,
                                    vipLevel = vipProg.vipLevel,
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit Name & PFP",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { showEditProfileDialog = true }
                                )
                            }
                            if (me.authIdentifier.isNotBlank()) {
                                Text(
                                    text = "${me.authProvider}: ${me.authIdentifier}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                LevelBadge(badgeLabel = levelInfo.badgeLabel, tierLevel = levelInfo.level)
                                if (me.isOfficialAdmin) OfficialAdminTag()
                            }
                            if (me.isMarried && me.cpPartnerName != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "💍 Married to ${me.cpPartnerName}",
                                    color = ZyroPink,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatChip("Charm", "${ZyroProgression.formatNumber(me.charmPoints)} 💖", ZyroPink, Modifier.weight(1f))
                        ProfileStatChip("Coins", "${ZyroProgression.formatCompactCoins(me.coins)} 🪙", ZyroGold, Modifier.weight(1f)) {
                            showInrTopUpDialog = true
                        }
                        ProfileStatChip("VIP Lv.", "${vipProg.vipLevel}/14", ZyroCyan, Modifier.weight(1f)) {
                            showVipDialog = true
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                if (levelInfo.level >= 10) "Max Charm Tier (${levelInfo.badgeLabel})" else "Next Tier (${levelInfo.nextBadgeLabel})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (levelInfo.level >= 10) "Maxed!" else "${ZyroProgression.formatNumber(levelInfo.charmToNext)} Charm needed",
                                color = ZyroPink,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { levelInfo.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ZyroPink
                        )
                    }
                }
            }
        }

        // 4. My Moments
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("My Moments (${myMoments.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (myMoments.isEmpty()) {
                    Text("Share photos or videos in Discover to showcase your moments here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(myMoments, key = { it.id }) { m ->
                            Card(
                                modifier = Modifier.width(210.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(m.mediaType, color = ZyroCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(m.caption, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("❤️ ${m.likesCount} • ${m.createdAtLabel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Gift Wall
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Gift Wall (${giftWall.size} Received)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (privacy.showGiftWallPublicly) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(giftWall, key = { it.id }) { gift ->
                            Card(
                                modifier = Modifier.width(136.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(gift.giftEmoji, style = MaterialTheme.typography.headlineMedium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(gift.giftName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("from ${gift.senderName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    Text("+${gift.charmEarned} 💖", color = ZyroPink, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    Text("Gift Wall is hidden by your Privacy settings.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // 6. Invite Friends Section with Shareable Live Web + App Deep Link across any platform
        item {
            val inviteUrl = buildInviteUrl(me.zyroId)
            val appDeepLink = buildAppDeepLink(me.zyroId)
            val inviteText = "Join me on Zyro! Play Uno & Carrom and hang out in Voice Rooms.\n" +
                "Open App/Web: $inviteUrl\n" +
                "Direct App Link: $appDeepLink\n" +
                "Invite ID: ${me.zyroId}"
            var friendInviteInput by remember { mutableStateOf("") }

            Column(
                modifier = Modifier.testTag("invite_friends_section"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Invite Friends", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "🔗 Live App Link",
                        color = ZyroCyan,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            Brush.horizontalGradient(listOf(ZyroCyan.copy(alpha = 0.55f), ZyroPink.copy(alpha = 0.55f))),
                            RoundedCornerShape(18.dp)
                        )
                        .testTag("invite_friends_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Link,
                                contentDescription = "Invite Link",
                                tint = ZyroCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Share Working Zyro Invite Link",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Opens live Zyro app in browser or launches installed Zyro app",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🔗 $inviteUrl",
                                style = MaterialTheme.typography.labelMedium,
                                color = ZyroCyan,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Zyro Invite Link", inviteUrl))
                                    onShareFeedback("Live invite link copied! Share it with friends.")
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("copy_invite_link_button")
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Invite Link", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.45f))
                                .clickable {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Zyro App Link", appDeepLink))
                                    onShareFeedback("App deep link copied: $appDeepLink")
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "App Deep Link: $appDeepLink",
                                style = MaterialTheme.typography.labelSmall,
                                color = ZyroGold,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Tap to Copy",
                                style = MaterialTheme.typography.labelSmall,
                                color = ZyroGold,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            InvitePlatformButton(
                                label = "WhatsApp",
                                accent = Color(0xFF25D366),
                                onClick = {
                                    launchZyroInviteShare(
                                        context = context,
                                        inviteText = inviteText,
                                        targetPackage = "com.whatsapp",
                                        platformLabel = "WhatsApp"
                                    )
                                },
                                modifier = Modifier.weight(1f).testTag("share_invite_whatsapp")
                            )
                            InvitePlatformButton(
                                label = "Instagram",
                                accent = Color(0xFFE1306C),
                                onClick = {
                                    launchZyroInviteShare(
                                        context = context,
                                        inviteText = inviteText,
                                        targetPackage = "com.instagram.android",
                                        platformLabel = "Instagram"
                                    )
                                },
                                modifier = Modifier.weight(1f).testTag("share_invite_instagram")
                            )
                            InvitePlatformButton(
                                label = "Snapchat",
                                accent = Color(0xFFFFD100),
                                darkText = true,
                                onClick = {
                                    launchZyroInviteShare(
                                        context = context,
                                        inviteText = inviteText,
                                        targetPackage = "com.snapchat.android",
                                        platformLabel = "Snapchat"
                                    )
                                },
                                modifier = Modifier.weight(1f).testTag("share_invite_snapchat")
                            )
                            InvitePlatformButton(
                                label = "Share",
                                accent = ZyroCyan,
                                darkText = true,
                                onClick = {
                                    launchZyroInviteShare(
                                        context = context,
                                        inviteText = inviteText,
                                        targetPackage = null,
                                        platformLabel = "Any Platform"
                                    )
                                },
                                modifier = Modifier.weight(0.85f).testTag("share_invite_more")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f))

                        Text(
                            text = "Have a Friend's Invite Link / ID?",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = friendInviteInput,
                                onValueChange = { friendInviteInput = it },
                                placeholder = { Text("Paste link or ZYRO-20411", style = MaterialTheme.typography.labelSmall) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("redeem_invite_input")
                            )
                            Button(
                                onClick = {
                                    onRedeemInviteLink(friendInviteInput)
                                    friendInviteInput = ""
                                },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("redeem_invite_button")
                            ) {
                                Text("Join", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 7. Joined Rooms
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Joined Rooms (${joinedRooms.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                joinedRooms.forEach { room ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onRejoinRoom(room.id) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${room.title} (${room.roomUid})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Lv.${room.level}/19 • ${room.adminSlotsMax} Admin Slots", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        FilledTonalButton(onClick = { onRejoinRoom(room.id) }) {
                            Text("Enter")
                        }
                    }
                }
            }
        }

        // 8. Four Compact Sections: VIP (1-14), Shop, Privacy, CP & Weddings + INR Top-Up & Sign Out
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Services & Customization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallSectionTile(
                        title = "VIP ${vipProg.vipLevel}/14",
                        subtitle = "Growth ${vipProg.growthPoints} pts",
                        icon = Icons.Filled.WorkspacePremium,
                        accent = ZyroGold,
                        onClick = { showVipDialog = true },
                        modifier = Modifier.weight(1f).testTag("profile_vip_tile")
                    )
                    SmallSectionTile(
                        title = "Shop",
                        subtitle = "Rings, Bubbles, Frames, Entry",
                        icon = Icons.Filled.Storefront,
                        accent = ZyroCyan,
                        onClick = { showShopDialog = true },
                        modifier = Modifier.weight(1f).testTag("profile_shop_tile")
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallSectionTile(
                        title = "Privacy",
                        subtitle = "Status & Room Visibility",
                        icon = Icons.Filled.Lock,
                        accent = ZyroViolet,
                        onClick = { showPrivacyDialog = true },
                        modifier = Modifier.weight(1f).testTag("profile_privacy_tile")
                    )
                    SmallSectionTile(
                        title = "CP & Weddings",
                        subtitle = "Rings, CP & Ceremony",
                        icon = Icons.Filled.Favorite,
                        accent = ZyroPink,
                        onClick = { showCpWeddingDialog = true },
                        modifier = Modifier.weight(1f).testTag("profile_wedding_tile")
                    )
                }

                OutlinedButton(
                    onClick = { showInrTopUpDialog = true },
                    modifier = Modifier.fillMaxWidth().testTag("open_inr_topup_button")
                ) {
                    Icon(Icons.Filled.QrCode2, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("INR Top-Up & Upload My Custom Payment QR")
                }

                TextButton(
                    onClick = onLogout,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().testTag("logout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Switch Account / Sign Out")
                }
            }
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            profile = me,
            onDismiss = { showEditProfileDialog = false },
            onSave = onUpdateProfileDetails
        )
    }

    if (showVipDialog) {
        VipDialog(
            profile = me,
            onDismiss = { showVipDialog = false },
            onActivateVipPlan = { months, priceInr ->
                onActivateVipPlan(months, priceInr)
                showVipDialog = false
            },
            onSpendVipCoinsForGrowth = onSpendVipCoinsForGrowth,
            onOpenInrTopUp = { showInrTopUpDialog = true }
        )
    }

    if (showShopDialog) {
        ShopDialog(
            profile = me,
            items = shopItems,
            onDismiss = { showShopDialog = false },
            onBuyOrEquip = onBuyOrEquipShopItem
        )
    }

    if (showPrivacyDialog) {
        PrivacyDialog(
            privacy = privacy,
            onDismiss = { showPrivacyDialog = false },
            onUpdate = onUpdatePrivacy
        )
    }

    if (showCpWeddingDialog) {
        CpAndWeddingDialog(
            profile = me,
            friends = friends,
            rings = shopItems.filter { it.category == "RING" },
            onDismiss = { showCpWeddingDialog = false },
            onBuyRing = onBuyOrEquipShopItem,
            onAcquireCp = onAcquireCp,
            onHostWedding = onHostWedding
        )
    }

    if (showInrTopUpDialog) {
        InrTopUpAndQrDialog(
            profile = me,
            onDismiss = { showInrTopUpDialog = false },
            onTopUpInr = onTopUpInr,
            onSaveCustomQr = onSaveCustomQr
        )
    }
}

@Composable
private fun ProfileStatChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
    }
}

@Composable
private fun SmallSectionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun InvitePlatformButton(
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    darkText: Boolean = false
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = if (darkText) Color.Black else Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        modifier = modifier.defaultMinSize(minHeight = 48.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

internal fun buildInviteUrl(zyroId: String): String = ZyroInviteLinks.buildWebInviteUrl(zyroId)

internal fun buildAppDeepLink(zyroId: String): String = ZyroInviteLinks.buildAppDeepLink(zyroId)

private fun launchZyroInviteShare(
    context: Context,
    inviteText: String,
    targetPackage: String?,
    platformLabel: String
) {
    val baseIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Join me on Zyro!")
        putExtra(Intent.EXTRA_TEXT, inviteText)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (targetPackage != null) {
        val targeted = Intent(baseIntent).apply { setPackage(targetPackage) }
        try {
            context.startActivity(targeted)
            return
        } catch (_: ActivityNotFoundException) {
            // Fall back to system share sheet when the target app is not installed
        }
    }
    val chooser = Intent.createChooser(baseIntent, "Invite Friends via $platformLabel").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
