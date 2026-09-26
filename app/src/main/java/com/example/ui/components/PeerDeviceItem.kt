package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transport.model.PeerDevice
import com.example.transport.model.PeerStatus
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonEmerald

@Composable
fun PeerDeviceItem(
    peer: PeerDevice,
    onConnect: (PeerDevice) -> Unit,
    onDisconnect: (PeerDevice) -> Unit,
    onChat: (PeerDevice) -> Unit,
    onVoiceCall: (PeerDevice) -> Unit,
    onVideoCall: (PeerDevice) -> Unit,
    onScreenShare: (PeerDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = peer.status == PeerStatus.CONNECTED
    val isConnecting = peer.status == PeerStatus.CONNECTING

    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device Icon / Avatar
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (isConnected) CyberCyan.copy(alpha = 0.2f) else ElectricViolet.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Peer Icon",
                        tint = if (isConnected) CyberCyan else ElectricViolet,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = peer.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${peer.address}:${peer.port} • ${peer.transportType.name.replace("_", " ")}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (peer.fingerprint.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Key: ${peer.fingerprint}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ElectricViolet
                        )
                    }
                }

                StatusBadge(
                    type = when (peer.status) {
                        PeerStatus.CONNECTED -> BadgeType.CONNECTED
                        PeerStatus.CONNECTING -> BadgeType.CONNECTING
                        else -> BadgeType.ONLINE
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connect / Disconnect button
                FilledTonalButton(
                    onClick = {
                        if (isConnected) onDisconnect(peer) else onConnect(peer)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.LinkOff else Icons.Default.Link,
                        contentDescription = if (isConnected) "Disconnect" else "Connect",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isConnected) "Disconnect" else if (isConnecting) "Connecting..." else "Connect",
                        fontSize = 12.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalIconButton(
                        onClick = { onChat(peer) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Open Chat", modifier = Modifier.size(18.dp))
                    }

                    FilledTonalIconButton(
                        onClick = { onVoiceCall(peer) },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = NeonEmerald.copy(alpha = 0.2f), contentColor = NeonEmerald)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Voice Call", modifier = Modifier.size(18.dp))
                    }

                    FilledTonalIconButton(
                        onClick = { onVideoCall(peer) },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = CyberCyan.copy(alpha = 0.2f), contentColor = CyberCyan)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call", modifier = Modifier.size(18.dp))
                    }

                    FilledTonalIconButton(
                        onClick = { onScreenShare(peer) },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = ElectricViolet.copy(alpha = 0.2f), contentColor = ElectricViolet)
                    ) {
                        Icon(Icons.Default.ScreenShare, contentDescription = "Share Screen", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
