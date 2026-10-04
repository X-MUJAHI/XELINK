/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: ChatsScreen.kt
 *
 * Commentary / Architectural Overview:
 * This screen displays all active offline encrypted peer-to-peer chat threads stored in Room database.
 * Every thread corresponds to a unique verified node ID. Messages sent through these threads are
 * protected by AES-256-GCM symmetric session keys negotiated during zero-knowledge pairing.
 *
 * Visual System & Styling:
 * - Built using the Cyberpunk Dark system tokens:
 *   - Screen background: CyberBackground (#0B0E14).
 *   - Conversation items: CyberCard with 16dp rounded radius, 1dp #24324D border, and #161D2A fill.
 *   - Unread badges: CyberBadge with #00E5FF cyan accent fill.
 *   - Headers: Small, bold, uppercase, letter-spaced section headers.
 *   - Fast, tactile navigation with smooth transitions.
 */

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.data.local.ConversationEntity
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberSectionHeader
import com.example.ui.components.CyberStatBoxes
import com.example.ui.components.CyberStatItem
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatsScreen(
    viewModel: MainViewModel,
    onOpenChat: (peerId: String) -> Unit,
    onStartNewChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.conversations.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onStartNewChat,
                containerColor = CyberAccentCyan,
                contentColor = CyberBackground,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .size(54.dp)
                    .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Chat",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(CyberBackground)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            text = "ENCRYPTED CHATS",
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
                            text = "OFFLINE AES-256-GCM SESSIONS",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = CyberAccentPurple
                            )
                        )
                    }

                    CyberBadge(
                        text = "ZERO CLOUD",
                        tint = CyberAccentGreen
                    )
                }
            }

            // Stat Box summary
            item {
                val stats = listOf(
                    CyberStatItem("ACTIVE THREADS", "${conversations.size}"),
                    CyberStatItem("CIPHER SUITE", "AES-GCM"),
                    CyberStatItem("DATABASE", "ROOM/SQLITE")
                )
                CyberStatBoxes(stats = stats)
            }

            item {
                CyberSectionHeader(title = "SECURE CONVERSATIONS (${conversations.size})")
            }

            if (conversations.isEmpty()) {
                item {
                    CyberCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "No chats",
                                tint = CyberAccentCyan.copy(alpha = 0.6f),
                                modifier = Modifier.size(46.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "NO OFFLINE THREADS",
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = CyberTextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Select a discovered peer on Radar or tap '+' to establish a direct encrypted channel.",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = CyberTextMuted
                                ),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(conversations, key = { it.peerId }) { convo ->
                    val isOnline = discoveredDevices.containsKey(convo.peerId) || convo.isOnline
                    ConversationItem(
                        conversation = convo,
                        isOnline = isOnline,
                        onClick = {
                            viewModel.openChat(convo.peerId)
                            onOpenChat(convo.peerId)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: ConversationEntity,
    isOnline: Boolean,
    onClick: () -> Unit
) {
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(conversation.lastTimestamp))

    CyberCard(
        onClick = onClick,
        padding = androidx.compose.foundation.layout.PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberCardElevated)
                    .border(1.dp, if (isOnline) CyberAccentGreen.copy(alpha = 0.5f) else CyberBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = conversation.peerName.take(2).uppercase(),
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOnline) CyberAccentGreen else CyberAccentCyan
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = conversation.peerName,
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberTextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) CyberAccentGreen else Color(0xFFFFB74D))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isOnline) "ONLINE" else "OFFLINE • SAVED",
                            style = TextStyle(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) CyberAccentGreen else Color(0xFFFFB74D),
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                    Text(
                        text = timeStr,
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = CyberTextMuted
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            tint = CyberAccentPurple,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = conversation.lastMessage.ifBlank { "Encrypted peer connection" },
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = CyberTextSecondary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (conversation.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(CyberAccentCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = conversation.unreadCount.toString(),
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberBackground
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
