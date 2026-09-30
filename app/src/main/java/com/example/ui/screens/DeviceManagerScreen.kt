package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SystemButton
import com.example.ui.components.SystemCard
import com.example.ui.components.SystemHeading
import com.example.ui.components.SystemSliderRow
import com.example.ui.components.SystemSubtitle
import com.example.ui.components.SystemToggleRow
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
fun DeviceManagerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var resolutionStr by remember { mutableStateOf("1080 x 2400 px") }
    var densityStr by remember { mutableStateOf("440 dpi") }
    var refreshRateStr by remember { mutableStateOf("120.0 Hz") }

    var displayScale by remember { mutableFloatStateOf(100f) }
    var touchSampleRate by remember { mutableFloatStateOf(240f) }
    var bufferSizeMb by remember { mutableFloatStateOf(16f) }

    var forcedPeakRefresh by remember { mutableStateOf(true) }
    var vulkanGameDriver by remember { mutableStateOf(true) }
    var wifiPowerSaveSuppress by remember { mutableStateOf(true) }
    var disableScanThrottling by remember { mutableStateOf(true) }
    var zeroLatencyTouch by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm?.defaultDisplay?.getRealMetrics(metrics)
            resolutionStr = "${metrics.widthPixels} x ${metrics.heightPixels} px"
            densityStr = "${metrics.densityDpi} dpi (${(metrics.density * 100).toInt()}%)"

            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display
            } else {
                @Suppress("DEPRECATION")
                wm?.defaultDisplay
            }
            refreshRateStr = "${display?.refreshRate ?: 120.0} Hz"
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
            SystemHeading("DEVICE MANAGER & HARDWARE CALIBRATION", fontSize = 20, color = AccentCyan)
            Spacer(modifier = Modifier.height(4.dp))
            SystemSubtitle("Display Resolution, DPI, Touch Latency & Kernel Parameters")
        }

        // Metrics Overview Card
        item {
            SystemCard {
                SystemHeading("DISPLAY & HARDWARE METRICS", fontSize = 14)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("RESOLUTION", color = SystemTextMuted, fontSize = 10.sp)
                        Text(resolutionStr, color = AccentCyan, fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                    Column {
                        Text("DENSITY / DPI", color = SystemTextMuted, fontSize = 10.sp)
                        Text(densityStr, color = AccentGreen, fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                    Column {
                        Text("REFRESH RATE", color = SystemTextMuted, fontSize = 10.sp)
                        Text(refreshRateStr, color = AccentPurple, fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }
        }

        // Sliders Card
        item {
            SystemCard {
                SystemHeading("PERFORMANCE & SCALING SLIDERS", fontSize = 14)
                Spacer(modifier = Modifier.height(8.dp))

                SystemSliderRow(
                    title = "Display Scaling Ratio",
                    valueLabel = "${displayScale.toInt()}%",
                    value = displayScale,
                    range = 80f..140f,
                    steps = 12,
                    onValueChange = { displayScale = it },
                    accentColor = AccentCyan
                )

                SystemSliderRow(
                    title = "Touch Sampling Poll Rate",
                    valueLabel = "${touchSampleRate.toInt()} Hz",
                    value = touchSampleRate,
                    range = 120f..480f,
                    steps = 6,
                    onValueChange = { touchSampleRate = it },
                    accentColor = AccentGreen
                )

                SystemSliderRow(
                    title = "P2P & Socket Direct Buffer",
                    valueLabel = "${bufferSizeMb.toInt()} MB",
                    value = bufferSizeMb,
                    range = 4f..32f,
                    steps = 7,
                    onValueChange = { bufferSizeMb = it },
                    accentColor = AccentPurple
                )
            }
        }

        // Toggles Card
        item {
            SystemCard {
                SystemHeading("SYSTEM TOGGLES & HARDWARE HOOKS", fontSize = 14)
                Spacer(modifier = Modifier.height(8.dp))

                SystemToggleRow(
                    title = "Forced 120Hz Peak Refresh Rate",
                    subtitle = "Applies settings put system peak_refresh_rate 120.0",
                    checked = forcedPeakRefresh,
                    onCheckedChange = { forcedPeakRefresh = it },
                    accentColor = AccentGreen
                )

                SystemToggleRow(
                    title = "Vulkan Game Driver Mode",
                    subtitle = "Forces GPU driver acceleration via game_driver_all_apps",
                    checked = vulkanGameDriver,
                    onCheckedChange = { vulkanGameDriver = it },
                    accentColor = AccentCyan
                )

                SystemToggleRow(
                    title = "Wi-Fi Power Save Suppression",
                    subtitle = "cmd wifi set-low-latency-mode enabled",
                    checked = wifiPowerSaveSuppress,
                    onCheckedChange = { wifiPowerSaveSuppress = it },
                    accentColor = AccentPurple
                )

                SystemToggleRow(
                    title = "Disable Scan Throttling",
                    subtitle = "Eliminates network packet ping spikes during competitive gaming",
                    checked = disableScanThrottling,
                    onCheckedChange = { disableScanThrottling = it },
                    accentColor = AccentGreen
                )

                SystemToggleRow(
                    title = "Zero-Latency Touch Mode",
                    subtitle = "Direct raw sensor dispatch without smoothing delay",
                    checked = zeroLatencyTouch,
                    onCheckedChange = { zeroLatencyTouch = it },
                    accentColor = AccentCyan
                )
            }
        }

        // Apply Button
        item {
            SystemButton(
                text = "APPLY CALIBRATION VIA SHIZUKU",
                onClick = {
                    scope.launch {
                        viewModel.shizukuManager.applyLowLatencyGamingMode()
                        viewModel.postToast("Device settings applied successfully: 120Hz Peak & ${bufferSizeMb.toInt()}MB Buffers Active")
                    }
                },
                accentColor = AccentGreen,
                textColor = SystemBg,
                icon = Icons.Default.Check
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
