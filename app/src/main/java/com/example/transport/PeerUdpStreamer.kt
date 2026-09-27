package com.example.transport

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class PeerUdpStreamer(
    private val localPort: Int = 8990,
    private val onAudioChunkReceived: (audioData: ByteArray) -> Unit
) {
    private val tag = "PeerUdpStreamer"
    private var socket: DatagramSocket? = null
    private var receiveJob: Job? = null
    private var sendJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val sendQueue = Channel<Pair<String, ByteArray>>(Channel.UNLIMITED)

    fun start() {
        if (socket != null && !socket!!.isClosed) return

        try {
            socket = DatagramSocket(localPort).apply {
                reuseAddress = true
                broadcast = true
                sendBufferSize = 64 * 1024
                receiveBufferSize = 64 * 1024
            }
            Log.d(tag, "UDP Audio Streamer bound to port $localPort")

            receiveJob = scope.launch {
                val buffer = ByteArray(4096)
                while (isActive && socket != null && !socket!!.isClosed) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket?.receive(packet)
                        if (packet.length > 0) {
                            val data = ByteArray(packet.length)
                            System.arraycopy(packet.data, packet.offset, data, 0, packet.length)
                            onAudioChunkReceived(data)
                        }
                    } catch (e: Exception) {
                        if (socket != null && !socket!!.isClosed) {
                            Log.w(tag, "UDP receive error: ${e.message}")
                        }
                    }
                }
            }

            sendJob = scope.launch {
                val addressCache = mutableMapOf<String, InetAddress>()
                while (isActive) {
                    val (remoteIp, data) = sendQueue.receive()
                    val s = socket
                    if (s != null && !s.isClosed) {
                        try {
                            val address = addressCache.getOrPut(remoteIp) { InetAddress.getByName(remoteIp) }
                            val packet = DatagramPacket(data, data.size, address, localPort)
                            s.send(packet)
                        } catch (e: Exception) {
                            Log.w(tag, "UDP send failed to $remoteIp: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to bind UDP streamer: ${e.message}")
        }
    }

    fun sendAudio(remoteIp: String, remotePort: Int = 8990, data: ByteArray) {
        sendQueue.trySend(Pair(remoteIp, data))
    }

    fun stop() {
        receiveJob?.cancel()
        sendJob?.cancel()
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
    }
}
