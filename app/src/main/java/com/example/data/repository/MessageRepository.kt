package com.example.data.repository

import com.example.data.local.ConversationDao
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageDao
import com.example.data.local.MessageEntity
import kotlinx.coroutines.flow.Flow

class MessageRepository(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao
) {
    val allConversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()

    fun getMessages(conversationId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForConversation(conversationId)
    }

    suspend fun saveOutgoingMessage(
        id: String,
        peerId: String,
        peerName: String,
        myId: String,
        myName: String,
        content: String,
        peerIp: String = "",
        peerPort: Int = 8988,
        status: String = "SENDING"
    ): MessageEntity {
        val now = System.currentTimeMillis()
        val msg = MessageEntity(
            id = id,
            conversationId = peerId,
            senderId = myId,
            senderName = myName,
            content = content,
            timestamp = now,
            status = status,
            isOutgoing = true,
            type = "TEXT"
        )
        messageDao.insertMessage(msg)

        val existing = conversationDao.getConversation(peerId)
        val conversation = existing?.copy(
            lastMessage = content,
            lastTimestamp = now,
            peerIp = if (peerIp.isNotBlank()) peerIp else existing.peerIp,
            peerPort = if (peerPort > 0) peerPort else existing.peerPort
        ) ?: ConversationEntity(
            peerId = peerId,
            peerName = peerName,
            lastMessage = content,
            lastTimestamp = now,
            unreadCount = 0,
            peerIp = peerIp,
            peerPort = peerPort,
            isOnline = true
        )
        conversationDao.upsertConversation(conversation)
        return msg
    }

    suspend fun saveIncomingMessage(
        id: String,
        peerId: String,
        peerName: String,
        content: String,
        senderId: String,
        senderName: String,
        timestamp: Long,
        peerIp: String = "",
        peerPort: Int = 8988
    ): Boolean {
        // Duplicate check
        if (messageDao.messageExists(id)) {
            return false
        }

        val msg = MessageEntity(
            id = id,
            conversationId = peerId,
            senderId = senderId,
            senderName = senderName,
            content = content,
            timestamp = timestamp,
            status = "DELIVERED",
            isOutgoing = false,
            type = "TEXT"
        )
        messageDao.insertMessage(msg)

        val existing = conversationDao.getConversation(peerId)
        val currentUnread = existing?.unreadCount ?: 0
        val conversation = existing?.copy(
            peerName = peerName,
            lastMessage = content,
            lastTimestamp = timestamp,
            unreadCount = currentUnread + 1,
            isOnline = true
        ) ?: ConversationEntity(
            peerId = peerId,
            peerName = peerName,
            lastMessage = content,
            lastTimestamp = timestamp,
            unreadCount = 1,
            peerIp = peerIp,
            peerPort = peerPort,
            isOnline = true
        )
        conversationDao.upsertConversation(conversation)
        return true
    }

    suspend fun updateMessageStatus(messageId: String, status: String) {
        messageDao.updateMessageStatus(messageId, status)
    }

    suspend fun markConversationRead(peerId: String) {
        conversationDao.resetUnreadCount(peerId)
    }

    suspend fun updatePeerOnline(peerId: String, isOnline: Boolean) {
        conversationDao.updateOnlineStatus(peerId, isOnline)
    }

    suspend fun markAllOffline() {
        conversationDao.markAllOffline()
    }

    suspend fun getPendingOutgoingMessages(): List<MessageEntity> {
        return messageDao.getPendingOutgoingMessages()
    }

    suspend fun getPendingOutgoingMessagesForPeer(peerId: String): List<MessageEntity> {
        return messageDao.getPendingOutgoingMessagesForPeer(peerId)
    }

    suspend fun clearConversation(peerId: String) {
        messageDao.clearConversationMessages(peerId)
        conversationDao.deleteConversation(peerId)
    }
}
