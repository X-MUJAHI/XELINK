package com.example.ui.screens

import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamebooster.GameBoostProfile
import com.example.shizuku.ShizukuStatus
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberSectionHeader
import com.example.ui.components.CyberSwitch
import com.example.ui.theme.CyberAccentAmber
import com.example.ui.theme.CyberAccentCyan
import com.example.ui.theme.CyberAccentGreen
import com.example.ui.theme.CyberAccentPurple
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.util.StoragePermissionHelper
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun GameBoosterScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val boosterStatus by viewModel.gameBoosterManager.status.collectAsState()
    val logs by viewModel.gameBoosterManager.boosterLogs.collectAsState()
    val shizukuStatus by viewModel.shizukuManager.status.collectAsState()
    val shizukuInfo by viewModel.shizukuManager.info.collectAsState()

    var permissionTrigger by remember { mutableIntStateOf(0) }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionTrigger++
        viewModel.gameBoosterManager.refreshStorageAndFileStatus()
    }

    val manageStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        permissionTrigger++
        viewModel.gameBoosterManager.refreshStorageAndFileStatus()
    }

    val hasStorage = run {
        val triggerVal = permissionTrigger
        StoragePermissionHelper.hasStoragePermission(context)
    }

    LaunchedEffect(Unit) {
        viewModel.gameBoosterManager.refreshStorageAndFileStatus()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Banner
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(CyberAccentGreen.copy(alpha = 0.3f), CyberAccentCyan.copy(alpha = 0.2f))
                                )
                            )
                            .border(1.dp, CyberAccentGreen.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = "Game Booster",
                            tint = CyberAccentGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "SHIZUKU GAME BOOSTER",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                            color = CyberTextPrimary
                        )
                        Text(
                            text = "Non-root ADB privileged gaming engine",
                            fontSize = 12.sp,
                            color = CyberTextSecondary
                        )
                    }
                }

                CyberBadge(
                    text = if (boosterStatus.isActive) "BOOST ON" else "STANDBY",
                    tint = if (boosterStatus.isActive) CyberAccentGreen else CyberTextMuted
                )
            }
        }

        // 2. Storage Permission Warning Card (if missing)
        if (!hasStorage) {
            item {
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = CyberAccentAmber
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = CyberAccentAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FILE PERMISSION REQUIRED",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = CyberAccentAmber
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "To place 'game_booster.cfg' in your /Download folder and store AI models, PeerLink needs storage write access. Tap below to grant permission.",
                            fontSize = 12.sp,
                            color = CyberTextSecondary,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    val intent = StoragePermissionHelper.createManageStorageIntent(context)
                                    manageStorageLauncher.launch(intent)
                                } else {
                                    storagePermissionLauncher.launch(StoragePermissionHelper.getLegacyStoragePermissions())
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberAccentAmber),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Grant Storage / Download Permission",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. Main Boost Action Card
        item {
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (boosterStatus.isActive) CyberAccentGreen else CyberBorder
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (boosterStatus.isActive) "SYSTEM OPTIMIZED" else "READY TO BOOST",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (boosterStatus.isActive) CyberAccentGreen else CyberAccentCyan
                            )
                            Text(
                                text = "Target: ${boosterStatus.profile.targetFps} FPS • Vulkan Engine • Zero Throttling",
                                fontSize = 12.sp,
                                color = CyberTextSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (boosterStatus.isActive) CyberAccentGreen else CyberTextMuted,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                if (boosterStatus.isActive) {
                                    viewModel.gameBoosterManager.deactivateBooster()
                                } else {
                                    viewModel.gameBoosterManager.activateBooster()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (boosterStatus.isActive) Color(0xFFDC2626) else CyberAccentGreen
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = if (boosterStatus.isActive) Icons.Default.Close else Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (boosterStatus.isActive) Color.White else Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (boosterStatus.isActive) "DEACTIVATE GAME BOOSTER" else "ACTIVATE 120 FPS GAME BOOSTER",
                            color = if (boosterStatus.isActive) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    if (boosterStatus.lastActionMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = boosterStatus.lastActionMessage,
                            fontSize = 11.sp,
                            color = CyberTextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 4. Download Folder System Tuning File Card
        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = CyberAccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SYSTEM CONFIG IN /DOWNLOAD",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = CyberTextPrimary
                            )
                        }

                        CyberBadge(
                            text = if (boosterStatus.isConfigFilePlaced) "PLACED" else if (hasStorage) "READY" else "PERMISSION REQ",
                            tint = if (boosterStatus.isConfigFilePlaced) CyberAccentGreen else if (hasStorage) CyberAccentCyan else CyberAccentAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Writes tuning parameters directly to /storage/emulated/0/Download/game_booster.cfg to maximize frame pacing, Vulkan rendering, and touch response.",
                        fontSize = 12.sp,
                        color = CyberTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "File Path: /Download/game_booster.cfg",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberAccentCyan
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.gameBoosterManager.activateBooster()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = CyberAccentCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Write / Refresh Config", fontSize = 11.sp, color = CyberAccentCyan)
                        }
                    }
                }
            }
        }

        // 5. Privileged Shizuku Integration Card
        item {
            val isShizukuAuth = shizukuStatus == ShizukuStatus.AUTHORIZED
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (isShizukuAuth) CyberAccentGreen.copy(alpha = 0.6f) else CyberBorder
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (isShizukuAuth) CyberAccentGreen else CyberAccentAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SHIZUKU PRIVILEGED COMMANDS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = CyberTextPrimary
                            )
                        }

                        CyberBadge(
                            text = if (isShizukuAuth) "UID ${shizukuInfo.uid ?: 2000}" else "STANDBY",
                            tint = if (isShizukuAuth) CyberAccentGreen else CyberAccentAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Optimization items list
                    OptimizationRow(
                        title = "Thermal Throttling Bypass",
                        description = "cmd thermalservice override-status 0",
                        isActive = boosterStatus.thermalOverrideActive
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = CyberBorder.copy(alpha = 0.5f))

                    OptimizationRow(
                        title = "240Hz Touch Polling Rate",
                        description = "settings put secure high_touch_polling_rate_enabled 1",
                        isActive = boosterStatus.touchBoostActive
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = CyberBorder.copy(alpha = 0.5f))

                    OptimizationRow(
                        title = "Low-Latency Gaming Wi-Fi",
                        description = "cmd wifi set-low-latency-mode enabled",
                        isActive = boosterStatus.networkLowLatencyActive
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = CyberBorder.copy(alpha = 0.5f))

                    OptimizationRow(
                        title = "Background RAM Purge",
                        description = "am kill-all (Maximum memory for game)",
                        isActive = boosterStatus.ramPurged
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isShizukuAuth) {
                            Button(
                                onClick = { viewModel.shizukuManager.requestAuthorization() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberAccentCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Authorize Shizuku", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = { viewModel.shizukuManager.openShizukuApp() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = CyberTextSecondary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open Shizuku App", fontSize = 11.sp, color = CyberTextSecondary)
                        }
                    }
                }
            }
        }

        // 6. Profiles Selection Card
        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "GAMING PRESETS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = CyberTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    GameBoostProfile.values().forEach { profile ->
                        val isSelected = boosterStatus.profile == profile
                        Button(
                            onClick = {
                                scope.launch {
                                    if (boosterStatus.isActive) {
                                        viewModel.gameBoosterManager.activateBooster(profile)
                                    } else {
                                        viewModel.gameBoosterManager.activateBooster(profile)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) CyberAccentGreen.copy(alpha = 0.2f) else CyberSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) CyberAccentGreen else CyberBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = profile.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) CyberAccentGreen else CyberTextPrimary
                                    )
                                    Text(
                                        text = profile.description,
                                        fontSize = 11.sp,
                                        color = CyberTextSecondary
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = CyberAccentGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Booster Logs Console
        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "BOOSTER ACTIVITY CONSOLE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = CyberTextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF070A0F))
                            .padding(8.dp)
                    ) {
                        LazyColumn {
                            if (logs.isEmpty()) {
                                item {
                                    Text(
                                        text = "> Engine initialized. Tap 'Activate' to write config & boost.",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyberTextMuted
                                    )
                                }
                            } else {
                                items(logs.size) { index ->
                                    Text(
                                        text = "> ${logs[index]}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (logs[index].contains("OK") || logs[index].contains("ACTIVE")) CyberAccentGreen else CyberTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptimizationRow(
    title: String,
    description: String,
    isActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyberTextPrimary
            )
            Text(
                text = description,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = CyberTextMuted
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isActive) CyberAccentGreen.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isActive) "ACTIVE" else "READY",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) CyberAccentGreen else CyberTextMuted
            )
        }
    }
}
