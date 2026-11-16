package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.GiftAnimationOverlay
import com.example.ui.components.UserProfileDialog
import com.example.ui.components.WeddingCelebrationOverlay
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.discover.DiscoverScreen
import com.example.ui.screens.games.GamesScreen
import com.example.ui.screens.profile.InrTopUpAndQrDialog
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.VipDialog
import com.example.ui.screens.rooms.RoomsScreen
import com.example.ui.state.ZyroViewModel
import com.example.ui.theme.ZyroTheme

private data class ZyroNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val zyroTabs = listOf(
    ZyroNavTab("games", "Games", Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports),
    ZyroNavTab("rooms", "Rooms", Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq),
    ZyroNavTab("chat", "Chat", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    ZyroNavTab("discover", "Discover", Icons.Filled.Explore, Icons.Outlined.Explore),
    ZyroNavTab("profile", "Me", Icons.Filled.Person, Icons.Outlined.PersonOutline)
)

class MainActivity : ComponentActivity() {
    private val pendingInviteUri = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingInviteUri.value = intent?.dataString
        enableEdgeToEdge()
        setContent {
            val viewModel: ZyroViewModel = viewModel()
            val privacy by viewModel.privacy.collectAsStateWithLifecycle()
            val profile by viewModel.profile.collectAsStateWithLifecycle()
            val incomingUri by pendingInviteUri

            LaunchedEffect(incomingUri, profile?.isLoggedIn) {
                val uri = incomingUri
                if (uri != null && profile?.isLoggedIn == true) {
                    viewModel.handleIncomingInvite(uri)
                    pendingInviteUri.value = null
                }
            }

            ZyroTheme(darkTheme = privacy.darkModeEnabled) {
                if (profile == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (!profile!!.isLoggedIn) {
                    LoginScreen(
                        onSignInSuccess = { provider, identifier, name ->
                            viewModel.loginWithProvider(provider, identifier, name)
                        }
                    )
                } else {
                    ZyroAppShell(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.let { pendingInviteUri.value = it }
    }
}

@Composable
private fun ZyroAppShell(viewModel: ZyroViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "games"

    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val rankings by viewModel.rankings.collectAsStateWithLifecycle()
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val moments by viewModel.moments.collectAsStateWithLifecycle()
    val myMoments by viewModel.myMoments.collectAsStateWithLifecycle()
    val giftWall by viewModel.giftWall.collectAsStateWithLifecycle()
    val shopItems by viewModel.shopItems.collectAsStateWithLifecycle()
    val dailyTasks by viewModel.dailyTasks.collectAsStateWithLifecycle()
    val privacy by viewModel.privacy.collectAsStateWithLifecycle()
    val activeRoomId by viewModel.activeRoomId.collectAsStateWithLifecycle()
    val selectedChatUid by viewModel.selectedChatFriendUid.collectAsStateWithLifecycle()
    val inspectedProfile by viewModel.inspectedProfile.collectAsStateWithLifecycle()
    val giftOverlay by viewModel.activeGiftOverlay.collectAsStateWithLifecycle()
    val weddingCelebration by viewModel.activeWeddingCelebration.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    var showHomeTopUpDialog by remember { mutableStateOf(false) }
    var showHomeVipDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    val messagesForSelected by remember(selectedChatUid) {
        if (selectedChatUid != null) viewModel.messagesForFriend(selectedChatUid!!)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!useRail && activeRoomId == null && selectedChatUid == null) {
                        NavigationBar {
                            zyroTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag("nav_${tab.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail && activeRoomId == null && selectedChatUid == null) {
                        NavigationRail {
                            zyroTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag("rail_${tab.route}")
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "games",
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        composable("games") {
                            GamesScreen(
                                profile = profile,
                                friends = friends,
                                rankings = rankings,
                                dailyTasks = dailyTasks,
                                darkModeEnabled = privacy.darkModeEnabled,
                                onToggleDarkMode = { viewModel.toggleDarkMode(it) },
                                onOpenProfileTab = { navigateToTab("profile") },
                                onOpenTopUp = { showHomeTopUpDialog = true },
                                onOpenVip = { showHomeVipDialog = true },
                                onClaimDailyTask = { viewModel.claimDailyTask(it) },
                                onInspectRankUser = { viewModel.inspectRankedUser(it) },
                                onInspectFriend = { viewModel.inspectFriend(it) },
                                onOpenChatWithFriend = { uid ->
                                    viewModel.openChat(uid)
                                    navigateToTab("chat")
                                },
                                onMatchReward = { game, charm, coins -> viewModel.rewardGameMatch(game, charm, coins) },
                                onOpenOfficialRoom = {
                                    viewModel.enterRoom("room_admin_official")
                                    navigateToTab("rooms")
                                },
                                onShowEventToast = { msg -> viewModel.showToast(msg) }
                            )
                        }

                        composable("rooms") {
                            RoomsScreen(
                                rooms = rooms,
                                friends = friends,
                                profile = profile,
                                privacy = privacy,
                                activeRoomId = activeRoomId,
                                onEnterRoom = { viewModel.enterRoom(it) },
                                onExitRoom = { viewModel.exitRoom() },
                                onCreateVoiceRoom = { title, topic, type -> viewModel.createVoiceRoom(title, topic, type) },
                                onSendGift = { gift, uid, roomId -> viewModel.sendGift(gift, uid, roomId) },
                                onUpdateYoutube = { rId, url, title -> viewModel.updateRoomYoutube(rId, url, title) },
                                onUpdateRoomTopic = { rId, topic -> viewModel.updateRoomTopic(rId, topic) },
                                onInspectFriend = { viewModel.inspectFriend(it) }
                            )
                        }

                        composable("chat") {
                            ChatScreen(
                                friends = friends,
                                profile = profile,
                                privacy = privacy,
                                selectedFriendUid = selectedChatUid,
                                messagesForSelected = messagesForSelected,
                                onOpenChat = { viewModel.openChat(it) },
                                onCloseChat = { viewModel.closeChat() },
                                onSendMessage = { uid, text -> viewModel.sendChatMessage(uid, text) },
                                onJumpToVoiceRoom = { roomId ->
                                    viewModel.closeChat()
                                    viewModel.enterRoom(roomId)
                                    navigateToTab("rooms")
                                },
                                onInspectFriend = { viewModel.inspectFriend(it) }
                            )
                        }

                        composable("discover") {
                            DiscoverScreen(
                                moments = moments,
                                friends = friends,
                                profile = profile,
                                onToggleLike = { viewModel.toggleLikeMoment(it) },
                                onPostMoment = { caption, type, uri -> viewModel.postMoment(caption, type, uri) },
                                onInspectFriend = { viewModel.inspectFriend(it) }
                            )
                        }

                        composable("profile") {
                            ProfileScreen(
                                profile = profile,
                                friends = friends,
                                myMoments = myMoments,
                                giftWall = giftWall,
                                rooms = rooms,
                                shopItems = shopItems,
                                privacy = privacy,
                                onUpdateProfileDetails = { name, uri -> viewModel.updateProfileDetails(name, uri) },
                                onToggleOfficialAdmin = { viewModel.toggleOfficialAdmin(it) },
                                onInspectMyProfile = { viewModel.inspectMyProfile() },
                                onRejoinRoom = { roomId ->
                                    viewModel.enterRoom(roomId)
                                    navigateToTab("rooms")
                                },
                                onActivateVipPlan = { months, price -> viewModel.activateVipPlan(months, price) },
                                onSpendVipCoinsForGrowth = { coins -> viewModel.spendVipCoinsForGrowth(coins) },
                                onTopUpInr = { inr, coins, cp -> viewModel.topUpInr(inr, coins, cp) },
                                onSaveCustomQr = { uri -> viewModel.saveCustomQr(uri) },
                                onBuyOrEquipShopItem = { item -> viewModel.buyOrEquipShopItem(item) },
                                onAcquireCp = { cost, cp -> viewModel.acquireCp(cost, cp) },
                                onHostWedding = { partner, ring -> viewModel.hostWedding(partner, ring) },
                                onUpdatePrivacy = { viewModel.updatePrivacy(it) },
                                onShareFeedback = { viewModel.showToast(it) },
                                onRedeemInviteLink = { input -> viewModel.handleIncomingInvite(input) },
                                onLogout = { viewModel.logout() }
                            )
                        }
                    }
                }
            }
        }

        // Home '+' Top-Up Dialog
        if (showHomeTopUpDialog && profile != null) {
            InrTopUpAndQrDialog(
                profile = profile!!,
                onDismiss = { showHomeTopUpDialog = false },
                onTopUpInr = { inr, coins, cp -> viewModel.topUpInr(inr, coins, cp) },
                onSaveCustomQr = { uri -> viewModel.saveCustomQr(uri) }
            )
        }

        // Home VIP 1-14 Dialog
        if (showHomeVipDialog && profile != null) {
            VipDialog(
                profile = profile!!,
                onDismiss = { showHomeVipDialog = false },
                onActivateVipPlan = { months, price ->
                    viewModel.activateVipPlan(months, price)
                    showHomeVipDialog = false
                },
                onSpendVipCoinsForGrowth = { coins -> viewModel.spendVipCoinsForGrowth(coins) },
                onOpenInrTopUp = { showHomeTopUpDialog = true }
            )
        }

        // Global Animated Gifting Overlay
        GiftAnimationOverlay(
            outcome = giftOverlay,
            onDismiss = { viewModel.dismissGiftOverlay() }
        )

        // Global Wedding Ceremony Overlay
        WeddingCelebrationOverlay(
            wedding = weddingCelebration,
            onDismiss = { viewModel.dismissWeddingCelebration() }
        )

        // Global Inspect Any Player Profile Dialog
        if (inspectedProfile != null) {
            val inspected = inspectedProfile!!
            UserProfileDialog(
                profile = inspected,
                onDismiss = { viewModel.closeInspectedProfile() },
                onSendGift = { gift ->
                    viewModel.sendGift(gift, inspected.uid, activeRoomId)
                },
                onOpenChat = if (!inspected.isMe) {
                    {
                        viewModel.openChat(inspected.uid)
                        navigateToTab("chat")
                    }
                } else null
            )
        }
    }
}
