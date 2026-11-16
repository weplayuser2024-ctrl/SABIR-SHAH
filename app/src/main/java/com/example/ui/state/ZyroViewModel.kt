package com.example.ui.state

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RankedUser(
    val uid: String,
    val zyroId: String,
    val name: String,
    val charmPoints: Long,
    val level: Int,
    val charmBadge: String,
    val isVip: Boolean,
    val vipLevel: Int,
    val isOfficialAdmin: Boolean,
    val isMe: Boolean,
    val avatarColorHex: Long,
    val avatarImagePath: String? = null
)

data class WeddingCelebration(
    val groomOrHostName: String,
    val partnerName: String,
    val ringName: String
)

data class InspectedUserProfile(
    val uid: String,
    val zyroId: String,
    val name: String,
    val charmPoints: Long,
    val exp: Long,
    val isVip: Boolean,
    val vipLevel: Int,
    val isOfficialAdmin: Boolean,
    val cpStatus: String?,
    val bio: String,
    val tags: List<String>,
    val currentVoiceRoomName: String?,
    val avatarColorHex: Long,
    val avatarImagePath: String?,
    val isMe: Boolean
)

class ZyroViewModel(application: Application) : AndroidViewModel(application) {
    private val db = ZyroDatabase.getInstance(application)
    private val repo = ZyroRepository(db.zyroDao(), application.applicationContext)

    init {
        viewModelScope.launch {
            repo.ensureSeeded()
        }
    }

