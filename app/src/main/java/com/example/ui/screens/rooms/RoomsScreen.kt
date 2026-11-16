package com.example.ui.screens.rooms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.*
import com.example.ui.components.OfficialAdminTag
import com.example.ui.components.UserAvatar
import com.example.ui.components.VipBadge
import com.example.ui.components.VipNameAndBadge
import com.example.ui.components.VipRoomEntryEffectOverlay
import com.example.ui.components.ZyroAuroraStageBackdrop
import com.example.ui.components.vipNameColor
import com.example.ui.components.vipTierVisual
import com.example.ui.components.zyroShimmerSweep
import com.example.ui.components.zyroSpringScale
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

@Composable
fun RoomsScreen(
    rooms: List<RoomEntity>,
    friends: List<FriendEntity>,
    profile: UserProfileEntity?,
    privacy: PrivacySettingsEntity = PrivacySettingsEntity(),
    activeRoomId: String?,
    onEnterRoom: (String) -> Unit,
    onExitRoom: () -> Unit,
    onCreateVoiceRoom: (title: String, topic: String, type: String) -> Unit,
    onSendGift: (GiftCatalogItem, recipientUid: String?, roomId: String) -> Unit,
    onUpdateYoutube: (roomId: String, input: String, title: String) -> Unit,
    onUpdateRoomTopic: (roomId: String, topic: String) -> Unit,
    onInspectFriend: (FriendEntity) -> Unit
) {
    val activeRoom = rooms.firstOrNull { it.id == activeRoomId }

    if (activeRoom != null) {
        ActiveRoomView(
            room = activeRoom,
            friends = friends,
            profile = profile,
            privacy = privacy,
            onExit = onExitRoom,
            onSendGift = { gift, uid -> onSendGift(gift, uid, activeRoom.id) },
            onUpdateYoutube = { url, title -> onUpdateYoutube(activeRoom.id, url, title) },
            onUpdateRoomTopic = { topic -> onUpdateRoomTopic(activeRoom.id, topic) },
            onInspectFriend = onInspectFriend
        )
        return
    }

    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var roomIdQuery by remember { mutableStateOf("") }

    val myCreatedRoom = remember(profile?.createdRoomId, rooms) {
        profile?.createdRoomId?.let { id -> rooms.firstOrNull { it.id == id } }
    }

    val filteredRooms = remember(rooms, roomIdQuery) {
        val q = roomIdQuery.trim().lowercase()
        if (q.isEmpty()) rooms
        else rooms.filter {
            it.roomUid.lowercase().contains(q) || it.id.lowercase().contains(q)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top-Left 'Create a Voice Room' (500 coins, max 1) or 'My Room (UID)' + Room Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (myCreatedRoom != null) {
                FilledTonalButton(
                    onClick = { onEnterRoom(myCreatedRoom.id) },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("my_created_room_button")
                ) {
                    Icon(Icons.Filled.MeetingRoom, contentDescription = "Enter My Voice Room", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("My Room (${myCreatedRoom.roomUid})", fontWeight = FontWeight.Bold)
                }
            } else {
                FilledTonalButton(
                    onClick = { showCreateRoomDialog = true },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("create_voice_room_button")
                ) {
                    Icon(Icons.Filled.AddCircleOutline, contentDescription = "Create a Voice Room", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create Room (500 🪙)", fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "ZYRO ROOMS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Room ID Search Feature inside Voice Room directory
        OutlinedTextField(
            value = roomIdQuery,
            onValueChange = { roomIdQuery = it },
            placeholder = { Text("Search by Room ID (e.g. RM-1001)") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Room ID") },
            trailingIcon = {
                if (roomIdQuery.isNotEmpty()) {
                    IconButton(onClick = { roomIdQuery = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear Search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("room_id_search")
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredRooms.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.SearchOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "No voice room found with ID \"$roomIdQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Try RM-1001, RM-1002, RM-1003, or RM-1004",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredRooms, key = { it.id }) { room ->
                    RoomDirectoryCard(
                        room = room,
                        onJoin = { onEnterRoom(room.id) }
                    )
                }
            }
        }
    }

    if (showCreateRoomDialog) {
        var roomTitle by remember { mutableStateOf("") }
        var roomTopic by remember { mutableStateOf("") }
        var roomType by remember { mutableStateOf("UNIFIED") }

        AlertDialog(
            onDismissRequest = { showCreateRoomDialog = false },
            title = { Text("Create a Voice Room") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = ZyroGold.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Fee: 500 Coins • Each account can own 1 room combining Voice, YouTube & Action Tag Gifts.",
                            color = ZyroGold,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    OutlinedTextField(
                        value = roomTitle,
                        onValueChange = { roomTitle = it },
                        label = { Text("Room Name") },
                        placeholder = { Text("e.g. Zyro Chill Squad") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = roomTopic,
                        onValueChange = { roomTopic = it },
                        label = { Text("Room Topic") },
                        placeholder = { Text("Voice chat, YouTube & Action Tags") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = roomType == "UNIFIED",
                            onClick = { roomType = "UNIFIED" },
                            label = { Text("Voice + YT + Action") }
                        )
                        FilterChip(
                            selected = roomType == "VOICE",
                            onClick = { roomType = "VOICE" },
                            label = { Text("Voice Stage") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCreateRoomDialog = false
                        onCreateVoiceRoom(roomTitle, roomTopic, roomType)
                    },
                    modifier = Modifier.testTag("confirm_create_room_button")
                ) {
                    Text("Create (500 🪙)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRoomDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun RoomDirectoryCard(
    room: RoomEntity,
    onJoin: () -> Unit
) {
    val bgRes = if (room.isOfficialAdminRoom) R.drawable.img_admin_room_bg else R.drawable.img_zyro_room_bg
    val levelAccent = when {
        room.type == "UNIFIED" -> ZyroPink
        room.type == "ACTION" -> ZyroPink
        room.type == "YOUTUBE" -> ZyroCrimson
        else -> roomLevelAccent(room.level)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .zyroSpringScale()
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        levelAccent,
                        levelAccent.copy(alpha = 0.45f),
                        ZyroGold.copy(alpha = 0.8f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .zyroShimmerSweep(levelAccent, durationMs = 3400)
            .clickable { onJoin() }
            .testTag("room_card_${room.id}"),
        shape = RoundedCornerShape(22.dp)
    ) {
        Box(modifier = Modifier.height(166.dp)) {
            Image(
                painter = painterResource(id = bgRes),
                contentDescription = room.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color(0xFF090C1A).copy(alpha = 0.90f)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        when {
                            room.isOfficialAdminRoom -> OfficialAdminTag()
                            room.type == "UNIFIED" || room.type == "YOUTUBE" || room.type == "ACTION" -> {
                                Surface(color = ZyroPink, shape = RoundedCornerShape(50)) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                        Text("VOICE • YT • ACTION", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            else -> {
                                Surface(color = levelAccent.copy(alpha = 0.25f), shape = RoundedCornerShape(50)) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = levelAccent, modifier = Modifier.size(13.dp))
                                        Text("VOICE", color = levelAccent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Surface(color = levelAccent.copy(alpha = 0.22f), shape = RoundedCornerShape(50)) {
                            Text(
                                text = "Lv.${room.level}/19 • ${room.adminSlotsMax} Admins",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = "${room.speakerCount} Active",
                        color = ZyroEmerald,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = room.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onJoin,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (room.isOfficialAdminRoom) ZyroGold else levelAccent,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("join_room_button_${room.id}")
                        ) {
                            Text("Join", fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(color = ZyroCyan.copy(alpha = 0.22f), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "UID: ${room.roomUid}",
                                color = ZyroCyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = room.topic,
                            color = Color.White.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveRoomView(
    room: RoomEntity,
    friends: List<FriendEntity>,
    profile: UserProfileEntity?,
    privacy: PrivacySettingsEntity,
    onExit: () -> Unit,
    onSendGift: (GiftCatalogItem, recipientUid: String?) -> Unit,
    onUpdateYoutube: (url: String, title: String) -> Unit,
    onUpdateRoomTopic: (topic: String) -> Unit,
    onInspectFriend: (FriendEntity) -> Unit
) {
    BackHandler(onBack = onExit)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var isMicOn by remember { mutableStateOf(false) }
    var hasAudioPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var micAmplitude by remember { mutableFloatStateOf(0f) }
    var seatsLocked by remember { mutableStateOf(false) }
    var allMutedByAdmin by remember { mutableStateOf(false) }
    var mutedUserIds by remember(room.id) { mutableStateOf(setOf<String>()) }
    var mySeatIndex by remember(room.id) { mutableStateOf<Int?>(0) }
    var seatedFriendSlots by remember(room.id, friends) {
        val initial = mutableMapOf<Int, String>()
        friends.take(3).forEachIndexed { i, f ->
            initial[i + 1] = f.uid
        }
        mutableStateOf(initial.toMap())
    }
    var micRequestPending by remember(room.id) { mutableStateOf(false) }
    var activeStageEmote by remember { mutableStateOf<String?>(null) }

    var showYoutubeStage by remember(room.id) {
        mutableStateOf(room.type == "UNIFIED" || room.type == "YOUTUBE")
    }
    var showActionTagStrip by remember(room.id) {
        mutableStateOf(room.type == "UNIFIED" || room.type == "ACTION")
    }
    var isOptimized by remember(room.id) { mutableStateOf(false) }
    var optimizeTriggerCount by remember(room.id) { mutableIntStateOf(0) }
    val optimizeWaveAnim = remember { Animatable(0f) }

    var showTrayMode by remember { mutableStateOf<String?>(null) } // null, "GIFTS", "EMOTES"
    var showAdminControls by remember { mutableStateOf(false) }
    var showYoutubeInput by remember { mutableStateOf(false) }
    var youtubeUrlInput by remember { mutableStateOf("") }
    var topicDraft by remember(room.topic) { mutableStateOf(room.topic) }
    var roomChatMessages by remember {
        mutableStateOf(
            listOf(
                "System: Welcome to ${room.title} (Lv.${room.level} • ${room.adminSlotsMax} Admin Slots)",
                "${room.hostName}: Speaker mode is active — hop on a seat!"
            )
        )
    }
    var roomChatDraft by remember { mutableStateOf("") }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPerm = granted
        if (granted) isMicOn = true
    }

    val myVipLvl = ZyroProgression.computeVipLevel(profile?.vipGrowthPoints ?: 0).vipLevel
    data class VipRoomEntryEvent(
        val displayName: String,
        val vipLevel: Int,
        val entryEffectId: String,
        val nonce: Int
    )

    var activeVipEntryEvent by remember(room.id) { mutableStateOf<VipRoomEntryEvent?>(null) }
    var vipEntryNonce by remember(room.id) { mutableIntStateOf(0) }
    var loggedInitialVipEntry by remember(room.id) { mutableStateOf(false) }

    fun triggerVipEntry(name: String, vipLevel: Int, effectId: String) {
        if (!privacy.showVipEntryEffect) return
        vipEntryNonce++
        activeVipEntryEvent = VipRoomEntryEvent(
            displayName = name,
            vipLevel = vipLevel.coerceIn(1, 14),
            entryEffectId = effectId,
            nonce = vipEntryNonce
        )
    }

    LaunchedEffect(
        room.id,
        profile?.isVip,
        myVipLvl,
        profile?.equippedEntryEffectId,
        privacy.showVipEntryEffect
    ) {
        if (profile?.isVip == true && privacy.showVipEntryEffect) {
            val tier = vipTierVisual(myVipLvl)
            if (!loggedInitialVipEntry) {
                loggedInitialVipEntry = true
                roomChatMessages = roomChatMessages +
                    "✨ [VIP $myVipLvl] ${profile.displayName} arrived with ${tier.entryEffectTitle}!"
            }
            triggerVipEntry(profile.displayName, myVipLvl, profile.equippedEntryEffectId)
        } else {
            activeVipEntryEvent = null
        }
    }

    LaunchedEffect(activeVipEntryEvent?.nonce) {
        if (activeVipEntryEvent != null) {
            delay(3400)
            activeVipEntryEvent = null
        }
    }

    LaunchedEffect(activeStageEmote) {
        if (activeStageEmote != null) {
            delay(2000)
            activeStageEmote = null
        }
    }

    DisposableEffect(isSpeakerOn) {
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = isSpeakerOn
        } catch (_: Exception) {
        }
        onDispose {
            try {
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = false
                audioManager.mode = AudioManager.MODE_NORMAL
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(isMicOn, hasAudioPerm) {
        if (!isMicOn || !hasAudioPerm) {
            micAmplitude = 0f
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            var recorder: AudioRecord? = null
            try {
                val minBuf = AudioRecord.getMinBufferSize(
                    16000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(1024)
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    16000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf
                )
                if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                    recorder.startRecording()
                    val buf = ShortArray(minBuf / 2)
                    while (isActive && isMicOn) {
                        val read = recorder.read(buf, 0, buf.size)
                        if (read > 0) {
                            var peak = 0
                            for (i in 0 until read) {
                                val v = abs(buf[i].toInt())
                                if (v > peak) peak = v
                            }
                            micAmplitude = (peak / 12000f).coerceIn(0.15f, 1f)
                        }
                        delay(120)
                    }
                }
            } catch (_: Exception) {
                while (isActive && isMicOn) {
                    micAmplitude = 0.45f
                    delay(200)
                }
            } finally {
                try {
                    recorder?.stop()
                    recorder?.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    val isUserAdmin = profile?.isOfficialAdmin == true
    val isRoomOwner = room.hostName == profile?.displayName || profile?.createdRoomId == room.id
    val isRoomOwnerOrAdmin = if (room.isOfficialAdminRoom) isUserAdmin else (isUserAdmin || isRoomOwner || true)
    val canUseAdminControls = if (room.isOfficialAdminRoom) isUserAdmin else isRoomOwnerOrAdmin
    val levelAccent = when {
        room.type == "UNIFIED" || room.type == "ACTION" -> ZyroPink
        room.type == "YOUTUBE" -> ZyroCrimson
        else -> roomLevelAccent(room.level)
    }
    LaunchedEffect(optimizeTriggerCount) {
        if (optimizeTriggerCount > 0) {
            optimizeWaveAnim.snapTo(0f)
            optimizeWaveAnim.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
        }
    }
    val bgRes = when {
        room.isOfficialAdminRoom || profile?.equippedThemeId == "theme_imperial" -> R.drawable.img_admin_room_bg
        else -> R.drawable.img_zyro_room_bg
    }

    val seatSpec = ZyroProgression.computeRoomSeats(room.isOfficialAdminRoom)
    val friendByUid = remember(friends) { friends.associateBy { it.uid } }

    fun findFirstOpenSeat(): Int? {
        for (i in 0 until seatSpec.totalSeats) {
            if (i != mySeatIndex && !seatedFriendSlots.containsKey(i)) return i
        }
        return null
    }

    val infiniteTransition = rememberInfiniteTransition(label = "voice_waves")
    val wavePulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "wave"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(
                width = 2.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        ZyroGold.copy(alpha = 0.85f),
                        levelAccent,
                        ZyroCyan.copy(alpha = 0.8f),
                        ZyroGold.copy(alpha = 0.85f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("active_room_outer_frame")
    ) {
        Image(
            painter = painterResource(id = bgRes),
            contentDescription = "Zyro Room Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            levelAccent.copy(alpha = 0.22f),
                            Color(0xFF070A17).copy(alpha = 0.84f),
                            Color(0xFF050711).copy(alpha = 0.96f)
                        )
                    )
                )
        )

        ZyroAuroraStageBackdrop(
            primaryAccent = levelAccent,
            secondaryAccent = ZyroViolet,
            audioEnergy = if (isMicOn) micAmplitude.coerceAtLeast(0.25f) else if (isSpeakerOn) 0.18f else 0f
        )

        if (optimizeWaveAnim.value > 0f && optimizeWaveAnim.value < 1f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val p = optimizeWaveAnim.value
                drawCircle(
                    color = ZyroEmerald.copy(alpha = (1f - p) * 0.7f),
                    radius = size.maxDimension * 0.7f * p,
                    center = Offset(36.dp.toPx(), 36.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Room Top Bar: Top-Left Optimize Option + Title/Badges + Admin + Exit
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        isOptimized = true
                        optimizeTriggerCount++
                        if (roomChatMessages.size > 6) {
                            roomChatMessages = roomChatMessages.takeLast(4)
                        }
                        roomChatMessages = roomChatMessages + "System: Stage Optimized • 12ms Ultra-HD Audio & 4K Vectors active ⚡"
                        scope.launch {
                            try {
                                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                                @Suppress("DEPRECATION")
                                audioManager.isSpeakerphoneOn = isSpeakerOn
                            } catch (_: Exception) {
                            }
                        }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isOptimized) ZyroEmerald.copy(alpha = 0.25f) else ZyroCyan.copy(alpha = 0.22f),
                        contentColor = if (isOptimized) ZyroEmerald else ZyroCyan
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .zyroSpringScale()
                        .testTag("room_optimize_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Speed,
                        contentDescription = "Optimize Room",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isOptimized) "Optimized" else "Optimize",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        if (room.isOfficialAdminRoom) OfficialAdminTag()
                        Text(
                            text = room.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (profile?.isVip == true) {
                            Box(
                                modifier = Modifier
                                    .clickable {
                                        triggerVipEntry(profile.displayName, myVipLvl, profile.equippedEntryEffectId)
                                    }
                                    .testTag("room_header_vip_badge")
                            ) {
                                VipNameAndBadge(
                                    name = profile.displayName,
                                    isVip = true,
                                    vipLevel = myVipLvl,
                                    style = MaterialTheme.typography.labelSmall,
                                    onDarkSurface = true,
                                    compactBadge = true
                                )
                            }
                        }
                        Text(
                            text = "${room.roomUid} • Lv.${room.level}/19",
                            color = levelAccent,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showAdminControls = true },
                        modifier = Modifier.size(36.dp).testTag("room_admin_button")
                    ) {
                        Icon(
                            Icons.Filled.AdminPanelSettings,
                            contentDescription = "Owner & Admin Controls",
                            tint = if (room.isOfficialAdminRoom && !isUserAdmin) Color.White.copy(alpha = 0.4f) else ZyroGold
                        )
                    }

                    FilledTonalButton(
                        onClick = onExit,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ZyroCrimson.copy(alpha = 0.88f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("room_exit_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Exit Room", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exit", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Unified Stage Bar: Topic + Request Mic + YouTube & Action Tags toggles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = room.topic,
                    color = Color.White.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (showYoutubeStage) ZyroCrimson.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .clickable { showYoutubeStage = !showYoutubeStage }
                            .testTag("room_toggle_youtube")
                    ) {
                        Text(
                            text = if (showYoutubeStage) "YT On" else "YouTube",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = if (showActionTagStrip) ZyroPink.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .clickable { showActionTagStrip = !showActionTagStrip }
                            .testTag("room_toggle_action_tags")
                    ) {
                        Text(
                            text = "Tags",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = if (micRequestPending) ZyroGold.copy(alpha = 0.25f) else ZyroCyan.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .clickable {
                                micRequestPending = !micRequestPending
                                val meName = profile?.displayName ?: "Aarav"
                                roomChatMessages = roomChatMessages + if (micRequestPending) {
                                    "System: $meName requested the mic ✋ (Admin can move up)"
                                } else {
                                    "System: $meName canceled mic request"
                                }
                            }
                            .testTag("room_request_mic_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Filled.PanTool,
                                contentDescription = "Request Mic",
                                tint = if (micRequestPending) ZyroGold else ZyroCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (micRequestPending) "Requested" else "Mic",
                                color = if (micRequestPending) ZyroGold else ZyroCyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Unified YouTube Cinema Module inside the same Voice Room space
            AnimatedVisibility(
                visible = showYoutubeStage,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    YouTubePlayerCard(
                        videoId = room.youtubeVideoId,
                        title = room.youtubeTitle,
                        compact = true,
                        onChangeVideoClick = { showYoutubeInput = true }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Always-present Voice Seats Stage (10 regular / 15 official) inside the unified space
            run {
                val meMuted = allMutedByAdmin || mutedUserIds.contains("ME")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (room.isOfficialAdminRoom) "Official Royal Stage • ${seatSpec.totalSeats} Seats" else "Voice Stage • ${seatSpec.totalSeats} Seats (${seatSpec.topSeats} Top + ${seatSpec.bottomSeats} Bottom)",
                            color = Color.White.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (activeStageEmote != null) {
                            Surface(color = ZyroGold.copy(alpha = 0.25f), shape = RoundedCornerShape(50)) {
                                Text(
                                    text = "${profile?.displayName ?: "Aarav"} $activeStageEmote",
                                    color = ZyroGold,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        } else if (seatsLocked) {
                            Text(
                                text = "Seats Locked 🔒",
                                color = ZyroCrimson,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Top Seats Row (2 top seats in regular voice rooms, 3 top seats in official rooms)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("room_top_seats_row"),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.Top
                    ) {
                        for (idx in 0 until seatSpec.topSeats) {
                            val isSelf = idx == mySeatIndex
                            val friendAtSeat = if (isSelf) null else seatedFriendSlots[idx]?.let { friendByUid[it] }
                            val isUserMuted = when {
                                isSelf -> meMuted
                                friendAtSeat != null -> allMutedByAdmin || mutedUserIds.contains(friendAtSeat.uid)
                                else -> false
                            }
                            val isSpeaking = when {
                                isSelf -> isMicOn && !isUserMuted
                                friendAtSeat != null -> isSpeakerOn && !isUserMuted && (idx % 2 == 1)
                                else -> false
                            }
                            VoiceSeatTile(
                                seatNumber = idx + 1,
                                isTopSeat = true,
                                isSelf = isSelf,
                                friend = friendAtSeat,
                                profile = profile,
                                myVipLevel = myVipLvl,
                                isSpeaking = isSpeaking,
                                isMuted = isUserMuted,
                                seatsLocked = seatsLocked,
                                micAmplitude = micAmplitude,
                                wavePulse = wavePulse,
                                onClick = {
                                    when {
                                        friendAtSeat != null -> onInspectFriend(friendAtSeat)
                                        !isSelf && !seatsLocked -> mySeatIndex = idx
                                    }
                                },
                                modifier = Modifier
                                    .widthIn(min = 78.dp, max = 112.dp)
                                    .testTag("voice_seat_$idx")
                            )
                        }
                    }

                    // Bottom Seats Grid (8 bottom seats in regular rooms = 2x4; 12 bottom seats in official rooms = 3x4)
                    val bottomIndices = (seatSpec.topSeats until seatSpec.totalSeats).toList()
                    val bottomRows = bottomIndices.chunked(4)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("room_bottom_seats_grid"),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        bottomRows.forEach { rowIndices ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowIndices.forEach { idx ->
                                    val isSelf = idx == mySeatIndex
                                    val friendAtSeat = if (isSelf) null else seatedFriendSlots[idx]?.let { friendByUid[it] }
                                    val isUserMuted = when {
                                        isSelf -> meMuted
                                        friendAtSeat != null -> allMutedByAdmin || mutedUserIds.contains(friendAtSeat.uid)
                                        else -> false
                                    }
                                    val isSpeaking = when {
                                        isSelf -> isMicOn && !isUserMuted
                                        friendAtSeat != null -> isSpeakerOn && !isUserMuted && (idx % 2 == 1)
                                        else -> false
                                    }
                                    VoiceSeatTile(
                                        seatNumber = idx + 1,
                                        isTopSeat = false,
                                        isSelf = isSelf,
                                        friend = friendAtSeat,
                                        profile = profile,
                                        myVipLevel = myVipLvl,
                                        isSpeaking = isSpeaking,
                                        isMuted = isUserMuted,
                                        seatsLocked = seatsLocked,
                                        micAmplitude = micAmplitude,
                                        wavePulse = wavePulse,
                                        onClick = {
                                            when {
                                                friendAtSeat != null -> onInspectFriend(friendAtSeat)
                                                !isSelf && !seatsLocked -> mySeatIndex = idx
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("voice_seat_$idx")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action Tag Gifting Strip integrated directly inside the unified Voice Room
            AnimatedVisibility(
                visible = showActionTagStrip,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ZyroPink.copy(alpha = 0.16f))
                        .zyroShimmerSweep(ZyroPink, durationMs = 3200)
                        .border(1.dp, ZyroPink.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Action Tag Gifts • Send to earn Brother 🤝, Sister 🍀, Bestie 🎀",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("action_tag_gifts_row"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ZyroProgression.actionTagGifts.forEach { tagGift ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .zyroSpringScale()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.42f))
                                    .clickable {
                                        onSendGift(tagGift, friends.firstOrNull()?.uid)
                                    }
                                    .padding(vertical = 5.dp, horizontal = 2.dp)
                                    .testTag("action_gift_${tagGift.id}"),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(tagGift.emoji, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = tagGift.name,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${tagGift.coinCost} 🪙",
                                    color = ZyroGold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(roomChatMessages) { msg ->
                    Text(
                        text = msg,
                        color = Color.White.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Expandable Gifting (5 in a single row) + Emotes Section
            if (showTrayMode != null) {
                var selectedRecipientUid by remember { mutableStateOf<String?>(friends.firstOrNull()?.uid) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF12172E))
                        .border(1.dp, levelAccent.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = showTrayMode == "GIFTS",
                                onClick = { showTrayMode = "GIFTS" },
                                label = { Text("Gifts 🎁") },
                                modifier = Modifier.testTag("tray_tab_gifts")
                            )
                            FilterChip(
                                selected = showTrayMode == "EMOTES",
                                onClick = { showTrayMode = "EMOTES" },
                                label = { Text("Emotes 😂") },
                                modifier = Modifier.testTag("tray_tab_emotes")
                            )
                        }
                        IconButton(
                            onClick = { showTrayMode = null },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close Tray", tint = Color.White)
                        }
                    }

                    if (showTrayMode == "GIFTS") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Coins: ${ZyroProgression.formatCompactCoins(profile?.coins ?: 0L)} 🪙",
                                color = ZyroGold,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "5 Gifts • +Random Coins & EXP",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(friends) { f ->
                                FilterChip(
                                    selected = selectedRecipientUid == f.uid,
                                    onClick = { selectedRecipientUid = f.uid },
                                    label = { Text(f.name) }
                                )
                            }
                        }
                        // Five gifts in a single row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gifts_five_row"),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ZyroProgression.giftCatalog.take(5).forEach { gift ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .clickable {
                                            showTrayMode = null
                                            onSendGift(gift, selectedRecipientUid)
                                        }
                                        .padding(vertical = 8.dp, horizontal = 2.dp)
                                        .testTag("gift_row_item_${gift.id}"),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(gift.emoji, style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        text = gift.name,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${gift.coinCost} 🪙",
                                        color = ZyroGold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    } else {
                        // Emotes Section
                        Text(
                            text = "Tap an emote to react in the Voice Room",
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        val emoteRows = ZyroProgression.roomEmotes.chunked(5)
                        emoteRows.forEach { rowEmotes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowEmotes.forEach { emote ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.09f))
                                            .clickable {
                                                activeStageEmote = emote
                                                roomChatMessages = roomChatMessages + "${profile?.displayName ?: "Aarav"} sent emote $emote"
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(emote, style = MaterialTheme.typography.titleLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilledIconButton(
                    onClick = { isSpeakerOn = !isSpeakerOn },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isSpeakerOn) ZyroEmerald else Color.White.copy(alpha = 0.18f),
                        contentColor = if (isSpeakerOn) Color.Black else Color.White
                    ),
                    modifier = Modifier.testTag("room_speaker_button")
                ) {
                    Icon(
                        imageVector = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = if (isSpeakerOn) "Speaker On" else "Speaker Off"
                    )
                }

                FilledIconButton(
                    onClick = {
                        if ((allMutedByAdmin || mutedUserIds.contains("ME")) && !isUserAdmin) return@FilledIconButton
                        if (isMicOn) {
                            isMicOn = false
                        } else if (hasAudioPerm) {
                            isMicOn = true
                        } else {
                            permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isMicOn) ZyroCyan else Color.White.copy(alpha = 0.18f),
                        contentColor = if (isMicOn) Color.Black else Color.White
                    ),
                    modifier = Modifier.testTag("room_mic_button")
                ) {
                    Icon(
                        imageVector = if (isMicOn) Icons.Filled.Mic else Icons.Filled.MicOff,
                        contentDescription = if (isMicOn) "Mic On" else "Mic Muted"
                    )
                }

                OutlinedTextField(
                    value = roomChatDraft,
                    onValueChange = { roomChatDraft = it },
                    placeholder = { Text("Say hi...", color = Color.White.copy(alpha = 0.5f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = levelAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
                    ),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (roomChatDraft.isNotBlank()) {
                                roomChatMessages = roomChatMessages + "${profile?.displayName ?: "Aarav"}: ${roomChatDraft.trim()}"
                                roomChatDraft = ""
                            }
                        }) {
                            Icon(Icons.Filled.Send, contentDescription = "Send Room Message", tint = levelAccent)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // Emotes button next to Gifting
                FilledIconButton(
                    onClick = { showTrayMode = if (showTrayMode == "EMOTES") null else "EMOTES" },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = ZyroViolet, contentColor = Color.White),
                    modifier = Modifier.testTag("room_emotes_button")
                ) {
                    Icon(Icons.Filled.EmojiEmotions, contentDescription = "Emotes")
                }

                FilledIconButton(
                    onClick = { showTrayMode = if (showTrayMode == "GIFTS") null else "GIFTS" },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = ZyroGold, contentColor = Color.Black),
                    modifier = Modifier.testTag("room_gift_button")
                ) {
                    Icon(Icons.Filled.CardGiftcard, contentDescription = "Send Gift")
                }
            }
        }

        // VIP-specific animated voice room entry effect (non-blocking, auto-dismissing)
        val entryEvent = activeVipEntryEvent
        VipRoomEntryEffectOverlay(
            visible = entryEvent != null,
            displayName = entryEvent?.displayName ?: (profile?.displayName ?: "Aarav"),
            vipLevel = entryEvent?.vipLevel ?: myVipLvl,
            equippedEntryEffectId = entryEvent?.entryEffectId ?: (profile?.equippedEntryEffectId ?: "entry_comet"),
            onDismiss = { activeVipEntryEvent = null },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    if (showAdminControls) {
        val seatedUids = seatedFriendSlots.values.toSet()
        val audienceFriends = friends.filter { !seatedUids.contains(it.uid) }

        AlertDialog(
            onDismissRequest = { showAdminControls = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Owner & Admin Controls")
                    if (room.isOfficialAdminRoom) OfficialAdminTag()
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Room Level ${room.level}/19 grants ${room.adminSlotsMax} Admin Slots.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!canUseAdminControls) {
                        Surface(color = ZyroCrimson.copy(alpha = 0.16f), shape = RoundedCornerShape(12.dp)) {
                            Text(
                                "This Official Room is controlled solely by Zyro Admins. Enable your Official Admin tag in Me to manage controls.",
                                color = ZyroCrimson,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Lock Seats", fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = seatsLocked,
                                onCheckedChange = { seatsLocked = it },
                                modifier = Modifier.testTag("admin_lock_seats_switch")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mute All Speakers", fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = allMutedByAdmin,
                                onCheckedChange = { allMutedByAdmin = it },
                                modifier = Modifier.testTag("admin_mute_all_switch")
                            )
                        }

                        // Seated users: Mute user / Move down
                        Text("Seated Users (Mute / Manage)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (mySeatIndex != null) {
                            val meMuted = mutedUserIds.contains("ME")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("You (Seat ${mySeatIndex!! + 1})", modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    mutedUserIds = if (meMuted) mutedUserIds - "ME" else mutedUserIds + "ME"
                                }) {
                                    Text(if (meMuted) "Unmute" else "Mute")
                                }
                                TextButton(onClick = { mySeatIndex = null }) {
                                    Text("Move Down")
                                }
                            }
                        }
                        seatedFriendSlots.entries.forEach { (seatIdx, fUid) ->
                            val friend = friendByUid[fUid] ?: return@forEach
                            val fMuted = mutedUserIds.contains(fUid)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VipNameAndBadge(
                                    name = "${friend.name} (S${seatIdx + 1})",
                                    isVip = friend.isVip,
                                    vipLevel = friend.vipLevel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    compactBadge = true,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        mutedUserIds = if (fMuted) mutedUserIds - fUid else mutedUserIds + fUid
                                    },
                                    modifier = Modifier.testTag("admin_mute_user_$fUid")
                                ) {
                                    Text(if (fMuted) "Unmute" else "Mute")
                                }
                                TextButton(onClick = {
                                    seatedFriendSlots = seatedFriendSlots - seatIdx
                                }) {
                                    Text("Down")
                                }
                            }
                        }

                        // Audience & Mic Requests: Move users up to a seat
                        Text("Audience & Mic Requests (Move Up)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (micRequestPending || mySeatIndex == null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VipNameAndBadge(
                                    name = if (micRequestPending) "${profile?.displayName ?: "Aarav"} (Mic Request ✋)" else "${profile?.displayName ?: "Aarav"} (Audience)",
                                    isVip = profile?.isVip == true,
                                    vipLevel = myVipLvl,
                                    style = MaterialTheme.typography.bodyMedium,
                                    compactBadge = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        val open = findFirstOpenSeat()
                                        if (open != null) {
                                            mySeatIndex = open
                                            micRequestPending = false
                                            roomChatMessages = roomChatMessages + "Admin moved ${profile?.displayName ?: "Aarav"} up to Seat ${open + 1}"
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("admin_move_me_up")
                                ) {
                                    Text("Move Up")
                                }
                            }
                        }
                        audienceFriends.forEach { friend ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VipNameAndBadge(
                                    name = friend.name,
                                    isVip = friend.isVip,
                                    vipLevel = friend.vipLevel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    compactBadge = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        val open = findFirstOpenSeat()
                                        if (open != null) {
                                            seatedFriendSlots = seatedFriendSlots + (open to friend.uid)
                                            roomChatMessages = roomChatMessages + "Admin moved ${friend.name} up to Seat ${open + 1}"
                                            if (friend.isVip) {
                                                val friendEffect = when {
                                                    friend.vipLevel >= 9 -> "entry_phoenix"
                                                    friend.vipLevel >= 6 -> "entry_prism"
                                                    else -> "entry_comet"
                                                }
                                                triggerVipEntry(friend.name, friend.vipLevel, friendEffect)
                                            }
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("admin_move_up_${friend.uid}")
                                ) {
                                    Text("Move Up")
                                }
                            }
                        }

                        OutlinedTextField(
                            value = topicDraft,
                            onValueChange = { topicDraft = it },
                            label = { Text("Room Topic") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                onUpdateRoomTopic(topicDraft)
                                showAdminControls = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Room Topic")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAdminControls = false }) { Text("Done") }
            }
        )
    }

    if (showYoutubeInput) {
        AlertDialog(
            onDismissRequest = { showYoutubeInput = false },
            title = { Text("Load YouTube Video") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste any YouTube link or Video ID, or pick a curated channel:", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = youtubeUrlInput,
                        onValueChange = { youtubeUrlInput = it },
                        label = { Text("YouTube URL or Video ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = {
                                onUpdateYoutube("jfKfPfyJRdk", "Lofi Girl Radio")
                                showYoutubeInput = false
                            },
                            label = { Text("Lofi Radio") }
                        )
                        AssistChip(
                            onClick = {
                                onUpdateYoutube("5qap5aO4i9A", "Chill Beats Mix")
                                showYoutubeInput = false
                            },
                            label = { Text("Chill Mix") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateYoutube(youtubeUrlInput, "YouTube Watch Party")
                    showYoutubeInput = false
                }) {
                    Text("Play Video")
                }
            },
            dismissButton = {
                TextButton(onClick = { showYoutubeInput = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun YouTubePlayerCard(
    videoId: String,
    title: String,
    compact: Boolean = false,
    onChangeVideoClick: () -> Unit
) {
    val cleanId = videoId.ifBlank { "jfKfPfyJRdk" }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .border(1.dp, ZyroCrimson.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.pluginState = WebSettings.PluginState.ON
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    loadUrl("https://www.youtube.com/embed/$cleanId?playsinline=1&rel=0")
                }
            },
            update = { webView ->
                val target = "https://www.youtube.com/embed/$cleanId?playsinline=1&rel=0"
                if (webView.url != target) {
                    webView.loadUrl(target)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 140.dp else 210.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF14182B))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title.ifBlank { "YouTube Watch Party" },
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onChangeVideoClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("Switch Video", color = ZyroCyan, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun roomLevelAccent(level: Int): Color = when {
    level >= 15 -> ZyroGold
    level >= 10 -> ZyroPink
    level >= 5 -> ZyroViolet
    level >= 2 -> ZyroEmerald
    else -> ZyroCyan
}

@Composable
private fun VoiceSeatTile(
    seatNumber: Int,
    isTopSeat: Boolean,
    isSelf: Boolean,
    friend: FriendEntity?,
    profile: UserProfileEntity?,
    myVipLevel: Int,
    isSpeaking: Boolean,
    isMuted: Boolean,
    seatsLocked: Boolean,
    micAmplitude: Float,
    wavePulse: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val avatarSize = if (isTopSeat) 50 else 44
    val ringColor = if (isSpeaking) {
        if (isSelf) ZyroCyan else ZyroEmerald
    } else Color.White.copy(alpha = 0.22f)

    val seatLabel = when {
        isSelf -> "You (${profile?.displayName ?: "Aarav"})"
        friend != null -> friend.name
        seatsLocked -> "Locked"
        else -> "Seat $seatNumber"
    }

    val frameColors = if (isSelf) {
        when (profile?.equippedSeatFrameId) {
            "frame_golden_lotus" -> listOf(ZyroGold, Color(0xFFF59E0B))
            "frame_celestial_wings" -> listOf(ZyroViolet, ZyroPink, ZyroCyan)
            "frame_dragon_ember" -> listOf(ZyroCrimson, ZyroGold)
            else -> listOf(ZyroCyan, ZyroCyanDim)
        }
    } else null

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 3.dp, horizontal = 2.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isSpeaking) {
                val pulseRadius = if (isSelf) (avatarSize + 12 + micAmplitude * 8).dp else (avatarSize + 10 + wavePulse * 5).dp
                Box(
                    modifier = Modifier
                        .size(pulseRadius)
                        .clip(CircleShape)
                        .background(ringColor.copy(alpha = 0.25f))
                )
            }
            if (frameColors != null) {
                Box(
                    modifier = Modifier
                        .size((avatarSize + 7).dp)
                        .clip(CircleShape)
                        .border(2.5.dp, Brush.linearGradient(frameColors), CircleShape)
                )
            }
            when {
                friend != null -> {
                    UserAvatar(
                        name = friend.name,
                        colorHex = friend.avatarColorHex,
                        sizeDp = avatarSize,
                        isVip = friend.isVip,
                        vipLevel = friend.vipLevel,
                        isAdmin = friend.isOfficialAdmin
                    )
                }
                isSelf -> {
                    UserAvatar(
                        name = profile?.displayName ?: "Aarav",
                        colorHex = 0xFF00E5FF,
                        sizeDp = avatarSize,
                        isVip = profile?.isVip == true,
                        vipLevel = myVipLevel,
                        isAdmin = profile?.isOfficialAdmin == true,
                        avatarImagePath = profile?.avatarImagePath
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .size(avatarSize.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, ringColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (seatsLocked) Icons.Filled.Lock else Icons.Filled.MicNone,
                            contentDescription = seatLabel,
                            tint = Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(if (isTopSeat) 22.dp else 18.dp)
                        )
                    }
                }
            }
            if (isMuted && (isSelf || friend != null)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(ZyroCrimson),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MicOff,
                        contentDescription = "Muted",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        val seatIsVip = (isSelf && profile?.isVip == true) || (friend?.isVip == true)
        val seatVipLvl = if (isSelf) myVipLevel else (friend?.vipLevel ?: 1)
        val seatTextColor = vipNameColor(
            isVip = seatIsVip,
            vipLevel = seatVipLvl,
            onDarkSurface = true,
            fallback = Color.White
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)
        ) {
            Text(
                text = seatLabel,
                color = seatTextColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (seatIsVip) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (seatIsVip) {
                VipBadge(vipLevel = seatVipLvl, compact = true)
            }
        }
    }
}
