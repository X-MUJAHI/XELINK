/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: AiManager.kt
 *
 * Commentary / Architectural Overview:
 * Central manager orchestrating on-device offline AI capabilities:
 * - Multi-Session Chat Engine: New Chat, History, Rename, Pin, Delete, and Project Folders.
 * - Model Selector integration: Switch active model seamlessly inside chat.
 * - Saves chats and project sessions continuously to persistent public external storage:
 *     /storage/emulated/0/Download/PeerLink/ai_chats/ai_sessions.json
 * - Auto-detects and restores existing models and multi-session chats on startup.
 * - Zero boilerplate or deflection tokens in responses: purely user-focused intelligence.
 */

package com.example.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class AiManager(private val context: Context) {
    private val tag = "AiManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val downloader = AiModelDownloader(context)
    val inferenceEngine = AiInferenceEngine(context)
    val chatStorage = AiChatStorage(context)

    private val initialSessionId = UUID.randomUUID().toString()
    private val _sessions = MutableStateFlow<List<AiChatSession>>(
        listOf(
            AiChatSession(
                id = initialSessionId,
                title = "New Chat",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                messages = emptyList()
            )
        )
    )
    val sessions: StateFlow<List<AiChatSession>> = _sessions.asStateFlow()

    private val _activeSessionId = MutableStateFlow(initialSessionId)
    val activeSessionId: StateFlow<String> = _activeSessionId.asStateFlow()

    val activeSession: StateFlow<AiChatSession?> = combine(_sessions, _activeSessionId) { list, id ->
        list.find { it.id == id } ?: list.firstOrNull()
    }.stateIn(scope, SharingStarted.Eagerly, null)

    val messages: StateFlow<List<AiChatMessage>> = combine(_sessions, _activeSessionId) { list, id ->
        list.find { it.id == id }?.messages ?: emptyList()
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val availableFolders: StateFlow<List<String>> = _sessions.combine(_sessions) { list, _ ->
        list.mapNotNull { it.folder }.distinct().filter { it.isNotBlank() }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _storageStatusMessage = MutableStateFlow<String?>(null)
    val storageStatusMessage: StateFlow<String?> = _storageStatusMessage.asStateFlow()

    private var currentGenerationJob: Job? = null

    init {
        // Auto-restore chat sessions and detect downloaded models on startup
        scope.launch(Dispatchers.IO) {
            val loadedSessions = chatStorage.loadSessions()
            if (loadedSessions.isNotEmpty()) {
                _sessions.value = loadedSessions
                _activeSessionId.value = loadedSessions.first().id
                Log.i(tag, "Loaded ${loadedSessions.size} persisted AI chat sessions.")
            }
            val restoredModels = downloader.scanAndRestoreModels()
            if (restoredModels > 0) {
                _storageStatusMessage.value = "Restored $restoredModels model(s) from persistent storage."
            }
        }
    }

    /**
     * Starts a new conversation session.
     */
    fun createNewChat(folder: String? = null, modelId: String? = null): String {
        stopGeneration()
        val activeModelName = modelId?.let { id -> downloader.models.value.find { it.id == id }?.name }
            ?: downloader.getActiveModel()?.name

        val newSession = AiChatSession(
            id = UUID.randomUUID().toString(),
            title = "New Chat",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            folder = folder,
            modelNameUsed = activeModelName,
            messages = emptyList()
        )

        _sessions.value = listOf(newSession) + _sessions.value
        _activeSessionId.value = newSession.id
        persistSessions()
        return newSession.id
    }

    fun switchSession(sessionId: String) {
        if (_activeSessionId.value == sessionId) return
        stopGeneration()
        _activeSessionId.value = sessionId
    }

    fun renameSession(sessionId: String, newTitle: String) {
        val trimmed = newTitle.trim().ifEmpty { "Chat" }
        _sessions.value = _sessions.value.map {
            if (it.id == sessionId) it.copy(title = trimmed, updatedAt = System.currentTimeMillis()) else it
        }
        persistSessions()
    }

    fun togglePinSession(sessionId: String) {
        _sessions.value = _sessions.value.map {
            if (it.id == sessionId) it.copy(isPinned = !it.isPinned, updatedAt = System.currentTimeMillis()) else it
        }
        persistSessions()
    }

    fun setSessionFolder(sessionId: String, folder: String?) {
        _sessions.value = _sessions.value.map {
            if (it.id == sessionId) it.copy(folder = folder?.trim()?.ifEmpty { null }, updatedAt = System.currentTimeMillis()) else it
        }
        persistSessions()
    }

    fun deleteSession(sessionId: String) {
        if (_sessions.value.size <= 1) {
            // Keep at least one empty session
            clearCurrentChat()
            return
        }

        if (_activeSessionId.value == sessionId) {
            stopGeneration()
            val remaining = _sessions.value.filterNot { it.id == sessionId }
            _sessions.value = remaining
            _activeSessionId.value = remaining.first().id
        } else {
            _sessions.value = _sessions.value.filterNot { it.id == sessionId }
        }
        persistSessions()
    }

    fun clearCurrentChat() {
        stopGeneration()
        val currentId = _activeSessionId.value
        _sessions.value = _sessions.value.map {
            if (it.id == currentId) it.copy(messages = emptyList(), updatedAt = System.currentTimeMillis()) else it
        }
        persistSessions()
    }

    fun selectModel(modelId: String) {
        downloader.setActiveModel(modelId)
        val selectedModel = downloader.models.value.find { it.id == modelId }
        if (selectedModel != null) {
            _sessions.value = _sessions.value.map {
                if (it.id == _activeSessionId.value) it.copy(modelNameUsed = selectedModel.name) else it
            }
            persistSessions()
        }
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        val currentSession = activeSession.value ?: return
        val isFirstMessage = currentSession.messages.isEmpty()

        // Auto-title session from first user message if still titled "New Chat"
        val updatedTitle = if (isFirstMessage && (currentSession.title == "New Chat" || currentSession.title.startsWith("Chat"))) {
            trimmed.take(32).replace("\n", " ").trim()
        } else {
            currentSession.title
        }

        val userMessage = AiChatMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.USER,
            text = trimmed
        )

        val updatedMessages = currentSession.messages + userMessage
        _sessions.value = _sessions.value.map {
            if (it.id == currentSession.id) {
                it.copy(
                    title = updatedTitle,
                    messages = updatedMessages,
                    updatedAt = System.currentTimeMillis()
                )
            } else it
        }
        persistSessions()

        val activeModel = downloader.getActiveModel()
        if (activeModel == null || activeModel.status != DownloadStatus.COMPLETED) {
            val systemWarning = AiChatMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.ASSISTANT,
                text = "⚠️ **No model active or downloaded.**\n\nTap the model selector above to choose a downloaded model or visit **Model Hub** to download one."
            )
            _sessions.value = _sessions.value.map {
                if (it.id == currentSession.id) it.copy(messages = it.messages + systemWarning) else it
            }
            persistSessions()
            return
        }

        // Placeholder for assistant streaming response
        val assistantMessageId = UUID.randomUUID().toString()
        val assistantMessage = AiChatMessage(
            id = assistantMessageId,
            sender = MessageSender.ASSISTANT,
            text = "",
            isStreaming = true,
            modelNameUsed = activeModel.name
        )

        _sessions.value = _sessions.value.map {
            if (it.id == currentSession.id) it.copy(messages = it.messages + assistantMessage) else it
        }

        currentGenerationJob?.cancel()
        currentGenerationJob = scope.launch {
            val responseBuilder = StringBuilder()
            val startMs = System.currentTimeMillis()
            var tokenCount = 0

            inferenceEngine.generateStreamingResponse(
                model = activeModel,
                history = updatedMessages,
                userPrompt = trimmed
            ).collect { token ->
                responseBuilder.append(token)
                tokenCount++

                _sessions.value = _sessions.value.map { session ->
                    if (session.id == currentSession.id) {
                        val newMsgs = session.messages.map { msg ->
                            if (msg.id == assistantMessageId) {
                                msg.copy(
                                    text = responseBuilder.toString(),
                                    isStreaming = true,
                                    tokensGenerated = tokenCount,
                                    generationTimeMs = System.currentTimeMillis() - startMs
                                )
                            } else msg
                        }
                        session.copy(messages = newMsgs)
                    } else session
                }
            }

            // Mark streaming as completed
            _sessions.value = _sessions.value.map { session ->
                if (session.id == currentSession.id) {
                    val finalMsgs = session.messages.map { msg ->
                        if (msg.id == assistantMessageId) {
                            msg.copy(
                                isStreaming = false,
                                tokensGenerated = tokenCount,
                                generationTimeMs = System.currentTimeMillis() - startMs
                            )
                        } else msg
                    }
                    session.copy(messages = finalMsgs, updatedAt = System.currentTimeMillis())
                } else session
            }

            persistSessions()
        }
    }

    fun stopGeneration() {
        inferenceEngine.requestStop()
        currentGenerationJob?.cancel()
        currentGenerationJob = null
        val currentId = _activeSessionId.value
        _sessions.value = _sessions.value.map { session ->
            if (session.id == currentId) {
                session.copy(messages = session.messages.map { if (it.isStreaming) it.copy(isStreaming = false) else it })
            } else session
        }
        persistSessions()
    }

    fun restoreFromPersistentStorage(onComplete: ((modelsRestored: Int, sessionsRestored: Int) -> Unit)? = null) {
        scope.launch(Dispatchers.IO) {
            val modelsRestored = downloader.scanAndRestoreModels()
            val loadedSessions = chatStorage.loadSessions()
            if (loadedSessions.isNotEmpty()) {
                _sessions.value = loadedSessions
                _activeSessionId.value = loadedSessions.first().id
            }
            _storageStatusMessage.value = "Synced: $modelsRestored model(s) & ${loadedSessions.size} session(s)."
            onComplete?.invoke(modelsRestored, loadedSessions.size)
        }
    }

    suspend fun exportCurrentChat(): File? {
        val current = activeSession.value ?: return null
        return chatStorage.exportChatToText(current)
    }

    private fun persistSessions() {
        scope.launch(Dispatchers.IO) {
            chatStorage.saveSessions(_sessions.value)
        }
    }
}
