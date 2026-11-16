package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "me",
    val isLoggedIn: Boolean = false,
    val authProvider: String = "", // "GMAIL", "PHONE", "FACEBOOK"
    val authIdentifier: String = "",
    val zyroId: String = "ZYRO-10482",
    val displayName: String = "Aarav",
    val avatarImagePath: String? = null,
    val charmPoints: Long = 5800L,
    val exp: Long = 820L,
    val coins: Long = 4500L,
    val cpPoints: Long = 350L,
    val isVip: Boolean = true,
    val vipLevel: Int = 1,
    val vipExpiryText: String = "Active • ₹200 / 1 Month",
    val vipGrowthPoints: Int = 4,
    val totalCoinsSpentForVip: Long = 420L,
    val isOfficialAdmin: Boolean = true,
    val equippedBubbleId: String = "bubble_cyber",
    val equippedThemeId: String = "theme_zyro",
    val equippedEntryEffectId: String = "entry_comet",
    val equippedSeatFrameId: String = "frame_neon_halo",
    val equippedRingId: String? = null,
    val cpPartnerName: String? = null,
    val isMarried: Boolean = false,
    val weddingDateText: String? = null,
    val tagsCsv: String = "Zyro Star ⭐",
    val customQrPath: String? = null,
    val currentVoiceRoomId: String? = null,
    val createdRoomId: String? = null
)

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val uid: String,
    val zyroId: String,
    val name: String,
    val avatarColorHex: Long,
    val charmPoints: Long,
    val exp: Long,
    val isVip: Boolean,
    val vipLevel: Int = 3,
    val isOfficialAdmin: Boolean,
    val currentVoiceRoomId: String?,
    val currentVoiceRoomName: String?,
    val charmGiftedToMe: Long,
    val cpStatus: String?,
    val bio: String,
    val tagsCsv: String,
    val lastMessage: String,
    val lastMessageTime: String,
    val unreadCount: Int
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val friendUid: String,
    val isFromMe: Boolean,
    val text: String,
    val timeLabel: String,
    val bubbleStyleId: String,
    val isGiftNotice: Boolean = false
)

@Entity(tableName = "moments")
data class MomentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val authorUid: String,
    val authorName: String,
    val authorZyroId: String,
    val authorIsVip: Boolean,
    val authorVipLevel: Int = 1,
    val authorIsAdmin: Boolean,
    val caption: String,
    val mediaType: String, // "PHOTO" or "VIDEO"
    val mediaPath: String?,
    val presetVisualKey: String,
    val likesCount: Int,
    val isLiked: Boolean,
    val createdAtLabel: String
)

@Entity(tableName = "gift_records")
data class GiftRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val giftId: String,
    val giftName: String,
    val giftEmoji: String,
    val senderName: String,
    val receiverName: String,
    val charmEarned: Long,
    val bonusCoins: Long,
    val expEarned: Long,
    val timestampLabel: String
)

@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey val id: String,
    val roomUid: String,
    val title: String,
    val type: String, // "VOICE", "ADMIN_VOICE", "YOUTUBE", "ACTION"
    val level: Int,
    val exp: Long,
    val adminSlotsMax: Int,
    val isOfficialAdminRoom: Boolean,
    val isJoined: Boolean,
    val youtubeVideoId: String,
    val youtubeTitle: String,
    val speakerCount: Int,
    val topic: String,
    val hostName: String
)

@Entity(tableName = "shop_inventory")
data class ShopInventoryEntity(
    @PrimaryKey val itemId: String,
    val category: String, // "RING", "BUBBLE", "FRAME", "THEME", "ENTRY"
    val name: String,
    val description: String,
    val priceCoins: Long,
    val isOwned: Boolean,
    val isEquipped: Boolean,
    val accentHex: Long
)

@Entity(tableName = "privacy_settings")
data class PrivacySettingsEntity(
    @PrimaryKey val id: Int = 0,
    val hideOnlineStatus: Boolean = false,
    val hideVoiceRoomPresence: Boolean = false,
    val allowStrangerMessages: Boolean = true,
    val showGiftWallPublicly: Boolean = true,
    val showVipEntryEffect: Boolean = true,
    val darkModeEnabled: Boolean = true
)

@Entity(tableName = "daily_tasks")
data class DailyTaskEntity(
    @PrimaryKey val taskId: String,
    val title: String,
    val description: String,
    val rewardCoins: Long,
    val rewardExp: Long,
    val isCompleted: Boolean = false
)
