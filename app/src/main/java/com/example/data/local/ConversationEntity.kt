package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val peerId: String,
    val peerName: String,
    val lastMessage: String = "",
    val lastTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val peerIp: String = "",
    val peerPort: Int = 8988,
    val keyFingerprint: String = "",
    val isOnline: Boolean = false
)
