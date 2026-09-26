package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calling.CallManager
import com.example.calling.CallType
import com.example.data.local.AppDatabase
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.repository.MessageRepository
import com.example.screenshare.ScreenShareManager
import com.example.security.CryptoManager
import com.example.security.DeviceIdentity
import com.example.shizuku.ShizukuManager
import com.example.transport.TransportManager
import com.example.transport.model.P2PPacket
import com.example.transport.model.PacketType
import com.example.transport.model.PeerDevice
import com.example.transport.model.PeerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val cryptoManager = CryptoManager()
    val deviceIdentity = DeviceIdentity.getOrCreate(application, cryptoManager.localKeyPair)

    private val db = AppDatabase.getInstance(application)
    val messageRepository = MessageRepository(db.messageDao(), db.conversationDao())

    val transportManager = TransportManager(application, deviceIdentity, cryptoManager)
    val callManager = CallManager(application, transportManager)
    val screenShareManager = ScreenShareManager(application, transportManager)
    val shizukuManager = ShizukuManager(application)
    val fileTransferManager = com.example.filetransfer.FileTransferManager(
        application,
        transportManager,
        messageRepository,
        cryptoManager,
        deviceIdentity
    )

    // Reactive State
    val conversations: StateFlow<List<ConversationEntity>> = messageRepository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val discoveredDevices: StateFlow<Map<String, PeerDevice>> = transportManager.discoveredDevices
    val isBroadcasting: StateFlow<Boolean> = transportManager.isBroadcasting
    val isScanning: StateFlow<Boolean> = transportManager.isScanning
    val localIp: StateFlow<String> = transportManager.localIp

    // Active Chat
    private val _selectedConversationPeerId = MutableStateFlow<String?>(null)
    val selectedConversationPeerId: StateFlow<String?> = _selectedConversationPeerId.asStateFlow()

    private val _currentChatMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val currentChatMessages: StateFlow<List<MessageEntity>> = _currentChatMessages.asStateFlow()

    // Notification toast / banner
    private val _uiToast = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val uiToast: SharedFlow<String> = _uiToast.asSharedFlow()

    // Active bottom navigation destination
    private val _currentTab = MutableStateFlow("home")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    init {
        transportManager.initialize()
        transportManager.startAdvertisingAndDiscovery()

        // Handle incoming packets for messages and ACKs
        viewModelScope.launch(Dispatchers.IO) {
            transportManager.incomingPackets.collect { (packet, remoteIp) ->
                handleIncomingPacket(packet, remoteIp)
            }
        }

        // Retry queue worker: periodically checks for pending SENDING messages to online peers
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(5000)
                retryPendingMessages()
            }
        }

        // File received notifications
        viewModelScope.launch {
            fileTransferManager.fileReceivedEvent.collect { progress ->
                _uiToast.emit("Received file: ${progress.fileName}")
            }
        }
    }

    fun sendFile(uri: android.net.Uri, peerId: String, peerName: String, peerIp: String) {
        viewModelScope.launch {
            _uiToast.emit("Starting file transfer...")
            fileTransferManager.sendFile(uri, peerId, peerName, peerIp)
        }
    }

    fun selectTab(tab: String) {
        _currentTab.value = tab
    }

    fun openChat(peerId: String) {
        _selectedConversationPeerId.value = peerId
        viewModelScope.launch {
            messageRepository.markConversationRead(peerId)
            messageRepository.getMessages(peerId).collect { msgs ->
                _currentChatMessages.value = msgs
            }
        }
    }

    fun closeChat() {
        _selectedConversationPeerId.value = null
        _currentChatMessages.value = emptyList()
    }

    fun sendMessage(peerId: String, peerName: String, peerIp: String, text: String) {
        if (text.isBlank()) return

        val msgId = UUID.randomUUID().toString()
        viewModelScope.launch(Dispatchers.IO) {
            // Persist message locally in SENDING state
            val entity = messageRepository.saveOutgoingMessage(
                id = msgId,
                peerId = peerId,
                peerName = peerName,
                myId = deviceIdentity.deviceId,
                myName = deviceIdentity.deviceName,
                content = text,
                peerIp = peerIp,
                status = "SENDING"
            )

            // Encrypt with peer's derived session key if available
            val sessionKey = cryptoManager.getSessionKey(peerId)
            val (payloadText, binPayload) = if (sessionKey != null) {
                val encBytes = cryptoManager.encrypt(text.toByteArray(Charsets.UTF_8), sessionKey)
                Pair("ENCRYPTED_AES_GCM", encBytes)
            } else {
                Pair(text, null)
            }

            val packet = P2PPacket(
                packetId = msgId,
                type = PacketType.MESSAGE,
                senderId = deviceIdentity.deviceId,
                senderName = deviceIdentity.deviceName,
                targetId = peerId,
                payload = payloadText,
                binaryPayload = binPayload,
                extraData = mapOf("peerName" to deviceIdentity.deviceName)
            )

            val targetAddress = if (peerIp.isNotBlank()) {
                peerIp
            } else {
                transportManager.discoveredDevices.value[peerId]?.address ?: ""
            }

            if (targetAddress.isNotBlank()) {
                val sent = transportManager.sendPacketToIp(targetAddress, packet)
                if (sent) {
                    messageRepository.updateMessageStatus(msgId, "SENT")
                } else {
                    messageRepository.updateMessageStatus(msgId, "FAILED")
                }
            } else {
                messageRepository.updateMessageStatus(msgId, "QUEUED")
            }
        }
    }

    private suspend fun handleIncomingPacket(packet: P2PPacket, remoteIp: String) {
        when (packet.type) {
            PacketType.MESSAGE -> {
                val peerId = packet.senderId
                val peerName = packet.senderName
                val cipherBytes = packet.binaryPayload
                val sessionKey = cryptoManager.getSessionKey(peerId)

                val decryptedText = if (packet.payload == "ENCRYPTED_AES_GCM" && cipherBytes != null && sessionKey != null) {
                    try {
                        String(cryptoManager.decrypt(cipherBytes, sessionKey), Charsets.UTF_8)
                    } catch (e: Exception) {
                        "[Encrypted message - Decryption failed]"
                    }
                } else {
                    packet.payload
                }

                val saved = messageRepository.saveIncomingMessage(
                    id = packet.packetId,
                    peerId = peerId,
                    peerName = peerName,
                    content = decryptedText,
                    senderId = peerId,
                    senderName = peerName,
                    timestamp = packet.timestamp,
                    peerIp = remoteIp
                )

                if (saved) {
                    _uiToast.emit("New message from $peerName")
                }
            }

            PacketType.MESSAGE_ACK -> {
                val ackedMsgId = packet.payload
                messageRepository.updateMessageStatus(ackedMsgId, "DELIVERED")
            }

            else -> {}
        }
    }

    private suspend fun retryPendingMessages() {
        val pending = messageRepository.getPendingOutgoingMessages()
        if (pending.isEmpty()) return

        for (msg in pending) {
            val peer = transportManager.discoveredDevices.value[msg.conversationId]
            if (peer != null && peer.status == PeerStatus.CONNECTED && peer.address.isNotBlank()) {
                val sessionKey = cryptoManager.getSessionKey(peer.id)
                val (payloadText, binPayload) = if (sessionKey != null) {
                    val encBytes = cryptoManager.encrypt(msg.content.toByteArray(Charsets.UTF_8), sessionKey)
                    Pair("ENCRYPTED_AES_GCM", encBytes)
                } else {
                    Pair(msg.content, null)
                }

                val packet = P2PPacket(
                    packetId = msg.id,
                    type = PacketType.MESSAGE,
                    senderId = deviceIdentity.deviceId,
                    senderName = deviceIdentity.deviceName,
                    targetId = peer.id,
                    payload = payloadText,
                    binaryPayload = binPayload
                )
                val sent = transportManager.sendPacketToIp(peer.address, packet)
                if (sent) {
                    messageRepository.updateMessageStatus(msg.id, "SENT")
                }
            }
        }
    }

    fun connectDirectIp(ip: String, port: Int = 8988) {
        viewModelScope.launch {
            _uiToast.emit("Connecting to $ip:$port...")
            transportManager.connectToPeer(ip, port, "Direct-$ip")
        }
    }

    fun startVoiceCall(peer: PeerDevice) {
        callManager.initiateCall(peer, CallType.VOICE)
    }

    fun startVideoCall(peer: PeerDevice) {
        callManager.initiateCall(peer, CallType.VIDEO)
    }

    fun updateDeviceName(newName: String) {
        DeviceIdentity.updateDeviceName(getApplication(), newName)
        _uiToast.tryEmit("Device name updated to $newName (Restart to apply everywhere)")
    }

    fun postToast(msg: String) {
        _uiToast.tryEmit(msg)
    }

    override fun onCleared() {
        super.onCleared()
        transportManager.shutdown()
        shizukuManager.cleanUp()
    }
}
