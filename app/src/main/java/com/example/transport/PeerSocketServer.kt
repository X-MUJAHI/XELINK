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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * High-Throughput Multiplexed P2P Socket Server with Dynamic 8 MB BDP Buffer Tuning.
 * Supports multiple concurrent parallel socket connections per remote peer IP
 * for high-speed multi-socket striping (bandwidth aggregation).
 */
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

    // Internal representation of a connected socket lane
    private class ChannelConnection(
        val socket: Socket,
        val output: DataOutputStream,
        val lock: Any
    )

    // Remote IP -> List of active parallel socket channels
    private val clientChannels = ConcurrentHashMap<String, CopyOnWriteArrayList<ChannelConnection>>()
    private val stripeCounters = ConcurrentHashMap<String, AtomicInteger>()

    val isRunning: Boolean
        get() = serverSocket != null && !serverSocket!!.isClosed

    fun start() {
        if (isRunning) return

        serverJob = serverScope.launch {
            try {
                val server = ServerSocket()
                server.reuseAddress = true
                // Auto-tune server socket backlog and receive buffer for Gigabit BDP
                try {
                    server.receiveBufferSize = 8 * 1024 * 1024
                } catch (_: Exception) {}
                server.bind(InetSocketAddress(port), 128)
                serverSocket = server
                Log.d(tag, "High-Throughput P2P Server started on port $port with 8MB BDP buffers")

                while (isActive && !server.isClosed) {
                    try {
                        val client = server.accept()
                        configureSocket(client)
                        val rawAddr = client.inetAddress.hostAddress ?: "unknown"
                        val remoteAddr = cleanIp(rawAddr)
                        Log.d(tag, "Client channel connected: $remoteAddr (Port: ${client.port})")

                        val output = DataOutputStream(
                            BufferedOutputStream(client.getOutputStream(), 1024 * 1024)
                        )
                        val lock = Any()
                        val channelConn = ChannelConnection(client, output, lock)

                        val list = clientChannels.computeIfAbsent(remoteAddr) {
                            CopyOnWriteArrayList()
                        }
                        val isFirstChannel = list.isEmpty()
                        list.add(channelConn)
                        stripeCounters.putIfAbsent(remoteAddr, AtomicInteger(0))

                        if (isFirstChannel) {
                            onClientConnected(remoteAddr)
                        }

                        // Launch dedicated high-throughput listener for this socket lane
                        launch {
                            handleClientChannel(channelConn, remoteAddr)
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

    private fun handleClientChannel(conn: ChannelConnection, remoteAddr: String) {
        val socket = conn.socket
        try {
            val inputStream = DataInputStream(
                BufferedInputStream(socket.getInputStream(), 1024 * 1024)
            )

            while (!socket.isClosed) {
                val length = inputStream.readInt()
                if (length <= 0 || length > 32 * 1024 * 1024) {
                    Log.w(tag, "Invalid packet length on channel: $length")
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
            Log.d(tag, "Client channel $remoteAddr (port ${socket.port}) closed: ${e.message}")
        } finally {
            synchronized(conn.lock) {
                try { conn.output.close() } catch (_: Exception) {}
            }
            try { socket.close() } catch (_: Exception) {}

            val list = clientChannels[remoteAddr]
            if (list != null) {
                list.remove(conn)
                if (list.isEmpty()) {
                    clientChannels.remove(remoteAddr)
                    stripeCounters.remove(remoteAddr)
                    onClientDisconnected(remoteAddr)
                }
            }
        }
    }

    fun sendToClient(remoteAddress: String, packet: P2PPacket): Boolean {
        val clean = cleanIp(remoteAddress)
        val channels = clientChannels[clean] ?: return false
        if (channels.isEmpty()) return false

        // Select channel via round-robin striping across all active socket lanes for this IP
        val counter = stripeCounters.computeIfAbsent(clean) { AtomicInteger(0) }
        val startIndex = Math.floorMod(counter.getAndIncrement(), channels.size)

        val frameBytes = packet.toTransportBytes()

        for (i in 0 until channels.size) {
            val idx = (startIndex + i) % channels.size
            val conn = channels.getOrNull(idx) ?: continue
            val socket = conn.socket
            if (socket.isClosed || !socket.isConnected) {
                channels.remove(conn)
                continue
            }

            try {
                synchronized(conn.lock) {
                    conn.output.writeInt(frameBytes.size)
                    conn.output.write(frameBytes)
                    conn.output.flush()
                }
                return true
            } catch (e: Exception) {
                Log.w(tag, "Channel write error to $clean: ${e.message}")
                try { socket.close() } catch (_: Exception) {}
                channels.remove(conn)
            }
        }

        if (channels.isEmpty()) {
            clientChannels.remove(clean)
            stripeCounters.remove(clean)
            onClientDisconnected(clean)
        }
        return false
    }

    fun broadcast(packet: P2PPacket) {
        clientChannels.keys.forEach { addr ->
            sendToClient(addr, packet)
        }
    }

    private fun configureSocket(socket: Socket) {
        socket.tcpNoDelay = true
        socket.keepAlive = true
        // 8 MB send & receive buffers to match Bandwidth-Delay Product (BDP) of Gigabit Wi-Fi
        try {
            socket.sendBufferSize = 8 * 1024 * 1024
            socket.receiveBufferSize = 8 * 1024 * 1024
        } catch (_: Exception) {}
        socket.setPerformancePreferences(0, 1, 2)
    }

    fun cleanIp(raw: String): String {
        return raw.trim().removePrefix("/").removePrefix("::ffff:").substringBefore('%')
    }

    fun stop() {
        try {
            serverSocket?.close()
            serverSocket = null
            clientChannels.values.forEach { list ->
                list.forEach { conn ->
                    try { conn.output.close() } catch (_: Exception) {}
                    try { conn.socket.close() } catch (_: Exception) {}
                }
            }
            clientChannels.clear()
            stripeCounters.clear()
            serverJob?.cancel()
            Log.d(tag, "P2P Server stopped")
        } catch (e: Exception) {
            Log.e(tag, "Error stopping server: ${e.message}")
        }
    }
}
