package com.example.transport

import android.util.Log
import com.example.transport.model.P2PPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class PeerSocketServer(
    private val port: Int = 8988,
    private val onPacketReceived: (packet: P2PPacket, remoteAddress: String) -> Unit,
    private val onClientConnected: (remoteAddress: String) -> Unit,
    private val onClientDisconnected: (remoteAddress: String) -> Unit
) {
    private val tag = "PeerSocketServer"
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val serverScope = CoroutineScope(Dispatchers.IO)
    private val activeClients = ConcurrentHashMap<String, Socket>()

    val isRunning: Boolean
        get() = serverSocket != null && !serverSocket!!.isClosed

    fun start() {
        if (isRunning) return

        serverJob = serverScope.launch {
            try {
                val server = ServerSocket()
                server.reuseAddress = true
                server.bind(InetSocketAddress(port))
                serverSocket = server
                Log.d(tag, "P2P Server started on port $port")

                while (isActive && !server.isClosed) {
                    try {
                        val client = server.accept()
                        val remoteAddr = client.inetAddress.hostAddress ?: "unknown"
                        Log.d(tag, "Client connected: $remoteAddr")
                        activeClients[remoteAddr] = client
                        onClientConnected(remoteAddr)

                        // Launch listener for this client
                        launch {
                            handleClient(client, remoteAddr)
                        }
                    } catch (e: Exception) {
                        if (!server.isClosed) {
                            Log.e(tag, "Error accepting client: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Server startup failed: ${e.message}")
            }
        }
    }

    private fun handleClient(socket: Socket, remoteAddr: String) {
        try {
            socket.tcpNoDelay = true
            socket.keepAlive = true
            val inputStream = DataInputStream(socket.getInputStream())

            while (!socket.isClosed) {
                val length = inputStream.readInt()
                if (length <= 0 || length > 15 * 1024 * 1024) { // 15MB limit for video/screen frames
                    Log.w(tag, "Invalid packet length: $length")
                    break
                }
                val buffer = ByteArray(length)
                inputStream.readFully(buffer)
                val jsonStr = String(buffer, Charsets.UTF_8)
                val packet = P2PPacket.fromJsonString(jsonStr)
                if (packet != null) {
                    onPacketReceived(packet, remoteAddr)
                }
            }
        } catch (e: Exception) {
            Log.d(tag, "Client $remoteAddr disconnected: ${e.message}")
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
            activeClients.remove(remoteAddr)
            onClientDisconnected(remoteAddr)
        }
    }

    fun sendToClient(remoteAddress: String, packet: P2PPacket): Boolean {
        val socket = activeClients[remoteAddress] ?: return false
        return try {
            val json = packet.toJsonString().toByteArray(Charsets.UTF_8)
            synchronized(socket) {
                val dos = DataOutputStream(socket.getOutputStream())
                dos.writeInt(json.size)
                dos.write(json)
                dos.flush()
            }
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to send packet to $remoteAddress: ${e.message}")
            false
        }
    }

    fun broadcast(packet: P2PPacket) {
        activeClients.keys.forEach { addr ->
            sendToClient(addr, packet)
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
            serverSocket = null
            activeClients.values.forEach { it.close() }
            activeClients.clear()
            serverJob?.cancel()
            Log.d(tag, "P2P Server stopped")
        } catch (e: Exception) {
            Log.e(tag, "Error stopping server: ${e.message}")
        }
    }
}
