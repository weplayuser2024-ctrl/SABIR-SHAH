package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Zyro", appName)
  }

  @Test
  fun `room admin slots grow across levels 1 to 19`() {
    val (lv1, slots1) = com.example.data.ZyroProgression.computeRoomLevelAndSlots(0L)
    assertEquals(1, lv1)
    assertEquals(3, slots1)

    val (lv3, slots3) = com.example.data.ZyroProgression.computeRoomLevelAndSlots(500L)
    assertEquals(3, lv3)
    assertEquals(5, slots3)

    val (lv19, slots19) = com.example.data.ZyroProgression.computeRoomLevelAndSlots(30_000L)
    assertEquals(19, lv19)
    assertEquals(21, slots19)
  }

  @Test
  fun `charm level thresholds produce expected tier badges`() {
    assertEquals("1⭐", com.example.data.ZyroProgression.computeUserLevel(5_000L).badgeLabel)
    assertEquals("2⭐", com.example.data.ZyroProgression.computeUserLevel(13_000L).badgeLabel)
    assertEquals("3⭐", com.example.data.ZyroProgression.computeUserLevel(40_000L).badgeLabel)
    assertEquals("4⭐", com.example.data.ZyroProgression.computeUserLevel(100_000L).badgeLabel)
    assertEquals("💎5", com.example.data.ZyroProgression.computeUserLevel(150_000L).badgeLabel)
    assertEquals("💎6", com.example.data.ZyroProgression.computeUserLevel(300_000L).badgeLabel)
    assertEquals("👑7", com.example.data.ZyroProgression.computeUserLevel(500_000L).badgeLabel)
    assertEquals("👑8", com.example.data.ZyroProgression.computeUserLevel(1_200_000L).badgeLabel)
    assertEquals("👑9", com.example.data.ZyroProgression.computeUserLevel(3_000_000L).badgeLabel)
    assertEquals("👑10", com.example.data.ZyroProgression.computeUserLevel(5_000_000L).badgeLabel)
  }

  @Test
  fun `vip level scales from 1 to 14 with growth points`() {
    val vip1 = com.example.data.ZyroProgression.computeVipLevel(4)
    assertEquals(1, vip1.vipLevel)

    val vip6 = com.example.data.ZyroProgression.computeVipLevel(100)
    assertEquals(6, vip6.vipLevel)

    val vip14 = com.example.data.ZyroProgression.computeVipLevel(850)
    assertEquals(14, vip14.vipLevel)
  }

  @Test
  fun `voice room seats match 10 regular (2 top + 8 bottom) and 15 official`() {
    val regular = com.example.data.ZyroProgression.computeRoomSeats(isOfficialRoom = false)
    assertEquals(10, regular.totalSeats)
    assertEquals(2, regular.topSeats)
    assertEquals(8, regular.bottomSeats)

    val official = com.example.data.ZyroProgression.computeRoomSeats(isOfficialRoom = true)
    assertEquals(15, official.totalSeats)
    assertEquals(3, official.topSeats)
    assertEquals(12, official.bottomSeats)
  }

  @Test
  fun `gift catalog has 5 items and action room grants relationship tags`() {
    assertEquals(5, com.example.data.ZyroProgression.giftCatalog.size)
    val rewards = com.example.data.ZyroProgression.actionTagGifts.mapNotNull { it.tagReward }
    assertEquals(true, rewards.contains("Brother \uD83E\uDD1D"))
    assertEquals(true, rewards.contains("Sister \uD83C\uDF40"))
    assertEquals(true, rewards.contains("Bestie \uD83C\uDF80"))
    assertEquals(10, com.example.data.ZyroProgression.roomEmotes.size)
  }

  @Test
  fun `vip tiers produce distinct level colors and entry effect titles`() {
    val tier1 = com.example.ui.components.vipTierVisual(1)
    val tier4 = com.example.ui.components.vipTierVisual(4)
    val tier6 = com.example.ui.components.vipTierVisual(6)
    val tier9 = com.example.ui.components.vipTierVisual(9)
    val tier11 = com.example.ui.components.vipTierVisual(11)
    val tier14 = com.example.ui.components.vipTierVisual(14)

    val distinctDarkColors = setOf(
      tier1.nameColorDark,
      tier4.nameColorDark,
      tier6.nameColorDark,
      tier9.nameColorDark,
      tier11.nameColorDark,
      tier14.nameColorDark
    )
    assertEquals(6, distinctDarkColors.size)
    assertEquals("Celestial Zenith", tier14.tierName)
    assertEquals("Royal Amethyst Vortex", tier9.entryEffectTitle)
  }

  @Test
  fun `invite link uses live shared app url and deep link parsing works`() {
    val webUrl = com.example.ui.screens.profile.buildInviteUrl("ZYRO-10482")
    val appDeepLink = com.example.ui.screens.profile.buildAppDeepLink("ZYRO-10482")

    assertEquals(
      "https://ais-pre-7npyuvowr2wqvyblqzac5u-997874239735.asia-southeast1.run.app/?invite=ZYRO-10482",
      webUrl
    )
    assertEquals("zyro://invite/ZYRO-10482", appDeepLink)
    assertEquals("ZYRO-10482", com.example.data.ZyroInviteLinks.parseInviteCode(webUrl))
    assertEquals("ZYRO-10482", com.example.data.ZyroInviteLinks.parseInviteCode(appDeepLink))
    assertEquals("ZYRO-10482", com.example.data.ZyroInviteLinks.parseInviteCode("zyro-10482"))
  }
}
