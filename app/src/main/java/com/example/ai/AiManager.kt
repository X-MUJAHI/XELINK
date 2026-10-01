/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: AiManager.kt
 *
 * Commentary / Architectural Overview:
 * Central manager orchestrating on-device offline AI capabilities:
 * - Coordinates the 5 Qwen GGUF model downloads, filesystem state, and active model selection.
 * - Manages conversation thread state, streaming token collection, and context reset.
 * - Exposes hardware metrics (device RAM, available storage, tokens/sec).
 * - Provides bridge to forward AI-generated answers directly into active P2P mesh chat threads.
 */

package com.example.ai

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AiManager(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val downloader = AiModelDownloader(context)
    val inferenceEngine = AiInferenceEngine(context)

    private val _messages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "Welcome to **PeerLink Offline AI**!\n\n" +
                       "You can run fully offline Qwen LLM models directly on your device. " +
                       "Download one of the 5 Qwen models in the **Model Hub** below, and chat with unlimited local intelligence without any internet connection."
            )
        )
    )
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    private var currentGenerationJob: Job? = null

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        val userMessage = AiChatMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.USER,
            text = trimmed
        )
        _messages.value = _messages.value + userMessage

        val activeModel = downloader.getActiveModel()
        if (activeModel == null || activeModel.status != DownloadStatus.COMPLETED) {
            val systemWarning = AiChatMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.ASSISTANT,
                text = "⚠️ **No model downloaded or active.**\n\n" +
                       "Please go to the **Model Hub** tab above and tap **Download** on one of the 5 Qwen models (e.g. Qwen3 0.6B or 1.7B) to enable offline AI generation."
            )
            _messages.value = _messages.value + systemWarning
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
        }
    }

    fun stopGeneration() {
        inferenceEngine.requestStop()
        currentGenerationJob?.cancel()
        currentGenerationJob = null
        _messages.value = _messages.value.map { msg ->
            if (msg.isStreaming) msg.copy(isStreaming = false) else msg
        }
    }

    fun clearChat() {
        stopGeneration()
        _messages.value = listOf(
            AiChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "Chat history cleared. Active model: **${downloader.getActiveModel()?.name ?: "None"}**. How can I help you today?"
            )
        )
    }
}
