/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: AiChatStorage.kt
 *
 * Commentary / Architectural Overview:
 * Persistent storage and backup/restore coordinator for Offline AI models and chat histories:
 * - Stores AI chat transcripts in public external storage:
 *     /storage/emulated/0/Download/PeerLink/ai_chats/ai_chat_history.json
 *     /storage/emulated/0/PeerLink/ai_chats/ai_chat_history.json
 * - Ensures all conversations survive app uninstallation and reinstallations.
 * - Automatically scans and restores prior chat histories and downloaded GGUF models on startup.
 * - Provides export to human-readable plain text and Room P2P chat database backup.
 */

package com.example.ai

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AiChatStorage(private val context: Context) {
    private val tag = "AiChatStorage"

    /**
     * Resolves the permanent external storage directory for AI chats:
     * Survives uninstallation because it is located outside /Android/data/.
     */
    fun getChatsDirectory(): File {
        val candidates = listOf(
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PeerLink/ai_chats"),
            File(Environment.getExternalStorageDirectory(), "PeerLink/ai_chats"),
            File(context.getExternalFilesDir(null), "PeerLink/ai_chats")
        )
        for (candidate in candidates) {
            if (!candidate.exists()) {
                try {
                    candidate.mkdirs()
                } catch (e: Exception) {
                    Log.w(tag, "Could not mkdirs ${candidate.absolutePath}: ${e.message}")
                }
            }
            if (candidate.exists() && candidate.canWrite()) {
                return candidate
            }
        }
        val fallback = File(context.filesDir, "ai_chats")
        fallback.mkdirs()
        return fallback
    }

    /**
     * Resolves the primary persistent JSON file for chat history.
     */
    fun getChatHistoryFile(): File {
        return File(getChatsDirectory(), "ai_chat_history.json")
    }

    /**
     * Saves the current AI chat conversation to persistent storage.
     * Writes to both the public persistent PeerLink/ai_chats directory
     * and the internal private cache for redundancy.
     */
    suspend fun saveChatHistory(messages: List<AiChatMessage>): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()
            for (msg in messages) {
                val obj = JSONObject().apply {
                    put("id", msg.id)
                    put("sender", msg.sender.name)
                    put("text", msg.text)
                    put("timestamp", msg.timestamp)
                    put("isStreaming", false) // Never persist streaming state
                    put("modelNameUsed", msg.modelNameUsed ?: "")
                    put("tokensGenerated", msg.tokensGenerated)
                    put("generationTimeMs", msg.generationTimeMs)
                }
                jsonArray.put(obj)
            }

            val jsonContent = jsonArray.toString(2)
            val bytes = jsonContent.toByteArray(StandardCharsets.UTF_8)

            // 1. Write to public persistent external storage (Survives uninstall)
            val externalFile = getChatHistoryFile()
            externalFile.parentFile?.mkdirs()
            FileOutputStream(externalFile).use { it.write(bytes) }

            // 2. Also write to internal cache for ultra-fast startup fallback
            val internalFile = File(context.filesDir, "ai_chat_history.json")
            FileOutputStream(internalFile).use { it.write(bytes) }

            Log.d(tag, "Saved ${messages.size} chat messages to ${externalFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to save AI chat history: ${e.message}", e)
            false
        }
    }

    /**
     * Restores chat history from persistent storage.
     * Checks public external directories first, then internal storage.
     */
    suspend fun loadChatHistory(): List<AiChatMessage> = withContext(Dispatchers.IO) {
        val candidates = listOf(
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PeerLink/ai_chats/ai_chat_history.json"),
            File(Environment.getExternalStorageDirectory(), "PeerLink/ai_chats/ai_chat_history.json"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PeerLink/ai_chat_history.json"),
            File(context.filesDir, "ai_chat_history.json")
        )

        for (file in candidates) {
            if (file.exists() && file.length() > 5) {
                try {
                    val content = file.readText(StandardCharsets.UTF_8)
                    val jsonArray = JSONArray(content)
                    val messages = mutableListOf<AiChatMessage>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val senderStr = obj.optString("sender", MessageSender.USER.name)
                        val sender = try {
                            MessageSender.valueOf(senderStr)
                        } catch (_: Exception) {
                            MessageSender.USER
                        }

                        messages.add(
                            AiChatMessage(
                                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                                sender = sender,
                                text = obj.optString("text", ""),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                                isStreaming = false,
                                modelNameUsed = obj.optString("modelNameUsed").ifEmpty { null },
                                tokensGenerated = obj.optInt("tokensGenerated", 0),
                                generationTimeMs = obj.optLong("generationTimeMs", 0L)
                            )
                        )
                    }

                    if (messages.isNotEmpty()) {
                        Log.i(tag, "Successfully restored ${messages.size} messages from ${file.absolutePath}")
                        return@withContext messages
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Failed parsing chat history from ${file.absolutePath}: ${e.message}")
                }
            }
        }
        emptyList()
    }

    /**
     * Exports human-readable plain text transcription of current chat.
     */
    suspend fun exportChatToText(messages: List<AiChatMessage>): File? = withContext(Dispatchers.IO) {
        try {
            val dir = getChatsDirectory()
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            val exportFile = File(dir, "PeerLink_AiChat_Export_$timestamp.txt")

            val sb = StringBuilder()
            sb.append("====================================================\n")
            sb.append("PeerLink Offline AI - Chat Transcript Export\n")
            sb.append("Export Date: ${Date()}\n")
            sb.append("Total Messages: ${messages.size}\n")
            sb.append("Storage Location: ${exportFile.absolutePath}\n")
            sb.append("====================================================\n\n")

            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            for (msg in messages) {
                val senderLabel = when (msg.sender) {
                    MessageSender.USER -> "USER"
                    MessageSender.ASSISTANT -> "OFFLINE AI (${msg.modelNameUsed ?: "Qwen"})"
                    MessageSender.SYSTEM -> "SYSTEM"
                }
                sb.append("[$senderLabel] - ${timeFormat.format(Date(msg.timestamp))}\n")
                sb.append(msg.text.trim())
                sb.append("\n\n----------------------------------------------------\n\n")
            }

            exportFile.writeText(sb.toString(), StandardCharsets.UTF_8)
            exportFile
        } catch (e: Exception) {
            Log.e(tag, "Failed exporting chat to text: ${e.message}", e)
            null
        }
    }

    /**
     * Returns true if a persistent backup exists from a previous installation.
     */
    fun hasPersistentBackup(): Boolean {
        val candidates = listOf(
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PeerLink/ai_chats/ai_chat_history.json"),
            File(Environment.getExternalStorageDirectory(), "PeerLink/ai_chats/ai_chat_history.json")
        )
        return candidates.any { it.exists() && it.length() > 5 }
    }
}
