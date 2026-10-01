/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: NearbyScreen.kt
 *
 * Commentary / Architectural Overview:
 * This screen manages local subnet device discovery, Wi-Fi hotspot pairing, QR-based peer authentication,
 * and direct socket connections. It visualizes reachable nodes on the local network (either through zero-conf
 * UDP broadcast, Wi-Fi Direct, or mobile hotspot gateways) and provides instant actions to start encrypted
 * messaging, voice/video calls, and real-time screen sharing sessions.
 *
 * UI Architecture:
 * - Adheres strictly to the Cyberpunk Dark theme (#0B0E14 background, #161D2A card surfaces, #00E5FF cyan accents).
 * - Utilizes CyberCard containers with 16dp padding, 1dp border, and 16dp rounded corners.
 * - Integrates CyberSectionHeader, CyberPrimaryButton, CyberSecondaryButton, and CyberTextField for
 *   tactile cybernetic interaction with immediate visual feedback.
 */

package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transport.QrCodeManager
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberPrimaryButton
import com.example.ui.components.CyberSecondaryButton
import com.example.ui.components.CyberSectionHeader
import com.example.ui.components.CyberStatBoxes
import com.example.ui.components.CyberStatItem
import com.example.ui.components.CyberTextField
import com.example.ui.components.PeerDeviceItem
import com.example.ui.components.ScanQrDialog
import com.example.ui.components.ShowQrDialog
import com.example.ui.theme.CyberAccentCyan
import com.example.ui.theme.CyberAccentGreen
import com.example.ui.theme.CyberAccentPurple
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.viewmodel.MainViewModel

@Composable
fun NearbyScreen(
    viewModel: MainViewModel,
    onNavigateToChat: (peerId: String) -> Unit,
    onNavigateToCalls: () -> Unit,
    onNavigateToScreenShare: (peerId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val localIp by viewModel.localIp.collectAsState()

    var showDirectConnect by remember { mutableStateOf(false) }
    var directIpInput by remember { mutableStateOf("") }
    var directPortInput by remember { mutableStateOf("8988") }

    var showQrDialog by remember { mutableStateOf(false) }
    var showScanDialog by remember { mutableStateOf(false) }
    var showHotspotGuide by remember { mutableStateOf(false) }

    if (showQrDialog) {
        val payload = QrCodeManager.createConnectionPayload(
            ip = localIp,
            port = viewModel.transportManager.serverPort,
            deviceId = viewModel.deviceIdentity.deviceId,
            deviceName = viewModel.deviceIdentity.deviceName
        )
        ShowQrDialog(
            payload = payload,
            deviceName = viewModel.deviceIdentity.deviceName,
            localIp = localIp,
            onDismiss = { showQrDialog = false }
        )
    }

    if (showScanDialog) {
        ScanQrDialog(
            onQrScanned = { info ->
                showScanDialog = false
                viewModel.transportManager.connectToPeer(info.ip, info.port)
            },
            onDismiss = { showScanDialog = false }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Screen Header Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NEARBY RADAR",
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.8.sp,
                            color = CyberTextPrimary,
                            fontFamily = FontFamily.SansSerif
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "SUBNET // ${localIp.ifEmpty { "OFFLINE MESH" }}",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = CyberAccentCyan
                        )
                    )
                }

                CyberSecondaryButton(
                    text = if (isScanning) "SCANNING..." else "RESCAN",
                    icon = Icons.Default.Refresh,
                    onClick = {
                        viewModel.transportManager.startDiscovery()
                        viewModel.transportManager.startBroadcast()
                    },
                    modifier = Modifier.width(135.dp),
                    borderColor = CyberAccentCyan,
                    textColor = CyberAccentCyan
                )
            }
        }

        // Radar Stats Row
        item {
            val stats = listOf(
                CyberStatItem("ONLINE PEERS", "${discoveredDevices.size}"),
                CyberStatItem("BROADCAST", if (isScanning) "ACTIVE" else "IDLE"),
                CyberStatItem("SUBNET PORT", "${viewModel.transportManager.serverPort}")
            )
            CyberStatBoxes(stats = stats)
        }

        // Quick Connect Action Bar: Scan QR & Show My QR
        item {
            CyberSectionHeader(title = "QR NODE AUTHENTICATION")
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CyberPrimaryButton(
                    text = "SCAN QR",
                    icon = Icons.Default.QrCodeScanner,
                    onClick = { showScanDialog = true },
                    modifier = Modifier.weight(1f)
                )
                CyberSecondaryButton(
                    text = "MY QR CODE",
                    icon = Icons.Default.QrCode,
                    onClick = { showQrDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Offline Hotspot Onboarding Helper
        item {
            CyberSectionHeader(title = "OFFLINE ZERO-CONFIG HOTSPOT")
            Spacer(modifier = Modifier.height(4.dp))
            CyberCard(
                borderColor = if (showHotspotGuide) CyberAccentGreen else CyberBorder
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showHotspotGuide = !showHotspotGuide },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberAccentGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = CyberAccentGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "OFFLINE DIRECT PAIRING",
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = CyberTextPrimary
                                    )
                                )
                                Text(
                                    text = "No SIM card, router, or cloud required",
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        color = CyberTextSecondary
                                    )
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = if (showHotspotGuide) CyberAccentGreen else CyberTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (showHotspotGuide) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "1. Turn on Phone Hotspot on Device A (no mobile data or internet needed).\n" +
                                    "2. Connect Device B to Device A's Wi-Fi Hotspot.\n" +
                                    "3. Open PeerLink on both phones:\n" +
                                    "   • Device A taps \"MY QR CODE\"\n" +
                                    "   • Device B taps \"SCAN QR\"\n" +
                                    "4. Devices pair instantly with authenticated symmetric AES-256 keys!",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = CyberTextSecondary,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }
        }

        // Direct IP / Hotspot Manual Connect Toggle Card
        item {
            CyberSectionHeader(title = "MANUAL SOCKET LINK")
            Spacer(modifier = Modifier.height(4.dp))
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = CyberAccentCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "MANUAL IP CONNECT",
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = CyberTextPrimary
                                )
                            )
                        }
                        CyberSecondaryButton(
                            text = if (showDirectConnect) "CLOSE" else "CONFIGURE",
                            onClick = { showDirectConnect = !showDirectConnect },
                            modifier = Modifier.width(110.dp)
                        )
                    }

                    if (showDirectConnect) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Direct socket link via gateway IP (e.g. 192.168.43.1):",
                            style = TextStyle(fontSize = 12.sp, color = CyberTextSecondary)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CyberTextField(
                                value = directIpInput,
                                onValueChange = { directIpInput = it.trim() },
                                label = "PEER IP",
                                placeholder = "192.168.43.1",
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            CyberTextField(
                                value = directPortInput,
                                onValueChange = { directPortInput = it.trim() },
                                label = "PORT",
                                placeholder = "8988",
                                modifier = Modifier.width(86.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        CyberPrimaryButton(
                            text = "ESTABLISH DIRECT LINK",
                            onClick = {
                                if (directIpInput.isNotBlank()) {
                                    if (viewModel.transportManager.isSelfAddress(directIpInput)) {
                                        viewModel.postToast("That is your own device's IP! Enter a peer's IP.")
                                    } else {
                                        val port = directPortInput.toIntOrNull() ?: 8988
                                        viewModel.transportManager.connectToPeer(directIpInput, port)
                                        showDirectConnect = false
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Subnet Peer Devices List
        item {
            CyberSectionHeader(title = "DISCOVERED NODES (${discoveredDevices.size})")
        }

        if (discoveredDevices.isEmpty()) {
            item {
                CyberCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            tint = CyberAccentCyan.copy(alpha = 0.6f),
                            modifier = Modifier.size(46.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "NO PEER NODES DETECTED",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = CyberTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ensure both devices are on the same Wi-Fi subnet or Wi-Fi hotspot.",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = CyberTextMuted
                            )
                        )
                    }
                }
            }
        } else {
            items(discoveredDevices.values.toList(), key = { it.id }) { peer ->
                PeerDeviceItem(
                    peer = peer,
                    onConnect = { viewModel.transportManager.connectToPeer(it.address, it.port) },
                    onDisconnect = { viewModel.transportManager.disconnectPeer(it.address) },
                    onChat = { onNavigateToChat(peer.id) },
                    onVoiceCall = {
                        viewModel.startVoiceCall(peer)
                        onNavigateToCalls()
                    },
                    onVideoCall = {
                        viewModel.startVideoCall(peer)
                        onNavigateToCalls()
                    },
                    onScreenShare = {
                        onNavigateToScreenShare(peer.id)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
