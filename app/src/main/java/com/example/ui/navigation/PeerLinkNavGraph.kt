package com.example.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import com.example.calling.CallState
import com.example.ui.components.LocalModernHazeState
import com.example.ui.components.ModernGlassLevel
import com.example.ui.components.ModernGlassSurface
import com.example.ui.screens.CallsScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NearbyScreen
import com.example.ui.screens.ScreenShareScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShizukuScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.LocalUiThemeStyle
import com.example.ui.theme.UiThemeStyle
import com.example.viewmodel.MainViewModel
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.hazeSource

private data class NavItem(
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
    val uiStyle = LocalUiThemeStyle.current
    val isModern = uiStyle == UiThemeStyle.MODERN
    val hazeState = rememberHazeState()

    LaunchedEffect(Unit) {
        viewModel.uiToast.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(callInfo?.callState) {
        val state = callInfo?.callState
        if (state == CallState.INCOMING_RINGING || state == CallState.OUTGOING_RINGING) {
            if (currentRoute != "calls") {
                navController.navigate("calls") { launchSingleTop = true }
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

    CompositionLocalProvider(LocalModernHazeState provides hazeState) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isModern) Modifier.hazeSource(hazeState) else Modifier)
        ) {
            AppAtmosphere(uiStyle = uiStyle)

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = if (isModern || uiStyle == UiThemeStyle.GLASSMORPHISM) {
                    Color.Transparent
                } else {
                    MaterialTheme.colorScheme.background
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (showBottomBar) {
                        if (isModern) {
                            ModernBottomBar(
                                navItems = navItems,
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    if (currentRoute != route) {
                                        navController.navigate(route) {
                                            popUpTo("home") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        } else {
                            LegacyBottomBar(
                                navItems = navItems,
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    if (currentRoute != route) {
                                        navController.navigate(route) {
                                            popUpTo("home") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                isGlassmorphism = uiStyle == UiThemeStyle.GLASSMORPHISM,
                            )
                        }
                    }
                }
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.padding(paddingValues),
                    enterTransition = {
                        if (isModern) {
                            fadeIn(tween(170)) + slideInHorizontally(tween(220)) { it / 7 }
                        } else {
                            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(250))
                        }
                    },
                    exitTransition = {
                        if (isModern) {
                            fadeOut(tween(130)) + slideOutHorizontally(tween(180)) { -it / 9 }
                        } else {
                            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(250))
                        }
                    }
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
                            onNavigateToChat = { peerId -> navController.navigate("chat_detail/$peerId") },
                            onNavigateToCalls = { navController.navigate("calls") },
                            onNavigateToScreenShare = { peerId -> navController.navigate("screenshare?peerId=$peerId") }
                        )
                    }
                    composable("chats") {
                        ChatsScreen(
                            viewModel = viewModel,
                            onOpenChat = { peerId -> navController.navigate("chat_detail/$peerId") },
                            onStartNewChat = { navController.navigate("nearby") }
                        )
                    }
                    composable("calls") { CallsScreen(viewModel = viewModel) }
                    composable(
                        route = "screenshare?peerId={peerId}",
                        arguments = listOf(navArgument("peerId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        })
                    ) { backStackEntry ->
                        ScreenShareScreen(
                            viewModel = viewModel,
                            initialTargetPeerId = backStackEntry.arguments?.getString("peerId")
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
                        arguments = listOf(navArgument("peerId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val peerId = backStackEntry.arguments?.getString("peerId") ?: ""
                        ChatDetailScreen(
                            peerId = peerId,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onStartVoiceCall = { peer ->
                                viewModel.startVoiceCall(peer)
                                if (currentRoute != "calls") navController.navigate("calls") { launchSingleTop = true }
                            },
                            onStartVideoCall = { peer ->
                                viewModel.startVideoCall(peer)
                                if (currentRoute != "calls") navController.navigate("calls") { launchSingleTop = true }
                            },
                            onStartScreenShare = { peer -> navController.navigate("screenshare?peerId=${peer.id}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernBottomBar(
    navItems: List<NavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        ModernGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            level = ModernGlassLevel.Elevated,
            shape = RoundedCornerShape(28.dp),
            contentPadding = 4.dp,
            tint = MaterialTheme.colorScheme.primary,
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0),
            ) {
                navItems.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onNavigate(item.route) },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                            )
                        },
                        label = { Text(item.title, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun LegacyBottomBar(
    navItems: List<NavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    isGlassmorphism: Boolean,
) {
    NavigationBar(
        containerColor = if (isGlassmorphism) Color(0xFF0B1324).copy(alpha = 0.58f) else DarkSurface,
        tonalElevation = 0.dp,
        modifier = if (isGlassmorphism) {
            Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent)
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
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title, fontSize = 11.sp) },
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

@Composable
private fun AppAtmosphere(uiStyle: UiThemeStyle) {
    val modern = uiStyle == UiThemeStyle.MODERN
    val glass = uiStyle == UiThemeStyle.GLASSMORPHISM
    val darkTheme = isSystemInDarkTheme()
    if (!modern && !glass) return

    val transition = rememberInfiniteTransition(label = "ambient_motion")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18_000), RepeatMode.Reverse),
        label = "ambient_drift",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (modern) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFEAF2FA).takeIf { !darkTheme } ?: Color(0xFF09111D),
                            Color(0xFFDCE8F4).takeIf { !darkTheme } ?: Color(0xFF101B2A),
                            Color(0xFFE9F0F6).takeIf { !darkTheme } ?: Color(0xFF0B1522),
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF060A14), Color(0xFF0B1224), Color(0xFF080D18))
                    )
                }
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (modern) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CyberCyan.copy(alpha = 0.20f), Color.Transparent),
                    ),
                    center = Offset(size.width * (0.82f + drift * 0.05f), size.height * 0.12f),
                    radius = size.width * 0.55f,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF9A84FF).copy(alpha = 0.15f), Color.Transparent),
                    ),
                    center = Offset(size.width * (0.16f - drift * 0.03f), size.height * 0.55f),
                    radius = size.width * 0.62f,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF8DD8BA).copy(alpha = 0.12f), Color.Transparent),
                    ),
                    center = Offset(size.width * 0.84f, size.height * 0.80f),
                    radius = size.width * 0.48f,
                )
            } else {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CyberCyan.copy(alpha = 0.38f), CyberCyan.copy(alpha = 0.15f), Color.Transparent),
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.10f),
                    radius = size.width * 0.70f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFE040FB).copy(alpha = 0.30f), ElectricViolet.copy(alpha = 0.36f), Color.Transparent),
                    ),
                    center = Offset(size.width * 0.08f, size.height * 0.46f),
                    radius = size.width * 0.80f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00E676).copy(alpha = 0.24f), Color(0xFF00B0FF).copy(alpha = 0.18f), Color.Transparent),
                    ),
                    center = Offset(size.width * 0.92f, size.height * 0.68f),
                    radius = size.width * 0.58f
                )
            }
        }
    }
}

