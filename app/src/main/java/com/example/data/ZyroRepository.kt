package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.util.Locale
import kotlin.random.Random

data class GiftCatalogItem(
    val id: String,
    val name: String,
    val emoji: String,
    val coinCost: Long,
    val charmValue: Long,
    val expValue: Long,
    val tagReward: String? = null
)

data class GiftSendOutcome(
    val gift: GiftCatalogItem,
    val recipientName: String,
    val randomBonusCoins: Long,
    val expEarned: Long,
    val charmEarned: Long,
    val newRoomLevel: Int?,
    val newAdminSlots: Int?,
    val earnedTag: String? = null
)

data class LevelProgress(
    val level: Int,
    val badgeLabel: String,
    val nextBadgeLabel: String,
    val currentLevelFloor: Long,
    val nextLevelTarget: Long,
    val charmToNext: Long,
    val progressFraction: Float
)

data class VipLevelProgress(
    val vipLevel: Int, // 1..14
    val growthPoints: Int,
    val nextLevelPoints: Int,
    val pointsToNext: Int,
    val progressFraction: Float
)

data class ZyroEventItem(
    val id: String,
    val cadence: String, // "WEEKLY" or "MONTHLY"
    val title: String,
    val subtitle: String,
    val rewardText: String,
    val timeLeft: String,
    val accentHex: Long
)

object ZyroProgression {
    // Charm Level Tier Thresholds:
    // 5,000 = 1⭐, 13,000 = 2⭐, 40,000 = 3⭐, 100,000 = 4⭐,
    // 150,000 = 💎5, 300,000 = 💎6, 500,000 = 👑7, 1.2M = 👑8, 3M = 👑9, 5M = 👑10
    private val charmTierThresholds = listOf(
        0L,
        5_000L,
        13_000L,
        40_000L,
        100_000L,
        150_000L,
        300_000L,
        500_000L,
        1_200_000L,
        3_000_000L,
        5_000_000L
    )

    private val charmTierBadges = listOf(
        "0⭐", "1⭐", "2⭐", "3⭐", "4⭐", "💎5", "💎6", "👑7", "👑8", "👑9", "👑10"
    )

    // Room Levels 1..19 EXP thresholds
    private val roomExpThresholds = listOf(
        0L, 150L, 400L, 750L, 1_200L, 1_800L, 2_500L, 3_300L, 4_200L, 5_200L,
        6_400L, 7_800L, 9_400L, 11_200L, 13_200L, 15_500L, 18_000L, 21_000L, 25_000L
    )

    // VIP Levels 1..14 thresholds in Growth Points (100 VIP Coins spent = 1 Growth Point)
    val vipLevelThresholds = listOf(
        0, 10, 25, 45, 70, 100, 140, 190, 250, 320, 410, 520, 650, 800
    )

    fun formatCompactCoins(amount: Long): String {
        return when {
            amount >= 1_000_000L -> {
                val m = amount / 1_000_000.0
                if (amount % 1_000_000L == 0L) "${amount / 1_000_000L}M"
                else String.format(Locale.US, "%.1fM", m)
            }
            amount >= 1_000L -> NumberFormat.getNumberInstance(Locale.US).format(amount)
            else -> amount.toString()
        }
    }

    fun formatNumber(amount: Long): String = NumberFormat.getNumberInstance(Locale.US).format(amount)

    fun computeUserLevel(charmPoints: Long): LevelProgress {
        var tierIdx = 0
        for (i in 1 until charmTierThresholds.size) {
            if (charmPoints >= charmTierThresholds[i]) {
                tierIdx = i
            } else {
                break
            }
        }
        val floor = charmTierThresholds[tierIdx]
        val isMax = tierIdx >= charmTierThresholds.lastIndex
        val nextTarget = if (isMax) charmTierThresholds.last() else charmTierThresholds[tierIdx + 1]
        val span = (nextTarget - floor).coerceAtLeast(1L)
        val frac = if (isMax) 1f else ((charmPoints - floor).toFloat() / span.toFloat()).coerceIn(0f, 1f)
        val nextBadge = if (isMax) charmTierBadges.last() else charmTierBadges[tierIdx + 1]
        return LevelProgress(
            level = tierIdx,
            badgeLabel = charmTierBadges[tierIdx],
            nextBadgeLabel = nextBadge,
            currentLevelFloor = floor,
            nextLevelTarget = nextTarget,
            charmToNext = if (isMax) 0L else (nextTarget - charmPoints).coerceAtLeast(0L),
            progressFraction = frac
        )
    }

    fun computeVipLevel(growthPoints: Int): VipLevelProgress {
        var lvl = 1
        for (i in 1 until vipLevelThresholds.size) {
            if (growthPoints >= vipLevelThresholds[i]) {
                lvl = i + 1
            } else {
                break
            }
        }
        val clampedLvl = lvl.coerceIn(1, 14)
        val floor = vipLevelThresholds[clampedLvl - 1]
        val next = if (clampedLvl < 14) vipLevelThresholds[clampedLvl] else vipLevelThresholds.last()
        val span = (next - floor).coerceAtLeast(1)
        val frac = if (clampedLvl >= 14) 1f else ((growthPoints - floor).toFloat() / span.toFloat()).coerceIn(0f, 1f)
        return VipLevelProgress(
            vipLevel = clampedLvl,
            growthPoints = growthPoints,
            nextLevelPoints = next,
            pointsToNext = if (clampedLvl >= 14) 0 else (next - growthPoints).coerceAtLeast(0),
            progressFraction = frac
        )
    }

    // Room Levels 1..19; Level 1 = 3 admin slots, rising by +1 per room level (Lv.19 = 21 slots)
    fun computeRoomLevelAndSlots(roomExp: Long): Pair<Int, Int> {
        var level = 1
        for (i in 1 until roomExpThresholds.size) {
            if (roomExp >= roomExpThresholds[i]) {
                level = i + 1
            } else {
                break
            }
        }
        val clampedLevel = level.coerceIn(1, 19)
        val adminSlots = 2 + clampedLevel // Lv.1 = 3, Lv.2 = 4, ..., Lv.19 = 21
        return clampedLevel to adminSlots
    }

