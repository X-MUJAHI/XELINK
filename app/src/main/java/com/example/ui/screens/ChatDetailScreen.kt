package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.local.MessageEntity
import com.example.filetransfer.FileTransferProgress
import com.example.transport.model.PeerDevice
import com.example.transport.model.PeerStatus
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassSurface
import com.example.ui.components.GlassTextField
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassLevel
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.NeonEmerald
import com.example.viewmodel.MainViewModel
import java.io.File
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
    var previewImagePath by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            if (uris.size == 1) {
                viewModel.sendFile(uris[0], peerId, peerName, peerIp)
            } else {
                viewModel.sendMultipleFiles(uris, peerId, peerName, peerIp)
            }
        }
    }

    if (previewImagePath != null) {
        Dialog(onDismissRequest = { previewImagePath = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { previewImagePath = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = if (previewImagePath!!.startsWith("content://")) Uri.parse(previewImagePath) else File(previewImagePath!!),
                    contentDescription = "Full preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { previewImagePath = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
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
        // Chat Header with Liquid Glass Surface
        GlassSurface(
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            level = GlassLevel.LEVEL_2_STANDARD,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    onClick = {
                        viewModel.closeChat()
                        onBack()
                    },
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    size = 40.dp
                )

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = 0.25f))
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), CircleShape),
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

                // Call, Screen Share, or Reconnect / Cancel actions with circular glass controls
                if (currentPeer != null && currentPeer.status == PeerStatus.CONNECTED) {
                    GlassIconButton(
                        onClick = { onStartVoiceCall(currentPeer) },
                        icon = Icons.Default.Call,
                        contentDescription = "Voice Call",
                        tint = NeonEmerald,
                        size = 38.dp,
                        iconSize = 18.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    GlassIconButton(
                        onClick = { onStartVideoCall(currentPeer) },
                        icon = Icons.Default.Videocam,
                        contentDescription = "Video Call",
                        tint = CyberCyan,
                        size = 38.dp,
                        iconSize = 18.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    GlassIconButton(
                        onClick = { onStartScreenShare(currentPeer) },
                        icon = Icons.Default.ScreenShare,
                        contentDescription = "Share Screen",
                        tint = ElectricViolet,
                        size = 38.dp,
                        iconSize = 18.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    GlassIconButton(
                        onClick = { viewModel.disconnectPeer(peerIp) },
                        icon = Icons.Default.Close,
                        contentDescription = "Disconnect",
                        tint = CrimsonError,
                        size = 38.dp,
                        iconSize = 18.dp
                    )
                } else if (currentPeer != null && currentPeer.status == PeerStatus.CONNECTING) {
                    Button(
                        onClick = { viewModel.disconnectPeer(peerIp) },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonError),
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel", fontSize = 12.sp, color = Color.White)
                    }
                } else if (peerIp.isNotBlank()) {
                    Button(
                        onClick = { viewModel.connectDirectIp(peerIp) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connect", fontSize = 12.sp, color = Color.Black)
                    }
                }
            }
        }

        // Instant Mesh Badge Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberCyan.copy(alpha = 0.1f))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "High-Speed P2P • Direct Socket Streaming",
                    fontSize = 11.sp,
                    color = CyberCyan,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Active Transfer Banner (Multi-file / Batch queue indicator)
        val activeTransfers = transfers.values.filter { !it.isComplete && it.error == null }
        if (activeTransfers.isNotEmpty()) {
            Surface(
                color = DarkSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val current = activeTransfers.first()
                        Text(
                            text = "Transferring (${activeTransfers.size} active): ${current.fileName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        val speed = if (current.speedBytesPerSec > 0) " • ${current.speedFormatted}" else ""
                        Text(
                            text = "${(current.progressPercent * 100).toInt()}%$speed",
                            fontSize = 10.sp,
                            color = CyberCyan
                        )
                    }
                }
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
                    onOpenFile = { path -> viewModel.fileTransferManager.openFile(path) },
                    onPreviewImage = { path -> previewImagePath = path },
                    onRetry = { viewModel.manualRetryMessage(msg) }
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Bottom Input Row with Liquid Glass Surface
        GlassSurface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            level = GlassLevel.LEVEL_2_STANDARD,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attach File Button (Supports single or multi-file batch selection)
                GlassIconButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    icon = Icons.Default.AttachFile,
                    contentDescription = "Attach Files",
                    tint = CyberCyan,
                    size = 40.dp,
                    iconSize = 20.dp
                )

                Spacer(modifier = Modifier.width(6.dp))

                GlassTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholderText = "Direct message...",
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                GlassIconButton(
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
                    icon = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = CyberCyan,
                    size = 42.dp,
                    iconSize = 20.dp
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: MessageEntity,
    transfer: FileTransferProgress?,
    onOpenFile: (String) -> Unit,
    onPreviewImage: (String) -> Unit,
    onRetry: () -> Unit
) {
    val isOutgoing = message.isOutgoing
    val isFile = transfer != null || message.content.startsWith("Received file:") || message.type == "FILE"
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        GlassSurface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isOutgoing) 18.dp else 4.dp,
                bottomEnd = if (isOutgoing) 4.dp else 18.dp
            ),
            level = if (isOutgoing) GlassLevel.LEVEL_2_STANDARD else GlassLevel.LEVEL_1_SUBTLE,
            tint = if (isOutgoing) CyberCyan else null,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column {
                if (isFile) {
                    // Rich File Attachment Layout
                    val fileName = transfer?.fileName ?: message.content.removePrefix("Received file:").trim()
                    val totalBytes = transfer?.totalBytes ?: message.fileSize
                    val formattedSize = formatFileSize(totalBytes)
                    val isImage = fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) ||
                                  fileName.endsWith(".png", true) || fileName.endsWith(".webp", true) ||
                                  fileName.endsWith(".gif", true)
                    val localPath = transfer?.localFilePath ?: message.filePath

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isImage) CyberCyan.copy(alpha = 0.25f) else ElectricViolet.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isImage) Icons.Default.Image else Icons.Default.InsertDriveFile,
                                contentDescription = "File",
                                tint = if (isImage) CyberCyan else ElectricViolet,
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

                    // In-App Image Thumbnail Preview
                    if (isImage && localPath != null && (transfer?.isComplete == true || transfer == null)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                                .clickable { onPreviewImage(localPath) }
                        ) {
                            AsyncImage(
                                model = if (localPath.startsWith("content://")) Uri.parse(localPath) else File(localPath),
                                contentDescription = "Image preview",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Crop
                            )
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
                        val speedStr = if (transfer.speedBytesPerSec > 0) " • ${transfer.speedFormatted}" else ""
                        Text(
                            text = "${(transfer.progressPercent * 100).toInt()}% • Transferring...$speedStr",
                            fontSize = 10.sp,
                            color = CyberCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (localPath != null && (transfer?.isComplete == true || transfer == null)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isImage) {
                                Button(
                                    onClick = { onPreviewImage(localPath) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Preview", color = Color.White, fontSize = 11.sp)
                                }
                            }
                            Button(
                                onClick = { onOpenFile(localPath) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.FileOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = Color(0xFF00363D)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open", color = Color(0xFF00363D), fontSize = 11.sp)
                            }
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
                            "FAILED" -> {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CrimsonError.copy(alpha = 0.2f))
                                        .clickable { onRetry() }
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = "Failed - Tap to retry",
                                        tint = CrimsonError,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "Retry",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CrimsonError
                                    )
                                }
                            }
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
