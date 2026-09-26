package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SENT", // "SENDING", "SENT", "DELIVERED", "FAILED"
    val isOutgoing: Boolean = false,
    val type: String = "TEXT", // "TEXT", "FILE", "SYSTEM", "CALL_RECORD"
    val filePath: String? = null,
    val fileSize: Long = 0L,
    val mimeType: String? = null
)
