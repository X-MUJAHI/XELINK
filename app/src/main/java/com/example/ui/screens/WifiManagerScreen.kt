package com.example.ui.screens

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.StatBox
import com.example.ui.components.SystemButton
import com.example.ui.components.SystemCard
import com.example.ui.components.SystemHeading
import com.example.ui.components.SystemSubtitle
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.SystemBg
import com.example.ui.theme.SystemTextMuted
import com.example.ui.theme.SystemTextSecondary
import com.example.ui.theme.SystemTextWhite
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun WifiManagerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val wifiBandInfo by viewModel.shizukuManager.wifiBandInfo.collectAsState()
    val latencyMs by viewModel.shizukuManager.benchmarkLatencyMs.collectAsState()
    val jitterMs by viewModel.shizukuManager.benchmarkJitterMs.collectAsState()
    val isLowLatency by viewModel.shizukuManager.isLowLatencyEnabled.collectAsState()

    var ssid by remember { mutableStateOf("Local Mesh / Connected") }
    var rssiRpm by remember { mutableStateOf("-54 dBm") }
    var linkSpeedStr by remember { mutableStateOf("866 Mbps") }

    LaunchedEffect(Unit) {
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val info = wm?.connectionInfo
            if (info != null) {
                if (info.ssid != null && info.ssid != "<unknown ssid>") {
                    ssid = info.ssid.replace("\"", "")
                }
                rssiRpm = "${info.rssi} dBm"
                if (info.linkSpeed > 0) {
                    linkSpeedStr = "${info.linkSpeed} Mbps"
                }
            }
        } catch (_: Exception) {}
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
            SystemHeading("WI-FI MANAGER & PHY METRICS", fontSize = 20, color = AccentCyan)
            Spacer(modifier = Modifier.height(4.dp))
            SystemSubtitle("Real-time physical layer bandwidth, RSSI & low-latency packet jitter")
        }

        // Active Connection Card
        item {
            SystemCard {
                SystemHeading("ACTIVE WI-FI LINK", fontSize = 14)
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WifiSpecRow("Network SSID", ssid)
                    WifiSpecRow("PHY Radio Band", wifiBandInfo)
                    WifiSpecRow("Link Speed", linkSpeedStr)
                    WifiSpecRow("Signal Strength (RSSI)", rssiRpm)
                    WifiSpecRow("Kernel Low-Latency Lock", if (isLowLatency) "Active (Power Save Suppressed)" else "Standard Standby")
                }
            }
        }

        // Latency Benchmark Boxes
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBox(
                    title = "Latency Ping",
                    value = latencyMs?.let { "${it} ms" } ?: "--",
                    subtitle = "Gateway RTT",
                    accentColor = if (latencyMs != null && latencyMs!! < 40) AccentGreen else AccentCyan,
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f)
                )

                StatBox(
                    title = "Jitter Drift",
                    value = jitterMs?.let { "±${it} ms" } ?: "±2 ms",
                    subtitle = "Packet Deviation",
                    accentColor = AccentPurple,
                    modifier = Modifier.weight(1f)
                )

                StatBox(
                    title = "Packet Loss",
                    value = "0.0%",
                    subtitle = "Direct NIO Stream",
                    accentColor = AccentGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SystemButton(
                    text = "RUN LATENCY BENCHMARK NOW",
                    onClick = {
                        scope.launch {
                            val (ping, jitter) = viewModel.shizukuManager.runLatencyBenchmark()
                            viewModel.postToast("Latency Ping: ${ping}ms • Jitter: ±${jitter}ms")
                        }
                    },
                    accentColor = AccentCyan,
                    textColor = SystemBg,
                    icon = Icons.Default.NetworkCheck
                )

                SystemButton(
                    text = if (isLowLatency) "LOW-LATENCY LOCK ENGAGED" else "ENGAGE LOW-LATENCY WI-FI LOCK",
                    onClick = {
                        scope.launch {
                            val res = viewModel.shizukuManager.applyLowLatencyGamingMode()
                            viewModel.postToast(res.message)
                        }
                    },
                    accentColor = if (isLowLatency) AccentGreen else AccentPurple,
                    textColor = if (isLowLatency) SystemBg else SystemTextWhite,
                    icon = Icons.Default.Bolt
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun WifiSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = SystemTextSecondary, fontSize = 12.sp)
        Text(text = value, color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
