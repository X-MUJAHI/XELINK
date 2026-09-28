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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transport.QrCodeManager
import com.example.ui.components.GlassCard
import com.example.ui.components.PeerDeviceItem
import com.example.ui.components.ScanQrDialog
import com.example.ui.components.ShowQrDialog
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonEmerald
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
                viewModel.postToast("Connecting to ${info.peerName} (${info.ip})...")
                onNavigateToChat(info.peerId)
            },
            onDismiss = { showScanDialog = false }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Nearby Devices",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Local subnet: $localIp",
                        fontSize = 12.sp,
                        color = CyberCyan
                    )
                }
                FilledTonalButton(
                    onClick = {
                        viewModel.transportManager.startDiscovery()
                        viewModel.transportManager.startBroadcast()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isScanning) "Scanning..." else "Rescan", fontSize = 12.sp)
                }
            }
        }

        // Quick Connect Action Bar: Scan QR & Show My QR
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showScanDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFF00363D), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan QR Code", color = Color(0xFF00363D), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Button(
                    onClick = { showQrDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("My QR Code", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Offline Hotspot Onboarding Helper
        item {
            GlassCard(
                borderColor = if (showHotspotGuide) NeonEmerald.copy(alpha = 0.5f) else DarkBorder
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showHotspotGuide = !showHotspotGuide },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(NeonEmerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Offline Direct Pairing (No Router / No SIM)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Zero internet required • 512KB binary pipelined stream", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = NeonEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (showHotspotGuide) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "1. Turn on Phone Hotspot on Device A (no mobile data or internet needed).\n" +
                                   "2. Connect Device B to Device A's Wi-Fi Hotspot.\n" +
                                   "3. Open PeerLink on both phones:\n" +
                                   "   • Device A taps \"My QR Code\"\n" +
                                   "   • Device B taps \"Scan QR Code\"\n" +
                                   "4. Devices pair instantly! Enjoy fast binary pipelined file transfers, crystal-clear voice/video calls, and screen mirroring over direct electromagnetic radio waves.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Direct IP / Hotspot Manual Connect Toggle Card
        item {
            GlassCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = CyberCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Manual IP Connect",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                        OutlinedButton(
                            onClick = { showDirectConnect = !showDirectConnect }
                        ) {
                            Text(if (showDirectConnect) "Close" else "Enter IP", fontSize = 12.sp)
                        }
                    }

                    if (showDirectConnect) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Connect directly to a peer via Wi-Fi hotspot gateway IP (e.g. 192.168.43.1) or manual node address:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = directIpInput,
                                onValueChange = { directIpInput = it.trim() },
                                label = { Text("Peer IP Address") },
                                placeholder = { Text("192.168.43.1") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            OutlinedTextField(
                                value = directPortInput,
                                onValueChange = { directPortInput = it.trim() },
                                label = { Text("Port") },
                                modifier = Modifier.width(80.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
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
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Connect to Peer", color = MaterialTheme.colorScheme.surface)
                        }
                    }
                }
            }
        }

        // Subnet Peer Devices List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discovered Peers (${discoveredDevices.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (discoveredDevices.isEmpty()) {
            item {
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            tint = CyberCyan.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No other devices detected on this subnet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ensure both devices are on the same Wi-Fi or Hotspot, or tap \"Scan QR Code\" above.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
