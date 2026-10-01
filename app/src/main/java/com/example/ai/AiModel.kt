/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: AiModel.kt
 *
 * Commentary / Architectural Overview:
 * Models and state definitions for on-device offline LLM execution:
 * - Represents the 5 quantized Qwen3 GGUF models (0.6B to 14B parameters).
 * - Tracks download lifecycle (Not Downloaded, Downloading, Completed, Failed).
 * - Computes device RAM and storage compatibility to prevent Android OOM aborts.
 * - Represents offline chat messages with streaming state, token counts, and timestamps.
 */

package com.example.ai

import java.util.UUID

enum class DownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    ERROR
}

data class QwenGgufModel(
    val id: String,
    val name: String,
    val parameters: String,
    val quantization: String,
    val estimatedSizeBytes: Long,
    val formattedSize: String,
    val minRamBytes: Long,
    val minRamFormatted: String,
    val recommendedTier: String,
    val downloadUrl: String,
    val localFileName: String,
    val description: String,
    val isRecommendedForDevice: Boolean = false,
    val status: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
    val downloadProgress: Float = 0f, // 0.0 to 1.0
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val downloadSpeedBps: Long = 0L,
    val errorMessage: String? = null,
    val localFilePath: String? = null,
    val localFileSize: Long = 0L,
    val isActive: Boolean = false
)

data class AiChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val modelNameUsed: String? = null,
    val tokensGenerated: Int = 0,
    val generationTimeMs: Long = 0L
)

enum class MessageSender {
    USER,
    ASSISTANT,
    SYSTEM
}

data class DeviceHardwareSpec(
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val totalRamFormatted: String,
    val availableRamFormatted: String,
    val freeStorageBytes: Long,
    val freeStorageFormatted: String,
    val cpuCores: Int,
    val archName: String
)

data class GgufMetadata(
    val magic: String,
    val version: UInt,
    val tensorCount: ULong,
    val kvCount: ULong,
    val architecture: String?,
    val modelName: String?,
    val contextLength: Long?,
    val isValidGguf: Boolean
)

data class AiChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Chat",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val folder: String? = null,
    val modelNameUsed: String? = null,
    val messages: List<AiChatMessage> = emptyList()
)

