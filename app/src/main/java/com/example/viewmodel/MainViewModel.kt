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
import kotlinx.coroutines.flow.map
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
    val wakeLockManager = com.example.transport.WakeLockManager(application)
    val callManager = CallManager(application, transportManager)
    val screenShareManager = ScreenShareManager(application, transportManager)
    val shizukuManager = ShizukuManager(application)
    val uiScaleManager = com.example.ui.scale.UiScaleManager(application)
    val fileTransferManager = com.example.filetransfer.FileTransferManager(
        application,
        transportManager,
        messageRepository,
        cryptoManager,
        deviceIdentity,
        wakeLockManager
    )

    // Reactive State - Strictly filters out own device so it never appears in chat history or peer lists
    val conversations: StateFlow<List<ConversationEntity>> = messageRepository.allConversations
        .map { list ->
            list.filterNot { convo ->
                convo.peerId == deviceIdentity.deviceId ||
                convo.peerId.startsWith("PL-${deviceIdentity.deviceId}") ||
                convo.peerId.contains(deviceIdentity.deviceId) ||
                convo.peerName.equals(deviceIdentity.deviceName, ignoreCase = true) ||
                transportManager.isSelfAddress(convo.peerIp)
            }
        }
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

        // Retry queue worker: periodically checks for pending messages to online peers
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(4000)
                retryPendingMessages()
            }
        }

        // Immediately trigger retry whenever any peer connects or reconnects
        viewModelScope.launch(Dispatchers.IO) {
            transportManager.peerConnectedEvent.collect {
                retryPendingMessages()
            }
        }

        // Retry whenever new peers are discovered online
        viewModelScope.launch(Dispatchers.IO) {
            transportManager.discoveredDevices.collect { map ->
                if (map.isNotEmpty()) {
                    retryPendingMessages()
                }
            }
        }

        // File received notifications
        viewModelScope.launch {
            fileTransferManager.fileReceivedEvent.collect { progress ->
                _uiToast.emit("Received file: ${progress.fileName}")
            }
        }

        // Automatic WakeLock Management for File Transfers (Max throughput lock)
        viewModelScope.launch {
            fileTransferManager.transfers.collect { transfers ->
                val hasActiveTransfer = transfers.values.any { !it.isComplete && it.error == null }
                if (hasActiveTransfer) {
                    wakeLockManager.acquire("HighSpeedTransfer")
                } else {
                    wakeLockManager.release("HighSpeedTransfer")
                }
            }
        }

        // Automatic WakeLock Management for Calls
        viewModelScope.launch {
            callManager.callInfo.collect { info ->
                if (info != null && info.callState != com.example.calling.CallState.IDLE && info.callState != com.example.calling.CallState.ENDED) {
                    wakeLockManager.acquire("ActiveCall")
                } else {
                    wakeLockManager.release("ActiveCall")
                }
            }
        }

        // Automatic WakeLock Management for Screen Sharing
        viewModelScope.launch {
            screenShareManager.isSharing.collect { sharing ->
                if (sharing) {
                    wakeLockManager.acquire("ScreenCapture")
                } else {
                    wakeLockManager.release("ScreenCapture")
                }
            }
        }

        // Automatic WakeLock Management for Low-Latency Gaming Boost
        viewModelScope.launch {
            shizukuManager.isLowLatencyEnabled.collect { boosted ->
                if (boosted) {
                    wakeLockManager.acquire("GamingLowLatency")
                } else {
                    wakeLockManager.release("GamingLowLatency")
                }
            }
        }
    }

    fun sendFile(uri: android.net.Uri, peerId: String, peerName: String, peerIp: String) {
        if (peerId == deviceIdentity.deviceId || transportManager.isSelfAddress(peerIp)) {
            _uiToast.tryEmit("Cannot transfer file to your own device")
            return
        }
        viewModelScope.launch {
            _uiToast.emit("Starting file transfer...")
            fileTransferManager.sendFile(uri, peerId, peerName, peerIp)
        }
    }

    fun sendMultipleFiles(uris: List<android.net.Uri>, peerId: String, peerName: String, peerIp: String) {
        if (uris.isEmpty()) return
        if (peerId == deviceIdentity.deviceId || transportManager.isSelfAddress(peerIp)) {
            _uiToast.tryEmit("Cannot transfer files to your own device")
            return
        }
        var targetIp = peerIp.trim().removePrefix("/").removePrefix("::ffff:").substringBefore('%')
        if (targetIp.isBlank() || transportManager.isSelfAddress(targetIp)) {
            targetIp = transportManager.discoveredDevices.value[peerId]?.address ?: ""
        }
        val finalIp = targetIp
        viewModelScope.launch {
            _uiToast.emit("Queuing ${uris.size} file(s) for transfer...")
            fileTransferManager.sendMultipleFiles(uris, peerId, peerName, finalIp)
        }
    }

    fun selectTab(tab: String) {
        _currentTab.value = tab
    }

    fun openChat(peerId: String) {
        if (peerId == deviceIdentity.deviceId) {
            _uiToast.tryEmit("Cannot chat with your own device")
            return
        }
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
        if (peerId == deviceIdentity.deviceId || transportManager.isSelfAddress(peerIp)) {
            _uiToast.tryEmit("Cannot send message to your own device")
            return
        }

        val msgId = UUID.randomUUID().toString()
        viewModelScope.launch(Dispatchers.IO) {
            // Persist message locally in SENDING state
            messageRepository.saveOutgoingMessage(
                id = msgId,
                peerId = peerId,
                peerName = peerName,
                myId = deviceIdentity.deviceId,
                myName = deviceIdentity.deviceName,
                content = text,
                peerIp = peerIp,
                status = "SENDING"
            )

            val packet = P2PPacket(
                packetId = msgId,
                type = PacketType.MESSAGE,
                senderId = deviceIdentity.deviceId,
                senderName = deviceIdentity.deviceName,
                targetId = peerId,
                payload = text,
                binaryPayload = null,
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

                val saved = messageRepository.saveIncomingMessage(
                    id = packet.packetId,
                    peerId = peerId,
                    peerName = peerName,
                    content = packet.payload,
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
            val peerIp = peer?.address?.takeIf { it.isNotBlank() }
                ?: db.conversationDao().getConversation(msg.conversationId)?.peerIp?.takeIf { it.isNotBlank() }
                ?: ""

            if (peerIp.isNotBlank() && !transportManager.isSelfAddress(peerIp)) {
                val packet = P2PPacket(
                    packetId = msg.id,
                    type = PacketType.MESSAGE,
                    senderId = deviceIdentity.deviceId,
                    senderName = deviceIdentity.deviceName,
                    targetId = msg.conversationId,
                    payload = msg.content,
                    binaryPayload = null
                )
                val sent = transportManager.sendPacketToIp(peerIp, packet)
                if (sent) {
                    messageRepository.updateMessageStatus(msg.id, "SENT")
                }
            }
        }
    }

    fun manualRetryMessage(msg: MessageEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            messageRepository.updateMessageStatus(msg.id, "SENDING")
            val peer = transportManager.discoveredDevices.value[msg.conversationId]
            val peerIp = peer?.address?.takeIf { it.isNotBlank() }
                ?: db.conversationDao().getConversation(msg.conversationId)?.peerIp?.takeIf { it.isNotBlank() }
                ?: ""

            if (peerIp.isBlank() || transportManager.isSelfAddress(peerIp)) {
                messageRepository.updateMessageStatus(msg.id, "FAILED")
                _uiToast.emit("Peer appears offline. Will auto-resend once connected.")
                return@launch
            }

            val packet = P2PPacket(
                packetId = msg.id,
                type = PacketType.MESSAGE,
                senderId = deviceIdentity.deviceId,
                senderName = deviceIdentity.deviceName,
                targetId = msg.conversationId,
                payload = msg.content,
                binaryPayload = null
            )
            val sent = transportManager.sendPacketToIp(peerIp, packet)
            if (sent) {
                messageRepository.updateMessageStatus(msg.id, "SENT")
                _uiToast.emit("Message resent successfully!")
            } else {
                messageRepository.updateMessageStatus(msg.id, "FAILED")
                _uiToast.emit("Delivery failed. Will retry automatically when online.")
            }
        }
    }

    fun connectDirectIp(ip: String, port: Int = 8988) {
        if (transportManager.isSelfAddress(ip)) {
            _uiToast.tryEmit("Cannot connect to your own device IP ($ip)")
            return
        }
        viewModelScope.launch {
            _uiToast.emit("Connecting to $ip:$port...")
            transportManager.connectToPeer(ip, port, "Direct-$ip")
        }
    }

    fun disconnectPeer(ip: String) {
        viewModelScope.launch {
            transportManager.disconnectPeer(ip)
            _uiToast.emit("Disconnected from $ip")
        }
    }

    fun deleteMessage(msgId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            messageRepository.deleteMessage(msgId)
        }
    }

    fun startVoiceCall(peer: PeerDevice) {
        if (transportManager.isSelf(peer)) {
            _uiToast.tryEmit("Cannot call your own device")
            return
        }
        callManager.initiateCall(peer, CallType.VOICE)
    }

    fun startVideoCall(peer: PeerDevice) {
        if (transportManager.isSelf(peer)) {
            _uiToast.tryEmit("Cannot call your own device")
            return
        }
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
