package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ZyroDao {

    @Query("SELECT * FROM user_profile WHERE id = 'me' LIMIT 1")
    fun observeMyProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 'me' LIMIT 1")
    suspend fun getMyProfileOnce(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMyProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM friends ORDER BY unreadCount DESC, charmPoints DESC")
    fun observeFriends(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends ORDER BY charmPoints DESC")
    fun observeRankingsByCharm(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE uid = :uid LIMIT 1")
    suspend fun getFriendByUid(uid: String): FriendEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFriends(friends: List<FriendEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFriend(friend: FriendEntity)

    @Query("SELECT * FROM chat_messages WHERE friendUid = :friendUid ORDER BY id ASC")
    fun observeMessagesForFriend(friendUid: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("SELECT * FROM moments ORDER BY id DESC")
    fun observeMoments(): Flow<List<MomentEntity>>

    @Query("SELECT * FROM moments WHERE authorUid = 'me' ORDER BY id DESC")
    fun observeMyMoments(): Flow<List<MomentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoment(moment: MomentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoments(moments: List<MomentEntity>)

    @Query("SELECT * FROM gift_records ORDER BY id DESC")
    fun observeGiftWall(): Flow<List<GiftRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGiftRecord(record: GiftRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGiftRecords(records: List<GiftRecordEntity>)

    @Query("SELECT * FROM rooms ORDER BY isOfficialAdminRoom DESC, level DESC")
    fun observeRooms(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE id = :roomId LIMIT 1")
    suspend fun getRoomById(roomId: String): RoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoom(room: RoomEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRooms(rooms: List<RoomEntity>)

    @Query("DELETE FROM rooms WHERE id = :roomId")
    suspend fun deleteRoomById(roomId: String)

    @Query("SELECT * FROM shop_inventory ORDER BY category ASC, priceCoins ASC")
    fun observeShopInventory(): Flow<List<ShopInventoryEntity>>

    @Query("SELECT * FROM shop_inventory WHERE itemId = :itemId LIMIT 1")
    suspend fun getShopItemById(itemId: String): ShopInventoryEntity?

    @Query("SELECT * FROM shop_inventory WHERE category = :category")
    suspend fun getShopItemsByCategory(category: String): List<ShopInventoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShopItems(items: List<ShopInventoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShopItem(item: ShopInventoryEntity)

    @Query("SELECT * FROM privacy_settings WHERE id = 0 LIMIT 1")
    fun observePrivacySettings(): Flow<PrivacySettingsEntity?>

    @Query("SELECT * FROM privacy_settings WHERE id = 0 LIMIT 1")
    suspend fun getPrivacySettingsOnce(): PrivacySettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrivacySettings(settings: PrivacySettingsEntity)

    @Query("SELECT * FROM daily_tasks")
    fun observeDailyTasks(): Flow<List<DailyTaskEntity>>

    @Query("SELECT * FROM daily_tasks WHERE taskId = :taskId LIMIT 1")
    suspend fun getDailyTaskById(taskId: String): DailyTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyTasks(tasks: List<DailyTaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyTask(task: DailyTaskEntity)
}
