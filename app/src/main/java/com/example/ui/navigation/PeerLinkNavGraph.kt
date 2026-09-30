package com.example.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.ScreenShare
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.components.GlassSurface
import com.example.ui.screens.CallsScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NearbyScreen
import com.example.ui.screens.ScreenShareScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShizukuScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassLevel
import com.example.ui.theme.LocalHazeState
import com.example.ui.theme.LocalUiThemeStyle
import com.example.ui.theme.UiThemeStyle
import com.example.viewmodel.MainViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

data class NavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun PeerLinkApp(
    viewModel: MainViewModel,
    navController: NavHostController = rememberNavController()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val callInfo by viewModel.callManager.callInfo.collectAsState()

    // Listen to toasts from ViewModel
    LaunchedEffect(Unit) {
        viewModel.uiToast.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Auto-navigate to Calls screen when incoming/outgoing call is triggered
    LaunchedEffect(callInfo?.callState) {
        val state = callInfo?.callState
        if (state == com.example.calling.CallState.INCOMING_RINGING || 
            state == com.example.calling.CallState.OUTGOING_RINGING) {
            if (currentRoute != "calls") {
                navController.navigate("calls") {
                    launchSingleTop = true
                }
            }
        }
    }

    val navItems = listOf(
        NavItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
        NavItem("nearby", "Nearby", Icons.Filled.Radar, Icons.Outlined.Radar),
        NavItem("chats", "Chats", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat),
        NavItem("calls", "Calls", Icons.Filled.Call, Icons.Outlined.Call),
        NavItem("screenshare", "Screen", Icons.Filled.ScreenShare, Icons.Outlined.ScreenShare),
        NavItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    val showBottomBar = currentRoute in navItems.map { it.route }
    val uiStyle = LocalUiThemeStyle.current
    val isGlassmorphism = uiStyle == UiThemeStyle.GLASSMORPHISM
    val hazeState = remember { HazeState() }

    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isGlassmorphism) {
                // Atmospheric luminous ambient mesh background for Glassmorphism
                // Haze samples this canvas to create genuine frosted / liquid glass blur across all layers!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(hazeState)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF060A14),
                                    Color(0xFF0B1224),
                                    Color(0xFF080D18)
                                )
                            )
                        )
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Orb 1: Luminous Cyber Cyan bloom (top-right)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    CyberCyan.copy(alpha = 0.40f),
                                    CyberCyan.copy(alpha = 0.16f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.88f, size.height * 0.10f),
                                radius = size.width * 0.72f
                            )
                        )
                        // Orb 2: Deep Electric Violet & Hot Magenta glow (mid-left)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFE040FB).copy(alpha = 0.32f),
                                    ElectricViolet.copy(alpha = 0.38f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.08f, size.height * 0.46f),
                                radius = size.width * 0.82f
                            )
                        )
                        // Orb 3: Radiant Aquamarine / Emerald accent (mid-right)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00E676).copy(alpha = 0.26f),
                                    Color(0xFF00B0FF).copy(alpha = 0.18f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.92f, size.height * 0.68f),
                                radius = size.width * 0.60f
                            )
                        )
                        // Orb 4: Deep Royal Purple / Indigo ambient foundation (bottom-center)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ElectricViolet.copy(alpha = 0.30f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.35f, size.height * 0.92f),
                                radius = size.width * 0.68f
                            )
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = if (isGlassmorphism) Color.Transparent else MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (showBottomBar) {
                        if (isGlassmorphism) {
                            // Floating detached glass navigation pill
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                GlassSurface(
                                    shape = RoundedCornerShape(26.dp),
                                    level = GlassLevel.LEVEL_2_STANDARD,
                                    borderWidth = 1.2.dp,
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        navItems.forEach { item ->
                                            val selected = currentRoute == item.route
                                            val interactionSource = remember { MutableInteractionSource() }
                                            val scale by animateFloatAsState(
                                                targetValue = if (selected) 1.05f else 1f,
                                                animationSpec = spring(stiffness = 500f, dampingRatio = 0.7f),
                                                label = "nav_scale"
                                            )
                                            val pillBg by animateColorAsState(
                                                targetValue = if (selected) CyberCyan.copy(alpha = 0.16f) else Color.Transparent,
                                                animationSpec = tween(200),
                                                label = "pill_bg"
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .scale(scale)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(pillBg)
                                                    .clickable(
                                                        interactionSource = interactionSource,
                                                        indication = null
                                                    ) {
                                                        if (currentRoute != item.route) {
                                                            navController.navigate(item.route) {
                                                                popUpTo("home") { saveState = true }
                                                                launchSingleTop = true
                                                                restoreState = true
                                                            }
                                                        }
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                        contentDescription = item.title,
                                                        tint = if (selected) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = item.title,
                                                        fontSize = 10.sp,
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (selected) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            NavigationBar(
                                containerColor = DarkSurface,
                                tonalElevation = 6.dp
                            ) {
                                navItems.forEach { item ->
                                    val selected = currentRoute == item.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            if (currentRoute != item.route) {
                                                navController.navigate(item.route) {
                                                    popUpTo("home") { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.title
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = item.title,
                                                fontSize = 11.sp
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color(0xFF00363D),
                                            indicatorColor = CyberCyan,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            selectedTextColor = CyberCyan,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToNearby = { navController.navigate("nearby") },
                    onNavigateToChats = { navController.navigate("chats") },
                    onNavigateToCalls = { navController.navigate("calls") },
                    onNavigateToScreenShare = { navController.navigate("screenshare") },
                    onNavigateToShizuku = { navController.navigate("shizuku") }
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
                    }
                )
            }

            composable("chats") {
                ChatsScreen(
                    viewModel = viewModel,
                    onOpenChat = { peerId ->
                        navController.navigate("chat_detail/$peerId")
                    },
                    onStartNewChat = { navController.navigate("nearby") }
                )
            }

            composable("calls") {
                CallsScreen(viewModel = viewModel)
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
                    initialTargetPeerId = peerId
                )
            }

            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToShizuku = { navController.navigate("shizuku") }
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
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(250))
                },
                exitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(250))
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
    }
}
}
}