    data class RoomSeatSpec(
        val totalSeats: Int,
        val topSeats: Int,
        val bottomSeats: Int
    )

    fun computeRoomSeats(isOfficialRoom: Boolean): RoomSeatSpec {
        return if (isOfficialRoom) {
            RoomSeatSpec(totalSeats = 15, topSeats = 3, bottomSeats = 12)
        } else {
            RoomSeatSpec(totalSeats = 10, topSeats = 2, bottomSeats = 8)
        }
    }

    // 5 Gifts displayed together in a single row
    val giftCatalog = listOf(
        GiftCatalogItem("gift_rose", "Rose", "🌹", 50L, 650L, 80L),
        GiftCatalogItem("gift_mic", "Mic", "🎙️", 120L, 1_500L, 180L),
        GiftCatalogItem("gift_ring", "Ring", "💍", 300L, 4_200L, 420L),
        GiftCatalogItem("gift_rocket", "Rocket", "🚀", 500L, 8_000L, 750L),
        GiftCatalogItem("gift_crown", "Crown", "👑", 800L, 14_000L, 1_200L)
    )

    // 5 Action Room Tag Gifts displayed in a single row to earn relationship tags
    val actionTagGifts = listOf(
        GiftCatalogItem("tag_brother", "Brother", "🤝", 150L, 2_000L, 220L, tagReward = "Brother 🤝"),
        GiftCatalogItem("tag_sister", "Sister", "🍀", 150L, 2_000L, 220L, tagReward = "Sister 🍀"),
        GiftCatalogItem("tag_bestie", "Bestie", "🎀", 200L, 3_000L, 300L, tagReward = "Bestie 🎀"),
        GiftCatalogItem("tag_soulmate", "Soulmate", "💞", 350L, 5_500L, 500L, tagReward = "Soulmate 💞"),
        GiftCatalogItem("tag_legend", "Legend", "🌟", 500L, 8_500L, 800L, tagReward = "Zyro Legend 🌟")
    )

    val roomEmotes = listOf("😂", "🔥", "❤️", "👏", "🥳", "😎", "🙌", "💃", "✨", "🎉")

    val activeEvents = listOf(
        ZyroEventItem(
            id = "ev_weekly_carrom",
            cadence = "WEEKLY EVENT",
            title = "Carrom Striker Masters",
            subtitle = "Pocket the Queen & climb Weekly Charm Points",
            rewardText = "+6,000 Charm • 15,000 Coins",
            timeLeft = "3d left",
            accentHex = 0xFFFFD166
        ),
        ZyroEventItem(
            id = "ev_monthly_uno",
            cadence = "MONTHLY EVENT",
            title = "Zyro Uno Grand Cup",
            subtitle = "Monthly championship across Voice & Arena tables",
            rewardText = "Imperial Ring • 50,000 Coins",
            timeLeft = "18d left",
            accentHex = 0xFF00E5FF
        ),
        ZyroEventItem(
            id = "ev_weekly_wedding",
            cadence = "WEEKLY EVENT",
            title = "Royal Wedding & CP Gala",
            subtitle = "Host an in-game ceremony for double CP aura",
            rewardText = "+10,000 Charm • VIP Growth Boost",
            timeLeft = "5d left",
            accentHex = 0xFFFF4D8D
        )
    )
}

