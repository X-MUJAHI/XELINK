package com.example.transport

import android.util.Log
import com.example.transport.model.P2PPacket
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

class PeerSocketClient(
    rawAddress: String,
    val peerPort: Int = 8988,
    private val onPacketReceived: (packet: P2PPacket) -> Unit,
    private val onConnectionChanged: (isConnected: Boolean, error: String?) -> Unit
) {
    val peerAddress: String = rawAddress.trim().removePrefix("/").removePrefix("::ffff:").substringBefore('%')
    private val tag = "PeerSocketClient"
    private var socket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private val outputLock = Any()
    private var clientJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var autoReconnect = true

    val isConnected: Boolean
        get() = socket != null && socket!!.isConnected && !socket!!.isClosed

    val isConnecting: Boolean
        get() = clientJob != null && clientJob!!.isActive && !isConnected

    fun connect() {
        if (isConnected) return
        autoReconnect = true

        clientJob?.cancel()
        clientJob = scope.launch {
            var attempt = 0
            while (isActive && autoReconnect) {
                attempt++
                var connectedSocket: Socket? = null
                try {
                    Log.d(tag, "Connecting to $peerAddress:$peerPort (Attempt $attempt)...")
                    val s = Socket()
                    connectedSocket = s
                    s.tcpNoDelay = true
                    s.keepAlive = true
                    s.sendBufferSize = 1024 * 1024
                    s.receiveBufferSize = 1024 * 1024
                    s.setPerformancePreferences(0, 1, 2)
                    s.connect(InetSocketAddress(peerAddress, peerPort), 2500)
                    socket = s
                    synchronized(outputLock) {
                        outputStream = DataOutputStream(
                            BufferedOutputStream(s.getOutputStream(), 256 * 1024)
                        )
                    }
                    Log.d(tag, "Successfully connected to $peerAddress:$peerPort")
                    onConnectionChanged(true, null)

                    val dis = DataInputStream(BufferedInputStream(s.getInputStream(), 256 * 1024))
                    while (isActive && !s.isClosed) {
                        val length = dis.readInt()
                        if (length <= 0 || length > 15 * 1024 * 1024) break
                        val buf = ByteArray(length)
                        dis.readFully(buf)
                        val packet = P2PPacket.fromTransportBytes(buf)
                        if (packet != null) {
                            onPacketReceived(packet)
                        }
                    }
                } catch (e: Exception) {
                    if (autoReconnect && isActive) {
                        Log.w(tag, "Connection to $peerAddress attempt $attempt failed: ${e.message}")
                    }
                    onConnectionChanged(false, e.message)
                } finally {
                    synchronized(outputLock) {
                        try {
                            outputStream?.close()
                        } catch (_: Exception) {}
                        outputStream = null
                    }
                    try {
                        connectedSocket?.close()
                    } catch (_: Exception) {}
                    if (socket === connectedSocket) {
                        socket = null
                    }
                }

                if (autoReconnect && isActive) {
                    val waitTime = minOf(1500L * attempt, 3000L)
                    delay(waitTime)
                }
            }
        }
    }

    suspend fun send(packet: P2PPacket): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected && (clientJob == null || !clientJob!!.isActive)) {
            connect()
        }

        var s = socket
        // If socket is still connecting, allow up to 2500ms grace period
        if (s == null || !s.isConnected || s.isClosed) {
            val start = System.currentTimeMillis()
            while ((s == null || !s.isConnected || s.isClosed) && (System.currentTimeMillis() - start < 2500) && isActive) {
                delay(60)
                s = socket
            }
        }

        if (s == null || !s.isConnected || s.isClosed) {
            Log.w(tag, "Send failed: socket to $peerAddress not connected")
            return@withContext false
        }

        try {
            val frameBytes = packet.toTransportBytes()
            synchronized(outputLock) {
                val dos = outputStream
                    ?: throw IllegalStateException("Socket output stream is not ready")
                dos.writeInt(frameBytes.size)
                dos.write(frameBytes)
                dos.flush()
            }
            true
        } catch (e: Exception) {
            Log.e(tag, "Error sending packet to $peerAddress: ${e.message}")
            false
        }
    }

    fun disconnect() {
        autoReconnect = false
        clientJob?.cancel()
        synchronized(outputLock) {
            try {
                outputStream?.close()
            } catch (_: Exception) {}
            outputStream = null
        }
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        onConnectionChanged(false, null)
    }
}
