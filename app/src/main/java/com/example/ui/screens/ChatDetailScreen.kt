package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MessageEntity
import com.example.filetransfer.FileTransferProgress
import com.example.transport.model.PeerDevice
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonEmerald
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatDetailScreen(
    peerId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onStartVoiceCall: (PeerDevice) -> Unit,
    onStartVideoCall: (PeerDevice) -> Unit,
    onStartScreenShare: (PeerDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.closeChat()
        onBack()
    }

    val messages by viewModel.currentChatMessages.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val transfers by viewModel.fileTransferManager.transfers.collectAsState()

    val currentPeer = discoveredDevices[peerId]
    val conversation = conversations.firstOrNull { it.peerId == peerId }
    val peerName = currentPeer?.name ?: conversation?.peerName ?: "Peer-$peerId"
    val peerIp = currentPeer?.address ?: conversation?.peerIp ?: ""

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.sendFile(uri, peerId, peerName, peerIp)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Chat Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    viewModel.closeChat()
                    onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = peerName.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CyberCyan
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = peerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (currentPeer != null) NeonEmerald else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (peerIp.isNotBlank()) "$peerIp:8988" else "Offline Node",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Call & Screen Share actions
                if (currentPeer != null) {
                    IconButton(onClick = { onStartVoiceCall(currentPeer) }) {
                        Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = NeonEmerald)
                    }
                    IconButton(onClick = { onStartVideoCall(currentPeer) }) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = CyberCyan)
                    }
                    IconButton(onClick = { onStartScreenShare(currentPeer) }) {
                        Icon(Icons.Default.ScreenShare, contentDescription = "Share Screen", tint = ElectricViolet)
                    }
                }
            }
        }

        // Encryption Badge Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ElectricViolet.copy(alpha = 0.1f))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "End-to-End Encrypted (AES-256-GCM) • Offline Direct",
                    fontSize = 11.sp,
                    color = ElectricViolet,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            items(messages, key = { it.id }) { msg ->
                val transfer = transfers[msg.id]
                MessageBubble(
                    message = msg,
                    transfer = transfer,
                    onOpenFile = { path -> viewModel.fileTransferManager.openFile(path) }
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Bottom Input Row
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attach File Button
                IconButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach File",
                        tint = CyberCyan
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Direct message...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilledIconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(
                                peerId = peerId,
                                peerName = peerName,
                                peerIp = peerIp,
                                text = inputText.trim()
                            )
                            inputText = ""
                        }
                    },
                    modifier = Modifier.size(46.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = CyberCyan,
                        contentColor = Color(0xFF00363D)
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: MessageEntity,
    transfer: FileTransferProgress?,
    onOpenFile: (String) -> Unit
) {
    val isOutgoing = message.isOutgoing
    val isFile = transfer != null || message.content.startsWith("Received file:") || message.type == "FILE"
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isOutgoing) 16.dp else 4.dp,
                bottomEnd = if (isOutgoing) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isOutgoing) CyberCyan.copy(alpha = 0.2f) else DarkSurfaceVariant
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isOutgoing) CyberCyan.copy(alpha = 0.5f) else DarkBorder
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (isFile) {
                    // Rich File Attachment Layout
                    val fileName = transfer?.fileName ?: message.content.removePrefix("Received file:").trim()
                    val totalBytes = transfer?.totalBytes ?: message.fileSize
                    val formattedSize = formatFileSize(totalBytes)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElectricViolet.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.InsertDriveFile,
                                contentDescription = "File",
                                tint = ElectricViolet,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fileName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (formattedSize.isNotBlank()) {
                                Text(
                                    text = formattedSize,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (transfer != null && !transfer.isComplete) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { transfer.progressPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = CyberCyan,
                            trackColor = DarkBorder
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(transfer.progressPercent * 100).toInt()}% • Transferring...",
                            fontSize = 10.sp,
                            color = CyberCyan
                        )
                    }

                    val localPath = transfer?.localFilePath ?: message.filePath
                    if (localPath != null && (transfer?.isComplete == true || transfer == null)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onOpenFile(localPath) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.FileOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF00363D)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open File", color = Color(0xFF00363D), fontSize = 12.sp)
                        }
                    }
                } else {
                    // Regular Text Message
                    Text(
                        text = message.content,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeStr,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        when (message.status) {
                            "DELIVERED" -> Icon(
                                Icons.Default.DoneAll,
                                contentDescription = "Delivered",
                                tint = NeonEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            "SENT" -> Icon(
                                Icons.Default.Check,
                                contentDescription = "Sent",
                                tint = CyberCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            else -> Icon(
                                Icons.Default.HourglassEmpty,
                                contentDescription = "Sending/Queued",
                                tint = Color.Gray,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.getDefault(), "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.getDefault(), "%.1f KB", kb)
        else -> "$bytes B"
    }
}
