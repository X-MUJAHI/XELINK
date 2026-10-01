/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: AiScreen.kt
 *
 * Commentary / Architectural Overview:
 * Offline AI interface with quantized Qwen GGUF models:
 * - Model Hub tab: Displays the 5 selectable Qwen GGUF model tiers (0.6B to 14B parameters).
 * - Persistent Storage in /storage/emulated/0/Download/PeerLink/ (survives app uninstall and reinstall).
 * - Automatic and manual "Rescan & Restore" for models and chats.
 * - Offline Chat tab: Real-time token streaming chat interface with zero internet dependency.
 * - Bridges offline AI outputs directly into PeerLink P2P chat messages.
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiChatMessage
import com.example.ai.AiModelDownloader
import com.example.ai.DownloadStatus
import com.example.ai.MessageSender
import com.example.ai.QwenGgufModel
import com.example.ui.components.markdown.CyberMarkdownRenderer
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberPrimaryButton
import com.example.ui.components.CyberSecondaryButton
import com.example.ui.components.CyberSectionHeader
import com.example.ui.components.CyberTextField
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
    val messages by aiManager.messages.collectAsState()
    val isGenerating by aiManager.inferenceEngine.isGenerating.collectAsState()
    val tokensPerSecond by aiManager.inferenceEngine.tokensPerSecond.collectAsState()
    val storageStatusMessage by aiManager.storageStatusMessage.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Offline Chat, 1: Model Hub
    var promptInput by remember { mutableStateOf("") }
    val chatListState = rememberLazyListState()

    // Auto-scroll to bottom of chat when new message or token arrives
    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) {
            chatListState.animateScrollToItem(messages.size - 1)
        }
    }

    val activeModel = models.find { it.isActive && it.status == DownloadStatus.COMPLETED }
        ?: models.find { it.status == DownloadStatus.COMPLETED }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Hardware info bar
        HardwareSpecStrip(
            hardwareSpec = hardwareSpec,
            activeModel = activeModel,
            onRefresh = { aiManager.downloader.refreshHardwareSpec() }
        )

        // Storage status banner if recently restored
        if (storageStatusMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                color = CyberAccentGreen.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberAccentGreen.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = CyberAccentGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = storageStatusMessage ?: "",
                        fontSize = 11.sp,
                        color = CyberAccentGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Top Segmented Navigation Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CyberSurface)
                .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf("OFFLINE CHAT", "MODEL HUB (5 GGUF)")
            tabs.forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CyberAccentCyan.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { selectedTab = index }
                        .padding(vertical = 10.dp),
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

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedTab == 0) {
            // ==================== TAB 0: OFFLINE CHAT ====================
            ChatTabContent(
                messages = messages,
                isGenerating = isGenerating,
                tokensPerSecond = tokensPerSecond,
                activeModel = activeModel,
                promptInput = promptInput,
                onPromptChange = { promptInput = it },
                onSendPrompt = {
                    aiManager.sendMessage(promptInput)
                    promptInput = ""
                },
                onStopGeneration = { aiManager.stopGeneration() },
                onClearChat = { aiManager.clearChat() },
                onRestoreChats = {
                    aiManager.restoreFromPersistentStorage { m, c ->
                        Toast.makeText(context, "Restored $m model(s) & $c chat(s)", Toast.LENGTH_SHORT).show()
                    }
                },
                onExportChat = {
                    scope.launch {
                        val file = aiManager.exportChatToText()
                        if (file != null) {
                            Toast.makeText(context, "Chat exported to ${file.name}", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onOpenModelHub = { selectedTab = 1 },
                chatListState = chatListState,
                onShareToPeer = { answerText ->
                    clipboardManager.setText(AnnotatedString(answerText))
                    Toast.makeText(context, "AI Response copied! Ready to paste in P2P chat.", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            // ==================== TAB 1: MODEL HUB ====================
            ModelHubTabContent(
                models = models,
                hardwareSpec = hardwareSpec,
                persistentPath = aiManager.downloader.persistentDirectoryPath,
                onStartDownload = { aiManager.downloader.startDownload(it) },
                onPauseDownload = { aiManager.downloader.pauseDownload(it) },
                onDeleteModel = { aiManager.downloader.deleteModel(it) },
                onSetActiveModel = {
                    aiManager.downloader.setActiveModel(it)
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
}

@Composable
private fun HardwareSpecStrip(
    hardwareSpec: com.example.ai.DeviceHardwareSpec,
    activeModel: QwenGgufModel?,
    onRefresh: () -> Unit
) {
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

            // Active model pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activeModel != null) CyberAccentCyan.copy(alpha = 0.15f) else CyberBorder.copy(alpha = 0.3f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = activeModel?.name?.take(16) ?: "NO MODEL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activeModel != null) CyberAccentCyan else CyberTextMuted
                )
            }
        }
    }
}

@Composable
private fun ChatTabContent(
    messages: List<AiChatMessage>,
    isGenerating: Boolean,
    tokensPerSecond: Float,
    activeModel: QwenGgufModel?,
    promptInput: String,
    onPromptChange: (String) -> Unit,
    onSendPrompt: () -> Unit,
    onStopGeneration: () -> Unit,
    onClearChat: () -> Unit,
    onRestoreChats: () -> Unit,
    onExportChat: () -> Unit,
    onOpenModelHub: () -> Unit,
    chatListState: androidx.compose.foundation.lazy.LazyListState,
    onShareToPeer: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Chat Header with status & persistent controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (activeModel != null) CyberAccentGreen else CyberAccentAmber)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (activeModel != null) "${activeModel.name} • OFFLINE" else "No model active",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeModel != null) CyberAccentGreen else CyberAccentAmber
                    )
                    Text(
                        text = "Saved in PeerLink/ai_chats/ (Persistent)",
                        fontSize = 9.sp,
                        color = CyberTextMuted
                    )
                }
                if (isGenerating && tokensPerSecond > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${String.format(java.util.Locale.US, "%.1f", tokensPerSecond)} tok/s",
                        fontSize = 11.sp,
                        color = CyberAccentCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Restore button
                IconButton(onClick = onRestoreChats, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Restore History", tint = CyberAccentCyan, modifier = Modifier.size(16.dp))
                }
                // Export button
                IconButton(onClick = onExportChat, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Export .txt", tint = CyberTextSecondary, modifier = Modifier.size(16.dp))
                }
                // Clear button
                IconButton(onClick = onClearChat, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear Chat", tint = CyberTextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // No model downloaded warning banner
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
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = CyberAccentAmber, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("No GGUF Model Downloaded", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberAccentAmber)
                        Text("Tap here to open Model Hub. Once downloaded, files stay saved in PeerLink/ai_models/.", fontSize = 11.sp, color = CyberTextSecondary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Chat message list
        LazyColumn(
            state = chatListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                ChatMessageBubble(message = message, onShare = { onShareToPeer(message.text) })
            }
        }

        // Quick suggestions strip
        if (messages.size <= 2 && !isGenerating) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val suggestions = listOf(
                    "Game booster tweaks for 60 FPS",
                    "Calculate 124 * 85",
                    "Write a Python script",
                    "How does GGUF quantization work?",
                    "What does Shizuku do?",
                    "Explain P2P mesh network"
                )
                suggestions.forEach { suggestion ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(CyberSurface)
                            .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                            .clickable { onPromptChange(suggestion) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(suggestion, fontSize = 11.sp, color = CyberAccentCyan)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Input field and controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
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

        // Bubble Content
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
                } else {
                    CyberMarkdownRenderer(
                        markdown = message.text,
                        baseTextColor = if (isUser) CyberTextPrimary else Color(0xFFE2E8F0),
                        baseFontSize = 13.sp,
                        baseLineHeight = 19.sp
                    )
                }

                // Streaming cursor indicator
                if (message.isStreaming && message.text.isNotEmpty()) {
                    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(400, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "cursor_alpha"
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 8.dp, height = 12.dp)
                            .alpha(alpha)
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

@Composable
private fun ModelHubTabContent(
    models: List<QwenGgufModel>,
    hardwareSpec: com.example.ai.DeviceHardwareSpec,
    persistentPath: String,
    onStartDownload: (String) -> Unit,
    onPauseDownload: (String) -> Unit,
    onDeleteModel: (String) -> Unit,
    onSetActiveModel: (String) -> Unit,
    onRescanModels: () -> Unit,
    onSwitchToChat: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Persistent Storage Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                color = CyberSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = CyberAccentGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Persistent Storage", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CyberTextPrimary)
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
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Folder: $persistentPath",
                        fontSize = 11.sp,
                        color = CyberAccentCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Models and chats are stored in public external storage so they will not be erased if you uninstall and reinstall PeerLink. Tap Rescan below to detect existing models anytime.",
                        fontSize = 11.sp,
                        color = CyberTextSecondary,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        CyberSecondaryButton(
                            text = "Rescan & Restore Models",
                            onClick = onRescanModels,
                            accentColor = CyberAccentCyan,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Qwen GGUF Catalog Overview
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                color = CyberSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberAccentCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Qwen GGUF Offline Model Catalog", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CyberTextPrimary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Download any of the 5 Qwen models below. Once downloaded, the model stays permanently on your device for unlimited, 100% offline chats.",
                        fontSize = 12.sp,
                        color = CyberTextSecondary,
                        lineHeight = 17.sp
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
                onDeleteModel = { onDeleteModel(model.id) },
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
    onDeleteModel: () -> Unit,
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
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Name & Badges
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
                            fontSize = 15.sp,
                            color = CyberTextPrimary
                        )
                        if (model.isActive) {
                            Spacer(modifier = Modifier.width(8.dp))
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
                        text = "${model.parameters} params • ${model.quantization} • ${model.formattedSize}",
                        fontSize = 12.sp,
                        color = CyberAccentCyan
                    )
                }

                // RAM requirement badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCompatible) CyberAccentGreen.copy(alpha = 0.15f) else CyberAccentAmber.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isCompatible) "✓ ${model.minRamFormatted}" else "⚠ ${model.minRamFormatted}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompatible) CyberAccentGreen else CyberAccentAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = model.description,
                fontSize = 12.sp,
                color = CyberTextSecondary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Download Progress Bar (if downloading or paused)
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

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${AiModelDownloader.formatBytes(model.downloadedBytes)} / ${model.formattedSize}",
                            fontSize = 10.sp,
                            color = CyberTextMuted
                        )
                        Text(
                            text = "Range-Resumable",
                            fontSize = 10.sp,
                            color = CyberTextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Action Buttons
            when {
                isCompleted -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!model.isActive) {
                            CyberPrimaryButton(
                                text = "Set as Active",
                                onClick = onSetActiveModel,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            CyberPrimaryButton(
                                text = "Chat Now",
                                onClick = onSwitchToChat,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        CyberSecondaryButton(
                            text = "Delete",
                            onClick = onDeleteModel,
                            modifier = Modifier.width(90.dp),
                            accentColor = CrimsonError
                        )
                    }
                }

                isDownloading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CyberSecondaryButton(
                            text = "Pause",
                            onClick = onPauseDownload,
                            modifier = Modifier.weight(1f),
                            accentColor = CyberAccentAmber
                        )
                        CyberSecondaryButton(
                            text = "Cancel",
                            onClick = onDeleteModel,
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
                            text = "Resume Download",
                            onClick = onStartDownload,
                            modifier = Modifier.weight(1f)
                        )
                        CyberSecondaryButton(
                            text = "Delete",
                            onClick = onDeleteModel,
                            modifier = Modifier.width(90.dp),
                            accentColor = CrimsonError
                        )
                    }
                }

                else -> {
                    // Not downloaded yet
                    CyberPrimaryButton(
                        text = "Download Model (${model.formattedSize})",
                        onClick = onStartDownload,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (model.status == DownloadStatus.ERROR && model.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Error: ${model.errorMessage}",
                    fontSize = 11.sp,
                    color = CrimsonError
                )
            }
        }
    }
}
