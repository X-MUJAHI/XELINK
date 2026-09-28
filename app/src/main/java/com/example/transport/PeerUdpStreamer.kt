package com.example.transport

import android.util.Log
import com.example.diagnostic.AppDiagnostics
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

class PeerUdpStreamer(
    private val localPort: Int = 8990,
    private val onAudioChunkReceived: (audioData: ByteArray) -> Unit
) {
    private val tag = "PeerUdpStreamer"
    private var socket: DatagramSocket? = null
    private var receiveJob: Job? = null
    private var sendJob: Job? = null
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        AppDiagnostics.log("PeerUdpStreamer", "Coroutine exception: ${throwable.message}", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineExceptionHandler)
    private val sendQueue = Channel<Pair<String, ByteArray>>(Channel.UNLIMITED)

    var packetsReceivedCount = 0L
        private set
    var packetsSentCount = 0L
        private set

    fun start() {
        if (socket != null && !socket!!.isClosed) return

        try {
            // Proper SO_REUSEADDR setup before bind
            val s = DatagramSocket(null)
            s.reuseAddress = true
            s.broadcast = true
            s.sendBufferSize = 64 * 1024
            s.receiveBufferSize = 64 * 1024
            s.bind(InetSocketAddress(localPort))
            socket = s
            AppDiagnostics.log("PeerUdpStreamer", "UDP Audio Streamer bound to port $localPort successfully")

            receiveJob?.cancel()
            receiveJob = scope.launch {
                val buffer = ByteArray(4096)
                while (isActive && socket != null && !socket!!.isClosed) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket?.receive(packet)
                        if (packet.length > 0) {
                            packetsReceivedCount++
                            val data = ByteArray(packet.length)
                            System.arraycopy(packet.data, packet.offset, data, 0, packet.length)
                            onAudioChunkReceived(data)
                        }
                    } catch (e: Throwable) {
                        if (socket != null && !socket!!.isClosed) {
                            Log.w(tag, "UDP receive error: ${e.message}")
                        }
                    }
                }
            }

            sendJob?.cancel()
            sendJob = scope.launch {
                val addressCache = mutableMapOf<String, InetAddress>()
                while (isActive) {
                    val (remoteIp, data) = sendQueue.receive()
                    val curSocket = socket
                    if (curSocket != null && !curSocket.isClosed) {
                        try {
                            val address = addressCache.getOrPut(remoteIp) { InetAddress.getByName(remoteIp) }
                            val packet = DatagramPacket(data, data.size, address, localPort)
                            curSocket.send(packet)
                            packetsSentCount++
                        } catch (e: Throwable) {
                            Log.w(tag, "UDP send failed to $remoteIp: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            AppDiagnostics.log("PeerUdpStreamer", "Failed to bind UDP streamer on $localPort: ${e.message}", e)
        }
    }

    fun sendAudio(remoteIp: String, remotePort: Int = 8990, data: ByteArray) {
        if (socket == null || socket!!.isClosed) {
            start()
        }
        sendQueue.trySend(Pair(remoteIp, data))
    }

    fun stop() {
        receiveJob?.cancel()
        sendJob?.cancel()
        try {
            socket?.close()
        } catch (_: Throwable) {}
        socket = null
        AppDiagnostics.log("PeerUdpStreamer", "UDP Audio Streamer stopped")
    }
}
