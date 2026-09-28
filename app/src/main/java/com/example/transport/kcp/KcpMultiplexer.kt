package com.example.transport.kcp

import android.util.Log
import com.example.transport.model.P2PPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * KCP Reliable UDP Multiplexer.
 * Designed for extreme-throughput local P2P wireless links.
 * Bypasses TCP slow-start and congestion avoidance algorithms using:
 * - nodelay = 1 (Immediate transmission without waiting for timer)
 * - interval = 10ms (Ultra-responsive internal clock tick)
 * - resend = 2 (Fast retransmit triggered after 2 skipped ACKs)
 * - nc = 1 (nocwnd: Congestion window collapse disabled for maximum local bandwidth saturation)
 */
class KcpMultiplexer(
    val port: Int = 8994,
    private val onPacketReceived: (packet: P2PPacket, remoteAddress: String) -> Unit
) {
    private val tag = "KcpMultiplexer"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var socket: DatagramSocket? = null
    private var receiveJob: Job? = null
    private var updateJob: Job? = null
    private val isRunning = AtomicBoolean(false)

    // Per-peer ARQ transmission state
    private class KcpSession(
        val remoteAddress: String,
        val conv: Int
    ) {
        val nextSendSn = AtomicLong(0L)
        val nextExpectedSn = AtomicLong(0L)
        val unackedPackets = ConcurrentHashMap<Long, UnackedEntry>()
        val receivedBuffers = ConcurrentHashMap<Long, ByteArray>()
        var lastRtt = 10L
    }

    private data class UnackedEntry(
        val sn: Long,
        val data: ByteArray,
        var sendTime: Long,
        var fastAckCount: Int = 0,
        var transmitCount: Int = 1
    )

    private val sessions = ConcurrentHashMap<String, KcpSession>()

    // Protocol Header Constants (KCP ARQ Frame)
    companion object {
        const val KCP_MAGIC: Int = 0x504B4350 // "PKCP"
        const val CMD_DATA: Byte = 1
        const val CMD_ACK: Byte = 2
        const val CMD_PING: Byte = 3
        const val HEADER_SIZE: Int = 4 + 4 + 1 + 8 + 8 + 4 // magic(4) + conv(4) + cmd(1) + sn(8) + una(8) + len(4) = 29 bytes
        const val MAX_UDP_PAYLOAD: Int = 1400 // Safe MTU size avoiding IP fragmentation
    }

    fun start() {
        if (isRunning.getAndSet(true)) return

        try {
            val s = DatagramSocket(null)
            s.reuseAddress = true
            s.broadcast = true
            // 8 MB UDP socket buffer auto-tuning
            try {
                s.sendBufferSize = 8 * 1024 * 1024
                s.receiveBufferSize = 8 * 1024 * 1024
            } catch (_: Exception) {}
            s.bind(InetSocketAddress(port))
            socket = s
            Log.d(tag, "KCP High-Speed UDP Multiplexer started on port $port (8MB buffers, nodelay=1, nc=1)")

            receiveJob = scope.launch {
                val recvBuf = ByteArray(65535)
                while (isActive && !s.isClosed) {
                    try {
                        val packet = DatagramPacket(recvBuf, recvBuf.size)
                        s.receive(packet)
                        if (packet.length >= HEADER_SIZE) {
                            val remoteIp = packet.address.hostAddress?.trim()
                                ?.removePrefix("/")?.removePrefix("::ffff:")?.substringBefore('%') ?: continue
                            handleIncomingFrame(packet.data, packet.offset, packet.length, remoteIp, packet.port)
                        }
                    } catch (e: Exception) {
                        if (!s.isClosed) {
                            Log.w(tag, "KCP receive error: ${e.message}")
                        }
                    }
                }
            }

            // 10ms aggressive interval clock loop (nodelay=1, interval=10ms)
            updateJob = scope.launch {
                while (isActive && isRunning.get()) {
                    val now = System.currentTimeMillis()
                    updateSessions(now)
                    delay(10)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to start KCP Multiplexer: ${e.message}", e)
            isRunning.set(false)
        }
    }

    private fun handleIncomingFrame(bytes: ByteArray, offset: Int, length: Int, remoteIp: String, remotePort: Int) {
        val buf = ByteBuffer.wrap(bytes, offset, length).order(ByteOrder.BIG_ENDIAN)
        val magic = buf.int
        if (magic != KCP_MAGIC) return

        val conv = buf.int
        val cmd = buf.get()
        val sn = buf.long
        val una = buf.long
        val payloadLen = buf.int

        if (payloadLen < 0 || payloadLen > length - HEADER_SIZE) return

        val session = sessions.computeIfAbsent(remoteIp) {
            KcpSession(remoteIp, conv)
        }

        // Process cumulative ACK (una) and fast-ack
        if (una > 0) {
            session.unackedPackets.keys.removeIf { it < una }
        }

        when (cmd) {
            CMD_ACK -> {
                session.unackedPackets.remove(sn)
                // Fast retransmit check for packets with lower sn
                session.unackedPackets.values.forEach { entry ->
                    if (entry.sn < sn) {
                        entry.fastAckCount++
                        if (entry.fastAckCount >= 2) { // resend = 2
                            resendEntry(session, entry, remotePort)
                        }
                    }
                }
            }

            CMD_DATA -> {
                // Send immediate ACK back (nodelay = 1)
                sendAck(session, sn, remotePort)

                val payload = ByteArray(payloadLen)
                buf.get(payload)

                val expected = session.nextExpectedSn.get()
                if (sn >= expected) {
                    session.receivedBuffers[sn] = payload
                    deliverOrderedPackets(session)
                }
            }
        }
    }

    private fun deliverOrderedPackets(session: KcpSession) {
        while (true) {
            val currentSn = session.nextExpectedSn.get()
            val data = session.receivedBuffers.remove(currentSn) ?: break
            session.nextExpectedSn.incrementAndGet()

            try {
                val packet = P2PPacket.fromTransportBytes(data)
                if (packet != null) {
                    onPacketReceived(packet, session.remoteAddress)
                }
            } catch (e: Exception) {
                Log.w(tag, "Error parsing KCP packet: ${e.message}")
            }
        }
    }

    private fun sendAck(session: KcpSession, sn: Long, targetPort: Int) {
        val s = socket ?: return
        try {
            val frame = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.BIG_ENDIAN)
            frame.putInt(KCP_MAGIC)
            frame.putInt(session.conv)
            frame.put(CMD_ACK)
            frame.putLong(sn)
            frame.putLong(session.nextExpectedSn.get())
            frame.putInt(0)

            val bytes = frame.array()
            val dp = DatagramPacket(bytes, bytes.size, InetSocketAddress(session.remoteAddress, targetPort))
            s.send(dp)
        } catch (_: Exception) {}
    }

    private fun resendEntry(session: KcpSession, entry: UnackedEntry, targetPort: Int) {
        val s = socket ?: return
        try {
            entry.sendTime = System.currentTimeMillis()
            entry.fastAckCount = 0
            entry.transmitCount++
            val dp = DatagramPacket(entry.data, entry.data.size, InetSocketAddress(session.remoteAddress, targetPort))
            s.send(dp)
        } catch (_: Exception) {}
    }

    private fun updateSessions(now: Long) {
        val s = socket ?: return
        sessions.values.forEach { session ->
            // RTO check: aggressive 100ms timeout for wireless P2P links
            val rto = 100L
            session.unackedPackets.values.forEach { entry ->
                if (now - entry.sendTime > rto) {
                    entry.sendTime = now
                    entry.transmitCount++
                    try {
                        val dp = DatagramPacket(entry.data, entry.data.size, InetSocketAddress(session.remoteAddress, port))
                        s.send(dp)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    suspend fun sendPacket(targetIp: String, packet: P2PPacket, targetPort: Int = port): Boolean {
        val s = socket ?: return false
        val session = sessions.computeIfAbsent(targetIp) {
            KcpSession(targetIp, (System.currentTimeMillis() and 0x7FFFFFFF).toInt())
        }

        return try {
            val transportBytes = packet.toTransportBytes()
            val sn = session.nextSendSn.getAndIncrement()

            val frame = ByteBuffer.allocate(HEADER_SIZE + transportBytes.size).order(ByteOrder.BIG_ENDIAN)
            frame.putInt(KCP_MAGIC)
            frame.putInt(session.conv)
            frame.put(CMD_DATA)
            frame.putLong(sn)
            frame.putLong(session.nextExpectedSn.get())
            frame.putInt(transportBytes.size)
            frame.put(transportBytes)

            val frameBytes = frame.array()
            val entry = UnackedEntry(sn, frameBytes, System.currentTimeMillis())
            session.unackedPackets[sn] = entry

            val dp = DatagramPacket(frameBytes, frameBytes.size, InetSocketAddress(targetIp, targetPort))
            s.send(dp)
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to send packet via KCP to $targetIp: ${e.message}")
            false
        }
    }

    fun stop() {
        isRunning.set(false)
        receiveJob?.cancel()
        updateJob?.cancel()
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        sessions.clear()
        Log.d(tag, "KCP Multiplexer stopped")
    }
}
