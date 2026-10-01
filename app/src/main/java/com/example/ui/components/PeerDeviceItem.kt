/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: PeerDeviceItem.kt
 *
 * Commentary / Architectural Overview:
 * This component represents an individual peer node card discovered on the local mesh/subnet.
 * It displays the peer's advertised name, IP address, socket port, transport medium (Wi-Fi,
 * Hotspot, Bluetooth LE), cryptographic identity fingerprint, and real-time connectivity status.
 *
 * Design Architecture:
 * - Built using the Cyberpunk Dark system tokens:
 *   - Container: CyberCard (#161D2A card background, 1dp #24324D border, 16dp rounded radius).
 *   - Status: CyberBadge with active status indicators (Green #00E676 for connected, Amber #FFB300 for connecting).
 *   - Actions: High-contrast buttons for instant Socket Connect/Disconnect, Chat, Voice Call,
 *     Video Call, and Low-Latency Screen Share initiation.
 */

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transport.model.PeerDevice
import com.example.transport.model.PeerStatus
import com.example.ui.theme.CyberAccentAmber
import com.example.ui.theme.CyberAccentCyan
import com.example.ui.theme.CyberAccentGreen
import com.example.ui.theme.CyberAccentPurple
import com.example.ui.theme.CyberAccentRed
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

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

    CyberCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device Icon / Status Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isConnected) CyberAccentGreen.copy(alpha = 0.15f) else CyberCardElevated)
                        .border(
                            1.dp,
                            if (isConnected) CyberAccentGreen.copy(alpha = 0.6f) else CyberBorder,
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Peer Icon",
                        tint = if (isConnected) CyberAccentGreen else CyberAccentCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = peer.name,
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${peer.address}:${peer.port} • ${peer.transportType.name.replace("_", " ")}",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = CyberTextSecondary
                        )
                    )

                    if (peer.fingerprint.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "FP: ${peer.fingerprint.take(16)}...",
                            style = TextStyle(
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberAccentPurple,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }

                CyberBadge(
                    text = when (peer.status) {
                        PeerStatus.CONNECTED -> "ONLINE"
                        PeerStatus.CONNECTING -> "SYNCING"
                        else -> "STANDBY"
                    },
                    tint = when (peer.status) {
                        PeerStatus.CONNECTED -> CyberAccentGreen
                        PeerStatus.CONNECTING -> CyberAccentAmber
                        else -> CyberAccentCyan
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
                CyberSecondaryButton(
                    text = if (isConnected) "DISCONNECT" else if (isConnecting) "LINKING..." else "LINK NODE",
                    icon = if (isConnected) Icons.Default.LinkOff else Icons.Default.Link,
                    onClick = {
                        if (isConnected) onDisconnect(peer) else onConnect(peer)
                    },
                    modifier = Modifier.weight(1f),
                    borderColor = if (isConnected) CyberAccentRed else CyberAccentCyan,
                    textColor = if (isConnected) CyberAccentRed else CyberAccentCyan
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Chat Action
                    ActionIconButton(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Open Chat",
                        tint = CyberAccentCyan,
                        onClick = { onChat(peer) }
                    )

                    // Voice Call Action
                    ActionIconButton(
                        icon = Icons.Default.Call,
                        contentDescription = "Voice Call",
                        tint = CyberAccentGreen,
                        onClick = { onVoiceCall(peer) }
                    )

                    // Video Call Action
                    ActionIconButton(
                        icon = Icons.Default.Videocam,
                        contentDescription = "Video Call",
                        tint = CyberAccentAmber,
                        onClick = { onVideoCall(peer) }
                    )

                    // Screen Share Action
                    ActionIconButton(
                        icon = Icons.Default.ScreenShare,
                        contentDescription = "Share Screen",
                        tint = CyberAccentPurple,
                        onClick = { onScreenShare(peer) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CyberCardElevated)
            .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(17.dp)
        )
    }
}
