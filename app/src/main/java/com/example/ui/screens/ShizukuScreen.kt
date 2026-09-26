package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shizuku.ShizukuStatus
import com.example.ui.components.GlassCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonEmerald
import com.example.viewmodel.MainViewModel
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
    val consoleLogs by viewModel.shizukuManager.consoleLog.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Top App Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Shizuku Game & Network Booster",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Optional Privileged Tuning & Safe System Fallbacks",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }

            // Status Card
            item {
                val (statusColor, statusTitle, statusDesc) = when (shizukuStatus) {
                    ShizukuStatus.AUTHORIZED -> Triple(
                        NeonEmerald,
                        "Shizuku Authorized & Connected",
                        "Privileged Android APIs are active. The app can optimize network latency and place performance configs directly."
                    )
                    ShizukuStatus.UNAUTHORIZED -> Triple(
                        AmberWarning,
                        "Shizuku Running (Not Authorized)",
                        "Shizuku is running on this device. Click 'Authorize Access' to grant privileged optimization capabilities."
                    )
                    ShizukuStatus.NOT_RUNNING -> Triple(
                        AmberWarning,
                        "Shizuku Service Not Running",
                        "Shizuku is installed, but the background service has not been started via wireless debugging / root."
                    )
                    ShizukuStatus.NOT_INSTALLED -> Triple(
                        Color.Gray,
                        "Shizuku Not Detected",
                        "Shizuku is optional. PeerLink works 100% normally without Shizuku using standard Android low-latency Wi-Fi APIs."
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
                                    imageVector = if (shizukuStatus == ShizukuStatus.AUTHORIZED) Icons.Default.CheckCircle else Icons.Default.Warning,
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
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (shizukuStatus == ShizukuStatus.UNAUTHORIZED) {
                                Button(
                                    onClick = { viewModel.shizukuManager.requestAuthorization() },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Authorize Access", color = Color(0xFF00363D))
                                }
                            }

                            FilledTonalButton(
                                onClick = { viewModel.shizukuManager.refreshStatus() },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Recheck")
                            }
                        }
                    }
                }
            }

            // Privileged Features Action Card
            item {
                GlassCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Performance Optimizations",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Actions leverage Shizuku when authorized, or gracefully fall back to standard Android APIs.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action 1: Wi-Fi Low Latency
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(CyberCyan.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = "Low-Latency Gaming Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(
                                        text = if (isLowLatency) "Active • Wi-Fi Power Save suppressed" else "Suppresses socket jitter & power throttling",
                                        fontSize = 11.sp,
                                        color = if (isLowLatency) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        val res = viewModel.shizukuManager.applyLowLatencyGamingMode()
                                        viewModel.postToast(res.message)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isLowLatency) NeonEmerald else CyberCyan),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(if (isLowLatency) "Active" else "Enable", color = if (isLowLatency) Color.White else Color(0xFF00363D), fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action 2: Place Game Booster Profile in Downloads folder
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                    Text(text = "Deploy Game Booster Profile", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(
                                        text = if (isProfilePlaced) "Deployed at Download/p2p_gaming_boost.cfg" else "Places tuning config in Download folder",
                                        fontSize = 11.sp,
                                        color = if (isProfilePlaced) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        val res = viewModel.shizukuManager.placeGameBoosterProfile()
                                        viewModel.postToast(res.message)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isProfilePlaced) NeonEmerald else ElectricViolet),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(if (isProfilePlaced) "Deployed" else "Deploy", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Reset Button
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val res = viewModel.shizukuManager.resetOptimizations()
                                    viewModel.postToast(res.message)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Optimizations to Defaults")
                        }
                    }
                }
            }

            // Console Log Card
            item {
                GlassCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Booster Diagnostic Console",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
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
                                .height(130.dp)
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
                                            color = CyberCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Help & Info Section
            item {
                GlassCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "How Shizuku Integration Works",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Shizuku allows Android apps to execute privileged system operations without requiring root access, via ADB / Wireless Debugging.\n\n" +
                                   "• In PeerLink, Shizuku is completely optional and only used to prevent Wi-Fi power throttling during intensive gaming/screen streaming, and to place optimal gaming scheduler profiles.\n\n" +
                                   "• If Shizuku is not running or authorization is denied, PeerLink seamlessly uses the official Android WifiManager.WIFI_MODE_FULL_LOW_LATENCY API.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
