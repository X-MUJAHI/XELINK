package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.repository.MessageRepository
import com.example.util.PeerNotificationHelper
import com.example.util.StoragePermissionHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PeerLink", appName)
    }

    @Test
    fun `test storage permission helper`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val description = StoragePermissionHelper.getStatusDescription(context)
        assertNotNull(description)
        assertTrue(description.isNotEmpty())

        val legacyPerms = StoragePermissionHelper.getLegacyStoragePermissions()
        assertTrue(legacyPerms.isNotEmpty())
    }

    @Test
    fun `test chat history locally saved when peer goes offline and restored when online`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val repository = MessageRepository(db.messageDao(), db.conversationDao())

        val peerId = "peer_test_alpha"
        val peerName = "AlphaNode"

        // 1. Send outgoing message while online
        repository.saveOutgoingMessage(
            id = "msg_1",
            peerId = peerId,
            peerName = peerName,
            myId = "my_device",
            myName = "Me",
            content = "Hello AlphaNode!",
            status = "SENT"
        )

        // 2. Receive reply from peer
        repository.saveIncomingMessage(
            id = "msg_2",
            peerId = peerId,
            peerName = peerName,
            content = "Hey there! Ready to chat.",
            senderId = peerId,
            senderName = peerName,
            timestamp = System.currentTimeMillis()
        )

        // 3. Peer goes offline -> update status and save offline marker in local history
        repository.updatePeerOnline(peerId, false)
        db.messageDao().insertMessage(
            MessageEntity(
                id = "sys_offline_1",
                conversationId = peerId,
                senderId = "system",
                senderName = "System",
                content = "$peerName went offline. Chat history saved locally.",
                type = "SYSTEM"
            )
        )

        // Verify conversation is offline and history is preserved locally
        val offlineConvo = db.conversationDao().getConversation(peerId)
        assertNotNull(offlineConvo)
        assertFalse("Conversation should be marked offline", offlineConvo!!.isOnline)

        val offlineMessages = repository.getMessages(peerId).first()
        assertEquals("Should have 3 messages in local history", 3, offlineMessages.size)
        assertEquals("Hello AlphaNode!", offlineMessages[0].content)
        assertEquals("Hey there! Ready to chat.", offlineMessages[1].content)
        assertTrue(offlineMessages[2].content.contains("went offline. Chat history saved locally."))

        // 4. User sends a message while peer is offline (queued in local Room DB)
        repository.saveOutgoingMessage(
            id = "msg_3_queued",
            peerId = peerId,
            peerName = peerName,
            myId = "my_device",
            myName = "Me",
            content = "Leaving this message while you are away.",
            status = "QUEUED"
        )

        val pending = repository.getPendingOutgoingMessagesForPeer(peerId)
        assertEquals(1, pending.size)
        assertEquals("Leaving this message while you are away.", pending[0].content)

        // 5. Peer comes back online -> restore connection and notify
        repository.updatePeerOnline(peerId, true)
        db.messageDao().insertMessage(
            MessageEntity(
                id = "sys_online_1",
                conversationId = peerId,
                senderId = "system",
                senderName = "System",
                content = "$peerName is back online! Connection restored.",
                type = "SYSTEM"
            )
        )

        // Verify conversation is online and full thread is intact
        val reconnectedConvo = db.conversationDao().getConversation(peerId)
        assertNotNull(reconnectedConvo)
        assertTrue("Conversation should be online", reconnectedConvo!!.isOnline)

        val fullHistory = repository.getMessages(peerId).first()
        assertEquals(5, fullHistory.size)
        assertTrue(fullHistory.last().content.contains("is back online! Connection restored."))
    }

    @Test
    fun `test peer reconnection notification helper`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PeerNotificationHelper.initNotificationChannel(context)
        PeerNotificationHelper.notifyPeerBackOnline(context, "peer_test_beta", "BetaNode")
        PeerNotificationHelper.clearNotification(context, "peer_test_beta")
    }
}

