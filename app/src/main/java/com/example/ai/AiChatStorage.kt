/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: AiChatStorage.kt
 *
 * Commentary / Architectural Overview:
 * Persistent multi-session chat and project/folder storage coordinator:
 * - Stores all chat sessions in public persistent external storage:
 *     /storage/emulated/0/Download/PeerLink/ai_chats/ai_sessions.json
 * - Supports modern AI features: Multiple chat threads, Session Renaming, Pinning,
 *   Project Folders/Categories, and Deletion.
 * - Backwards compatible with legacy single-session ai_chat_history.json.
 * - Ensures conversations survive app uninstallation and reinstallations.
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
import java.util.UUID

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

    fun getSessionsFile(): File {
        return File(getChatsDirectory(), "ai_sessions.json")
    }

    /**
     * Saves all multi-session chat records to persistent external storage.
     */
    suspend fun saveSessions(sessions: List<AiChatSession>): Boolean = withContext(Dispatchers.IO) {
        try {
            val rootArray = JSONArray()
            for (session in sessions) {
                val sessionObj = JSONObject().apply {
                    put("id", session.id)
                    put("title", session.title)
                    put("createdAt", session.createdAt)
                    put("updatedAt", session.updatedAt)
                    put("isPinned", session.isPinned)
                    put("folder", session.folder ?: "")
                    put("modelNameUsed", session.modelNameUsed ?: "")

                    val msgArray = JSONArray()
                    for (msg in session.messages) {
                        val mObj = JSONObject().apply {
                            put("id", msg.id)
                            put("sender", msg.sender.name)
                            put("text", msg.text)
                            put("timestamp", msg.timestamp)
                            put("modelNameUsed", msg.modelNameUsed ?: "")
                            put("tokensGenerated", msg.tokensGenerated)
                            put("generationTimeMs", msg.generationTimeMs)
                        }
                        msgArray.put(mObj)
                    }
                    put("messages", msgArray)
                }
                rootArray.put(sessionObj)
            }

            val jsonContent = rootArray.toString(2)
            val bytes = jsonContent.toByteArray(StandardCharsets.UTF_8)

            // 1. Write to public persistent external storage (Survives uninstall)
            val externalFile = getSessionsFile()
            externalFile.parentFile?.mkdirs()
            FileOutputStream(externalFile).use { it.write(bytes) }

            // 2. Also write to internal cache for speed
            val internalFile = File(context.filesDir, "ai_sessions.json")
            FileOutputStream(internalFile).use { it.write(bytes) }

            Log.d(tag, "Saved ${sessions.size} chat sessions to ${externalFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed saving chat sessions: ${e.message}", e)
            false
        }
    }

    /**
     * Loads all chat sessions from persistent external storage or fallback files.
     */
    suspend fun loadSessions(): List<AiChatSession> = withContext(Dispatchers.IO) {
        val candidates = listOf(
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PeerLink/ai_chats/ai_sessions.json"),
            File(Environment.getExternalStorageDirectory(), "PeerLink/ai_chats/ai_sessions.json"),
            File(context.filesDir, "ai_sessions.json")
        )

        for (file in candidates) {
            if (file.exists() && file.length() > 5) {
                try {
                    val content = file.readText(StandardCharsets.UTF_8)
                    val jsonArray = JSONArray(content)
                    val sessions = mutableListOf<AiChatSession>()

                    for (i in 0 until jsonArray.length()) {
                        val sObj = jsonArray.getJSONObject(i)
                        val msgArray = sObj.optJSONArray("messages") ?: JSONArray()
                        val msgs = mutableListOf<AiChatMessage>()

                        for (j in 0 until msgArray.length()) {
                            val mObj = msgArray.getJSONObject(j)
                            val senderStr = mObj.optString("sender", MessageSender.USER.name)
                            val sender = try { MessageSender.valueOf(senderStr) } catch (_: Exception) { MessageSender.USER }

                            msgs.add(
                                AiChatMessage(
                                    id = mObj.optString("id", UUID.randomUUID().toString()),
                                    sender = sender,
                                    text = mObj.optString("text", ""),
                                    timestamp = mObj.optLong("timestamp", System.currentTimeMillis()),
                                    isStreaming = false,
                                    modelNameUsed = mObj.optString("modelNameUsed").ifEmpty { null },
                                    tokensGenerated = mObj.optInt("tokensGenerated", 0),
                                    generationTimeMs = mObj.optLong("generationTimeMs", 0L)
                                )
                            )
                        }

                        sessions.add(
                            AiChatSession(
                                id = sObj.optString("id", UUID.randomUUID().toString()),
                                title = sObj.optString("title", "Chat ${i + 1}"),
                                createdAt = sObj.optLong("createdAt", System.currentTimeMillis()),
                                updatedAt = sObj.optLong("updatedAt", System.currentTimeMillis()),
                                isPinned = sObj.optBoolean("isPinned", false),
                                folder = sObj.optString("folder").ifEmpty { null },
                                modelNameUsed = sObj.optString("modelNameUsed").ifEmpty { null },
                                messages = msgs
                            )
                        )
                    }

                    if (sessions.isNotEmpty()) {
                        Log.i(tag, "Loaded ${sessions.size} chat sessions from ${file.absolutePath}")
                        return@withContext sessions
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Failed parsing sessions from ${file.absolutePath}: ${e.message}")
                }
            }
        }

        // Check legacy single-session file if no multi-session file exists
        val legacyMessages = loadLegacyChatHistory()
        if (legacyMessages.isNotEmpty()) {
            val migratedSession = AiChatSession(
                id = UUID.randomUUID().toString(),
                title = "Previous Conversation",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                messages = legacyMessages
            )
            return@withContext listOf(migratedSession)
        }

        // Default empty fresh session
        listOf(
            AiChatSession(
                id = UUID.randomUUID().toString(),
                title = "New Chat",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                messages = emptyList()
            )
        )
    }

    private fun loadLegacyChatHistory(): List<AiChatMessage> {
        val candidates = listOf(
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PeerLink/ai_chats/ai_chat_history.json"),
            File(Environment.getExternalStorageDirectory(), "PeerLink/ai_chats/ai_chat_history.json"),
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
                        val sender = try { MessageSender.valueOf(senderStr) } catch (_: Exception) { MessageSender.USER }
                        messages.add(
                            AiChatMessage(
                                id = obj.optString("id", UUID.randomUUID().toString()),
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
                    if (messages.isNotEmpty()) return messages
                } catch (_: Exception) {}
            }
        }
        return emptyList()
    }

    /**
     * Exports human-readable plain text transcription of a given session.
     */
    suspend fun exportChatToText(session: AiChatSession): File? = withContext(Dispatchers.IO) {
        try {
            val dir = getChatsDirectory()
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            val safeTitle = session.title.replace(Regex("[^a-zA-Z0-9_]"), "_").take(24)
            val exportFile = File(dir, "PeerLink_${safeTitle}_$timestamp.txt")

            val sb = StringBuilder()
            sb.append("====================================================\n")
            sb.append("PeerLink Offline AI - Transcript: ${session.title}\n")
            sb.append("Export Date: ${Date()}\n")
            sb.append("Model: ${session.modelNameUsed ?: "Offline Qwen"}\n")
            sb.append("Total Messages: ${session.messages.size}\n")
            sb.append("====================================================\n\n")

            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            for (msg in session.messages) {
                val senderLabel = when (msg.sender) {
                    MessageSender.USER -> "USER"
                    MessageSender.ASSISTANT -> "AI (${msg.modelNameUsed ?: "Qwen"})"
                    MessageSender.SYSTEM -> "SYSTEM"
                }
                sb.append("[$senderLabel] - ${timeFormat.format(Date(msg.timestamp))}\n")
                sb.append(msg.text.trim())
                sb.append("\n\n----------------------------------------------------\n\n")
            }

            exportFile.writeText(sb.toString(), StandardCharsets.UTF_8)
            exportFile
        } catch (e: Exception) {
            Log.e(tag, "Failed exporting session to text: ${e.message}", e)
            null
        }
    }
}
