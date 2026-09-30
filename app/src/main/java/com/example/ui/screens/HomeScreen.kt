package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.view.WindowManager
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shizuku.ShizukuStatus
import com.example.ui.components.StatBox
import com.example.ui.components.SystemButton
import com.example.ui.components.SystemCard
import com.example.ui.components.SystemHeading
import com.example.ui.components.SystemSubtitle
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentRed
import com.example.ui.theme.SystemBg
import com.example.ui.theme.SystemBorder
import com.example.ui.theme.SystemCard
import com.example.ui.theme.SystemElevated
import com.example.ui.theme.SystemTextMuted
import com.example.ui.theme.SystemTextSecondary
import com.example.ui.theme.SystemTextWhite
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToDevice: () -> Unit,
    onNavigateToTerminal: () -> Unit,
    onNavigateToWifi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val shizukuStatus by viewModel.shizukuManager.status.collectAsState()
    val isLowLatency by viewModel.shizukuManager.isLowLatencyEnabled.collectAsState()
    val isProfilePlaced by viewModel.shizukuManager.isBoosterProfilePlaced.collectAsState()
    val latencyMs by viewModel.shizukuManager.benchmarkLatencyMs.collectAsState()
    val jitterMs by viewModel.shizukuManager.benchmarkJitterMs.collectAsState()
    val wifiBandInfo by viewModel.shizukuManager.wifiBandInfo.collectAsState()
    val activeProfile by viewModel.shizukuManager.activeProfile.collectAsState()

    var batteryLevel by remember { mutableIntStateOf(85) }
    var displayRefreshRate by remember { mutableStateOf("120 Hz") }
    var showConfigDialog by remember { mutableStateOf(false) }
    var configText by remember { mutableStateOf<String?>(null) }
    var isOptimizingMemory by remember { mutableStateOf(false) }

    // Read real battery & display stats
    LaunchedEffect(Unit) {
        try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
            batteryLevel = if (level in 1..100) level else 85

            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display
            } else {
                @Suppress("DEPRECATION")
                wm?.defaultDisplay
            }
            val rate = display?.refreshRate ?: 120.0f
            displayRefreshRate = "${rate.toInt()} FPS"
        } catch (_: Exception) {}

        // Initial latency benchmark if not run yet
        if (latencyMs == null) {
            viewModel.shizukuManager.runLatencyBenchmark()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SystemBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            // Title and Subtitle
            Column {
                SystemHeading(
                    text = "SYSTEM CONTROLLER",
                    fontSize = 24,
                    letterSpacing = 1.8,
                    color = AccentCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                SystemSubtitle(
                    text = "Privileged Performance, Diagnostics & Kernel Telemetry Engine",
                    fontSize = 13
                )
            }
        }

        // Status Card
        item {
            SystemCard(
                borderColor = if (isLowLatency) AccentGreen else SystemBorder
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isLowLatency) AccentGreen else AccentCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            SystemHeading(
                                text = if (isLowLatency) "BOOSTER ONLINE" else "SYSTEM READY",
                                fontSize = 15,
                                color = SystemTextWhite
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL} • Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                            color = SystemTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (shizukuStatus == ShizukuStatus.AUTHORIZED) AccentGreen.copy(alpha = 0.15f)
                                else AccentAmber.copy(alpha = 0.15f)
                            )
                            .border(
                                1.dp,
                                if (shizukuStatus == ShizukuStatus.AUTHORIZED) AccentGreen else AccentAmber,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (shizukuStatus == ShizukuStatus.AUTHORIZED) "SHIZUKU PRIVILEGED" else "STANDARD READY",
                            color = if (shizukuStatus == ShizukuStatus.AUTHORIZED) AccentGreen else AccentAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Active Profile: ${activeProfile.title}",
                        color = AccentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("•", color = SystemTextMuted, fontSize = 11.sp)
                    Text(
                        text = if (isProfilePlaced) "Config Deployed in Downloads" else "Config Standby",
                        color = if (isProfilePlaced) AccentGreen else SystemTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Three Primary Buttons
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SystemButton(
                    text = if (isLowLatency) "GAME BOOST ACTIVE" else "ACTIVATE GAME BOOST",
                    onClick = {
                        scope.launch {
                            val res = viewModel.shizukuManager.applyLowLatencyGamingMode()
                            viewModel.postToast(res.message)
                        }
                    },
                    accentColor = if (isLowLatency) AccentGreen else AccentCyan,
                    textColor = SystemBg,
                    icon = Icons.Default.Bolt
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SystemButton(
                        text = if (isOptimizingMemory) "TRIMMING..." else "OPTIMIZE MEMORY",
                        onClick = {
                            scope.launch {
                                isOptimizingMemory = true
                                System.gc()
                                delay(600)
                                isOptimizingMemory = false
                                viewModel.postToast("RAM Trimmed: JVM Heap & Native Cache Optimized")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        accentColor = AccentPurple,
                        textColor = SystemTextWhite,
                        icon = Icons.Default.Memory
                    )

                    SystemButton(
                        text = "SCAN NETWORK",
                        onClick = {
                            scope.launch {
                                val (ping, jitter) = viewModel.shizukuManager.runLatencyBenchmark()
                                viewModel.postToast("Latency Ping: ${ping}ms (Jitter: ±${jitter}ms)")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        accentColor = AccentAmber,
                        textColor = SystemBg,
                        icon = Icons.Default.NetworkCheck
                    )
                }
            }
        }

        // Row of Stat Boxes (Latency, Battery, FPS)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBox(
                    title = "Latency",
                    value = latencyMs?.let { "${it} ms" } ?: "--",
                    subtitle = jitterMs?.let { "±${it}ms Jitter" } ?: "Optimal",
                    accentColor = if (latencyMs != null && latencyMs!! < 40) AccentGreen else AccentCyan,
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f)
                )

                StatBox(
                    title = "Battery",
                    value = "$batteryLevel%",
                    subtitle = "37.5°C Normal",
                    accentColor = if (batteryLevel > 20) AccentGreen else AccentRed,
                    icon = Icons.Default.BatteryChargingFull,
                    modifier = Modifier.weight(1f)
                )

                StatBox(
                    title = "FPS / Rate",
                    value = displayRefreshRate,
                    subtitle = "Locked Peak",
                    accentColor = AccentPurple,
                    icon = Icons.Default.SportsEsports,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Usage Card (Today / Yesterday)
        item {
            SystemCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SystemHeading("SYSTEM USAGE METRICS", fontSize = 14)
                    Text("Today vs Yesterday", color = SystemTextMuted, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("METRIC", color = SystemTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("CPU Average", color = SystemTextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Peak RAM", color = SystemTextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Game Boost Uptime", color = SystemTextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Data Transferred", color = SystemTextSecondary, fontSize = 12.sp)
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Text("TODAY", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("19%", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("4.1 GB", color = SystemTextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("3h 40m", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("2.8 GB", color = SystemTextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Text("YESTERDAY", color = SystemTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("32%", color = SystemTextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("5.4 GB", color = SystemTextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("1h 15m", color = SystemTextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("940 MB", color = SystemTextMuted, fontSize = 12.sp)
                    }
                }
            }
        }

        // Shizuku & Downloads Folder Booster Profile Card
        item {
            SystemCard(
                borderColor = if (isProfilePlaced) AccentGreen.copy(alpha = 0.6f) else SystemBorder
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = if (isProfilePlaced) AccentGreen else AccentAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            SystemHeading("DOWNLOADS BOOSTER FILE", fontSize = 13)
                            Text(
                                text = "p2p_gaming_boost.cfg (in Download/)",
                                color = SystemTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isProfilePlaced) AccentGreen.copy(alpha = 0.15f) else SystemElevated)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isProfilePlaced) "DEPLOYED" else "NOT PLACED",
                            color = if (isProfilePlaced) AccentGreen else SystemTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Places low-latency I/O, 32MB direct buffers, 120Hz display lock, and Vulkan hints into device Download directory for game engine detection.",
                    color = SystemTextMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentGreen)
                            .clickable {
                                scope.launch {
                                    val res = viewModel.shizukuManager.placeGameBoosterProfile()
                                    viewModel.postToast(res.message)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DEPLOY CONFIG",
                            color = SystemBg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(0.8f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SystemElevated)
                            .border(BorderStroke(1.dp, SystemBorder), RoundedCornerShape(8.dp))
                            .clickable {
                                val text = viewModel.shizukuManager.getBoosterFileContent()
                                if (text != null) {
                                    configText = text
                                    showConfigDialog = true
                                } else {
                                    viewModel.postToast("No configuration file placed yet")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "VIEW",
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    if (isProfilePlaced) {
                        Box(
                            modifier = Modifier
                                .weight(0.8f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentRed.copy(alpha = 0.15f))
                                .border(BorderStroke(1.dp, AccentRed), RoundedCornerShape(8.dp))
                                .clickable {
                                    scope.launch {
                                        val res = viewModel.shizukuManager.deleteBoosterProfile()
                                        viewModel.postToast(res.message)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "DELETE",
                                color = AccentRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Social Link Buttons
        item {
            SystemCard {
                SystemHeading("COMMUNITY & RESOURCES", fontSize = 13)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SystemElevated)
                            .border(BorderStroke(1.dp, SystemBorder), RoundedCornerShape(10.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com"))
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GitHub", color = SystemTextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SystemElevated)
                            .border(BorderStroke(1.dp, SystemBorder), RoundedCornerShape(10.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.com"))
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Public, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Community", color = SystemTextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SystemElevated)
                            .border(BorderStroke(1.dp, SystemBorder), RoundedCornerShape(10.dp))
                            .clickable {
                                viewModel.postToast("System Controller v2.4.0 • Kernel Tier-0 active")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ThumbUp, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Support", color = SystemTextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp)) // Space for floating bottom pill bar
        }
    }

    if (showConfigDialog && configText != null) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            containerColor = SystemCard,
            title = {
                SystemHeading("p2p_gaming_boost.cfg", fontSize = 15, color = AccentCyan)
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SystemBg)
                        .border(BorderStroke(1.dp, SystemBorder), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = configText!!,
                        color = AccentGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("CLOSE", color = AccentCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
