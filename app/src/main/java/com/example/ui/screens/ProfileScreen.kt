package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diagnostic.AppDiagnostics
import com.example.shizuku.ShizukuStatus
import com.example.ui.components.SystemButton
import com.example.ui.components.SystemCard
import com.example.ui.components.SystemHeading
import com.example.ui.components.SystemSubtitle
import com.example.ui.components.SystemToggleRow
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.SystemBg
import com.example.ui.theme.SystemBorder
import com.example.ui.theme.SystemElevated
import com.example.ui.theme.SystemTextMuted
import com.example.ui.theme.SystemTextSecondary
import com.example.ui.theme.SystemTextWhite
import com.example.viewmodel.MainViewModel

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shizukuStatus by viewModel.shizukuManager.status.collectAsState()
    val isWakeLock by viewModel.wakeLockManager.manualOverride.collectAsState()

    var autoBoostEnabled by remember { mutableStateOf(true) }
    var realTimeTelemetry by remember { mutableStateOf(true) }
    var thermalAlerts by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SystemBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            SystemHeading("OPERATOR PROFILE & PRIVILEGES", fontSize = 20, color = AccentCyan)
            Spacer(modifier = Modifier.height(4.dp))
            SystemSubtitle("System Controller Access Tier, Security Credential & Hardware Node")
        }

        // Operator ID Card
        item {
            SystemCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(AccentCyan.copy(alpha = 0.15f))
                            .border(BorderStroke(1.5.dp, AccentCyan), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SystemHeading(text = "SYS-OP-88219", fontSize = 16)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AccentGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "TIER-0",
                                    color = AccentGreen,
                                    fontSize = 10.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Access: Root / Shizuku Shell Privilege (UID 2000)",
                            color = SystemTextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Node ID: ${viewModel.deviceIdentity.deviceId.take(16)}...",
                            color = SystemTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Hardware Node Specs Card
        item {
            SystemCard {
                SystemHeading("DEVICE HARDWARE PROFILE", fontSize = 14)
                Spacer(modifier = Modifier.height(12.dp))

                val maxMemMb = (Runtime.getRuntime().maxMemory() / (1024 * 1024)).toInt()
                val totalMemMb = (Runtime.getRuntime().totalMemory() / (1024 * 1024)).toInt()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HardwareSpecRow("Hardware Model", "${Build.MANUFACTURER} ${Build.MODEL}")
                    HardwareSpecRow("Android OS Version", "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                    HardwareSpecRow("CPU Architecture", Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a")
                    HardwareSpecRow("JVM Allocated Heap", "$totalMemMb MB / $maxMemMb MB Max")
                    HardwareSpecRow("Kernel Shizuku Status", if (shizukuStatus == ShizukuStatus.AUTHORIZED) "Authorized (Active)" else "Standby")
                }
            }
        }

        // Privilege & Telemetry Toggles
        item {
            SystemCard {
                SystemHeading("AUTOMATION & PRIVILEGE PREFERENCES", fontSize = 14)
                Spacer(modifier = Modifier.height(8.dp))

                SystemToggleRow(
                    title = "Auto-Engage Game Booster",
                    subtitle = "Automatically deploy low-latency 120Hz config on launch",
                    checked = autoBoostEnabled,
                    onCheckedChange = { autoBoostEnabled = it },
                    accentColor = AccentGreen
                )

                SystemToggleRow(
                    title = "CPU WakeLock Hold",
                    subtitle = "Suppresses CPU sleep and prevents kernel task suspension",
                    checked = isWakeLock,
                    onCheckedChange = { viewModel.wakeLockManager.setManualWakeLock(it) },
                    accentColor = AccentPurple
                )

                SystemToggleRow(
                    title = "Real-Time Telemetry Dispatch",
                    subtitle = "Streams FPS, packet latency, and CPU metrics to message deck",
                    checked = realTimeTelemetry,
                    onCheckedChange = { realTimeTelemetry = it },
                    accentColor = AccentCyan
                )

                SystemToggleRow(
                    title = "Thermal Throttle Notification",
                    subtitle = "Alerts if battery temperature exceeds 42°C threshold",
                    checked = thermalAlerts,
                    onCheckedChange = { thermalAlerts = it },
                    accentColor = AccentAmber
                )
            }
        }

        // Export Actions
        item {
            SystemButton(
                text = "EXPORT DIAGNOSTIC REPORT",
                onClick = {
                    AppDiagnostics.copyReportToClipboard(context)
                    viewModel.postToast("Diagnostic telemetry report copied to clipboard")
                },
                accentColor = AccentCyan,
                textColor = SystemBg,
                icon = Icons.Default.Share
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun HardwareSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = SystemTextSecondary, fontSize = 12.sp)
        Text(text = value, color = SystemTextWhite, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
    }
}
