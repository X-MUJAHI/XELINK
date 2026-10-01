/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: AiManager.kt
 *
 * Commentary / Architectural Overview:
 * Central manager orchestrating on-device offline AI capabilities:
 * - Coordinates the 5 Qwen GGUF model downloads, filesystem state, and active model selection.
 * - Saves chats continuously to persistent public external storage:
 *     /storage/emulated/0/Download/PeerLink/ai_chats/ai_chat_history.json
 * - Ensures conversations and models survive app uninstallation and reinstallations.
 * - Auto-detects and restores existing models and past chats on startup.
 * - Manages conversation thread state, streaming token collection, and context reset.
 * - Exposes hardware metrics (device RAM, available storage, tokens/sec).
 * - Provides bridge to forward AI-generated answers directly into active P2P mesh chat threads.
 */

package com.example.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class AiManager(private val context: Context) {
    private val tag = "AiManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val downloader = AiModelDownloader(context)
    val inferenceEngine = AiInferenceEngine(context)
    val chatStorage = AiChatStorage(context)

    private val defaultWelcomeMessage = AiChatMessage(
        sender = MessageSender.ASSISTANT,
        text = "Welcome to **PeerLink Offline AI**!\n\n" +
               "• Download any of the 5 Qwen GGUF models in **Model Hub**.\n" +
               "• Models and chats are saved in **PeerLink/ai_models/** and **PeerLink/ai_chats/** so they survive uninstalls and reinstallations.\n" +
               "• 100% offline, zero internet, zero cloud dependency."
    )

    private val _messages = MutableStateFlow<List<AiChatMessage>>(listOf(defaultWelcomeMessage))
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    private val _storageStatusMessage = MutableStateFlow<String?>(null)
    val storageStatusMessage: StateFlow<String?> = _storageStatusMessage.asStateFlow()

    private var currentGenerationJob: Job? = null

    init {
        // Auto-restore chat history and detect downloaded models on startup
        scope.launch(Dispatchers.IO) {
            val restoredChats = chatStorage.loadChatHistory()
            if (restoredChats.isNotEmpty()) {
                _messages.value = restoredChats
                Log.i(tag, "Loaded ${restoredChats.size} persisted AI chat messages.")
            }
            val restoredModels = downloader.scanAndRestoreModels()
            if (restoredModels > 0 || restoredChats.isNotEmpty()) {
                _storageStatusMessage.value = "Restored $restoredModels model(s) & ${restoredChats.size} chat message(s) from PeerLink storage."
            }
        }
    }

    /**
     * Manually triggers a complete rescan and restoration of both models and chats from PeerLink storage.
     */
    fun restoreFromPersistentStorage(onComplete: ((modelsRestored: Int, chatsRestored: Int) -> Unit)? = null) {
        scope.launch(Dispatchers.IO) {
            val modelsRestored = downloader.scanAndRestoreModels()
            val restoredChats = chatStorage.loadChatHistory()
            if (restoredChats.isNotEmpty()) {
                _messages.value = restoredChats
            }
            _storageStatusMessage.value = "Persistent storage synced: $modelsRestored model(s), ${restoredChats.size} message(s)."
            onComplete?.invoke(modelsRestored, restoredChats.size)
        }
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        val userMessage = AiChatMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.USER,
            text = trimmed
        )
        _messages.value = _messages.value + userMessage
        persistChat()

        val activeModel = downloader.getActiveModel()
        if (activeModel == null || activeModel.status != DownloadStatus.COMPLETED) {
            val systemWarning = AiChatMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.ASSISTANT,
                text = "⚠️ **No model downloaded or active.**\n\n" +
                       "Please go to the **Model Hub** tab above and tap **Download** on one of the 5 Qwen models (e.g. Qwen3 0.6B or 1.7B) to enable offline AI generation."
            )
            _messages.value = _messages.value + systemWarning
            persistChat()
            return
        }

        // Create placeholder for assistant response
        val assistantMessageId = UUID.randomUUID().toString()
        val assistantMessage = AiChatMessage(
            id = assistantMessageId,
            sender = MessageSender.ASSISTANT,
            text = "",
            isStreaming = true,
            modelNameUsed = activeModel.name
        )
        _messages.value = _messages.value + assistantMessage

        currentGenerationJob?.cancel()
        currentGenerationJob = scope.launch {
            val responseBuilder = StringBuilder()
            val startMs = System.currentTimeMillis()
            var tokenCount = 0

            inferenceEngine.generateStreamingResponse(
                model = activeModel,
                history = _messages.value.dropLast(1),
                userPrompt = trimmed
            ).collect { token ->
                responseBuilder.append(token)
                tokenCount++

                _messages.value = _messages.value.map { msg ->
                    if (msg.id == assistantMessageId) {
                        msg.copy(
                            text = responseBuilder.toString(),
                            isStreaming = true,
                            tokensGenerated = tokenCount,
                            generationTimeMs = System.currentTimeMillis() - startMs
                        )
                    } else msg
                }
            }

            // Mark streaming as completed
            _messages.value = _messages.value.map { msg ->
                if (msg.id == assistantMessageId) {
                    msg.copy(
                        isStreaming = false,
                        tokensGenerated = tokenCount,
                        generationTimeMs = System.currentTimeMillis() - startMs
                    )
                } else msg
            }

            persistChat()
        }
    }

    fun stopGeneration() {
        inferenceEngine.requestStop()
        currentGenerationJob?.cancel()
        currentGenerationJob = null
        _messages.value = _messages.value.map { msg ->
            if (msg.isStreaming) msg.copy(isStreaming = false) else msg
        }
        persistChat()
    }

    fun clearChat() {
        stopGeneration()
        val clearedMsg = AiChatMessage(
            sender = MessageSender.ASSISTANT,
            text = "Chat history cleared. Active model: **${downloader.getActiveModel()?.name ?: "None"}**. How can I help you today?"
        )
        _messages.value = listOf(clearedMsg)
        persistChat()
    }

    suspend fun exportChatToText(): File? {
        return chatStorage.exportChatToText(_messages.value)
    }

    private fun persistChat() {
        scope.launch(Dispatchers.IO) {
            chatStorage.saveChatHistory(_messages.value)
        }
    }
}