class ZyroRepository(
    private val dao: ZyroDao,
    private val appContext: Context
) {
    val profileFlow: Flow<UserProfileEntity?> = dao.observeMyProfile()
    val friendsFlow: Flow<List<FriendEntity>> = dao.observeFriends()
    val rankingsFlow: Flow<List<FriendEntity>> = dao.observeRankingsByCharm()
    val momentsFlow: Flow<List<MomentEntity>> = dao.observeMoments()
    val myMomentsFlow: Flow<List<MomentEntity>> = dao.observeMyMoments()
    val giftWallFlow: Flow<List<GiftRecordEntity>> = dao.observeGiftWall()
    val roomsFlow: Flow<List<RoomEntity>> = dao.observeRooms()
    val shopFlow: Flow<List<ShopInventoryEntity>> = dao.observeShopInventory()
    val privacyFlow: Flow<PrivacySettingsEntity?> = dao.observePrivacySettings()
    val dailyTasksFlow: Flow<List<DailyTaskEntity>> = dao.observeDailyTasks()

    fun messagesFlow(friendUid: String): Flow<List<ChatMessageEntity>> =
        dao.observeMessagesForFriend(friendUid)

    suspend fun ensureSeeded() {
        if (dao.getMyProfileOnce() != null) {
            dao.deleteRoomById("room_youtube_cinema")
            dao.deleteRoomById("room_action_tags")
            if (dao.getRoomById("room_youtube_action") == null) {
                dao.upsertRoom(
                    RoomEntity(
                        id = "room_youtube_action",
                        roomUid = "RM-1004",
                        title = "Zyro Stage • YouTube & Action",
                        type = "UNIFIED",
                        level = 2,
                        exp = 260L,
                        adminSlotsMax = 4,
                        isOfficialAdminRoom = false,
                        isJoined = true,
                        youtubeVideoId = "jfKfPfyJRdk",
                        youtubeTitle = "Lofi Hip Hop Radio - Beats to Relax/Study to",
                        speakerCount = 8,
                        topic = "All-in-One Voice Room • Watch YouTube + Action Tag Gifts!",
                        hostName = "Meera Nair"
                    )
                )
            }
            return
        }

        dao.upsertMyProfile(UserProfileEntity())
        dao.upsertPrivacySettings(PrivacySettingsEntity())

        dao.upsertDailyTasks(
            listOf(
                DailyTaskEntity("task_login", "Daily Lounge Check-In", "Sign into Zyro and visit the Arena", 1200L, 40L),
                DailyTaskEntity("task_game", "Tabletop Challenger", "Play a match of Carrom Pro or Zyro Uno", 2500L, 80L),
                DailyTaskEntity("task_voice", "Voice Room Host", "Join or create a Voice Room", 2000L, 70L),
                DailyTaskEntity("task_gift", "Charm Giver", "Send any gift to earn EXP & random coins", 3000L, 100L)
            )
        )

        val friends = listOf(
            FriendEntity(
                uid = "f_zoya",
                zyroId = "ZYRO-20411",
                name = "Zoya Khan",
                avatarColorHex = 0xFFFF4D8D,
                charmPoints = 520_000L, // 👑7
                exp = 42_000L,
                isVip = true,
                vipLevel = 9,
                isOfficialAdmin = true,
                currentVoiceRoomId = "room_admin_official",
                currentVoiceRoomName = "Zyro Official Admin Lounge",
                charmGiftedToMe = 620L,
                cpStatus = "Single",
                bio = "Official Zyro Admin • Carrom Grandmaster & Night Voice Host",
                tagsCsv = "Bestie 🎀,Sister 🍀",
                lastMessage = "Join the Official Admin Hall! We're spinning Carrom.",
                lastMessageTime = "2m",
                unreadCount = 2
            ),
            FriendEntity(
                uid = "f_rohan",
                zyroId = "ZYRO-30892",
                name = "Rohan Verma",
                avatarColorHex = 0xFF00E5FF,
                charmPoints = 165_000L, // 💎5
                exp = 18_500L,
                isVip = true,
                vipLevel = 6,
                isOfficialAdmin = false,
                currentVoiceRoomId = "room_zyro_lounge",
                currentVoiceRoomName = "Zyro Midnight Beats & Chill",
                charmGiftedToMe = 440L,
                cpStatus = "Single",
                bio = "Uno strategist & Lo-Fi YouTube watch party regular.",
                tagsCsv = "Bro 🤝",
                lastMessage = "Ready for a quick Uno match?",
                lastMessageTime = "14m",
                unreadCount = 1
            ),
            FriendEntity(
                uid = "f_meera",
                zyroId = "ZYRO-44105",
                name = "Meera Nair",
                avatarColorHex = 0xFF8B5CF6,
                charmPoints = 48_000L, // 3⭐
                exp = 9_500L,
                isVip = true,
                vipLevel = 4,
                isOfficialAdmin = false,
                currentVoiceRoomId = "room_youtube_action",
                currentVoiceRoomName = "Zyro Stage • YouTube & Action",
                charmGiftedToMe = 280L,
                cpStatus = "Looking for CP",
                bio = "Collecting celestial rings & hosting music nights.",
                tagsCsv = "Sister 🍀,Bestie 🎀",
                lastMessage = "Sent you a Neon Rose on your Gift Wall!",
                lastMessageTime = "1h",
                unreadCount = 0
            ),
            FriendEntity(
                uid = "f_kabir",
                zyroId = "ZYRO-51290",
                name = "Kabir Singh",
                avatarColorHex = 0xFF10B981,
                charmPoints = 14_200L, // 2⭐
                exp = 3_200L,
                isVip = false,
                vipLevel = 1,
                isOfficialAdmin = false,
                currentVoiceRoomId = null,
                currentVoiceRoomName = null,
                charmGiftedToMe = 140L,
                cpStatus = "Single",
                bio = "Always down for Carrom Queen shots.",
                tagsCsv = "Bro 🤝",
                lastMessage = "GG on that striker shot!",
                lastMessageTime = "3h",
                unreadCount = 0
            )
        )
        dao.upsertFriends(friends)

        dao.insertMessages(
            listOf(
                ChatMessageEntity(
                    friendUid = "f_zoya",
                    isFromMe = false,
                    text = "Hey! Admin Hall just hit Level 3 — 5 admin slots unlocked!",
                    timeLabel = "10:12 AM",
                    bubbleStyleId = "bubble_royal"
                ),
                ChatMessageEntity(
                    friendUid = "f_zoya",
                    isFromMe = false,
                    text = "Join the Official Admin Hall! We're spinning Carrom.",
                    timeLabel = "10:15 AM",
                    bubbleStyleId = "bubble_royal"
                ),
                ChatMessageEntity(
                    friendUid = "f_rohan",
                    isFromMe = false,
                    text = "Ready for a quick Uno match?",
                    timeLabel = "09:40 AM",
                    bubbleStyleId = "bubble_cyber"
                ),
                ChatMessageEntity(
                    friendUid = "f_meera",
                    isFromMe = false,
                    text = "Sent you a Neon Rose on your Gift Wall!",
                    timeLabel = "Yesterday",
                    bubbleStyleId = "bubble_sakura"
                )
            )
        )

        dao.insertMoments(
            listOf(
                MomentEntity(
                    authorUid = "f_zoya",
                    authorName = "Zoya Khan",
                    authorZyroId = "ZYRO-20411",
                    authorIsVip = true,
                    authorVipLevel = 9,
                    authorIsAdmin = true,
                    caption = "Royal night in the Official Zyro Admin Voice Room — come grab a speaker seat!",
                    mediaType = "PHOTO",
                    mediaPath = null,
                    presetVisualKey = "admin_hall",
                    likesCount = 128,
                    isLiked = false,
                    createdAtLabel = "12m ago"
                ),
                MomentEntity(
                    authorUid = "f_rohan",
                    authorName = "Rohan Verma",
                    authorZyroId = "ZYRO-30892",
                    authorIsVip = true,
                    authorVipLevel = 6,
                    authorIsAdmin = false,
                    caption = "Clip from our Uno +4 comeback & late-night beats session!",
                    mediaType = "VIDEO",
                    mediaPath = null,
                    presetVisualKey = "uno_clip",
                    likesCount = 84,
                    isLiked = true,
                    createdAtLabel = "45m ago"
                ),
                MomentEntity(
                    authorUid = "f_meera",
                    authorName = "Meera Nair",
                    authorZyroId = "ZYRO-44105",
                    authorIsVip = true,
                    authorVipLevel = 4,
                    authorIsAdmin = false,
                    caption = "Twinkle lights at the Zyro Wedding Pavilion look unreal tonight.",
                    mediaType = "PHOTO",
                    mediaPath = null,
                    presetVisualKey = "wedding_glow",
                    likesCount = 196,
                    isLiked = false,
                    createdAtLabel = "2h ago"
                ),
                MomentEntity(
                    authorUid = "me",
                    authorName = "Aarav",
                    authorZyroId = "ZYRO-10482",
                    authorIsVip = true,
                    authorVipLevel = 1,
                    authorIsAdmin = true,
                    caption = "Cleared the Carrom board with a clean Queen + cover shot on Zyro!",
                    mediaType = "PHOTO",
                    mediaPath = null,
                    presetVisualKey = "carrom_win",
                    likesCount = 47,
                    isLiked = true,
                    createdAtLabel = "4h ago"
                )
            )
        )

        dao.insertGiftRecords(
            listOf(
                GiftRecordEntity(
                    giftId = "gift_crown",
                    giftName = "Imperial Crown",
                    giftEmoji = "👑",
                    senderName = "Zoya Khan",
                    receiverName = "Aarav",
                    charmEarned = 2_400L,
                    bonusCoins = 54L,
                    expEarned = 280L,
                    timestampLabel = "Today"
                ),
                GiftRecordEntity(
                    giftId = "gift_rocket",
                    giftName = "Zyro Rocket",
                    giftEmoji = "🚀",
                    senderName = "Rohan Verma",
                    receiverName = "Aarav",
                    charmEarned = 1_800L,
                    bonusCoins = 38L,
                    expEarned = 180L,
                    timestampLabel = "Today"
                ),
                GiftRecordEntity(
                    giftId = "gift_rose",
                    giftName = "Neon Rose",
                    giftEmoji = "🌹",
                    senderName = "Meera Nair",
                    receiverName = "Aarav",
                    charmEarned = 650L,
                    bonusCoins = 19L,
                    expEarned = 80L,
                    timestampLabel = "Yesterday"
                ),
                GiftRecordEntity(
                    giftId = "gift_mic",
                    giftName = "Golden Mic",
                    giftEmoji = "🎙️",
                    senderName = "Kabir Singh",
                    receiverName = "Aarav",
                    charmEarned = 950L,
                    bonusCoins = 22L,
                    expEarned = 120L,
                    timestampLabel = "2d ago"
                )
            )
        )

        dao.upsertRooms(
            listOf(
                RoomEntity(
                    id = "room_admin_official",
                    roomUid = "RM-1001",
                    title = "Zyro Official Admin Lounge",
                    type = "ADMIN_VOICE",
                    level = 3,
                    exp = 500L,
                    adminSlotsMax = 5,
                    isOfficialAdminRoom = true,
                    isJoined = true,
                    youtubeVideoId = "",
                    youtubeTitle = "",
                    speakerCount = 6,
                    topic = "Official Admin-Controlled Voice Room • Lv 1-19 Progression",
                    hostName = "Zoya Khan"
                ),
                RoomEntity(
                    id = "room_zyro_lounge",
                    roomUid = "RM-1002",
                    title = "Zyro Midnight Beats & Chill",
                    type = "VOICE",
                    level = 1,
                    exp = 80L,
                    adminSlotsMax = 3,
                    isOfficialAdminRoom = false,
                    isJoined = true,
                    youtubeVideoId = "",
                    youtubeTitle = "",
                    speakerCount = 4,
                    topic = "Level 1 Room (3 Admin Slots) • Gift to Level Up 1–19!",
                    hostName = "Rohan Verma"
                ),
                RoomEntity(
                    id = "room_aurora_club",
                    roomUid = "RM-1003",
                    title = "Zyro Aurora Gaming Club",
                    type = "VOICE",
                    level = 2,
                    exp = 220L,
                    adminSlotsMax = 4,
                    isOfficialAdminRoom = false,
                    isJoined = false,
                    youtubeVideoId = "",
                    youtubeTitle = "",
                    speakerCount = 5,
                    topic = "Carrom & Uno Tournament Voice Strategy",
                    hostName = "Kabir Singh"
                ),
                RoomEntity(
                    id = "room_youtube_action",
                    roomUid = "RM-1004",
                    title = "Zyro Stage • YouTube & Action",
                    type = "UNIFIED",
                    level = 2,
                    exp = 260L,
                    adminSlotsMax = 4,
                    isOfficialAdminRoom = false,
                    isJoined = true,
                    youtubeVideoId = "jfKfPfyJRdk",
                    youtubeTitle = "Lofi Hip Hop Radio - Beats to Relax/Study to",
                    speakerCount = 8,
                    topic = "All-in-One Voice Room • Watch YouTube + Action Tag Gifts!",
                    hostName = "Meera Nair"
                )
            )
        )

        dao.upsertShopItems(
            listOf(
                ShopInventoryEntity(
                    "ring_promise", "RING", "Velvet Promise Ring",
                    "Delicate rose-gold band to begin your CP journey.", 600L,
                    isOwned = false, isEquipped = false, accentHex = 0xFFFF4D8D
                ),
                ShopInventoryEntity(
                    "ring_diamond", "RING", "Zyro Diamond Solitaire",
                    "Radiant cyan-cut ring for official in-game weddings.", 1800L,
                    isOwned = false, isEquipped = false, accentHex = 0xFF00E5FF
                ),
                ShopInventoryEntity(
                    "ring_imperial", "RING", "Imperial Crown Ring",
                    "24K golden crest ring with celestial wedding aura.", 3200L,
                    isOwned = false, isEquipped = false, accentHex = 0xFFFFD166
                ),
                ShopInventoryEntity(
                    "bubble_cyber", "BUBBLE", "Cyber Cyan Neon",
                    "Sleek electric cyan glowing chat frame.", 0L,
                    isOwned = true, isEquipped = true, accentHex = 0xFF00E5FF
                ),
                ShopInventoryEntity(
                    "bubble_sakura", "BUBBLE", "Sakura Petal",
                    "Soft rose gradient chat bubble with petal shimmer.", 450L,
                    isOwned = false, isEquipped = false, accentHex = 0xFFFF4D8D
                ),
                ShopInventoryEntity(
                    "bubble_royal", "BUBBLE", "Royal Gold Scroll",
                    "Brushed gold border reserved for Zyro royalty.", 900L,
                    isOwned = false, isEquipped = false, accentHex = 0xFFFFD166
                ),
                ShopInventoryEntity(
                    "theme_zyro", "THEME", "Zyro Signature Neon",
                    "Official Zyro midnight indigo & cyan laser hall.", 0L,
                    isOwned = true, isEquipped = true, accentHex = 0xFF00E5FF
                ),
                ShopInventoryEntity(
                    "theme_aurora", "THEME", "Aurora Borealis Loft",
                    "Emerald and violet northern lights ceiling.", 750L,
                    isOwned = false, isEquipped = false, accentHex = 0xFF10B981
                ),
                ShopInventoryEntity(
                    "theme_imperial", "THEME", "Imperial Crimson Palace",
                    "Gold pillars and velvet crimson VIP ambiance.", 1500L,
                    isOwned = false, isEquipped = false, accentHex = 0xFFE63946
                ),
                ShopInventoryEntity(
                    "entry_comet", "ENTRY", "Cyan Comet Trail",
                    "Sleek neon sweep showing Name + Level badge.", 0L,
                    isOwned = true, isEquipped = true, accentHex = 0xFF00E5FF
                ),
                ShopInventoryEntity(
                    "entry_phoenix", "ENTRY", "Golden Phoenix Flare",
                    "Radiant gold entry ribbon on room join.", 650L,
                    isOwned = false, isEquipped = false, accentHex = 0xFFFFD166
                ),
                ShopInventoryEntity(
                    "entry_prism", "ENTRY", "Prism Starlight Wave",
                    "Iridescent violet & rose aura banner.", 1100L,
                    isOwned = false, isEquipped = false, accentHex = 0xFF8B5CF6
                ),
                ShopInventoryEntity(
                    "frame_neon_halo", "FRAME", "Neon Halo Frame",
                    "Sleek cyan orbit frame around your voice room seat.", 0L,
                    isOwned = true, isEquipped = true, accentHex = 0xFF00E5FF
                ),
                ShopInventoryEntity(
                    "frame_golden_lotus", "FRAME", "Golden Lotus Frame",
                    "Radiant 24K gold double-bezel voice seat frame.", 800L,
                    isOwned = false, isEquipped = false, accentHex = 0xFFFFD166
                ),
                ShopInventoryEntity(
                    "frame_celestial_wings", "FRAME", "Celestial Prism Frame",
                    "Iridescent violet & rose crown halo for voice seats.", 1600L,
                    isOwned = false, isEquipped = false, accentHex = 0xFFFF4D8D
                ),
                ShopInventoryEntity(
                    "frame_dragon_ember", "FRAME", "Imperial Ember Frame",
                    "Crimson & gold royal flame border for top speakers.", 2800L,
                    isOwned = false, isEquipped = false, accentHex = 0xFFE63946
                )
            )
        )
    }

    suspend fun loginWithProvider(provider: String, identifier: String, suggestedName: String?) {
        val current = dao.getMyProfileOnce() ?: UserProfileEntity()
        val updatedName = suggestedName?.trim()?.takeIf { it.isNotEmpty() } ?: current.displayName
        dao.upsertMyProfile(
            current.copy(
                isLoggedIn = true,
                authProvider = provider,
                authIdentifier = identifier.trim(),
                displayName = updatedName
            )
        )
    }

    suspend fun logout() {
        val current = dao.getMyProfileOnce() ?: return
        dao.upsertMyProfile(current.copy(isLoggedIn = false))
    }

    suspend fun updateProfileDetails(newName: String, newPfpUri: Uri?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val current = dao.getMyProfileOnce() ?: return@withContext Result.failure(IllegalStateException("Profile missing"))
            var pfpPath = current.avatarImagePath
            if (newPfpUri != null) {
                val dest = File(appContext.filesDir, "zyro_my_pfp_${System.currentTimeMillis()}.jpg")
                appContext.contentResolver.openInputStream(newPfpUri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                pfpPath = dest.absolutePath
            }
            val cleanedName = newName.trim().ifEmpty { current.displayName }
            dao.upsertMyProfile(current.copy(displayName = cleanedName, avatarImagePath = pfpPath))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createVoiceRoom(title: String, topic: String, type: String): Result<RoomEntity> {
        val me = dao.getMyProfileOnce() ?: return Result.failure(IllegalStateException("Profile missing"))
        if (me.createdRoomId != null) {
            val existing = dao.getRoomById(me.createdRoomId)
            if (existing != null) {
                return Result.failure(IllegalStateException("You already own Room ${existing.roomUid}!"))
            }
        }
        val createCost = 500L
        if (me.coins < createCost) {
            return Result.failure(IllegalStateException("Creating a Voice Room costs 500 coins!"))
        }

        val newId = "room_custom_${System.currentTimeMillis()}"
        val uniqueUid = "RM-${Random.nextInt(2000, 9999)}"
        val room = RoomEntity(
            id = newId,
            roomUid = uniqueUid,
            title = title.trim().ifEmpty { "${me.displayName}'s Voice Lounge" },
            type = type,
            level = 1,
            exp = 0L,
            adminSlotsMax = 3,
            isOfficialAdminRoom = false,
            isJoined = true,
            youtubeVideoId = if (type == "UNIFIED" || type == "YOUTUBE") "jfKfPfyJRdk" else "",
            youtubeTitle = if (type == "UNIFIED" || type == "YOUTUBE") "Zyro YouTube Watch Party" else "",
            speakerCount = 1,
            topic = topic.trim().ifEmpty { "All-in-One Voice Stage • YouTube, Tags & 3 Admin Slots" },
            hostName = me.displayName
        )
        dao.upsertRoom(room)
        val updatedMe = applyCoinSpendToVipGrowth(
            me.copy(
                coins = me.coins - createCost,
                currentVoiceRoomId = newId,
                createdRoomId = newId
            ),
            createCost
        )
        dao.upsertMyProfile(updatedMe)
        return Result.success(room)
    }

    suspend fun claimDailyTask(taskId: String): Result<DailyTaskEntity> {
        val task = dao.getDailyTaskById(taskId) ?: return Result.failure(IllegalStateException("Task not found"))
        if (task.isCompleted) return Result.failure(IllegalStateException("Already claimed today!"))
        val me = dao.getMyProfileOnce() ?: return Result.failure(IllegalStateException("Profile missing"))
        dao.upsertDailyTask(task.copy(isCompleted = true))
        dao.upsertMyProfile(
            me.copy(
                coins = me.coins + task.rewardCoins,
                exp = me.exp + task.rewardExp
            )
        )
        return Result.success(task)
    }

    suspend fun toggleOfficialAdmin(isAdmin: Boolean) {
        val current = dao.getMyProfileOnce() ?: return
        dao.upsertMyProfile(current.copy(isOfficialAdmin = isAdmin))
    }

    suspend fun addCustomTag(tag: String) {
        val current = dao.getMyProfileOnce() ?: return
        val list = current.tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        val trimmed = tag.trim()
        if (trimmed.isNotEmpty() && !list.contains(trimmed)) {
            list.add(trimmed)
            dao.upsertMyProfile(current.copy(tagsCsv = list.joinToString(",")))
        }
    }

    suspend fun removeTag(tag: String) {
        val current = dao.getMyProfileOnce() ?: return
        val list = current.tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() && it != tag }
        dao.upsertMyProfile(current.copy(tagsCsv = list.joinToString(",")))
    }

    suspend fun toggleDarkMode(enabled: Boolean) {
        val current = dao.getPrivacySettingsOnce() ?: PrivacySettingsEntity()
        dao.upsertPrivacySettings(current.copy(darkModeEnabled = enabled))
    }

    suspend fun updatePrivacy(transform: (PrivacySettingsEntity) -> PrivacySettingsEntity) {
        val current = dao.getPrivacySettingsOnce() ?: PrivacySettingsEntity()
        dao.upsertPrivacySettings(transform(current))
    }

    private fun applyCoinSpendToVipGrowth(me: UserProfileEntity, spentCoins: Long): UserProfileEntity {
        val newSpent = me.totalCoinsSpentForVip + spentCoins
        val newGrowth = (newSpent / 100L).toInt()
        val newVipLvl = ZyroProgression.computeVipLevel(newGrowth).vipLevel
        return me.copy(
            totalCoinsSpentForVip = newSpent,
            vipGrowthPoints = newGrowth,
            vipLevel = newVipLvl
        )
    }

    suspend fun spendVipCoinsForGrowth(coinsToSpend: Long): Result<VipLevelProgress> {
        val me = dao.getMyProfileOnce() ?: return Result.failure(IllegalStateException("Profile missing"))
        if (me.coins < coinsToSpend) {
            return Result.failure(IllegalStateException("Not enough VIP coins! Top up using the + button."))
        }
        val boosted = applyCoinSpendToVipGrowth(me.copy(coins = me.coins - coinsToSpend, isVip = true), coinsToSpend)
        dao.upsertMyProfile(boosted)
        return Result.success(ZyroProgression.computeVipLevel(boosted.vipGrowthPoints))
    }

    suspend fun sendGift(
        gift: GiftCatalogItem,
        recipientUid: String?,
        roomId: String?
    ): Result<GiftSendOutcome> {
        val me = dao.getMyProfileOnce() ?: return Result.failure(IllegalStateException("Profile missing"))
        if (me.coins < gift.coinCost) {
            return Result.failure(IllegalStateException("Not enough coins! Tap + on Home to top up in INR."))
        }

        val maxBonus = (gift.coinCost / 2L).coerceAtLeast(18L).toInt()
        val randomBonusCoins = Random.nextInt(5, maxBonus + 1).toLong()

        var recipientName = "Zyro Lounge"
        if (recipientUid != null && recipientUid != "me") {
            val friend = dao.getFriendByUid(recipientUid)
            if (friend != null) {
                recipientName = friend.name
                dao.upsertFriend(friend.copy(charmPoints = friend.charmPoints + gift.charmValue))
            }
        }

        val updatedCharm = if (recipientUid == null || recipientUid == "me") {
            me.charmPoints + gift.charmValue
        } else {
            me.charmPoints + (gift.charmValue / 3L)
        }

        val updatedTagsCsv = if (gift.tagReward != null) {
            val currentTags = me.tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            if (!currentTags.contains(gift.tagReward)) {
                currentTags.add(gift.tagReward)
            }
            currentTags.joinToString(",")
        } else {
            me.tagsCsv
        }

        val updatedMe = applyCoinSpendToVipGrowth(
            me.copy(
                coins = me.coins - gift.coinCost + randomBonusCoins,
                exp = me.exp + gift.expValue,
                charmPoints = updatedCharm,
                tagsCsv = updatedTagsCsv
            ),
            gift.coinCost
        )
        dao.upsertMyProfile(updatedMe)

        dao.insertGiftRecord(
            GiftRecordEntity(
                giftId = gift.id,
                giftName = gift.name,
                giftEmoji = gift.emoji,
                senderName = me.displayName,
                receiverName = recipientName,
                charmEarned = gift.charmValue,
                bonusCoins = randomBonusCoins,
                expEarned = gift.expValue,
                timestampLabel = "Just now"
            )
        )

        var newRoomLevel: Int? = null
        var newAdminSlots: Int? = null
        if (roomId != null) {
            val room = dao.getRoomById(roomId)
            if (room != null) {
                val updatedExp = room.exp + gift.expValue
                val (lvl, slots) = ZyroProgression.computeRoomLevelAndSlots(updatedExp)
                if (lvl > room.level) {
                    newRoomLevel = lvl
                    newAdminSlots = slots
                }
                dao.upsertRoom(room.copy(exp = updatedExp, level = lvl, adminSlotsMax = slots))
            }
        }

        return Result.success(
            GiftSendOutcome(
                gift = gift,
                recipientName = recipientName,
                randomBonusCoins = randomBonusCoins,
                expEarned = gift.expValue,
                charmEarned = gift.charmValue,
                newRoomLevel = newRoomLevel,
                newAdminSlots = newAdminSlots,
                earnedTag = gift.tagReward
            )
        )
    }

    suspend fun activateVipPlan(months: Int, priceInr: Int): Result<Unit> {
        val me = dao.getMyProfileOnce() ?: return Result.failure(IllegalStateException("Profile missing"))
        val growthBonus = if (months >= 3) 15 else 5
        val coinBonus = if (months >= 3) 1_500L else 500L
        val newGrowth = me.vipGrowthPoints + growthBonus
        val newLvl = ZyroProgression.computeVipLevel(newGrowth).vipLevel
        val label = if (months >= 3) "Active • ₹$priceInr / 3 Months" else "Active • ₹$priceInr / 1 Month"
        dao.upsertMyProfile(
            me.copy(
                isVip = true,
                vipLevel = newLvl,
                vipExpiryText = label,
                coins = me.coins + coinBonus,
                vipGrowthPoints = newGrowth
            )
        )
        return Result.success(Unit)
    }

    suspend fun processInrTopUp(amountInr: Int, coinsAdded: Long, cpAdded: Long): Result<Unit> {
        val me = dao.getMyProfileOnce() ?: return Result.failure(IllegalStateException("Profile missing"))
        dao.upsertMyProfile(
            me.copy(
                coins = me.coins + coinsAdded,
                cpPoints = me.cpPoints + cpAdded
            )
        )
        return Result.success(Unit)
    }

    suspend fun saveCustomQrFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val destFile = File(appContext.filesDir, "zyro_custom_qr.jpg")
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext Result.failure(IllegalStateException("Unable to read image"))
            val me = dao.getMyProfileOnce() ?: return@withContext Result.failure(IllegalStateException("Profile missing"))
            dao.upsertMyProfile(me.copy(customQrPath = destFile.absolutePath))
            Result.success(destFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun buyOrEquipShopItem(item: ShopInventoryEntity): Result<String> {
        val me = dao.getMyProfileOnce() ?: return Result.failure(IllegalStateException("Profile missing"))
        if (!item.isOwned) {
            if (me.coins < item.priceCoins) {
                return Result.failure(IllegalStateException("Need ${item.priceCoins} coins!"))
            }
            val updated = applyCoinSpendToVipGrowth(me.copy(coins = me.coins - item.priceCoins), item.priceCoins)
            dao.upsertMyProfile(updated)
        }

        val sameCategory = dao.getShopItemsByCategory(item.category)
        val updatedList = sameCategory.map {
            if (it.itemId == item.itemId) it.copy(isOwned = true, isEquipped = true)
            else it.copy(isEquipped = false)
        }
        dao.upsertShopItems(updatedList)

        val latestMe = dao.getMyProfileOnce()!!
        val updatedProfile = when (item.category) {
            "RING" -> latestMe.copy(equippedRingId = item.itemId)
            "BUBBLE" -> latestMe.copy(equippedBubbleId = item.itemId)
            "THEME" -> latestMe.copy(equippedThemeId = item.itemId)
            "ENTRY" -> latestMe.copy(equippedEntryEffectId = item.itemId)
            "FRAME" -> latestMe.copy(equippedSeatFrameId = item.itemId)
            else -> latestMe
        }
        dao.upsertMyProfile(updatedProfile)
        return Result.success(if (item.isOwned) "Equipped ${item.name}" else "Purchased & equipped ${item.name}!")
    }

    suspend fun acquireCpPoints(costCoins: Long, cpEarned: Long): Result<Unit> {
        val me = dao.getMyProfileOnce() ?: return Result.failure(IllegalStateException("Profile missing"))
        if (me.coins < costCoins) return Result.failure(IllegalStateException("Not enough coins"))
        val updated = applyCoinSpendToVipGrowth(me.copy(coins = me.coins - costCoins, cpPoints = me.cpPoints + cpEarned), costCoins)
        dao.upsertMyProfile(updated)
        return Result.success(Unit)
    }

    suspend fun hostWedding(partnerName: String, ringItem: ShopInventoryEntity): Result<Unit> {
        val me = dao.getMyProfileOnce() ?: return Result.failure(IllegalStateException("Profile missing"))
        if (!ringItem.isOwned) {
            return Result.failure(IllegalStateException("Purchase ${ringItem.name} first!"))
        }
        val weddingFee = 300L
        if (me.coins < weddingFee) {
            return Result.failure(IllegalStateException("Need 300 coins for Wedding Chapel ceremony!"))
        }
        val updated = applyCoinSpendToVipGrowth(
            me.copy(
                coins = me.coins - weddingFee,
                cpPoints = me.cpPoints + 500L,
                charmPoints = me.charmPoints + 2_000L,
                equippedRingId = ringItem.itemId,
                cpPartnerName = partnerName,
                isMarried = true,
                weddingDateText = "Married • ${ringItem.name}"
            ),
            weddingFee
        )
        dao.upsertMyProfile(updated)
        return Result.success(Unit)
    }

    suspend fun sendChatMessage(friendUid: String, text: String) {
        val me = dao.getMyProfileOnce() ?: return
        val friend = dao.getFriendByUid(friendUid) ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        dao.insertMessage(
            ChatMessageEntity(
                friendUid = friendUid,
                isFromMe = true,
                text = trimmed,
                timeLabel = "Now",
                bubbleStyleId = me.equippedBubbleId
            )
        )
        dao.upsertFriend(friend.copy(lastMessage = trimmed, lastMessageTime = "Now", unreadCount = 0))
    }

    suspend fun markChatRead(friendUid: String) {
        val friend = dao.getFriendByUid(friendUid) ?: return
        if (friend.unreadCount > 0) {
            dao.upsertFriend(friend.copy(unreadCount = 0))
        }
    }

    suspend fun postMoment(caption: String, mediaType: String, mediaUri: Uri?) {
        val me = dao.getMyProfileOnce() ?: return
        var savedPath: String? = null
        if (mediaUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val ext = if (mediaType == "VIDEO") "mp4" else "jpg"
                    val file = File(appContext.filesDir, "moment_${System.currentTimeMillis()}.$ext")
                    appContext.contentResolver.openInputStream(mediaUri)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    savedPath = file.absolutePath
                } catch (_: Exception) {
                }
            }
        }
        val myVipLvl = ZyroProgression.computeVipLevel(me.vipGrowthPoints).vipLevel
        dao.insertMoment(
            MomentEntity(
                authorUid = "me",
                authorName = me.displayName,
                authorZyroId = me.zyroId,
                authorIsVip = me.isVip,
                authorVipLevel = myVipLvl,
                authorIsAdmin = me.isOfficialAdmin,
                caption = caption.ifBlank { "Sharing a moment on Zyro!" },
                mediaType = mediaType,
                mediaPath = savedPath,
                presetVisualKey = if (mediaType == "VIDEO") "uno_clip" else "zyro_lounge",
                likesCount = 1,
                isLiked = true,
                createdAtLabel = "Just now"
            )
        )
    }

    suspend fun toggleLikeMoment(moment: MomentEntity) {
        val updatedLikes = if (moment.isLiked) (moment.likesCount - 1).coerceAtLeast(0) else moment.likesCount + 1
        dao.insertMoment(moment.copy(isLiked = !moment.isLiked, likesCount = updatedLikes))
    }

    suspend fun joinRoom(roomId: String) {
        val me = dao.getMyProfileOnce() ?: return
        val room = dao.getRoomById(roomId) ?: return
        dao.upsertMyProfile(me.copy(currentVoiceRoomId = roomId))
        dao.upsertRoom(room.copy(isJoined = true))
    }

    suspend fun leaveRoom() {
        val me = dao.getMyProfileOnce() ?: return
        dao.upsertMyProfile(me.copy(currentVoiceRoomId = null))
    }

    suspend fun updateRoomYoutubeVideo(roomId: String, videoId: String, title: String) {
        val room = dao.getRoomById(roomId) ?: return
        dao.upsertRoom(room.copy(youtubeVideoId = videoId, youtubeTitle = title))
    }

    suspend fun updateRoomTopic(roomId: String, newTopic: String) {
        val room = dao.getRoomById(roomId) ?: return
        dao.upsertRoom(room.copy(topic = newTopic))
    }

    suspend fun rewardGameMatch(gameName: String, charmGain: Long, coinGain: Long) {
        val me = dao.getMyProfileOnce() ?: return
        dao.upsertMyProfile(
            me.copy(
                charmPoints = me.charmPoints + charmGain,
                coins = me.coins + coinGain,
                exp = me.exp + 40L
            )
        )
    }

    suspend fun redeemInviteCode(rawInputOrUri: String): Result<String> {
        val code = ZyroInviteLinks.parseInviteCode(rawInputOrUri)
            ?: return Result.failure(IllegalArgumentException("Enter a valid Zyro invite link or ID (e.g., ZYRO-20411)."))
        val me = dao.getMyProfileOnce() ?: return Result.failure(IllegalStateException("Profile missing"))
        if (code.equals(me.zyroId, ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("That is your own Zyro invite link ($code)! Share it with a friend."))
        }
        val prefs = appContext.getSharedPreferences("zyro_invites", Context.MODE_PRIVATE)
        val redeemedKey = "redeemed_${code.uppercase(Locale.US)}"
        if (prefs.getBoolean(redeemedKey, false)) {
            return Result.success("Connected with friend $code on Zyro!")
        }
        prefs.edit().putBoolean(redeemedKey, true).apply()
        val bonusCoins = 1_000L
        dao.upsertMyProfile(me.copy(coins = me.coins + bonusCoins, exp = me.exp + 50L))
        return Result.success("Joined via $code! +1,000 Bonus Coins & +50 EXP credited!")
    }
}

