package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StopScreenShare
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import com.example.diagnostic.AppDiagnostics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.calling.CallState
import androidx.compose.foundation.BorderStroke
import com.example.shizuku.ShizukuStatus
import com.example.ui.components.BadgeType
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberDestructiveButton
import com.example.ui.components.CyberPrimaryButton
import com.example.ui.components.CyberSecondaryButton
import com.example.ui.components.CyberSectionHeader
import com.example.ui.components.CyberStatBoxes
import com.example.ui.components.CyberStatItem
import com.example.ui.components.CyberStatusIndicator
import com.example.ui.components.CyberSwitch
import com.example.ui.components.GlassCard
import com.example.ui.components.PeerDeviceItem
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberAccentAmber
import com.example.ui.theme.CyberAccentCyan
import com.example.ui.theme.CyberAccentGreen
import com.example.ui.theme.CyberAccentPurple
import com.example.ui.theme.CyberAccentRed
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.LocalUiThemeStyle
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.UiThemeStyle
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToNearby: () -> Unit,
    onNavigateToChats: () -> Unit,
    onNavigateToCalls: () -> Unit,
    onNavigateToScreenShare: () -> Unit,
    onNavigateToShizuku: () -> Unit,
    modifier: Modifier = Modifier
) {
    val localIp by viewModel.localIp.collectAsState()
    val isBroadcasting by viewModel.isBroadcasting.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val callInfo by viewModel.callManager.callInfo.collectAsState()
    val isSharingScreen by viewModel.screenShareManager.isSharing.collectAsState()
    val remoteScreenBitmap by viewModel.screenShareManager.remoteScreenBitmap.collectAsState()
    val shizukuStatus by viewModel.shizukuManager.status.collectAsState()
    val isLowLatency by viewModel.shizukuManager.isLowLatencyEnabled.collectAsState()
    val activeProfile by viewModel.shizukuManager.activeProfile.collectAsState()
    val wifiBandInfo by viewModel.shizukuManager.wifiBandInfo.collectAsState()
    val latencyMs by viewModel.shizukuManager.benchmarkLatencyMs.collectAsState()
    val isProfilePlaced by viewModel.shizukuManager.isBoosterProfilePlaced.collectAsState()
    val isWakeLockActive by viewModel.wakeLockManager.isWakeLockActive.collectAsState()
    val isManualWakeLock by viewModel.wakeLockManager.manualOverride.collectAsState()
    val transfers by viewModel.fileTransferManager.transfers.collectAsState()
    val lastCrash by AppDiagnostics.lastCrashMessage.collectAsState()

    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Crash Warning Alert Banner (if previous session crashed)
        if (lastCrash != null) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                GlassCard(borderColor = CrimsonError) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = CrimsonError, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Previous Session Crash Log Detected", fontWeight = FontWeight.Bold, color = CrimsonError, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "A crash or auto-exit was detected in the last session. You can copy the full stack trace and send it to the developer.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    AppDiagnostics.copyReportToClipboard(context)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonError)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Crash Log")
                            }
                            FilledTonalButton(
                                onClick = { AppDiagnostics.clearSavedCrashLog(context) }
                            ) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            val uiStyle = LocalUiThemeStyle.current
            val isGlass = uiStyle == UiThemeStyle.GLASSMORPHISM
            val isModern = uiStyle == UiThemeStyle.MODERN
            val isTranslucent = isGlass || isModern
            // Hero Banner Card with generated illustration
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isTranslucent) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = if (isTranslucent) {
                    BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.85f),
                                CyberCyan.copy(alpha = 0.70f),
                                ElectricViolet.copy(alpha = 0.50f),
                                Color.White.copy(alpha = 0.20f)
                            )
                        )
                    )
                } else null,
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.p2p_mesh_banner_1790448401406),
                        contentDescription = "P2P Mesh Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = if (isGlass) {
                                        listOf(
                                            Color.White.copy(alpha = 0.08f),
                                            Color(0xDD070B16)
                                        )
                                    } else {
                                        listOf(Color.Transparent, Color(0xDD0A0F1D))
                                    }
                                )
                            )
                    )
                    if (isGlass) {
                        // Specular top highlight line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.5.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.85f),
                                            CyberCyan.copy(alpha = 0.70f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PeerLink Direct",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusBadge(type = BadgeType.ENCRYPTED)
                        }
                        Text(
                            text = "Offline local mesh • No Internet or cloud required",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }
        }

        // Stat boxes: a row of equal-width small tiles
        item {
            val stats = listOf(
                CyberStatItem(
                    label = "LOCAL IP",
                    value = if (localIp.isNotEmpty()) localIp.substringAfterLast('.') else "OFFLINE",
                    valueColor = CyberAccentCyan,
                    subtext = localIp.ifEmpty { "127.0.0.1" }
                ),
                CyberStatItem(
                    label = "PEERS",
                    value = "${discoveredDevices.size}",
                    valueColor = CyberAccentGreen,
                    subtext = if (isScanning) "SCANNING" else "STANDBY"
                ),
                CyberStatItem(
                    label = "PORT",
                    value = "${viewModel.transportManager.serverPort}",
                    valueColor = CyberAccentPurple,
                    subtext = "TCP DIRECT"
                ),
                CyberStatItem(
                    label = "BOOSTER",
                    value = if (isLowLatency) "120HZ" else "STD",
                    valueColor = if (isLowLatency) CyberAccentGreen else CyberTextSecondary,
                    subtext = activeProfile.title.take(7)
                )
            )
            CyberStatBoxes(stats = stats)
        }

        // Active Call Banner (if any)
        if (callInfo != null) {
            item {
                val call = callInfo!!
                GlassCard(
                    borderColor = if (call.callState == CallState.INCOMING_RINGING) CrimsonError else CyberCyan
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (call.callState == CallState.INCOMING_RINGING) CrimsonError.copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (call.callType == com.example.calling.CallType.VIDEO) Icons.Default.Videocam else Icons.Default.Call,
                                    contentDescription = "Call",
                                    tint = if (call.callState == CallState.INCOMING_RINGING) CrimsonError else CyberCyan
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (call.callState == CallState.INCOMING_RINGING) "Incoming ${call.callType} Call" else "Active ${call.callType} Call",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${call.peerName} • ${if (call.callState == CallState.CONNECTED) "${call.durationSeconds}s" else "Ringing..."}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (call.callState == CallState.INCOMING_RINGING) {
                                Button(
                                    onClick = {
                                        viewModel.callManager.acceptCall()
                                        onNavigateToCalls()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald)
                                ) {
                                    Text("Accept")
                                }
                                Button(
                                    onClick = { viewModel.callManager.declineCall() },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonError)
                                ) {
                                    Text("Decline")
                                }
                            } else {
                                Button(
                                    onClick = onNavigateToCalls,
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                                ) {
                                    Text("View", color = Color(0xFF00363D))
                                }
                                IconButton(onClick = { viewModel.callManager.endCall() }) {
                                    Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = CrimsonError)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Screen Share Banner
        if (isSharingScreen || remoteScreenBitmap != null) {
            item {
                GlassCard(borderColor = ElectricViolet) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ScreenShare, contentDescription = "Screen Sharing", tint = ElectricViolet)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isSharingScreen) "Transmitting Screen Share" else "Viewing Remote Screen",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (isSharingScreen) "Streaming live VirtualDisplay to peer" else "Incoming live screen stream active",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        FilledTonalButton(onClick = onNavigateToScreenShare) {
                            Text("Open")
                        }
                    }
                }
            }
        }

        // Active High-Speed Multiplexed File Transfers
        val activeTransfers = transfers.values.filter { !it.isComplete }
        if (activeTransfers.isNotEmpty()) {
            item {
                GlassCard(borderColor = CyberCyan) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Sync, contentDescription = "Active Transfer", tint = CyberCyan)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "High-Speed File Sharing Active",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${activeTransfers.size} transfer(s) • 4x Multiplexed NIO",
                                        fontSize = 12.sp,
                                        color = CyberCyan
                                    )
                                }
                            }
                            FilledTonalButton(onClick = onNavigateToChats) {
                                Text("Chats")
                            }
                        }
                        activeTransfers.forEach { transfer ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = transfer.fileName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${(transfer.progressPercent * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { transfer.progressPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = CyberCyan,
                                trackColor = DarkBorder
                            )
                            if (transfer.speedFormatted.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = transfer.speedFormatted,
                                    fontSize = 11.sp,
                                    color = CyberCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Local Device Identity & Network Node Card
        item {
            GlassCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = viewModel.deviceIdentity.deviceName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Local Node: $localIp : ${viewModel.transportManager.serverPort}",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan
                            )
                        }
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString("$localIp:${viewModel.transportManager.serverPort}"))
                            viewModel.postToast("IP & Port copied to clipboard")
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy IP", modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = "Key Fingerprint", tint = ElectricViolet, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Key Fingerprint: ${viewModel.deviceIdentity.keyFingerprint}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Radio Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Radar, contentDescription = "Discovery", tint = CyberCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Nearby Scanner", fontSize = 13.sp)
                        }
                        CyberSwitch(
                            checked = isScanning,
                            onCheckedChange = { if (it) viewModel.transportManager.startDiscovery() else viewModel.transportManager.stopDiscovery() },
                            accentColor = CyberAccentCyan
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NetworkCheck, contentDescription = "Broadcast", tint = NeonEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Service Broadcast (mDNS)", fontSize = 13.sp)
                        }
                        CyberSwitch(
                            checked = isBroadcasting,
                            onCheckedChange = { if (it) viewModel.transportManager.startBroadcast() else viewModel.transportManager.stopBroadcast() },
                            accentColor = CyberAccentGreen
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Power,
                                contentDescription = "WakeLock",
                                tint = if (isWakeLockActive) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(text = "CPU WakeLock", fontSize = 13.sp)
                                Text(
                                    text = if (isWakeLockActive) "Active (No Sleep)" else "Standby",
                                    fontSize = 10.sp,
                                    color = if (isWakeLockActive) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        CyberSwitch(
                            checked = isManualWakeLock,
                            onCheckedChange = { viewModel.wakeLockManager.setManualWakeLock(it) },
                            accentColor = CyberAccentGreen
                        )
                    }
                }
            }
        }

        // Shizuku & Game Booster Quick Card
        item {
            GlassCard(
                borderColor = if (isLowLatency) NeonEmerald.copy(alpha = 0.8f) else DarkBorder
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isLowLatency) NeonEmerald.copy(alpha = 0.2f) else ElectricViolet.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Booster",
                                    tint = if (isLowLatency) NeonEmerald else ElectricViolet
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Game & Stream Booster",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ${activeProfile.title}",
                                        fontSize = 11.sp,
                                        color = CyberCyan,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = if (isLowLatency) "Low-Latency Active • 120Hz Peak • Wi-Fi Power Save OFF" else "Privileged Shizuku & Wi-Fi Low-Latency Engine",
                                    fontSize = 11.sp,
                                    color = if (isLowLatency) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        StatusBadge(
                            type = when (shizukuStatus) {
                                ShizukuStatus.AUTHORIZED -> BadgeType.SHIZUKU_ACTIVE
                                else -> BadgeType.SHIZUKU_INACTIVE
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Telemetry mini-chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Column {
                                Text("WI-FI PHY", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text(wifiBandInfo, fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(0.7f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Column {
                                Text("PING", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text(latencyMs?.let { "${it}ms" } ?: "--", fontSize = 11.sp, color = if (latencyMs != null && latencyMs!! < 40) NeonEmerald else CyberCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Column {
                                Text("CONFIG FILE", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text(if (isProfilePlaced) "Deployed" else "Standby", fontSize = 11.sp, color = if (isProfilePlaced) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CyberPrimaryButton(
                            text = if (isLowLatency) "BOOST ACTIVE" else "ACTIVATE BOOST",
                            onClick = {
                                scope.launch {
                                    val res = viewModel.shizukuManager.applyLowLatencyGamingMode()
                                    viewModel.postToast(res.message)
                                }
                            },
                            modifier = Modifier.weight(1.3f),
                            leadingIcon = Icons.Default.Speed,
                            accentColor = if (isLowLatency) CyberAccentGreen else CyberAccentCyan
                        )

                        CyberSecondaryButton(
                            text = "DECK",
                            onClick = onNavigateToShizuku,
                            modifier = Modifier.weight(0.7f),
                            leadingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                            accentColor = CyberAccentCyan
                        )
                    }
                }
            }
        }

        // System Diagnostics & Copy Logs Card
        item {
            GlassCard(borderColor = CyberCyan.copy(alpha = 0.5f)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Diagnostics & Logs", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Button(
                            onClick = {
                                AppDiagnostics.copyReportToClipboard(
                                    context,
                                    mapOf(
                                        "Local IP" to viewModel.transportManager.localIp.value,
                                        "Online Peers" to "${discoveredDevices.size}",
                                        "Shizuku Status" to shizukuStatus.name,
                                        "Low Latency Wi-Fi" to "$isLowLatency",
                                        "WakeLock Active" to "$isWakeLockActive"
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF00363D), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Logs", color = Color(0xFF00363D), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Copy complete runtime diagnostic logs, connection states, and exception history to share with developer.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Discovered Devices Header & List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discovered Devices (${discoveredDevices.size})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "View All",
                    fontSize = 13.sp,
                    color = CyberCyan,
                    modifier = Modifier.clickable { onNavigateToNearby() }
                )
            }
        }

        if (discoveredDevices.isEmpty()) {
            item {
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Radar,
                            contentDescription = "Scanning",
                            tint = CyberCyan,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Scanning for nearby PeerLink nodes...",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Connect devices to the same Wi-Fi, Hotspot, or Wi-Fi Direct to communicate directly.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(discoveredDevices.values.toList().take(3).size) { index ->
                val peer = discoveredDevices.values.toList()[index]
                PeerDeviceItem(
                    peer = peer,
                    onConnect = { viewModel.transportManager.connectToPeer(it.address, it.port, it.name) },
                    onDisconnect = { viewModel.transportManager.disconnectPeer(it.address) },
                    onChat = {
                        viewModel.openChat(it.id)
                        onNavigateToChats()
                    },
                    onVoiceCall = {
                        viewModel.startVoiceCall(it)
                        onNavigateToCalls()
                    },
                    onVideoCall = {
                        viewModel.startVideoCall(it)
                        onNavigateToCalls()
                    },
                    onScreenShare = {
                        onNavigateToScreenShare()
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
