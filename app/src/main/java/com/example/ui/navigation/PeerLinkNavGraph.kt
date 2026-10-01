/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: PeerLinkNavGraph.kt
 *
 * Commentary / Architectural Overview:
 * Implements the single-screen Cyberpunk navigation shell with a swappable content area.
 *
 * Core Navigation Components:
 * - Floating Pill Bottom Bar: Features 4 equal-width tabs (HOME, CHATS, RADAR, BOOSTER). Active tab
 *   highlights with a slightly lighter filled background (#1E2738) and cyan accent text (#00E5FF).
 *   Inactive tabs are transparent with muted text (#64748B).
 * - Floating Corner Icon Buttons: Top-left button toggles the slide-out menu drawer; top-right
 *   button accesses Settings. Both have rounded borders and cyan-colored icons.
 * - Slide-out Side Drawer: Provides quick links to secondary sections (Screen Share, Calls Center,
 *   Nearby Radar, Game Deck, Settings) with an accented header block showing active node status.
 * - Splash Screen: Full-screen cyberpunk launch screen with bold, wide letter-spaced title and
 *   muted subtitle, fading out smoothly after 1.3 seconds.
 */

package com.example.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.calling.CallState
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberCornerIconButton
import com.example.ui.components.CyberFloatingBottomBar
import com.example.ui.components.CyberTabItem
import com.example.ui.screens.CallsScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NearbyScreen
import com.example.ui.screens.ScreenShareScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShizukuScreen
import com.example.ui.theme.CyberAccentCyan
import com.example.ui.theme.CyberAccentGreen
import com.example.ui.theme.CyberAccentPurple
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.LocalHazeState
import com.example.viewmodel.MainViewModel
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PeerLinkApp(
    viewModel: MainViewModel,
    navController: NavHostController = rememberNavController()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val callInfo by viewModel.callManager.callInfo.collectAsState()
    val localIp by viewModel.localIp.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val hazeState = remember { HazeState() }

    // Full-screen cyberpunk splash on launch
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1300)
        showSplash = false
    }

    // Listen to toasts from ViewModel
    LaunchedEffect(Unit) {
        viewModel.uiToast.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Auto-navigate to Calls screen when incoming/outgoing call is triggered
    LaunchedEffect(callInfo?.callState) {
        val state = callInfo?.callState
        if (state == CallState.INCOMING_RINGING || state == CallState.OUTGOING_RINGING) {
            if (currentRoute != "calls") {
                navController.navigate("calls") {
                    launchSingleTop = true
                }
            }
        }
    }

    // 4 equal-width core tabs for floating bottom pill bar
    val mainTabs = listOf(
        CyberTabItem("home", "HOME", Icons.Filled.Home),
        CyberTabItem("chats", "CHATS", Icons.AutoMirrored.Filled.Chat),
        CyberTabItem("nearby", "RADAR", Icons.Filled.Radar),
        CyberTabItem("shizuku", "BOOSTER", Icons.Filled.Bolt)
    )

    val isTopLevelRoute = currentRoute in mainTabs.map { it.id }

    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBackground)
        ) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = isTopLevelRoute && !showSplash,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = CyberSurface,
                        drawerContentColor = CyberTextPrimary,
                        modifier = Modifier
                            .width(310.dp)
                            .background(CyberSurface)
                            .border(
                                width = 1.dp,
                                color = CyberBorder,
                                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                        ) {
                            // Header block at the top
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp)),
                                color = CyberCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "PEERLINK // NODE",
                                            style = TextStyle(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.4.sp,
                                                color = CyberAccentCyan,
                                                fontFamily = FontFamily.SansSerif
                                            )
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(CyberAccentGreen)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = viewModel.deviceIdentity.deviceName,
                                        style = TextStyle(
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberTextPrimary
                                        ),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = localIp.ifEmpty { "127.0.0.1 (Offline Mesh)" },
                                        style = TextStyle(
                                            fontSize = 12.sp,
                                            color = CyberTextSecondary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    CyberBadge(text = "LOCAL MESH ACTIVE", tint = CyberAccentGreen)
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "SECONDARY SECTIONS",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.6.sp,
                                    color = CyberTextMuted
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Secondary Drawer Links with accent-tinted icons
                            val drawerLinks = listOf(
                                DrawerNavEntry("Screen Share", Icons.Filled.ScreenShare, "screenshare"),
                                DrawerNavEntry("Voice & Video Calls", Icons.Filled.Call, "calls"),
                                DrawerNavEntry("Nearby Device Radar", Icons.Filled.Radar, "nearby"),
                                DrawerNavEntry("Game & Shizuku Deck", Icons.Filled.SportsEsports, "shizuku"),
                                DrawerNavEntry("Settings & Display", Icons.Filled.Settings, "settings")
                            )

                            drawerLinks.forEach { entry ->
                                DrawerItemRow(
                                    label = entry.title,
                                    icon = entry.icon,
                                    isSelected = currentRoute == entry.route,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        if (currentRoute != entry.route) {
                                            navController.navigate(entry.route) {
                                                popUpTo("home") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Spacer(modifier = Modifier.weight(1f))
                            HorizontalDivider(color = CyberBorder, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "v2.6 // ZERO CLOUD",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberTextMuted,
                                    letterSpacing = 1.sp
                                )
                                IconButton(onClick = { scope.launch { drawerState.close() } }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Menu",
                                        tint = CyberTextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            ) {
                // Single-screen shell with swappable content area
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = CyberBackground,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        // Floating pill-shaped bottom bar with 4 equal-width tabs
                        if (isTopLevelRoute && !showSplash) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                CyberFloatingBottomBar(
                                    tabs = mainTabs,
                                    selectedTabId = currentRoute ?: "home",
                                    onTabSelected = { tabId ->
                                        if (currentRoute != tabId) {
                                            navController.navigate(tabId) {
                                                popUpTo("home") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Swappable content area
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable("home") {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToNearby = { navController.navigate("nearby") },
                                    onNavigateToChats = { navController.navigate("chats") },
                                    onNavigateToCalls = { navController.navigate("calls") },
                                    onNavigateToScreenShare = { navController.navigate("screenshare") },
                                    onNavigateToShizuku = { navController.navigate("shizuku") },
                                    modifier = Modifier.padding(top = 54.dp)
                                )
                            }

                            composable("nearby") {
                                NearbyScreen(
                                    viewModel = viewModel,
                                    onNavigateToChat = { peerId ->
                                        navController.navigate("chat_detail/$peerId")
                                    },
                                    onNavigateToCalls = { navController.navigate("calls") },
                                    onNavigateToScreenShare = { peerId ->
                                        navController.navigate("screenshare?peerId=$peerId")
                                    },
                                    modifier = Modifier.padding(top = 54.dp)
                                )
                            }

                            composable("chats") {
                                ChatsScreen(
                                    viewModel = viewModel,
                                    onOpenChat = { peerId ->
                                        navController.navigate("chat_detail/$peerId")
                                    },
                                    onStartNewChat = { navController.navigate("nearby") },
                                    modifier = Modifier.padding(top = 54.dp)
                                )
                            }

                            composable("calls") {
                                CallsScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(top = 54.dp)
                                )
                            }

                            composable(
                                route = "screenshare?peerId={peerId}",
                                arguments = listOf(navArgument("peerId") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                })
                            ) { backStackEntry ->
                                val peerId = backStackEntry.arguments?.getString("peerId")
                                ScreenShareScreen(
                                    viewModel = viewModel,
                                    initialTargetPeerId = peerId,
                                    modifier = Modifier.padding(top = 54.dp)
                                )
                            }

                            composable("settings") {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onNavigateToShizuku = { navController.navigate("shizuku") },
                                    modifier = Modifier.padding(top = 54.dp)
                                )
                            }

                            composable("shizuku") {
                                ShizukuScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = "chat_detail/{peerId}",
                                arguments = listOf(navArgument("peerId") { type = NavType.StringType }),
                                enterTransition = {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Left,
                                        animationSpec = tween(250)
                                    )
                                },
                                exitTransition = {
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Right,
                                        animationSpec = tween(250)
                                    )
                                }
                            ) { backStackEntry ->
                                val peerId = backStackEntry.arguments?.getString("peerId") ?: ""
                                ChatDetailScreen(
                                    peerId = peerId,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onStartVoiceCall = { peer ->
                                        viewModel.startVoiceCall(peer)
                                        if (currentRoute != "calls") {
                                            navController.navigate("calls") {
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    onStartVideoCall = { peer ->
                                        viewModel.startVideoCall(peer)
                                        if (currentRoute != "calls") {
                                            navController.navigate("calls") {
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    onStartScreenShare = { peer ->
                                        navController.navigate("screenshare?peerId=${peer.id}")
                                    }
                                )
                            }
                        }

                        // Two small square icon buttons floating at the top corners (menu on the left, settings on the right)
                        if (isTopLevelRoute && !showSplash) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CyberCornerIconButton(
                                    icon = Icons.Default.Menu,
                                    onClick = { scope.launch { drawerState.open() } },
                                    contentDescription = "Slide-out Menu",
                                    iconTint = CyberAccentCyan
                                )

                                CyberCornerIconButton(
                                    icon = Icons.Default.Settings,
                                    onClick = {
                                        if (currentRoute != "settings") {
                                            navController.navigate("settings") { launchSingleTop = true }
                                        }
                                    },
                                    contentDescription = "System Settings",
                                    iconTint = CyberAccentCyan
                                )
                            }
                        }
                    }
                }
            }

            // Full-screen Splash Overlay
            AnimatedVisibility(
                visible = showSplash,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(300))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CyberBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "PEERLINK",
                            style = TextStyle(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 4.sp,
                                color = CyberAccentCyan,
                                fontFamily = FontFamily.SansSerif
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "OFFLINE P2P MESH // ZERO TRACKING",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.8.sp,
                                color = CyberTextMuted,
                                fontFamily = FontFamily.SansSerif
                            )
                        )
                    }
                }
            }
        }
    }
}

private data class DrawerNavEntry(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@Composable
private fun DrawerItemRow(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) CyberCardElevated else Color.Transparent
    val textAndIcon = if (isSelected) CyberAccentCyan else CyberTextSecondary
    val border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberBorder) else null

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = border
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = textAndIcon,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textAndIcon,
                    fontFamily = FontFamily.SansSerif
                )
            )
        }
    }
}