object ZyroInviteLinks {
    const val LIVE_SHARED_APP_BASE_URL =
        "https://ais-pre-7npyuvowr2wqvyblqzac5u-997874239735.asia-southeast1.run.app"

    fun buildWebInviteUrl(zyroId: String): String =
        "$LIVE_SHARED_APP_BASE_URL/?invite=${Uri.encode(zyroId)}"

    fun buildAppDeepLink(zyroId: String): String =
        "zyro://invite/${Uri.encode(zyroId)}"

    fun parseInviteCode(rawInputOrUri: String): String? {
        val trimmed = rawInputOrUri.trim()
        if (trimmed.isEmpty()) return null
        val queryMatch = Regex("[?&]invite=([A-Za-z0-9_-]+)").find(trimmed)
        if (queryMatch != null) return queryMatch.groupValues[1].uppercase(Locale.US)
        val pathMatch = Regex("(?:zyro://invite/|/invite/)([A-Za-z0-9_-]+)").find(trimmed)
        if (pathMatch != null) return pathMatch.groupValues[1].uppercase(Locale.US)
        val idMatch = Regex("^(ZYRO-[A-Za-z0-9_-]+)$", RegexOption.IGNORE_CASE).find(trimmed)
        if (idMatch != null) return idMatch.groupValues[1].uppercase(Locale.US)
        return null
    }
}
