package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diagnostic.AppDiagnostics
import com.example.shizuku.GamingProfile
import com.example.shizuku.ShizukuStatus
import com.example.ui.components.GlassCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.LocalUiThemeStyle
import com.example.ui.theme.UiThemeStyle
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun ShizukuScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shizukuStatus by viewModel.shizukuManager.status.collectAsState()
    val isLowLatency by viewModel.shizukuManager.isLowLatencyEnabled.collectAsState()
    val isProfilePlaced by viewModel.shizukuManager.isBoosterProfilePlaced.collectAsState()
    val activeProfile by viewModel.shizukuManager.activeProfile.collectAsState()
    val latencyMs by viewModel.shizukuManager.benchmarkLatencyMs.collectAsState()
    val jitterMs by viewModel.shizukuManager.benchmarkJitterMs.collectAsState()
    val isBenchmarking by viewModel.shizukuManager.isBenchmarking.collectAsState()
    val wifiBandInfo by viewModel.shizukuManager.wifiBandInfo.collectAsState()
    val consoleLogs by viewModel.shizukuManager.consoleLog.collectAsState()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showConfigDialog by remember { mutableStateOf(false) }
    var currentConfigFileContent by remember { mutableStateOf<String?>(null) }
    val isModern = LocalUiThemeStyle.current == UiThemeStyle.MODERN

    // Pulsing animation for active booster state
    val infiniteTransition = rememberInfiniteTransition(label = "BoosterPulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    // Periodic state check while screen is foregrounded
    LaunchedEffect(Unit) {
        while (isActive) {
            viewModel.shizukuManager.refreshStatus()
            delay(2500)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isModern) Color.Transparent else DarkBg)
    ) {
        // Futuristic Top App Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(0.5.dp, DarkBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyberCyan)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Game Booster & Shizuku Deck",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Privileged Kernel Jitter Reduction • 120Hz Peak • Safe Fallbacks",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        AppDiagnostics.copyReportToClipboard(
                            context,
                            mapOf(
                                "Shizuku Status" to shizukuStatus.name,
                                "Active Profile" to activeProfile.title,
                                "Booster Active" to "$isLowLatency",
                                "Config Placed" to "$isProfilePlaced",
                                "Wi-Fi Band" to wifiBandInfo,
                                "Ping" to (latencyMs?.let { "${it}ms" } ?: "N/A")
                            )
                        )
                        viewModel.postToast("Diagnostic report copied to clipboard")
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Report", tint = CyberCyan)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Live Telemetry & Ping Gauge HUD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = BorderStroke(
                        1.dp,
                        if (isLowLatency) NeonEmerald.copy(alpha = pulseGlow) else DarkBorder
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isLowLatency) NeonEmerald else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isLowLatency) "BOOSTER ACTIVE (${activeProfile.title})" else "GAMING ENGINE STANDBY",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLowLatency) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            FilledTonalButton(
                                onClick = {
                                    scope.launch {
                                        viewModel.shizukuManager.runLatencyBenchmark()
                                    }
                                },
                                enabled = !isBenchmarking,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isBenchmarking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = CyberCyan
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Testing...", fontSize = 11.sp)
                                } else {
                                    Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test Ping", fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3 Telemetry Metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Latency Metric
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkSurfaceVariant)
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("LATENCY", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = latencyMs?.let { "${it} ms" } ?: "-- ms",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = when {
                                            latencyMs == null -> MaterialTheme.colorScheme.onSurface
                                            latencyMs!! < 25 -> NeonEmerald
                                            latencyMs!! < 65 -> CyberCyan
                                            else -> AmberWarning
                                        }
                                    )
                                    Text(
                                        text = jitterMs?.let { "Jitter: ±${it}ms" } ?: "Tap Test Ping",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Wi-Fi Band & PHY Speed
                            Box(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkSurfaceVariant)
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("WI-FI PHY LINK", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = wifiBandInfo,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberCyan,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (isLowLatency) "Power-Save Suppressed" else "Standard Power-Save",
                                        fontSize = 10.sp,
                                        color = if (isLowLatency) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Buffer Window
                            Box(
                                modifier = Modifier
                                    .weight(0.9f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkSurfaceVariant)
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("BUFFER", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = activeProfile.bufferSize,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = ElectricViolet
                                    )
                                    Text(
                                        text = activeProfile.refreshRate,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Gaming Booster Profiles Selector
            item {
                GlassCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Performance Profile",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Customizes TCP/UDP socket buffers, screen refresh rates, and Android task priority.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        GamingProfile.values().forEach { profile ->
                            val isSelected = activeProfile == profile
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.shizukuManager.setGamingProfile(profile)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) DarkSurfaceVariant else Color(0xFF0F1626)
                                ),
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 0.5.dp,
                                    if (isSelected) CyberCyan else DarkBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, if (isSelected) CyberCyan else DarkBorder, CircleShape)
                                            .padding(3.dp)
                                    ) {
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                                    .background(CyberCyan)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = profile.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (isSelected) CyberCyan else MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "• ${profile.refreshRate}",
                                                fontSize = 11.sp,
                                                color = ElectricViolet,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = profile.subtitle,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Big Action Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val res = viewModel.shizukuManager.applyLowLatencyGamingMode()
                                        viewModel.postToast(res.message)
                                        viewModel.shizukuManager.runLatencyBenchmark()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLowLatency) NeonEmerald else CyberCyan
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLowLatency) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color(0xFF00363D),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isLowLatency) "Re-Apply Boost" else "Engage Game Boost",
                                    color = Color(0xFF00363D),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }

                            if (isLowLatency || isProfilePlaced) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            val res = viewModel.shizukuManager.resetOptimizations()
                                            viewModel.postToast(res.message)
                                        }
                                    },
                                    modifier = Modifier.height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.7f))
                                ) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = CrimsonError, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset", color = CrimsonError, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Download Folder Performance Profile Card
            item {
                GlassCard(
                    borderColor = if (isProfilePlaced) NeonEmerald.copy(alpha = 0.7f) else DarkBorder
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(ElectricViolet.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "System Profile File (Downloads)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Download/p2p_gaming_boost.cfg",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyberCyan
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isProfilePlaced) NeonEmerald.copy(alpha = 0.2f) else DarkSurfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isProfilePlaced) "DEPLOYED" else "NOT PLACED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isProfilePlaced) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Places small system optimization configuration in Download folder for game performance enhancers, custom ROM game drivers, and low-latency P2P mesh.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val res = viewModel.shizukuManager.placeGameBoosterProfile()
                                        viewModel.postToast(res.message)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isProfilePlaced) "Re-Deploy File" else "Deploy to Downloads", fontSize = 12.sp)
                            }

                            if (isProfilePlaced) {
                                FilledTonalButton(
                                    onClick = {
                                        currentConfigFileContent = viewModel.shizukuManager.getBoosterFileContent()
                                        showConfigDialog = true
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Inspect", fontSize = 12.sp)
                                }

                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            val res = viewModel.shizukuManager.deleteBoosterProfile()
                                            viewModel.postToast(res.message)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonError)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Shizuku Connection & Privilege Status Card
            item {
                val (statusColor, statusTitle, statusDesc) = when (shizukuStatus) {
                    ShizukuStatus.AUTHORIZED -> Triple(
                        NeonEmerald,
                        "Shizuku Authorized & Connected",
                        "Privileged Android APIs are active. Direct system commands (`cmd wifi`, display refresh rate, Wi-Fi sleep override) are executed with ADB privileges."
                    )
                    ShizukuStatus.UNAUTHORIZED -> Triple(
                        AmberWarning,
                        "Shizuku Running (Needs Permission)",
                        "Shizuku is running on this device. Click 'Grant Permission' below to authorize privileged optimizations."
                    )
                    ShizukuStatus.NOT_RUNNING -> Triple(
                        AmberWarning,
                        "Shizuku Service Not Running",
                        "Shizuku app is present, but the background service has not been started. Start it via Wireless Debugging in the Shizuku app."
                    )
                    ShizukuStatus.NOT_INSTALLED -> Triple(
                        Color.Gray,
                        "Shizuku Not Detected (Safe Fallback)",
                        "PeerLink is running with official Android WifiManager.WIFI_MODE_FULL_LOW_LATENCY. Shizuku is completely optional!"
                    )
                }

                GlassCard(borderColor = statusColor.copy(alpha = 0.8f)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(statusColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (shizukuStatus == ShizukuStatus.AUTHORIZED) Icons.Default.CheckCircle else Icons.Default.Security,
                                    contentDescription = null,
                                    tint = statusColor
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = statusTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = statusColor
                                )
                                Text(
                                    text = "Status: ${shizukuStatus.name}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = statusDesc,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (shizukuStatus == ShizukuStatus.UNAUTHORIZED || shizukuStatus == ShizukuStatus.NOT_RUNNING) {
                                Button(
                                    onClick = { viewModel.shizukuManager.requestAuthorization() },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF00363D))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Grant Permission", color = Color(0xFF00363D), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            FilledTonalButton(
                                onClick = { viewModel.shizukuManager.openShizukuApp() },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Open Shizuku", fontSize = 12.sp)
                            }

                            FilledTonalButton(
                                onClick = { viewModel.shizukuManager.refreshStatus() },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Recheck", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 5. Live Booster Terminal & Telemetry Log
            item {
                GlassCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Kernel & Telemetry Console",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = "${consoleLogs.size} events",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF070B14))
                                .padding(8.dp)
                        ) {
                            if (consoleLogs.isEmpty()) {
                                Text(
                                    text = "Ready. Operations and telemetry logs will display here.",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(consoleLogs.reversed()) { log ->
                                        Text(
                                            text = log,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = when {
                                                log.contains("error", ignoreCase = true) || log.contains("failed", ignoreCase = true) -> CrimsonError
                                                log.contains("Active", ignoreCase = true) || log.contains("authorized", ignoreCase = true) -> NeonEmerald
                                                else -> CyberCyan
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Educational Guide: How Shizuku & Fallbacks Work
            item {
                GlassCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "How Shizuku & Safe Fallbacks Work",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Shizuku allows Android apps to execute privileged system operations without requiring root access, via Wireless Debugging or ADB.\n\n" +
                                   "• When Shizuku is authorized, PeerLink sets `cmd wifi set-low-latency-mode enabled`, suppresses Wi-Fi scan throttling, forces peak 120Hz display refresh rates, and allocates huge 16-32MB socket windows.\n\n" +
                                   "• In addition, PeerLink places an optimized performance configuration file in your Download directory (`p2p_gaming_boost.cfg`) for external game enhancers and direct I/O tuning.\n\n" +
                                   "• If Shizuku is not running on your phone, PeerLink automatically activates Android's official `WifiManager.WIFI_MODE_FULL_LOW_LATENCY` lock with 100% stability.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Inspect Configuration File Modal Dialog
    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = CyberCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("p2p_gaming_boost.cfg", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF070B14))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = currentConfigFileContent ?: "File not found or empty.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = CyberCyan
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentConfigFileContent?.let {
                            clipboardManager.setText(AnnotatedString(it))
                            viewModel.postToast("Config copied to clipboard")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF00363D), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
