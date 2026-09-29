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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import com.example.ui.theme.LocalUiThemeStyle
import com.example.ui.theme.UiThemeStyle
import com.example.viewmodel.MainViewModel

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

    Box(modifier = Modifier.fillMaxSize()) {
        if (isGlassmorphism) {
            // Atmospheric luminous ambient mesh background for Glassmorphism
            // Glass requires vibrant, multi-hued light sources underneath to shine and refract through cards!
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                                CyberCyan.copy(alpha = 0.38f),
                                CyberCyan.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.88f, size.height * 0.10f),
                            radius = size.width * 0.70f
                        )
                    )
                    // Orb 2: Deep Electric Violet & Hot Magenta glow (mid-left)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFE040FB).copy(alpha = 0.30f),
                                ElectricViolet.copy(alpha = 0.36f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.08f, size.height * 0.46f),
                            radius = size.width * 0.80f
                        )
                    )
                    // Orb 3: Radiant Aquamarine / Emerald accent (mid-right)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00E676).copy(alpha = 0.24f),
                                Color(0xFF00B0FF).copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.92f, size.height * 0.68f),
                            radius = size.width * 0.58f
                        )
                    )
                    // Orb 4: Deep Royal Purple / Indigo ambient foundation (bottom-center)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ElectricViolet.copy(alpha = 0.28f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.35f, size.height * 0.92f),
                            radius = size.width * 0.65f
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
                    NavigationBar(
                        containerColor = if (isGlassmorphism) Color(0xFF0B1324).copy(alpha = 0.58f) else DarkSurface,
                        tonalElevation = 0.dp,
                        modifier = if (isGlassmorphism) {
                            Modifier
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.10f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .border(
                                    BorderStroke(
                                        1.5.dp,
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.70f),
                                                CyberCyan.copy(alpha = 0.60f),
                                                ElectricViolet.copy(alpha = 0.60f),
                                                Color.White.copy(alpha = 0.30f)
                                            )
                                        )
                                    )
                                )
                        } else {
                            Modifier
                        }
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
