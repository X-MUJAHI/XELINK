package com.example.transport

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private val scope = CoroutineScope(Dispatchers.IO)

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
        } catch (e: Exception) {
            Log.e(tag, "Failed to bind UDP streamer: ${e.message}")
        }
    }

    fun sendAudio(remoteIp: String, remotePort: Int = 8990, data: ByteArray) {
        val s = socket ?: return
        if (s.isClosed) return

        scope.launch {
            try {
                val address = InetAddress.getByName(remoteIp)
                val packet = DatagramPacket(data, data.size, address, remotePort)
                s.send(packet)
            } catch (e: Exception) {
                Log.w(tag, "UDP send failed: ${e.message}")
            }
        }
    }

    fun stop() {
        receiveJob?.cancel()
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
    }
}
