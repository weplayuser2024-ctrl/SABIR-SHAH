package com.example.ui.screens.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ChatMessageEntity
import com.example.data.FriendEntity
import com.example.data.PrivacySettingsEntity
import com.example.data.UserProfileEntity
import com.example.ui.components.OfficialAdminTag
import com.example.ui.components.UserAvatar
import com.example.ui.components.VipNameAndBadge
import com.example.ui.theme.*

@Composable
fun ChatScreen(
    friends: List<FriendEntity>,
    profile: UserProfileEntity?,
    privacy: PrivacySettingsEntity,
    selectedFriendUid: String?,
    messagesForSelected: List<ChatMessageEntity>,
    onOpenChat: (String) -> Unit,
    onCloseChat: () -> Unit,
    onSendMessage: (friendUid: String, text: String) -> Unit,
    onJumpToVoiceRoom: (roomId: String) -> Unit,
    onInspectFriend: (FriendEntity) -> Unit
) {
    val activeFriend = friends.firstOrNull { it.uid == selectedFriendUid }

    if (activeFriend != null) {
        ChatConversationScreen(
            friend = activeFriend,
            myBubbleStyleId = profile?.equippedBubbleId ?: "bubble_cyber",
            messages = messagesForSelected,
            onBack = onCloseChat,
            onSend = { text -> onSendMessage(activeFriend.uid, text) },
            onInspect = { onInspectFriend(activeFriend) },
            onJoinFriendRoom = { roomId -> onJumpToVoiceRoom(roomId) }
        )
        return
    }

    var idSearchQuery by remember { mutableStateOf("") }
    val filteredFriends = remember(friends, idSearchQuery) {
        val q = idSearchQuery.trim().lowercase()
        if (q.isEmpty()) friends
        else friends.filter {
            it.zyroId.lowercase().contains(q) || it.name.lowercase().contains(q)
        }
    }

    // Requirement: ID search bar at the very top, rest of the screen dedicated solely to the chat list
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OutlinedTextField(
            value = idSearchQuery,
            onValueChange = { idSearchQuery = it },
            placeholder = { Text("Search by User ID (e.g. ZYRO-20411)") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search User ID") },
            trailingIcon = {
                if (idSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { idSearchQuery = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear Search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("chat_id_search_bar")
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filteredFriends.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No player found matching \"$idSearchQuery\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(filteredFriends, key = { it.uid }) { friend ->
                ChatListRow(
                    friend = friend,
                    showVoicePresence = !privacy.hideVoiceRoomPresence && friend.currentVoiceRoomName != null,
                    onClick = { onOpenChat(friend.uid) },
                    onJoinVoiceRoom = {
                        friend.currentVoiceRoomId?.let { onJumpToVoiceRoom(it) }
                    }
                )
            }
        }
    }
}

@Composable
private fun ChatListRow(
    friend: FriendEntity,
    showVoicePresence: Boolean,
    onClick: () -> Unit,
    onJoinVoiceRoom: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("chat_row_${friend.uid}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UserAvatar(
                name = friend.name,
                colorHex = friend.avatarColorHex,
                sizeDp = 52,
                isVip = friend.isVip,
                vipLevel = friend.vipLevel,
                isAdmin = friend.isOfficialAdmin
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VipNameAndBadge(
                            name = friend.name,
                            isVip = friend.isVip,
                            vipLevel = friend.vipLevel,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (friend.isOfficialAdmin) OfficialAdminTag()
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = friend.lastMessageTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = friend.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (showVoicePresence && friend.currentVoiceRoomName != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ZyroEmerald.copy(alpha = 0.16f))
                            .clickable { onJoinVoiceRoom() }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Filled.GraphicEq, contentDescription = "In Voice Room", tint = ZyroEmerald, modifier = Modifier.size(14.dp))
                        Text(
                            text = "In Voice Room: ${friend.currentVoiceRoomName}",
                            color = ZyroEmerald,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (friend.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${friend.unreadCount}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatConversationScreen(
    friend: FriendEntity,
    myBubbleStyleId: String,
    messages: List<ChatMessageEntity>,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onInspect: () -> Unit,
    onJoinFriendRoom: (String) -> Unit
) {
    BackHandler(onBack = onBack)
    var draft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Conversation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Chat List")
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onInspect() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UserAvatar(
                    name = friend.name,
                    colorHex = friend.avatarColorHex,
                    sizeDp = 42,
                    isVip = friend.isVip,
                    vipLevel = friend.vipLevel,
                    isAdmin = friend.isOfficialAdmin
                )
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VipNameAndBadge(
                            name = friend.name,
                            isVip = friend.isVip,
                            vipLevel = friend.vipLevel
                        )
                        if (friend.isOfficialAdmin) OfficialAdminTag()
                    }
                    Text(
                        text = "ID: ${friend.zyroId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (friend.currentVoiceRoomId != null) {
                FilledTonalButton(
                    onClick = { onJoinFriendRoom(friend.currentVoiceRoomId) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Join Room", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val styleId = if (msg.isFromMe) myBubbleStyleId else msg.bubbleStyleId
                val bubbleBrush = when (styleId) {
                    "bubble_sakura" -> Brush.horizontalGradient(listOf(Color(0xFF9D174D), ZyroPink))
                    "bubble_royal" -> Brush.horizontalGradient(listOf(Color(0xFF78350F), ZyroGoldDeep))
                    else -> Brush.horizontalGradient(listOf(Color(0xFF0E7490), Color(0xFF2563EB)))
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (msg.isFromMe) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = if (msg.isFromMe) 18.dp else 4.dp,
                                    bottomEnd = if (msg.isFromMe) 4.dp else 18.dp
                                )
                            )
                            .then(
                                if (msg.isFromMe) {
                                    Modifier
                                        .background(bubbleBrush)
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
                                } else {
                                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                }
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = msg.text,
                            color = if (msg.isFromMe) Color.White else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = msg.timeLabel,
                            color = if (msg.isFromMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Message ${friend.name}...") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f)
            )
            FilledIconButton(
                onClick = {
                    if (draft.isNotBlank()) {
                        onSend(draft)
                        draft = ""
                    }
                }
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send Message")
            }
        }
    }
}
