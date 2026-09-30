package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shizuku.ShizukuStatus
import com.example.ui.components.BottomTab
import com.example.ui.components.SystemFloatingBottomPillBar
import com.example.ui.components.SystemHeading
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AppManagerScreen
import com.example.ui.screens.DeviceManagerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.ProcessManagerScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShellTerminalScreen
import com.example.ui.screens.ShizukuScreen
import com.example.ui.screens.UpdateScreen
import com.example.ui.screens.WifiManagerScreen
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.SystemBg
import com.example.ui.theme.SystemBorder
import com.example.ui.theme.SystemCard
import com.example.ui.theme.SystemElevated
import com.example.ui.theme.SystemSurface
import com.example.ui.theme.SystemTextMuted
import com.example.ui.theme.SystemTextSecondary
import com.example.ui.theme.SystemTextWhite
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ScreenDestination {
    HOME,
    MSG,
    PROFILE,
    ABOUT,
    DEVICE,
    APPS,
    PROCESSES,
    TERMINAL,
    WIFI,
    SHIZUKU,
    SETTINGS,
    UPDATE
}

@Composable
fun SystemControllerApp(
    viewModel: MainViewModel
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var currentScreen by remember { mutableStateOf(ScreenDestination.HOME) }
    var currentBottomTab by remember { mutableStateOf(BottomTab.HOME) }
    var showSplash by remember { mutableStateOf(true) }

    val shizukuStatus by viewModel.shizukuManager.status.collectAsState()
    val isLowLatency by viewModel.shizukuManager.isLowLatencyEnabled.collectAsState()

    // Listen to toasts from ViewModel
    LaunchedEffect(Unit) {
        viewModel.uiToast.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Splash timeout: auto dismiss after 1.2s
    LaunchedEffect(Unit) {
        delay(1200)
        showSplash = false
    }

    // Back handling for secondary screens
    if (currentScreen != ScreenDestination.HOME) {
        BackHandler {
            currentScreen = ScreenDestination.HOME
            currentBottomTab = BottomTab.HOME
        }
    }

    // Splash screen overlay
    if (showSplash) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SystemBg)
                .clickable { showSplash = false },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(AccentCyan.copy(alpha = 0.15f))
                        .border(BorderStroke(2.dp, AccentCyan), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                SystemHeading(
                    text = "SYSTEM CONTROLLER",
                    fontSize = 26,
                    letterSpacing = 2.4,
                    color = AccentCyan
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "PRIVILEGED PERFORMANCE & SUBSYSTEM ENGINE",
                    color = SystemTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(30.dp))

                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = AccentCyan,
                    strokeWidth = 2.5.dp
                )
            }
        }
        return
    }

    // Main App with DrawerLayout & Floating Pill Bar
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SystemSurface,
                drawerContentColor = SystemTextWhite,
                modifier = Modifier
                    .width(300.dp)
                    .border(BorderStroke(1.dp, SystemBorder))
            ) {
                // Drawer Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SystemElevated)
                        .border(BorderStroke(1.dp, SystemBorder))
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AccentCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = SystemBg,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            SystemHeading("SYSTEM CONTROLLER", fontSize = 15, color = AccentCyan)
                            Text(
                                text = "Privileged Subsystem Deck",
                                color = SystemTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (shizukuStatus == ShizukuStatus.AUTHORIZED) AccentGreen else AccentAmber)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (shizukuStatus == ShizukuStatus.AUTHORIZED) "Shizuku Privileged (UID 2000)" else "Shizuku Standby",
                            color = if (shizukuStatus == ShizukuStatus.AUTHORIZED) AccentGreen else AccentAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Drawer Items as requested:
                // App Manager, Process Manager, Shell Terminal, WiFi Manager and Device, plus Game Booster, Settings, Update
                DrawerNavItem(
                    title = "App Manager",
                    icon = Icons.Default.Apps,
                    isSelected = currentScreen == ScreenDestination.APPS,
                    onClick = {
                        currentScreen = ScreenDestination.APPS
                        scope.launch { drawerState.close() }
                    }
                )

                DrawerNavItem(
                    title = "Process Manager",
                    icon = Icons.Default.Memory,
                    isSelected = currentScreen == ScreenDestination.PROCESSES,
                    onClick = {
                        currentScreen = ScreenDestination.PROCESSES
                        scope.launch { drawerState.close() }
                    }
                )

                DrawerNavItem(
                    title = "Shell Terminal",
                    icon = Icons.Default.Terminal,
                    isSelected = currentScreen == ScreenDestination.TERMINAL,
                    onClick = {
                        currentScreen = ScreenDestination.TERMINAL
                        scope.launch { drawerState.close() }
                    }
                )

                DrawerNavItem(
                    title = "WiFi Manager",
                    icon = Icons.Default.Wifi,
                    isSelected = currentScreen == ScreenDestination.WIFI,
                    onClick = {
                        currentScreen = ScreenDestination.WIFI
                        scope.launch { drawerState.close() }
                    }
                )

                DrawerNavItem(
                    title = "Device Manager",
                    icon = Icons.Default.PhoneAndroid,
                    isSelected = currentScreen == ScreenDestination.DEVICE,
                    onClick = {
                        currentScreen = ScreenDestination.DEVICE
                        scope.launch { drawerState.close() }
                    }
                )

                DrawerNavItem(
                    title = "Game Booster Deck",
                    icon = Icons.Default.SportsEsports,
                    isSelected = currentScreen == ScreenDestination.SHIZUKU,
                    onClick = {
                        currentScreen = ScreenDestination.SHIZUKU
                        scope.launch { drawerState.close() }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(SystemBorder)
                )
                Spacer(modifier = Modifier.height(8.dp))

                DrawerNavItem(
                    title = "Settings",
                    icon = Icons.Default.Settings,
                    isSelected = currentScreen == ScreenDestination.SETTINGS,
                    onClick = {
                        currentScreen = ScreenDestination.SETTINGS
                        scope.launch { drawerState.close() }
                    }
                )

                DrawerNavItem(
                    title = "System Update",
                    icon = Icons.Default.SystemUpdate,
                    isSelected = currentScreen == ScreenDestination.UPDATE,
                    onClick = {
                        currentScreen = ScreenDestination.UPDATE
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = SystemBg,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SystemSurface)
                        .border(BorderStroke(1.dp, SystemBorder))
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = AccentCyan)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        SystemHeading("SYSTEM CONTROLLER", fontSize = 16, color = SystemTextWhite)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (shizukuStatus == ShizukuStatus.AUTHORIZED) AccentGreen else AccentAmber)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = {
                            scope.launch {
                                val res = viewModel.shizukuManager.applyLowLatencyGamingMode()
                                viewModel.postToast(res.message)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Quick Boost",
                                tint = if (isLowLatency) AccentGreen else AccentCyan
                            )
                        }
                    }
                }
            },
            bottomBar = {
                // Floating bottom pill bar with four equal-width tabs:
                // HOME, MSG, PROFILE, ABOUT
                // Selected tab has elevated fill #1E2738 and cyan text #00E5FF.
                SystemFloatingBottomPillBar(
                    currentTab = currentBottomTab,
                    onTabSelected = { tab ->
                        currentBottomTab = tab
                        currentScreen = when (tab) {
                            BottomTab.HOME -> ScreenDestination.HOME
                            BottomTab.MSG -> ScreenDestination.MSG
                            BottomTab.PROFILE -> ScreenDestination.PROFILE
                            BottomTab.ABOUT -> ScreenDestination.ABOUT
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    ScreenDestination.HOME -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToDevice = { currentScreen = ScreenDestination.DEVICE },
                        onNavigateToTerminal = { currentScreen = ScreenDestination.TERMINAL },
                        onNavigateToWifi = { currentScreen = ScreenDestination.WIFI }
                    )
                    ScreenDestination.MSG -> MessagesScreen(viewModel = viewModel)
                    ScreenDestination.PROFILE -> ProfileScreen(viewModel = viewModel)
                    ScreenDestination.ABOUT -> AboutScreen()
                    ScreenDestination.DEVICE -> DeviceManagerScreen(viewModel = viewModel)
                    ScreenDestination.APPS -> AppManagerScreen(viewModel = viewModel)
                    ScreenDestination.PROCESSES -> ProcessManagerScreen(viewModel = viewModel)
                    ScreenDestination.TERMINAL -> ShellTerminalScreen(viewModel = viewModel)
                    ScreenDestination.WIFI -> WifiManagerScreen(viewModel = viewModel)
                    ScreenDestination.SHIZUKU -> ShizukuScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = ScreenDestination.HOME }
                    )
                    ScreenDestination.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    ScreenDestination.UPDATE -> UpdateScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun DrawerNavItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = title,
                color = if (isSelected) AccentCyan else SystemTextWhite,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp
            )
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) AccentCyan else SystemTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        },
        selected = isSelected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = SystemElevated,
            unselectedContainerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
    )
}
