package com.example.transport

import android.util.Log
import com.example.transport.model.P2PPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
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
    private val activeOutputs = ConcurrentHashMap<String, DataOutputStream>()
    private val outputLocks = ConcurrentHashMap<String, Any>()

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
                        configureSocket(client)
                        val rawAddr = client.inetAddress.hostAddress ?: "unknown"
                        val remoteAddr = cleanIp(rawAddr)
                        Log.d(tag, "Client connected: $remoteAddr")

                        val output = DataOutputStream(
                            BufferedOutputStream(client.getOutputStream(), 256 * 1024)
                        )
                        val lock = Any()
                        val prev = activeClients.put(remoteAddr, client)
                        activeOutputs[remoteAddr]?.closeQuietly()
                        activeOutputs[remoteAddr] = output
                        outputLocks[remoteAddr] = lock
                        if (prev != null && prev != client) {
                            try { prev.close() } catch (_: Exception) {}
                        }

                        onClientConnected(remoteAddr)

                        // Launch listener for this client
                        launch {
                            handleClient(client, remoteAddr, output, lock)
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

    private fun handleClient(
        socket: Socket,
        remoteAddr: String,
        output: DataOutputStream,
        outputLock: Any
    ) {
        try {
            val inputStream = DataInputStream(
                BufferedInputStream(socket.getInputStream(), 256 * 1024)
            )

            while (!socket.isClosed) {
                val length = inputStream.readInt()
                if (length <= 0 || length > 15 * 1024 * 1024) {
                    Log.w(tag, "Invalid packet length: $length")
                    break
                }
                val buffer = ByteArray(length)
                inputStream.readFully(buffer)
                val packet = P2PPacket.fromTransportBytes(buffer)
                if (packet != null) {
                    onPacketReceived(packet, remoteAddr)
                }
            }
        } catch (e: Exception) {
            Log.d(tag, "Client $remoteAddr disconnected: ${e.message}")
        } finally {
            synchronized(outputLock) {
                try { output.close() } catch (_: Exception) {}
            }
            try {
                socket.close()
            } catch (_: Exception) {}
            activeOutputs.remove(remoteAddr, output)
            outputLocks.remove(remoteAddr, outputLock)
            activeClients.remove(remoteAddr, socket)
            onClientDisconnected(remoteAddr)
        }
    }

    fun sendToClient(remoteAddress: String, packet: P2PPacket): Boolean {
        val clean = cleanIp(remoteAddress)
        val socket = activeClients[clean] ?: return false
        if (socket.isClosed || !socket.isConnected) {
            activeClients.remove(clean, socket)
            return false
        }

        val output = activeOutputs[clean] ?: return false
        val outputLock = outputLocks[clean] ?: return false

        return try {
            val frameBytes = packet.toTransportBytes()
            synchronized(outputLock) {
                output.writeInt(frameBytes.size)
                output.write(frameBytes)
                output.flush()
            }
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to send packet to $clean: ${e.message}")
            try { socket.close() } catch (_: Exception) {}
            activeClients.remove(clean, socket)
            if (activeOutputs.remove(clean, output)) {
                output.closeQuietly()
            }
            outputLocks.remove(clean, outputLock)
            false
        }
    }

    fun broadcast(packet: P2PPacket) {
        activeClients.keys.forEach { addr ->
            sendToClient(addr, packet)
        }
    }

    private fun configureSocket(socket: Socket) {
        socket.tcpNoDelay = true
        socket.keepAlive = true
        socket.sendBufferSize = 1024 * 1024
        socket.receiveBufferSize = 1024 * 1024
        socket.setPerformancePreferences(0, 1, 2)
    }

    private fun DataOutputStream.closeQuietly() {
        try { close() } catch (_: Exception) {}
    }

    fun cleanIp(raw: String): String {
        return raw.trim().removePrefix("/").removePrefix("::ffff:").substringBefore('%')
    }

    fun stop() {
        try {
            serverSocket?.close()
            serverSocket = null
            activeOutputs.values.forEach { it.closeQuietly() }
            activeOutputs.clear()
            activeClients.values.forEach { it.close() }
            activeClients.clear()
            outputLocks.clear()
            serverJob?.cancel()
            Log.d(tag, "P2P Server stopped")
        } catch (e: Exception) {
            Log.e(tag, "Error stopping server: ${e.message}")
        }
    }
}
