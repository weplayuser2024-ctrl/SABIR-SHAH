package com.example.ui.screens.discover

import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.data.FriendEntity
import com.example.data.MomentEntity
import com.example.data.UserProfileEntity
import com.example.data.ZyroProgression
import com.example.ui.components.OfficialAdminTag
import com.example.ui.components.UserAvatar
import com.example.ui.components.VipNameAndBadge
import com.example.ui.theme.*
import java.io.File

@Composable
fun DiscoverScreen(
    moments: List<MomentEntity>,
    friends: List<FriendEntity>,
    profile: UserProfileEntity? = null,
    onToggleLike: (MomentEntity) -> Unit,
    onPostMoment: (caption: String, mediaType: String, mediaUri: Uri?) -> Unit,
    onInspectFriend: (FriendEntity) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var activeVideoMoment by remember { mutableStateOf<MomentEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Dedicated Compact Header for Friends' Photos & Videos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Friends Moments", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Photos & videos shared by your friends",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.testTag("post_moment_button")
            ) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(moments, key = { it.id }) { moment ->
                val authorFriend = friends.firstOrNull { it.uid == moment.authorUid }
                val isAuthorMe = moment.authorUid == "me"
                val effectiveIsVip = when {
                    isAuthorMe && profile != null -> profile.isVip
                    authorFriend != null -> authorFriend.isVip
                    else -> moment.authorIsVip
                }
                val effectiveVipLevel = when {
                    isAuthorMe && profile != null -> ZyroProgression.computeVipLevel(profile.vipGrowthPoints).vipLevel
                    authorFriend != null -> authorFriend.vipLevel
                    else -> moment.authorVipLevel
                }
                MomentCard(
                    moment = moment,
                    authorIsVip = effectiveIsVip,
                    authorVipLevel = effectiveVipLevel,
                    onLike = { onToggleLike(moment) },
                    onOpenAuthor = { if (authorFriend != null) onInspectFriend(authorFriend) },
                    onPlayVideo = { activeVideoMoment = moment }
                )
            }
        }
    }

    if (showCreateDialog) {
        var caption by remember { mutableStateOf("") }
        var pickedUri by remember { mutableStateOf<Uri?>(null) }
        var mediaType by remember { mutableStateOf("PHOTO") }

        val photoPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                pickedUri = uri
            }
        }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Post Photo / Video Moment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text("Write a caption...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = mediaType == "PHOTO",
                            onClick = { mediaType = "PHOTO" },
                            label = { Text("Photo") }
                        )
                        FilterChip(
                            selected = mediaType == "VIDEO",
                            onClick = { mediaType = "VIDEO" },
                            label = { Text("Video") }
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val req = if (mediaType == "VIDEO") {
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            } else {
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            }
                            photoPicker.launch(req)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (pickedUri != null) "Media Selected ✓" else "Pick from Gallery")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onPostMoment(caption, mediaType, pickedUri)
                    showCreateDialog = false
                }) {
                    Text("Post")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (activeVideoMoment != null) {
        val vm = activeVideoMoment!!
        Dialog(onDismissRequest = { activeVideoMoment = null }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF0B0E1D)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${vm.authorName}'s Video", color = Color.White, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { activeVideoMoment = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    if (vm.mediaPath != null && File(vm.mediaPath).exists()) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    setVideoPath(vm.mediaPath)
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                        start()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(listOf(UnoFeltGreen, ZyroMidnightCard))),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = ZyroGold, modifier = Modifier.size(56.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Playing Friend Clip", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(vm.caption, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun MomentCard(
    moment: MomentEntity,
    authorIsVip: Boolean,
    authorVipLevel: Int,
    onLike: () -> Unit,
    onOpenAuthor: () -> Unit,
    onPlayVideo: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAuthor() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UserAvatar(
                    name = moment.authorName,
                    colorHex = if (moment.authorIsAdmin) 0xFFFF4D8D else 0xFF00E5FF,
                    sizeDp = 42,
                    isVip = authorIsVip,
                    vipLevel = authorVipLevel,
                    isAdmin = moment.authorIsAdmin
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VipNameAndBadge(
                            name = moment.authorName,
                            isVip = authorIsVip,
                            vipLevel = authorVipLevel,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (moment.authorIsAdmin) OfficialAdminTag()
                    }
                    Text(
                        text = "${moment.authorZyroId} • ${moment.createdAtLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(moment.caption, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(10.dp))

            // Media Preview Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        if (moment.mediaType == "VIDEO") onPlayVideo()
                    }
            ) {
                if (moment.mediaPath != null && File(moment.mediaPath).exists() && moment.mediaType == "PHOTO") {
                    AsyncImage(
                        model = File(moment.mediaPath),
                        contentDescription = moment.caption,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val drawableId = when (moment.presetVisualKey) {
                        "admin_hall" -> R.drawable.img_admin_room_bg
                        "wedding_glow" -> R.drawable.img_wedding_chapel
                        "carrom_win", "uno_clip" -> R.drawable.img_lounge_hero
                        else -> R.drawable.img_zyro_room_bg
                    }
                    Image(
                        painter = painterResource(id = drawableId),
                        contentDescription = moment.caption,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (moment.mediaType == "VIDEO") {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play Friend Video", tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onLike) {
                    Icon(
                        imageVector = if (moment.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like Moment",
                        tint = if (moment.isLiked) ZyroPink else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${moment.likesCount}")
                }
            }
        }
    }
}
