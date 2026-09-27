package com.example.calling

import android.content.Context
import android.util.Log
import com.example.transport.TransportManager
import com.example.transport.model.P2PPacket
import com.example.transport.model.PacketType
import com.example.transport.model.PeerDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _callInfo = MutableStateFlow<ActiveCallInfo?>(null)
    val callInfo: StateFlow<ActiveCallInfo?> = _callInfo.asStateFlow()

    val audioCallManager = AudioCallManager(context) { audioChunk ->
        val current = _callInfo.value ?: return@AudioCallManager
        if (current.callState == CallState.CONNECTED && current.peerIp.isNotBlank()) {
            transportManager.sendAudio(current.peerIp, audioChunk)
        }
    }

    val videoCallManager = VideoCallManager(context) { jpegBytes ->
        val current = _callInfo.value ?: return@VideoCallManager
        if (current.callState == CallState.CONNECTED && current.callType == CallType.VIDEO && current.peerIp.isNotBlank()) {
            val packet = P2PPacket(
                type = PacketType.VIDEO_FRAME,
                senderId = transportManager.deviceIdentity.deviceId,
                senderName = transportManager.deviceIdentity.deviceName,
                targetId = current.peerId,
                binaryPayload = jpegBytes
            )
            scope.launch(Dispatchers.IO) {
                transportManager.sendPacketToIp(current.peerIp, packet)
            }
        }
    }

    private var timerJob: Job? = null

    init {
        // Listen for incoming audio datagrams
        scope.launch(Dispatchers.IO) {
            transportManager.incomingAudio.collect { audioData ->
                if (_callInfo.value?.callState == CallState.CONNECTED) {
                    audioCallManager.playAudioChunk(audioData)
                }
            }
        }

        // Listen for incoming packets (call signaling & video frames)
        scope.launch(Dispatchers.IO) {
            transportManager.incomingPackets.collect { (packet, remoteIp) ->
                handlePacket(packet, remoteIp)
            }
        }
    }

    fun initiateCall(peer: PeerDevice, type: CallType) {
        if (_callInfo.value != null && _callInfo.value?.callState != CallState.IDLE) return

        _callInfo.value = ActiveCallInfo(
            peerId = peer.id,
            peerName = peer.name,
            peerIp = peer.address,
            callType = type,
            callState = CallState.OUTGOING_RINGING
        )

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
            transportManager.sendPacketToIp(peer.address, offerPacket)
        }
    }

    fun acceptCall() {
        val current = _callInfo.value ?: return
        if (current.callState != CallState.INCOMING_RINGING) return

        _callInfo.value = current.copy(callState = CallState.CONNECTED)
        startCallSession(current.callType)

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
            transportManager.sendPacketToIp(current.peerIp, answerPacket)
        }
    }

    fun declineCall() {
        val current = _callInfo.value ?: return
        val rejectPacket = P2PPacket(
            type = PacketType.CALL_REJECT,
            senderId = transportManager.deviceIdentity.deviceId,
            senderName = transportManager.deviceIdentity.deviceName,
            targetId = current.peerId,
            payload = "DECLINED"
        )
        scope.launch(Dispatchers.IO) {
            transportManager.sendPacketToIp(current.peerIp, rejectPacket)
        }
        cleanupCall()
    }

    fun endCall() {
        val current = _callInfo.value ?: return
        val hangupPacket = P2PPacket(
            type = PacketType.CALL_HANGUP,
            senderId = transportManager.deviceIdentity.deviceId,
            senderName = transportManager.deviceIdentity.deviceName,
            targetId = current.peerId,
            payload = "HANGUP"
        )
        scope.launch(Dispatchers.IO) {
            transportManager.sendPacketToIp(current.peerIp, hangupPacket)
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
                        transportManager.sendPacketToIp(remoteIp, reject)
                    }
                }
            }

            PacketType.CALL_ANSWER -> {
                val current = _callInfo.value
                if (current != null && current.callState == CallState.OUTGOING_RINGING) {
                    val answerIp = packet.extraData["ip"]?.takeIf { it.isNotBlank() && !transportManager.isSelfAddress(it) } ?: current.peerIp
                    scope.launch(Dispatchers.Main) {
                        _callInfo.value = current.copy(
                            callState = CallState.CONNECTED,
                            peerIp = answerIp
                        )
                        startCallSession(current.callType)
                    }
                }
            }

            PacketType.CALL_REJECT, PacketType.CALL_HANGUP -> {
                scope.launch(Dispatchers.Main) {
                    cleanupCall()
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

    private fun startCallSession(type: CallType) {
        audioCallManager.startCall()
        startCallTimer()
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
        _callInfo.value = null
        Log.d(tag, "Call session cleaned up")
    }
}