    val profile: StateFlow<UserProfileEntity?> = repo.profileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val friends: StateFlow<List<FriendEntity>> = repo.friendsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rankings: StateFlow<List<RankedUser>> = combine(repo.profileFlow, repo.friendsFlow) { me, friends ->
        val list = mutableListOf<RankedUser>()
        if (me != null) {
            val meLvl = ZyroProgression.computeUserLevel(me.charmPoints)
            list.add(
                RankedUser(
                    uid = "me",
                    zyroId = me.zyroId,
                    name = "${me.displayName} (You)",
                    charmPoints = me.charmPoints,
                    level = meLvl.level,
                    charmBadge = meLvl.badgeLabel,
                    isVip = me.isVip,
                    vipLevel = ZyroProgression.computeVipLevel(me.vipGrowthPoints).vipLevel,
                    isOfficialAdmin = me.isOfficialAdmin,
                    isMe = true,
                    avatarColorHex = 0xFF00E5FF,
                    avatarImagePath = me.avatarImagePath
                )
            )
        }
        friends.forEach { f ->
            val fLvl = ZyroProgression.computeUserLevel(f.charmPoints)
            list.add(
                RankedUser(
                    uid = f.uid,
                    zyroId = f.zyroId,
                    name = f.name,
                    charmPoints = f.charmPoints,
                    level = fLvl.level,
                    charmBadge = fLvl.badgeLabel,
                    isVip = f.isVip,
                    vipLevel = f.vipLevel,
                    isOfficialAdmin = f.isOfficialAdmin,
                    isMe = false,
                    avatarColorHex = f.avatarColorHex
                )
            )
        }
        list.sortedByDescending { it.charmPoints }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rooms: StateFlow<List<RoomEntity>> = repo.roomsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val moments: StateFlow<List<MomentEntity>> = repo.momentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myMoments: StateFlow<List<MomentEntity>> = repo.myMomentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val giftWall: StateFlow<List<GiftRecordEntity>> = repo.giftWallFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shopItems: StateFlow<List<ShopInventoryEntity>> = repo.shopFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyTasks: StateFlow<List<DailyTaskEntity>> = repo.dailyTasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val privacy: StateFlow<PrivacySettingsEntity> = repo.privacyFlow
        .map { it ?: PrivacySettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrivacySettingsEntity())

    private val _activeRoomId = MutableStateFlow<String?>(null)
    val activeRoomId: StateFlow<String?> = _activeRoomId.asStateFlow()

    private val _selectedChatFriendUid = MutableStateFlow<String?>(null)
    val selectedChatFriendUid: StateFlow<String?> = _selectedChatFriendUid.asStateFlow()

    private val _inspectedProfile = MutableStateFlow<InspectedUserProfile?>(null)
    val inspectedProfile: StateFlow<InspectedUserProfile?> = _inspectedProfile.asStateFlow()

    private val _activeGiftOverlay = MutableStateFlow<GiftSendOutcome?>(null)
    val activeGiftOverlay: StateFlow<GiftSendOutcome?> = _activeGiftOverlay.asStateFlow()

    private val _activeWeddingCelebration = MutableStateFlow<WeddingCelebration?>(null)
    val activeWeddingCelebration: StateFlow<WeddingCelebration?> = _activeWeddingCelebration.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun messagesForFriend(uid: String): Flow<List<ChatMessageEntity>> = repo.messagesFlow(uid)

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun handleIncomingInvite(rawUriOrCode: String) {
        viewModelScope.launch {
            repo.redeemInviteCode(rawUriOrCode).onSuccess { msg ->
                _toastMessage.value = msg
            }.onFailure { err ->
                _toastMessage.value = err.message ?: "Invalid invite link"
            }
        }
    }

    fun loginWithProvider(provider: String, identifier: String, suggestedName: String? = null) {
        viewModelScope.launch {
            repo.loginWithProvider(provider, identifier, suggestedName)
            _toastMessage.value = "Signed in via $provider"
        }
    }

    fun logout() {
        viewModelScope.launch {
            _activeRoomId.value = null
            _selectedChatFriendUid.value = null
            repo.logout()
        }
    }

    fun updateProfileDetails(newName: String, newPfpUri: Uri?) {
        viewModelScope.launch {
            repo.updateProfileDetails(newName, newPfpUri).onSuccess {
                _toastMessage.value = "Profile updated!"
            }.onFailure {
                _toastMessage.value = "Could not update profile picture"
            }
        }
    }

    fun createVoiceRoom(title: String, topic: String, type: String) {
        viewModelScope.launch {
            repo.createVoiceRoom(title, topic, type).onSuccess { room ->
                enterRoom(room.id)
                _toastMessage.value = "Created Room ${room.roomUid} (-500 Coins)!"
            }.onFailure { err ->
                _toastMessage.value = err.message ?: "Unable to create voice room"
            }
        }
    }

    fun claimDailyTask(taskId: String) {
        viewModelScope.launch {
            repo.claimDailyTask(taskId).onSuccess { task ->
                _toastMessage.value = "Task claimed! +${task.rewardCoins} Coins & +${task.rewardExp} EXP"
            }.onFailure { err ->
                _toastMessage.value = err.message ?: "Already claimed"
            }
        }
    }

    fun spendVipCoinsForGrowth(coinsToSpend: Long) {
        viewModelScope.launch {
            repo.spendVipCoinsForGrowth(coinsToSpend).onSuccess { vipProg ->
                _toastMessage.value = "Spent $coinsToSpend VIP Coins! VIP Lv.${vipProg.vipLevel} (${vipProg.growthPoints} Growth pts)"
            }.onFailure { err ->
                _toastMessage.value = err.message ?: "Unable to spend VIP coins"
            }
        }
    }

    fun toggleOfficialAdmin(isAdmin: Boolean) {
        viewModelScope.launch {
            repo.toggleOfficialAdmin(isAdmin)
            _toastMessage.value = if (isAdmin) "Official Admin Tag Activated" else "Admin Tag Disabled"
        }
    }

    fun addTag(tag: String) {
        viewModelScope.launch { repo.addCustomTag(tag) }
    }

    fun removeTag(tag: String) {
        viewModelScope.launch { repo.removeTag(tag) }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch { repo.toggleDarkMode(enabled) }
    }

    fun updatePrivacy(transform: (PrivacySettingsEntity) -> PrivacySettingsEntity) {
        viewModelScope.launch { repo.updatePrivacy(transform) }
    }

    fun sendGift(gift: GiftCatalogItem, recipientUid: String?, roomId: String?) {
        viewModelScope.launch {
            val result = repo.sendGift(gift, recipientUid, roomId)
            result.onSuccess { outcome ->
                _activeGiftOverlay.value = outcome
                if (outcome.earnedTag != null) {
                    _toastMessage.value = "Tag Earned: ${outcome.earnedTag}!"
                }
                delay(3200)
                if (_activeGiftOverlay.value == outcome) {
                    _activeGiftOverlay.value = null
                }
            }.onFailure { err ->
                _toastMessage.value = err.message ?: "Failed to send gift"
            }
        }
    }

    fun dismissGiftOverlay() {
        _activeGiftOverlay.value = null
    }

    fun activateVipPlan(months: Int, priceInr: Int) {
        viewModelScope.launch {
            repo.activateVipPlan(months, priceInr).onSuccess {
                val bonusCoins = if (months >= 3) 1500 else 500
                val bonusGrowth = if (months >= 3) 15 else 5
                _toastMessage.value = "Zyro VIP ($months mo • ₹$priceInr) Activated! +$bonusCoins Coins & +$bonusGrowth Growth"
            }
        }
    }

    fun topUpInr(amountInr: Int, coins: Long, cp: Long) {
        viewModelScope.launch {
            repo.processInrTopUp(amountInr, coins, cp).onSuccess {
                _toastMessage.value = "₹$amountInr INR Top-Up Verified! +${ZyroProgression.formatCompactCoins(coins)} Coins"
            }
        }
    }

    fun saveCustomQr(uri: Uri) {
        viewModelScope.launch {
            repo.saveCustomQrFromUri(uri).onSuccess {
                _toastMessage.value = "Your custom INR Payment QR code is saved!"
            }.onFailure {
                _toastMessage.value = "Could not load QR image"
            }
        }
    }

    fun buyOrEquipShopItem(item: ShopInventoryEntity) {
        viewModelScope.launch {
            repo.buyOrEquipShopItem(item).onSuccess { msg ->
                _toastMessage.value = msg
            }.onFailure { err ->
                _toastMessage.value = err.message ?: "Purchase failed"
            }
        }
    }

    fun acquireCp(costCoins: Long, cpEarned: Long) {
        viewModelScope.launch {
            repo.acquireCpPoints(costCoins, cpEarned).onSuccess {
                _toastMessage.value = "Acquired +$cpEarned CP!"
            }.onFailure { err ->
                _toastMessage.value = err.message ?: "Not enough coins"
            }
        }
    }

    fun hostWedding(partnerName: String, ringItem: ShopInventoryEntity) {
        viewModelScope.launch {
            val me = profile.value
            repo.hostWedding(partnerName, ringItem).onSuccess {
                _activeWeddingCelebration.value = WeddingCelebration(
                    groomOrHostName = me?.displayName ?: "Aarav",
                    partnerName = partnerName,
                    ringName = ringItem.name
                )
            }.onFailure { err ->
                _toastMessage.value = err.message ?: "Could not host wedding"
            }
        }
    }

    fun dismissWeddingCelebration() {
        _activeWeddingCelebration.value = null
    }

    fun openChat(friendUid: String) {
        _selectedChatFriendUid.value = friendUid
        viewModelScope.launch { repo.markChatRead(friendUid) }
    }

    fun closeChat() {
        _selectedChatFriendUid.value = null
    }

    fun sendChatMessage(friendUid: String, text: String) {
        viewModelScope.launch { repo.sendChatMessage(friendUid, text) }
    }

    fun postMoment(caption: String, mediaType: String, mediaUri: Uri?) {
        viewModelScope.launch {
            repo.postMoment(caption, mediaType, mediaUri)
            _toastMessage.value = "Moment shared with friends!"
        }
    }

    fun toggleLikeMoment(moment: MomentEntity) {
        viewModelScope.launch { repo.toggleLikeMoment(moment) }
    }

    fun enterRoom(roomId: String) {
        _activeRoomId.value = roomId
        viewModelScope.launch {
            repo.joinRoom(roomId)
        }
    }

    fun exitRoom() {
        _activeRoomId.value = null
        viewModelScope.launch { repo.leaveRoom() }
    }

    fun updateRoomYoutube(roomId: String, videoInput: String, title: String) {
        val extracted = extractYoutubeId(videoInput)
        if (extracted.isBlank()) {
            _toastMessage.value = "Enter a valid YouTube URL or 11-char Video ID"
            return
        }
        viewModelScope.launch {
            repo.updateRoomYoutubeVideo(roomId, extracted, title.ifBlank { "YouTube Video" })
            _toastMessage.value = "YouTube Video Loaded"
        }
    }

    fun updateRoomTopic(roomId: String, topic: String) {
        viewModelScope.launch {
            repo.updateRoomTopic(roomId, topic)
            _toastMessage.value = "Room topic updated by Admin"
        }
    }

    fun rewardGameMatch(gameName: String, charmGain: Long, coinGain: Long) {
        viewModelScope.launch {
            repo.rewardGameMatch(gameName, charmGain, coinGain)
            _toastMessage.value = "$gameName Win! +$charmGain Charm • +$coinGain Coins"
        }
    }

    fun inspectFriend(friend: FriendEntity) {
        _inspectedProfile.value = InspectedUserProfile(
            uid = friend.uid,
            zyroId = friend.zyroId,
            name = friend.name,
            charmPoints = friend.charmPoints,
            exp = friend.exp,
            isVip = friend.isVip,
            vipLevel = friend.vipLevel,
            isOfficialAdmin = friend.isOfficialAdmin,
            cpStatus = friend.cpStatus,
            bio = friend.bio,
            tags = friend.tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            currentVoiceRoomName = friend.currentVoiceRoomName,
            avatarColorHex = friend.avatarColorHex,
            avatarImagePath = null,
            isMe = false
        )
    }

    fun inspectMyProfile() {
        val me = profile.value ?: return
        val vipLvl = ZyroProgression.computeVipLevel(me.vipGrowthPoints).vipLevel
        _inspectedProfile.value = InspectedUserProfile(
            uid = "me",
            zyroId = me.zyroId,
            name = me.displayName,
            charmPoints = me.charmPoints,
            exp = me.exp,
            isVip = me.isVip,
            vipLevel = vipLvl,
            isOfficialAdmin = me.isOfficialAdmin,
            cpStatus = if (me.isMarried && me.cpPartnerName != null) "Married to ${me.cpPartnerName}" else "CP Points: ${me.cpPoints}",
            bio = "Zyro Player • VIP Lv.$vipLvl (${me.vipGrowthPoints} Growth pts)",
            tags = me.tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            currentVoiceRoomName = null,
            avatarColorHex = 0xFF00E5FF,
            avatarImagePath = me.avatarImagePath,
            isMe = true
        )
    }

    fun inspectRankedUser(ranked: RankedUser) {
        if (ranked.isMe) {
            inspectMyProfile()
        } else {
            val found = friends.value.firstOrNull { it.uid == ranked.uid }
            if (found != null) inspectFriend(found)
        }
    }

    fun closeInspectedProfile() {
        _inspectedProfile.value = null
    }

    private fun extractYoutubeId(input: String): String {
        val trimmed = input.trim()
        if (trimmed.length == 11 && !trimmed.contains("/") && !trimmed.contains("?")) {
            return trimmed
        }
        val watchMatch = Regex("[?&]v=([a-zA-Z0-9_-]{11})").find(trimmed)
        if (watchMatch != null) return watchMatch.groupValues[1]
        val shortMatch = Regex("(?:youtu\\.be/|youtube\\.com/embed/|youtube\\.com/shorts/|youtube\\.com/live/)([a-zA-Z0-9_-]{11})").find(trimmed)
        if (shortMatch != null) return shortMatch.groupValues[1]
        return ""
    }
}
