package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transport.model.PeerDevice
import com.example.ui.components.GlassCard
import com.example.ui.components.PeerDeviceItem
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
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
    val isBroadcasting by viewModel.isBroadcasting.collectAsState()
    val localIp by viewModel.localIp.collectAsState()

    var showDirectConnect by remember { mutableStateOf(false) }
    var directIpInput by remember { mutableStateOf("") }
    var directPortInput by remember { mutableStateOf("8988") }

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

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                text = "Direct IP / Hotspot Connect",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }

                        Button(
                            onClick = { showDirectConnect = !showDirectConnect },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                        ) {
                            Text(if (showDirectConnect) "Close" else "Direct Connect", color = MaterialTheme.colorScheme.surface)
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
                                    val port = directPortInput.toIntOrNull() ?: 8988
                                    viewModel.connectDirectIp(directIpInput, port)
                                    showDirectConnect = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Initiate P2P Connection")
                        }
                    }
                }
            }
        }

        // Discovered Devices List
        if (discoveredDevices.isEmpty()) {
            item {
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = "Radar",
                            tint = CyberCyan,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Nearby Devices Discovered Yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Make sure nearby devices have PeerLink open and are connected to the same Wi-Fi network, tethered hotspot, or Wi-Fi Direct group.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(discoveredDevices.values.toList(), key = { it.id }) { peer ->
                PeerDeviceItem(
                    peer = peer,
                    onConnect = { viewModel.transportManager.connectToPeer(it.address, it.port, it.name) },
                    onDisconnect = { viewModel.transportManager.disconnectPeer(it.address) },
                    onChat = {
                        viewModel.openChat(it.id)
                        onNavigateToChat(it.id)
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
                        onNavigateToScreenShare(it.id)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
