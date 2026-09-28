package com.example.calling

import android.content.Context
import android.util.Log
import com.example.diagnostic.AppDiagnostics
import com.example.transport.TransportManager
import com.example.transport.model.P2PPacket
import com.example.transport.model.PacketType
import com.example.transport.model.PeerDevice
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class CallState {
    IDLE,
    OUTGOING_RINGING,
    INCOMING_RINGING,
    CONNECTED,
    ENDED
}

enum class CallType {
    VOICE,
    VIDEO
}

data class ActiveCallInfo(
    val peerId: String,
    val peerName: String,
    val peerIp: String,
    val callType: CallType,
    val callState: CallState,
    val durationSeconds: Long = 0L
)

class CallManager(
    private val context: Context,
    private val transportManager: TransportManager
) {
    private val tag = "CallManager"
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        AppDiagnostics.log("CallManager", "Uncaught coroutine exception: ${throwable.message}", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + coroutineExceptionHandler)

    private val _callInfo = MutableStateFlow<ActiveCallInfo?>(null)
    val callInfo: StateFlow<ActiveCallInfo?> = _callInfo.asStateFlow()

    private var isSendingFrame = false

    val audioCallManager = AudioCallManager(context) { audioChunk ->
        val current = _callInfo.value ?: return@AudioCallManager
        if (current.callState == CallState.CONNECTED && current.peerIp.isNotBlank()) {
            transportManager.sendAudio(current.peerIp, audioChunk)
        }
    }

    val videoCallManager = VideoCallManager(
        context = context,
        onVideoFrameReady = { jpegBytes ->
        val current = _callInfo.value ?: return@VideoCallManager
        if (current.callState == CallState.CONNECTED && current.callType == CallType.VIDEO && current.peerIp.isNotBlank()) {
            if (isSendingFrame) return@VideoCallManager
            isSendingFrame = true
            val packet = P2PPacket(
                type = PacketType.VIDEO_FRAME,
                senderId = transportManager.deviceIdentity.deviceId,
                senderName = transportManager.deviceIdentity.deviceName,
                targetId = current.peerId,
                binaryPayload = jpegBytes
            )
            scope.launch(Dispatchers.IO) {
                try {
                    transportManager.sendPacketToIp(current.peerIp, packet)
                } catch (t: Throwable) {
                    AppDiagnostics.log("CallManager", "Video frame send error: ${t.message}")
                } finally {
                    isSendingFrame = false
                }
            }
        }
    }
)

    val callRecordingManager = CallRecordingManager(context)

    private var timerJob: Job? = null

    init {
        videoCallManager.onRemoteFrameDecoded = { bitmap ->
            callRecordingManager.feedVideoFrame(bitmap)
        }
        // Listen for incoming audio datagrams (UDP)
        scope.launch(Dispatchers.IO) {
            try {
                transportManager.incomingAudio.collect { audioData ->
                    if (_callInfo.value?.callState == CallState.CONNECTED) {
                        audioCallManager.playAudioChunk(audioData)
                    }
                }
            } catch (t: Throwable) {
                AppDiagnostics.log("CallManager", "Incoming audio collector error: ${t.message}", t)
            }
        }

        // Listen for incoming packets (call signaling, video frames, TCP audio)
        scope.launch(Dispatchers.IO) {
            try {
                transportManager.incomingPackets.collect { (packet, remoteIp) ->
                    handlePacket(packet, remoteIp)
                }
            } catch (t: Throwable) {
                AppDiagnostics.log("CallManager", "Incoming packet collector error: ${t.message}", t)
            }
        }
    }

    fun initiateCall(peer: PeerDevice, type: CallType) {
        if (_callInfo.value != null && _callInfo.value?.callState != CallState.IDLE && _callInfo.value?.callState != CallState.ENDED) {
            AppDiagnostics.log("CallManager", "Cannot initiate call: already in call session")
            return
        }

        AppDiagnostics.log("CallManager", "Initiating ${type.name} call to ${peer.name} (${peer.address})")

        _callInfo.value = ActiveCallInfo(
            peerId = peer.id,
            peerName = peer.name,
            peerIp = peer.address,
            callType = type,
            callState = CallState.OUTGOING_RINGING
        )

        // Pre-warm client connection
        scope.launch(Dispatchers.IO) {
            try {
                transportManager.connectToPeer(peer.address)
            } catch (_: Throwable) {}
        }

        val offerPacket = P2PPacket(
            type = PacketType.CALL_OFFER,
            senderId = transportManager.deviceIdentity.deviceId,
            senderName = transportManager.deviceIdentity.deviceName,
            targetId = peer.id,
            payload = type.name,
            extraData = mapOf(
                "callType" to type.name,
                "ip" to transportManager.localIp.value,
                "senderName" to transportManager.deviceIdentity.deviceName
            )
        )

        scope.launch(Dispatchers.IO) {
            try {
                transportManager.sendPacketToIp(peer.address, offerPacket)
            } catch (t: Throwable) {
                AppDiagnostics.log("CallManager", "Failed to send call offer: ${t.message}", t)
            }
        }
    }

    fun acceptCall() {
        val current = _callInfo.value ?: return
        if (current.callState != CallState.INCOMING_RINGING) return

        AppDiagnostics.log("CallManager", "Accepting call from ${current.peerName} (${current.peerIp})")

        _callInfo.value = current.copy(callState = CallState.CONNECTED)
        startCallSession(current.callType, current.peerName)

        // Ensure active client socket is established back to caller
        scope.launch(Dispatchers.IO) {
            try {
                transportManager.connectToPeer(current.peerIp)
            } catch (_: Throwable) {}
        }

        val answerPacket = P2PPacket(
            type = PacketType.CALL_ANSWER,
            senderId = transportManager.deviceIdentity.deviceId,
            senderName = transportManager.deviceIdentity.deviceName,
            targetId = current.peerId,
            payload = "ACCEPTED",
            extraData = mapOf(
                "callType" to current.callType.name,
                "ip" to transportManager.localIp.value
            )
        )

        scope.launch(Dispatchers.IO) {
            try {
                transportManager.sendPacketToIp(current.peerIp, answerPacket)
            } catch (t: Throwable) {
                AppDiagnostics.log("CallManager", "Failed to send call answer: ${t.message}", t)
            }
        }
    }

    fun declineCall() {
        val current = _callInfo.value ?: return
        AppDiagnostics.log("CallManager", "Declining call from ${current.peerName}")
        val rejectPacket = P2PPacket(
            type = PacketType.CALL_REJECT,
            senderId = transportManager.deviceIdentity.deviceId,
            senderName = transportManager.deviceIdentity.deviceName,
            targetId = current.peerId,
            payload = "DECLINED"
        )
        scope.launch(Dispatchers.IO) {
            try {
                transportManager.sendPacketToIp(current.peerIp, rejectPacket)
            } catch (_: Throwable) {}
        }
        cleanupCall()
    }

    fun endCall() {
        val current = _callInfo.value ?: return
        AppDiagnostics.log("CallManager", "Ending call with ${current.peerName}")
        val hangupPacket = P2PPacket(
            type = PacketType.CALL_HANGUP,
            senderId = transportManager.deviceIdentity.deviceId,
            senderName = transportManager.deviceIdentity.deviceName,
            targetId = current.peerId,
            payload = "HANGUP"
        )
        scope.launch(Dispatchers.IO) {
            try {
                transportManager.sendPacketToIp(current.peerIp, hangupPacket)
            } catch (_: Throwable) {}
        }
        cleanupCall()
    }

    private fun handlePacket(packet: P2PPacket, remoteIp: String) {
        when (packet.type) {
            PacketType.CALL_OFFER -> {
                if (_callInfo.value == null || _callInfo.value?.callState == CallState.IDLE || _callInfo.value?.callState == CallState.ENDED) {
                    val typeStr = packet.extraData["callType"] ?: packet.payload
                    val callType = if (typeStr == "VIDEO") CallType.VIDEO else CallType.VOICE
                    val senderIp = packet.extraData["ip"]?.takeIf { it.isNotBlank() && !transportManager.isSelfAddress(it) } ?: remoteIp

                    AppDiagnostics.log("CallManager", "Received CALL_OFFER from ${packet.senderName} @ $senderIp ($callType)")

                    // Immediately pre-warm client socket back to sender
                    scope.launch(Dispatchers.IO) {
                        try {
                            transportManager.connectToPeer(senderIp)
                        } catch (_: Throwable) {}
                    }

                    scope.launch(Dispatchers.Main) {
                        _callInfo.value = ActiveCallInfo(
                            peerId = packet.senderId,
                            peerName = packet.senderName.ifBlank { "Nearby Peer" },
                            peerIp = senderIp,
                            callType = callType,
                            callState = CallState.INCOMING_RINGING
                        )
                    }
                } else {
                    // Send Busy signal
                    val reject = P2PPacket(
                        type = PacketType.CALL_REJECT,
                        senderId = transportManager.deviceIdentity.deviceId,
                        senderName = transportManager.deviceIdentity.deviceName,
                        targetId = packet.senderId,
                        payload = "BUSY"
                    )
                    scope.launch(Dispatchers.IO) {
                        try {
                            transportManager.sendPacketToIp(remoteIp, reject)
                        } catch (_: Throwable) {}
                    }
                }
            }

            PacketType.CALL_ANSWER -> {
                val current = _callInfo.value
                if (current != null && current.callState == CallState.OUTGOING_RINGING) {
                    val answerIp = packet.extraData["ip"]?.takeIf { it.isNotBlank() && !transportManager.isSelfAddress(it) } ?: current.peerIp
                    AppDiagnostics.log("CallManager", "Received CALL_ANSWER from ${packet.senderName} @ $answerIp")

                    // Pre-warm client connection
                    scope.launch(Dispatchers.IO) {
                        try {
                            transportManager.connectToPeer(answerIp)
                        } catch (_: Throwable) {}
                    }

                    scope.launch(Dispatchers.Main) {
                        _callInfo.value = current.copy(
                            callState = CallState.CONNECTED,
                            peerIp = answerIp
                        )
                        startCallSession(current.callType, current.peerName)
                    }
                }
            }

            PacketType.CALL_REJECT, PacketType.CALL_HANGUP -> {
                AppDiagnostics.log("CallManager", "Received ${packet.type} from ${packet.senderName}")
                scope.launch(Dispatchers.Main) {
                    cleanupCall()
                }
            }

            PacketType.AUDIO_CHUNK -> {
                val bytes = packet.binaryPayload
                if (bytes != null && _callInfo.value?.callState == CallState.CONNECTED) {
                    audioCallManager.playAudioChunk(bytes)
                }
            }

            PacketType.VIDEO_FRAME -> {
                val bytes = packet.binaryPayload
                if (bytes != null && _callInfo.value?.callState == CallState.CONNECTED) {
                    videoCallManager.onRemoteFrameReceived(bytes)
                }
            }

            else -> {}
        }
    }

    private fun startCallSession(type: CallType, peerName: String) {
        AppDiagnostics.log("CallManager", "Starting call session: type=$type, peer=$peerName")
        audioCallManager.startCall()
        startCallTimer()
        if (type == CallType.VIDEO && callRecordingManager.isAutoRecordEnabled.value) {
            callRecordingManager.startRecording(peerName)
        }
    }

    private fun startCallTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            var seconds = 0L
            while (isActive && _callInfo.value?.callState == CallState.CONNECTED) {
                delay(1000)
                seconds++
                _callInfo.value = _callInfo.value?.copy(durationSeconds = seconds)
            }
        }
    }

    private fun cleanupCall() {
        timerJob?.cancel()
        audioCallManager.stopCall()
        videoCallManager.stop()
        try {
            callRecordingManager.stopRecording()
        } catch (_: Throwable) {}
        _callInfo.value = null
        AppDiagnostics.log("CallManager", "Call session cleaned up")
    }
}
