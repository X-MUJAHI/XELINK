package com.example.transport

import android.util.Log
import com.example.transport.model.P2PPacket
import com.example.transport.model.PacketType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * High-Throughput Multiplexed P2P Socket Client with Bandwidth Aggregation.
 * Establishes up to 4 parallel TCP socket connections with dynamic 8 MB kernel BDP buffer tuning.
 * Automatically stripes heavy data packets (e.g., FILE_CHUNK) across all parallel channels
 * to saturate modern Wi-Fi 5/6/7 PHY link rates.
 */
class PeerSocketClient(
    rawAddress: String,
    val peerPort: Int = 8988,
    private val multiplexChannels: Int = 4,
    private val onPacketReceived: (packet: P2PPacket) -> Unit,
    private val onConnectionChanged: (isConnected: Boolean, error: String?) -> Unit
) {
    val peerAddress: String = rawAddress.trim().removePrefix("/").removePrefix("::ffff:").substringBefore('%')
    private val tag = "PeerSocketClient"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var clientJobs = mutableListOf<Job>()
    private var autoReconnect = true

    private class ClientChannel(
        val channelIndex: Int,
        var socket: Socket? = null,
        var outputStream: DataOutputStream? = null,
        val outputLock: Any = Any()
    ) {
        val isConnected: Boolean
            get() = socket != null && socket!!.isConnected && !socket!!.isClosed
    }

    private val channels = List(multiplexChannels) { ClientChannel(it) }
    private val connectedChannels = CopyOnWriteArrayList<ClientChannel>()
    private val stripeCounter = AtomicInteger(0)

    val isConnected: Boolean
        get() = connectedChannels.isNotEmpty()

    val isConnecting: Boolean
        get() = autoReconnect && !isConnected

    fun connect() {
        if (isConnected) return
        autoReconnect = true

        clientJobs.forEach { it.cancel() }
        clientJobs.clear()

        // Launch connection workers for each channel in the multiplex pool
        for (channel in channels) {
            val job = scope.launch {
                runChannelLoop(channel)
            }
            clientJobs.add(job)
        }
    }

    private suspend fun CoroutineScope.runChannelLoop(channel: ClientChannel) {
        var attempt = 0
        while (isActive && autoReconnect) {
            attempt++
            var connectedSocket: Socket? = null
            try {
                if (channel.channelIndex == 0) {
                    Log.d(tag, "Connecting primary channel to $peerAddress:$peerPort (Attempt $attempt)...")
                }
                val s = Socket()
                connectedSocket = s
                s.tcpNoDelay = true
                s.keepAlive = true
                // Dynamic 8 MB kernel buffer tuning
                try {
                    s.sendBufferSize = 8 * 1024 * 1024
                    s.receiveBufferSize = 8 * 1024 * 1024
                } catch (_: Exception) {}
                s.setPerformancePreferences(0, 1, 2)

                // Stagger secondary channels slightly to avoid SYN collisions on local Wi-Fi router
                if (channel.channelIndex > 0) {
                    delay((channel.channelIndex * 50).toLong())
                }

                s.connect(InetSocketAddress(peerAddress, peerPort), 3000)
                channel.socket = s

                synchronized(channel.outputLock) {
                    channel.outputStream = DataOutputStream(
                        BufferedOutputStream(s.getOutputStream(), 1024 * 1024)
                    )
                }

                if (!connectedChannels.contains(channel)) {
                    val wasEmpty = connectedChannels.isEmpty()
                    connectedChannels.add(channel)
                    Log.d(tag, "Channel ${channel.channelIndex} connected to $peerAddress:$peerPort (Active: ${connectedChannels.size}/$multiplexChannels)")
                    if (wasEmpty) {
                        onConnectionChanged(true, null)
                    }
                }

                val dis = DataInputStream(BufferedInputStream(s.getInputStream(), 1024 * 1024))
                while (isActive && !s.isClosed) {
                    val length = dis.readInt()
                    if (length <= 0 || length > 32 * 1024 * 1024) break
                    val buf = ByteArray(length)
                    dis.readFully(buf)
                    val packet = P2PPacket.fromTransportBytes(buf)
                    if (packet != null) {
                        onPacketReceived(packet)
                    }
                }
            } catch (e: Exception) {
                if (channel.channelIndex == 0 && autoReconnect && isActive) {
                    Log.w(tag, "Connection to $peerAddress attempt $attempt failed: ${e.message}")
                }
            } finally {
                connectedChannels.remove(channel)
                synchronized(channel.outputLock) {
                    try {
                        channel.outputStream?.close()
                    } catch (_: Exception) {}
                    channel.outputStream = null
                }
                try {
                    connectedSocket?.close()
                } catch (_: Exception) {}
                if (channel.socket === connectedSocket) {
                    channel.socket = null
                }
                if (connectedChannels.isEmpty() && autoReconnect && isActive) {
                    onConnectionChanged(false, "Disconnected")
                }
            }

            if (autoReconnect && isActive) {
                val waitTime = minOf(1500L * attempt, 3000L)
                delay(waitTime)
            }
        }
    }

    suspend fun send(packet: P2PPacket): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected) {
            connect()
        }

        // Wait up to 2500ms for at least one channel to connect
        if (connectedChannels.isEmpty()) {
            val start = System.currentTimeMillis()
            while (connectedChannels.isEmpty() && (System.currentTimeMillis() - start < 2500) && isActive) {
                delay(50)
            }
        }

        val activeList = connectedChannels
        if (activeList.isEmpty()) {
            Log.w(tag, "Send failed: No connected channels to $peerAddress")
            return@withContext false
        }

        // Striping strategy:
        // FILE_CHUNK packets are striped round-robin across all available parallel channels.
        // Control packets (HANDSHAKE, FILE_START, FILE_ACK) prefer Channel 0 for strict sequencing.
        val targetChannel = if (packet.type == PacketType.FILE_CHUNK) {
            val idx = Math.floorMod(stripeCounter.getAndIncrement(), activeList.size)
            activeList.getOrNull(idx) ?: activeList[0]
        } else {
            activeList.firstOrNull { it.channelIndex == 0 } ?: activeList[0]
        }

        val s = targetChannel.socket
        if (s == null || !s.isConnected || s.isClosed) {
            return@withContext false
        }

        try {
            val frameBytes = packet.toTransportBytes()
            synchronized(targetChannel.outputLock) {
                val dos = targetChannel.outputStream
                    ?: throw IllegalStateException("Socket output stream is not ready")
                dos.writeInt(frameBytes.size)
                dos.write(frameBytes)
                dos.flush()
            }
            true
        } catch (e: Exception) {
            Log.e(tag, "Error sending packet on channel ${targetChannel.channelIndex} to $peerAddress: ${e.message}")
            // Channel failure; remove and let recovery handle it
            connectedChannels.remove(targetChannel)
            false
        }
    }

    fun disconnect() {
        autoReconnect = false
        clientJobs.forEach { it.cancel() }
        clientJobs.clear()

        channels.forEach { ch ->
            synchronized(ch.outputLock) {
                try {
                    ch.outputStream?.close()
                } catch (_: Exception) {}
                ch.outputStream = null
            }
            try {
                ch.socket?.close()
            } catch (_: Exception) {}
            ch.socket = null
        }
        connectedChannels.clear()
        onConnectionChanged(false, null)
    }
}
