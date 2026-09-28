package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transport.model.PeerDevice
import com.example.ui.components.GlassCard
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonEmerald
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenShareScreen(
    viewModel: MainViewModel,
    initialTargetPeerId: String? = null,
    modifier: Modifier = Modifier
) {
    val isSharing by viewModel.screenShareManager.isSharing.collectAsState()
    val targetPeerName by viewModel.screenShareManager.sharingTargetPeer.collectAsState()
    val durationSeconds by viewModel.screenShareManager.shareDurationSeconds.collectAsState()
    val remoteScreenBitmap by viewModel.screenShareManager.remoteScreenBitmap.collectAsState()
    val remotePeerName by viewModel.screenShareManager.remotePeerName.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()

    var selectedPeer by remember(initialTargetPeerId, discoveredDevices) {
        mutableStateOf(
            if (initialTargetPeerId != null) {
                discoveredDevices[initialTargetPeerId]
            } else {
                discoveredDevices.values.firstOrNull()
            }
        )
    }

    var dropdownExpanded by remember { mutableStateOf(false) }

    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val peer = selectedPeer ?: discoveredDevices.values.firstOrNull()
            if (peer != null) {
                viewModel.screenShareManager.startScreenCapture(
                    resultCode = result.resultCode,
                    data = result.data!!,
                    targetPeerIp = peer.address,
                    targetPeerName = peer.name
                )
            } else {
                viewModel.postToast("No target peer selected for screen sharing")
            }
        } else {
            viewModel.postToast("Screen share authorization cancelled")
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Column {
                Text(
                    text = "P2P Screen Sharing",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Direct high-framerate display mirroring over local Wi-Fi",
                    fontSize = 12.sp,
                    color = ElectricViolet
                )
            }
        }

        // Ongoing Screen Share Status Banner (Active Outgoing Stream)
        if (isSharing) {
            item {
                GlassCard(borderColor = NeonEmerald) {
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
                                        .background(NeonEmerald.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ScreenShare, contentDescription = null, tint = NeonEmerald)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Screen Sharing Active",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = NeonEmerald
                                    )
                                    Text(
                                        text = "Target: $targetPeerName • Duration: ${durationSeconds}s",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.screenShareManager.stopScreenCapture() },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonError),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop Sharing")
                            }
                        }
                    }
                }
            }
        }

        // Remote Screen Viewer (Incoming Stream)
        if (remoteScreenBitmap != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tv, contentDescription = null, tint = CyberCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Remote Display (${remotePeerName ?: "Peer"})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            IconButton(onClick = { viewModel.screenShareManager.clearRemoteScreen() }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Viewer", tint = Color.LightGray)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = remoteScreenBitmap!!.asImageBitmap(),
                                contentDescription = "Incoming Screen Stream",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }

        // Start Screen Sharing Card
        if (!isSharing) {
            item {
                GlassCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Share This Device's Screen",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Mirrors your screen directly to the selected peer device in real-time. Requires system screen capture approval.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Target Peer Selector
                        Text(
                            text = "Select Destination Peer:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (discoveredDevices.isEmpty()) {
                            Text(
                                text = "No nearby peers connected. Scan in Nearby tab first.",
                                fontSize = 12.sp,
                                color = CrimsonError
                            )
                        } else {
                            ExposedDropdownMenuBox(
                                expanded = dropdownExpanded,
                                onExpandedChange = { dropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedPeer?.name ?: "Select a device",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = DarkBorder
                                    )
                                )

                                ExposedDropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false }
                                ) {
                                    discoveredDevices.values.forEach { peer ->
                                        DropdownMenuItem(
                                            text = { Text("${peer.name} (${peer.address})") },
                                            onClick = {
                                                selectedPeer = peer
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val intent = viewModel.screenShareManager.createScreenCaptureIntent()
                                if (intent != null) {
                                    projectionLauncher.launch(intent)
                                } else {
                                    viewModel.postToast("MediaProjection not supported on this device")
                                }
                            },
                            enabled = selectedPeer != null,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Request Approval & Start Screen Share")
                        }
                    }
                }
            }
        }

        // Info / Features card
        item {
            GlassCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Screen Sharing Capabilities",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Official MediaProjection API with Foreground Service protection\n" +
                               "• Compressed low-latency JPEG stream over encrypted P2P sockets\n" +
                               "• Automatic cleanup and lifecycle management\n" +
                               "• High-resolution scaled rendering for mobile screens",
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
