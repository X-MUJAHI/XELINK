// PeerLink Production Sync - Active
/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: AiScreen.kt
 *
 * Commentary / Architectural Overview:
 * Modern Offline AI Interface with Qwen GGUF Models:
 * - Model Selector Pill in Chat Header: Switch models seamlessly while chatting.
 * - Multi-Session Chat System: New Chat, History Drawer, Rename, Pin, Delete, and Project Folders.
 * - Clean Chat Surface: Heavy RAM/Storage stats moved into Model Hub to maximize conversation space.
 * - Purely Focused Responses: Zero boilerplate or deflection text.
 * - Comprehensive Markdown Rendering: Code blocks with copy, tables, bold, lists, quotes.
 * - Persistent Storage in PeerLink/ai_chats/ and PeerLink/ai_models/ surviving uninstallation.
 */

package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.AiChatMessage
import com.example.ai.AiChatSession
import com.example.ai.AiModelDownloader
import com.example.ai.DownloadStatus
import com.example.ai.MessageSender
import com.example.ai.QwenGgufModel
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberPrimaryButton
import com.example.ui.components.CyberSecondaryButton
import com.example.ui.components.CyberTextField
import com.example.ui.components.markdown.CyberMarkdownRenderer
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberAccentAmber
import com.example.ui.theme.CyberAccentCyan
import com.example.ui.theme.CyberAccentGreen
import com.example.ui.theme.CyberAccentPurple
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val aiManager = viewModel.aiManager
    val models by aiManager.downloader.models.collectAsState()
    val hardwareSpec by aiManager.downloader.hardwareSpec.collectAsState()
    val sessions by aiManager.sessions.collectAsState()
    val activeSession by aiManager.activeSession.collectAsState()
    val messages by aiManager.messages.collectAsState()
    val availableFolders by aiManager.availableFolders.collectAsState()
    val isGenerating by aiManager.inferenceEngine.isGenerating.collectAsState()
    val tokensPerSecond by aiManager.inferenceEngine.tokensPerSecond.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Chat, 1: Model Hub
    var promptInput by remember { mutableStateOf("") }
    val chatListState = rememberLazyListState()

    // Dialog & Sheet States
    var showModelPickerSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var sessionToRename by remember { mutableStateOf<AiChatSession?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var sessionToFolder by remember { mutableStateOf<AiChatSession?>(null) }
    var folderInput by remember { mutableStateOf("") }
    var sessionToDelete by remember { mutableStateOf<AiChatSession?>(null) }
    var modelToDelete by remember { mutableStateOf<QwenGgufModel?>(null) }

    // Safe auto-scroll: animate smoothly when a new message is added; use instant scrollToItem on stream updates
    val lastMessage = messages.lastOrNull()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            try {
                chatListState.animateScrollToItem(messages.size - 1)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    LaunchedEffect(lastMessage?.text?.length) {
        if (lastMessage?.isStreaming == true && messages.isNotEmpty()) {
            try {
                chatListState.scrollToItem(messages.size - 1)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    val activeModel = models.find { it.isActive && it.status == DownloadStatus.COMPLETED }
        ?: models.find { it.status == DownloadStatus.COMPLETED }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBackground)
                .padding(horizontal = 14.dp)
                .blur(if (modelToDelete != null) 16.dp else 0.dp)
        ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Top Navigation: Segmented Tabs (Chat / Model Hub)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CyberSurface)
                .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf("OFFLINE CHAT", "MODEL HUB (${models.size} GGUF)")
            tabs.forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CyberAccentCyan.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { selectedTab = index }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) CyberAccentCyan else CyberTextSecondary,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedTab == 0) {
            // ==================== TAB 0: OFFLINE CHAT ====================
            ChatTabContent(
                messages = messages,
                activeSession = activeSession,
                activeModel = activeModel,
                isGenerating = isGenerating,
                promptInput = promptInput,
                onPromptChange = { promptInput = it },
                onSendPrompt = {
                    aiManager.sendMessage(promptInput)
                    promptInput = ""
                },
                onStopGeneration = { aiManager.stopGeneration() },
                onOpenModelPicker = { showModelPickerSheet = true },
                onOpenHistory = { showHistorySheet = true },
                onNewChat = {
                    aiManager.createNewChat()
                    Toast.makeText(context, "New chat started", Toast.LENGTH_SHORT).show()
                },
                onOpenModelHub = { selectedTab = 1 },
                chatListState = chatListState,
                onShareToPeer = { answerText ->
                    clipboardManager.setText(AnnotatedString(answerText))
                    Toast.makeText(context, "Copied response to clipboard", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            // ==================== TAB 1: MODEL HUB ====================
            // RAM & Storage stats are placed here where downloading happens!
            ModelHubTabContent(
                models = models,
                hardwareSpec = hardwareSpec,
                persistentPath = aiManager.downloader.persistentDirectoryPath,
                onRefreshSpecs = { aiManager.downloader.refreshHardwareSpec() },
                onStartDownload = { aiManager.downloader.startDownload(it) },
                onPauseDownload = { aiManager.downloader.pauseDownload(it) },
                onRequestDeleteModel = { modelToDelete = it },
                onCancelDownload = { aiManager.downloader.deleteModel(it) },
                onSetActiveModel = {
                    aiManager.selectModel(it)
                    Toast.makeText(context, "Active model updated!", Toast.LENGTH_SHORT).show()
                },
                onRescanModels = {
                    val count = aiManager.downloader.scanAndRestoreModels()
                    Toast.makeText(context, "Scanned: $count model(s) found in PeerLink/ai_models/", Toast.LENGTH_SHORT).show()
                },
                onSwitchToChat = { selectedTab = 0 }
            )
        }
    }

    // Frosted Blue Confirmation Modal with background blur
    if (modelToDelete != null) {
        FrostedBlueDeleteModelModal(
            model = modelToDelete,
            onDismiss = { modelToDelete = null },
            onConfirmDelete = { target ->
                val fileName = target.localFileName
                val freedSize = target.formattedSize
                aiManager.deleteModel(target.id)
                modelToDelete = null
                Toast.makeText(
                    context,
                    "Deleted $fileName ($freedSize freed)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}

    // ==================== MODEL SELECTOR SHEET ====================
    if (showModelPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModelPickerSheet = false },
            containerColor = CyberBackground,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(CyberBorder)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Offline Model", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                    TextButton(onClick = {
                        showModelPickerSheet = false
                        selectedTab = 1
                    }) {
                        Text("Model Hub", color = CyberAccentCyan, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(models) { model ->
                        val isDownloaded = model.status == DownloadStatus.COMPLETED
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = isDownloaded) {
                                    aiManager.selectModel(model.id)
                                    showModelPickerSheet = false
                                    Toast.makeText(context, "Switched to ${model.name}", Toast.LENGTH_SHORT).show()
                                },
                            color = if (model.isActive) CyberAccentCyan.copy(alpha = 0.12f) else CyberSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (model.isActive) CyberAccentCyan else CyberBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(model.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CyberTextPrimary)
                                        if (model.isActive) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(CyberAccentCyan.copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberAccentCyan)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "${model.parameters} • ${model.quantization} • Requires ${model.minRamFormatted}",
                                        fontSize = 11.sp,
                                        color = CyberTextSecondary
                                    )
                                }

                                if (isDownloaded) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (model.id != "builtin_neural_core") {
                                            IconButton(
                                                onClick = {
                                                    showModelPickerSheet = false
                                                    modelToDelete = model
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete Model",
                                                    tint = CrimsonError.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(CyberAccentGreen.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("READY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberAccentGreen)
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(CyberBorder.copy(alpha = 0.4f))
                                            .clickable {
                                                showModelPickerSheet = false
                                                selectedTab = 1
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("GET IN HUB", fontSize = 10.sp, color = CyberAccentAmber, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // ==================== CHAT HISTORY & PROJECTS DRAWER ====================
    if (showHistorySheet) {
        var selectedFolderFilter by remember { mutableStateOf<String?>(null) }

        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            containerColor = CyberBackground,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(CyberBorder)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 16.dp)
            ) {
                // Header with New Chat button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Chat Sessions & Projects", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                        Text("${sessions.size} saved conversations", fontSize = 11.sp, color = CyberTextMuted)
                    }

                    CyberPrimaryButton(
                        text = "New Chat",
                        icon = Icons.Default.Add,
                        onClick = {
                            aiManager.createNewChat(folder = selectedFolderFilter)
                            showHistorySheet = false
                        },
                        modifier = Modifier.width(135.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Folder / Project Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // "All" chip
                    FilterChip(
                        label = "All Chats",
                        isSelected = selectedFolderFilter == null,
                        onClick = { selectedFolderFilter = null }
                    )

                    for (folder in availableFolders) {
                        FilterChip(
                            label = folder,
                            leadingIcon = Icons.Default.Folder,
                            isSelected = selectedFolderFilter == folder,
                            onClick = { selectedFolderFilter = folder }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val filteredSessions = sessions.filter {
                    selectedFolderFilter == null || it.folder == selectedFolderFilter
                }.sortedWith(compareByDescending<AiChatSession> { it.isPinned }.thenByDescending { it.updatedAt })

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredSessions, key = { it.id }) { session ->
                        val isActive = session.id == activeSession?.id
                        val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    aiManager.switchSession(session.id)
                                    showHistorySheet = false
                                },
                            color = if (isActive) CyberAccentCyan.copy(alpha = 0.12f) else CyberSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isActive) CyberAccentCyan else CyberBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (session.isPinned) {
                                            Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = CyberAccentAmber, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = session.title,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = CyberTextPrimary,
                                            maxLines = 1
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${session.messages.size} msgs • ${dateFormat.format(Date(session.updatedAt))}",
                                            fontSize = 10.sp,
                                            color = CyberTextMuted
                                        )
                                        if (!session.folder.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = CyberAccentPurple,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = session.folder,
                                                fontSize = 10.sp,
                                                color = CyberAccentPurple
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Pin toggle
                                    IconButton(
                                        onClick = { aiManager.togglePinSession(session.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.PushPin,
                                            contentDescription = "Pin",
                                            tint = if (session.isPinned) CyberAccentAmber else CyberTextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    // Rename
                                    IconButton(
                                        onClick = {
                                            sessionToRename = session
                                            renameInput = session.title
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Rename", tint = CyberTextSecondary, modifier = Modifier.size(14.dp))
                                    }

                                    // Folder
                                    IconButton(
                                        onClick = {
                                            sessionToFolder = session
                                            folderInput = session.folder ?: ""
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Folder, contentDescription = "Project Folder", tint = CyberAccentPurple, modifier = Modifier.size(14.dp))
                                    }

                                    // Delete
                                    IconButton(
                                        onClick = { sessionToDelete = session },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonError.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Rename Dialog
    sessionToRename?.let { target ->
        AlertDialog(
            onDismissRequest = { sessionToRename = null },
            containerColor = CyberCard,
            title = { Text("Rename Chat", color = CyberTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                CyberTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    hint = "Chat Title"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    aiManager.renameSession(target.id, renameInput)
                    sessionToRename = null
                }) {
                    Text("Save", color = CyberAccentCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToRename = null }) {
                    Text("Cancel", color = CyberTextMuted)
                }
            }
        )
    }

    // Folder / Project Dialog
    sessionToFolder?.let { target ->
        AlertDialog(
            onDismissRequest = { sessionToFolder = null },
            containerColor = CyberCard,
            title = { Text("Assign to Project / Folder", color = CyberTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column {
                    Text("Organize this conversation into a project category (e.g. Gaming, Code, General):", fontSize = 12.sp, color = CyberTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    CyberTextField(
                        value = folderInput,
                        onValueChange = { folderInput = it },
                        hint = "Folder Name (leave empty to remove)"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    aiManager.setSessionFolder(target.id, folderInput)
                    sessionToFolder = null
                }) {
                    Text("Set Folder", color = CyberAccentPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToFolder = null }) {
                    Text("Cancel", color = CyberTextMuted)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    sessionToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            containerColor = CyberCard,
            title = { Text("Delete Chat Session?", color = CyberTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Text("Are you sure you want to delete \"${target.title}\"? This cannot be undone.", color = CyberTextSecondary, fontSize = 12.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    aiManager.deleteSession(target.id)
                    sessionToDelete = null
                }) {
                    Text("Delete", color = CrimsonError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancel", color = CyberTextMuted)
                }
            }
        )
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) CyberAccentCyan.copy(alpha = 0.2f) else CyberSurface)
            .border(1.dp, if (isSelected) CyberAccentCyan else CyberBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (isSelected) CyberAccentCyan else CyberTextSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) CyberAccentCyan else CyberTextSecondary
            )
        }
    }
}

// ==================== TAB 0: CHAT TAB CONTENT ====================
@Composable
private fun ChatTabContent(
    messages: List<AiChatMessage>,
    activeSession: AiChatSession?,
    activeModel: QwenGgufModel?,
    isGenerating: Boolean,
    promptInput: String,
    onPromptChange: (String) -> Unit,
    onSendPrompt: () -> Unit,
    onStopGeneration: () -> Unit,
    onOpenModelPicker: () -> Unit,
    onOpenHistory: () -> Unit,
    onNewChat: () -> Unit,
    onOpenModelHub: () -> Unit,
    chatListState: androidx.compose.foundation.lazy.LazyListState,
    onShareToPeer: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Modern Top Bar: History button | Model Dropdown Pill | New Chat button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: History drawer button
            IconButton(
                onClick = onOpenHistory,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Default.Menu, contentDescription = "History", tint = CyberAccentCyan, modifier = Modifier.size(18.dp))
            }

            // Center: Model Selector Pill
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onOpenModelPicker() },
                color = CyberSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberAccentCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (activeModel != null) CyberAccentGreen else CyberAccentAmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = activeModel?.name?.take(18) ?: "Select Model",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeModel != null) CyberAccentCyan else CyberAccentAmber
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = CyberAccentCyan, modifier = Modifier.size(14.dp))
                }
            }

            // Right: New Chat button
            IconButton(
                onClick = onNewChat,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Chat", tint = CyberAccentCyan, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Warning banner if no model is downloaded yet
        if (activeModel == null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenModelHub() }
                    .clip(RoundedCornerShape(10.dp)),
                color = CyberAccentAmber.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberAccentAmber.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = CyberAccentAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("No Model Downloaded", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberAccentAmber)
                        Text("Tap to open Model Hub to download a Qwen model.", fontSize = 10.sp, color = CyberTextSecondary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Empty state when chat session has 0 messages
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(CyberAccentCyan.copy(alpha = 0.15f))
                            .border(1.dp, CyberAccentCyan.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberAccentCyan, modifier = Modifier.size(26.dp))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("How can I help you today?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeModel?.name ?: "Ready to assist",
                        fontSize = 11.sp,
                        color = CyberTextMuted
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 4 Quick suggestion chips
                    val suggestions = listOf(
                        "Game booster tweaks for 60 FPS",
                        "Calculate 124 * 85",
                        "Write a Python script",
                        "Explain quantum mechanics"
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (s in suggestions) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyberSurface)
                                    .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                                    .clickable { onPromptChange(s) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(s, fontSize = 12.sp, color = CyberAccentCyan)
                            }
                        }
                    }
                }
            }
        } else {
            // Chat message list
            LazyColumn(
                state = chatListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages.distinctBy { it.id }, key = { it.id }) { message ->
                    ChatMessageBubble(message = message, onShare = { onShareToPeer(message.text) })
                }
            }
        }

        // Input field and controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CyberTextField(
                value = promptInput,
                onValueChange = onPromptChange,
                hint = if (activeModel != null) "Ask offline AI anything..." else "Download model to chat...",
                modifier = Modifier.weight(1f)
            )

            if (isGenerating) {
                IconButton(
                    onClick = onStopGeneration,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CrimsonError)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
                }
            } else {
                IconButton(
                    onClick = onSendPrompt,
                    enabled = promptInput.isNotBlank(),
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (promptInput.isNotBlank()) CyberAccentCyan else CyberSurface)
                        .border(1.dp, if (promptInput.isNotBlank()) CyberAccentCyan else CyberBorder, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (promptInput.isNotBlank()) Color(0xFF00363D) else CyberTextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: AiChatMessage,
    onShare: () -> Unit
) {
    val isUser = message.sender == MessageSender.USER
    val isAssistant = message.sender == MessageSender.ASSISTANT

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Sender and Model Tag
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isUser) "YOU" else (message.modelNameUsed ?: "OFFLINE AI"),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isUser) CyberAccentCyan else CyberAccentGreen
            )
            if (!isUser && message.tokensGenerated > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${message.tokensGenerated} tokens • ${message.generationTimeMs}ms",
                    fontSize = 9.sp,
                    color = CyberTextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Bubble Content with CyberMarkdownRenderer
        Surface(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isUser) 14.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 14.dp
                    )
                ),
            color = if (isUser) CyberAccentCyan.copy(alpha = 0.12f) else CyberSurface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isUser) CyberAccentCyan.copy(alpha = 0.4f) else CyberBorder
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.text.isEmpty() && message.isStreaming) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = CyberAccentCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reasoning offline...", fontSize = 12.sp, color = CyberTextSecondary)
                    }
                } else if (message.isStreaming) {
                    // FAST STREAMING RENDERER: Plain text during active streaming completely eliminates AST parsing and layout freeze!
                    Text(
                        text = message.text,
                        fontSize = 13.sp,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 19.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                } else {
                    // FULL MARKDOWN RENDERER: Rich tables, code blocks, copy buttons rendered once generation is finished
                    CyberMarkdownRenderer(
                        markdown = message.text,
                        baseTextColor = if (isUser) CyberTextPrimary else Color(0xFFE2E8F0),
                        baseFontSize = 13.sp,
                        baseLineHeight = 19.sp
                    )
                }

                // Streaming cursor indicator (zero-overhead static cyan cursor block)
                if (message.isStreaming && message.text.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 8.dp, height = 12.dp)
                            .background(CyberAccentCyan)
                    )
                }

                // Action Bar for AI answers (Copy / Share to P2P)
                if (isAssistant && !message.isStreaming && message.text.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = CyberBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onShare() }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyberAccentCyan, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy & Share to Mesh", fontSize = 10.sp, color = CyberAccentCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==================== TAB 1: MODEL HUB CONTENT ====================
@Composable
private fun ModelHubTabContent(
    models: List<QwenGgufModel>,
    hardwareSpec: com.example.ai.DeviceHardwareSpec,
    persistentPath: String,
    onRefreshSpecs: () -> Unit,
    onStartDownload: (String) -> Unit,
    onPauseDownload: (String) -> Unit,
    onRequestDeleteModel: (QwenGgufModel) -> Unit,
    onCancelDownload: (String) -> Unit,
    onSetActiveModel: (String) -> Unit,
    onRescanModels: () -> Unit,
    onSwitchToChat: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Hardware Specs Strip (Placed here in Model Hub where it's needed!)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = CyberSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = CyberAccentPurple, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("RAM TOTAL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberTextMuted)
                                Text(hardwareSpec.totalRamFormatted, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = CyberAccentGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("FREE STORAGE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberTextMuted)
                                Text(hardwareSpec.freeStorageFormatted, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                            }
                        }
                    }

                    IconButton(onClick = onRefreshSpecs, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Hardware", tint = CyberAccentCyan, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Persistent Storage Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                color = CyberSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = CyberAccentGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Persistent Model Storage", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CyberTextPrimary)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberAccentGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("SURVIVES UNINSTALL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberAccentGreen)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = persistentPath,
                        fontSize = 10.sp,
                        color = CyberAccentCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Downloaded models are saved in public storage. If you reinstall PeerLink, tap Rescan to instantly detect and restore your models.",
                        fontSize = 11.sp,
                        color = CyberTextSecondary,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CyberSecondaryButton(
                        text = "Rescan & Restore Models",
                        icon = Icons.Default.Refresh,
                        onClick = onRescanModels,
                        accentColor = CyberAccentCyan,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        items(models, key = { it.id }) { model ->
            ModelCard(
                model = model,
                hardwareSpec = hardwareSpec,
                onStartDownload = { onStartDownload(model.id) },
                onPauseDownload = { onPauseDownload(model.id) },
                onRequestDelete = { onRequestDeleteModel(model) },
                onCancelDownload = { onCancelDownload(model.id) },
                onSetActiveModel = { onSetActiveModel(model.id) },
                onSwitchToChat = onSwitchToChat
            )
        }
    }
}

@Composable
private fun ModelCard(
    model: QwenGgufModel,
    hardwareSpec: com.example.ai.DeviceHardwareSpec,
    onStartDownload: () -> Unit,
    onPauseDownload: () -> Unit,
    onRequestDelete: () -> Unit,
    onCancelDownload: () -> Unit,
    onSetActiveModel: () -> Unit,
    onSwitchToChat: () -> Unit
) {
    val isCompleted = model.status == DownloadStatus.COMPLETED
    val isDownloading = model.status == DownloadStatus.DOWNLOADING
    val isPaused = model.status == DownloadStatus.PAUSED
    val isCompatible = hardwareSpec.totalRamBytes >= model.minRamBytes

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        color = CyberCard,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (model.isActive) CyberAccentCyan else CyberBorder
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = model.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = CyberTextPrimary
                        )
                        if (model.isActive) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyberAccentCyan.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberAccentCyan)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${model.parameters} • ${model.quantization} • ${model.formattedSize}",
                        fontSize = 11.sp,
                        color = CyberAccentCyan
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCompatible) CyberAccentGreen.copy(alpha = 0.15f) else CyberAccentAmber.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isCompatible) Icons.Default.Check else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isCompatible) CyberAccentGreen else CyberAccentAmber,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = model.minRamFormatted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompatible) CyberAccentGreen else CyberAccentAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = model.description,
                fontSize = 11.sp,
                color = CyberTextSecondary,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (isDownloading || isPaused) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isDownloading) "Downloading: ${(model.downloadProgress * 100).toInt()}%" else "Paused",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDownloading) CyberAccentCyan else CyberAccentAmber
                        )
                        if (isDownloading && model.downloadSpeedBps > 0) {
                            Text(
                                text = AiModelDownloader.formatSpeed(model.downloadSpeedBps),
                                fontSize = 11.sp,
                                color = CyberAccentCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { model.downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = CyberAccentCyan,
                        trackColor = CyberSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            when {
                isCompleted -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!model.isActive) {
                            CyberPrimaryButton(
                                text = "Set as Active",
                                icon = Icons.Default.Check,
                                onClick = onSetActiveModel,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            CyberPrimaryButton(
                                text = "Chat Now",
                                icon = Icons.AutoMirrored.Filled.Chat,
                                onClick = onSwitchToChat,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (model.id != "builtin_neural_core") {
                            CyberSecondaryButton(
                                text = "Delete",
                                icon = Icons.Default.Delete,
                                onClick = onRequestDelete,
                                modifier = Modifier.width(96.dp),
                                accentColor = CrimsonError
                            )
                        }
                    }
                }

                isDownloading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CyberSecondaryButton(
                            text = "Pause",
                            icon = Icons.Default.Pause,
                            onClick = onPauseDownload,
                            modifier = Modifier.weight(1f),
                            accentColor = CyberAccentAmber
                        )
                        CyberSecondaryButton(
                            text = "Cancel",
                            icon = Icons.Default.Close,
                            onClick = onCancelDownload,
                            modifier = Modifier.weight(1f),
                            accentColor = CrimsonError
                        )
                    }
                }

                isPaused -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CyberPrimaryButton(
                            text = "Resume",
                            icon = Icons.Default.PlayArrow,
                            onClick = onStartDownload,
                            modifier = Modifier.weight(1f)
                        )
                        CyberSecondaryButton(
                            text = "Delete",
                            icon = Icons.Default.Delete,
                            onClick = onRequestDelete,
                            modifier = Modifier.width(96.dp),
                            accentColor = CrimsonError
                        )
                    }
                }

                else -> {
                    CyberPrimaryButton(
                        text = "Download Model (${model.formattedSize})",
                        icon = Icons.Default.CloudDownload,
                        onClick = onStartDownload,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * FrostedBlueDeleteModelModal:
 * Frosted blue confirmation dialog with background blur and specular rim highlights.
 * Safely confirms permanent deletion of the physical .gguf binary file from device storage.
 */
@Composable
fun FrostedBlueDeleteModelModal(
    model: QwenGgufModel?,
    onDismiss: () -> Unit,
    onConfirmDelete: (QwenGgufModel) -> Unit
) {
    if (model == null) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        // Frosted deep blue ambient backdrop scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF031633).copy(alpha = 0.75f),
                            Color(0xFF020914).copy(alpha = 0.90f)
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            // Frosted Blue Modal Surface
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 440.dp)
                    .padding(vertical = 24.dp)
                    .shadow(
                        elevation = 32.dp,
                        shape = RoundedCornerShape(26.dp),
                        spotColor = Color(0xFF0284C7).copy(alpha = 0.65f),
                        ambientColor = Color(0xFF38BDF8).copy(alpha = 0.35f)
                    )
                    .clip(RoundedCornerShape(26.dp))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF7DD3FC).copy(alpha = 0.90f), // Luminous cyan top specular highlight
                                Color(0xFF0284C7).copy(alpha = 0.55f), // Translucent ocean blue
                                Color(0xFF1E3A8A).copy(alpha = 0.35f), // Deep cobalt
                                Color(0xFF38BDF8).copy(alpha = 0.65f)  // Soft sky blue rim
                            )
                        ),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F2B48).copy(alpha = 0.93f), // Frosted blue top sheen
                                Color(0xFF0A1B30).copy(alpha = 0.96f),
                                Color(0xFF05101E).copy(alpha = 0.98f)
                            )
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume click inside card
                    )
            ) {
                // Frosted glass top-edge ambient highlight glow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF38BDF8).copy(alpha = 0.18f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    // Header Row: Frosted Badge, Title, and Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Frosted Ice-Blue Icon Badge
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0284C7).copy(alpha = 0.22f))
                                    .border(1.2.dp, Color(0xFF38BDF8).copy(alpha = 0.55f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Model",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Delete AI Model",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "Remove .gguf file from device",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF7DD3FC),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Close "X" Button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F243A).copy(alpha = 0.6f))
                                .border(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Model & File Details Card (Frosted Deep Blue)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF04101F).copy(alpha = 0.85f))
                            .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Model Name & Quantization Tag
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = model.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF0369A1).copy(alpha = 0.35f))
                                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = model.quantization,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = Color(0xFF0F2744),
                                thickness = 1.dp
                            )

                            // File Name
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = null,
                                    tint = Color(0xFF7DD3FC),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = model.localFileName,
                                    fontSize = 11.sp,
                                    color = Color(0xFFE2E8F0),
                                    maxLines = 1
                                )
                            }

                            // Storage Space Reclaimed
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Reclaim storage: ${model.formattedSize}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF34D399)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Reassurance & Warning note
                    Text(
                        text = "The actual .gguf binary file will be completely wiped from device storage to free up space. You can re-download this model anytime from the Model Hub.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons Row: Cancel (Keep) vs. Delete .GGUF File
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Cancel button: Frosted blue-gray
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0C1B2E).copy(alpha = 0.9f))
                                .border(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color(0xFFBAE6FD),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        // Confirm Delete button: Frosted Crimson & Sky Blue accented danger button
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFDC2626),
                                            Color(0xFFB91C1C)
                                        )
                                    )
                                )
                                .border(1.2.dp, Color(0xFFF87171).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .clickable {
                                    onConfirmDelete(model)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Delete .gguf",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
