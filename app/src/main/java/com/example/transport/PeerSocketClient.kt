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
    private var clientJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var autoReconnect = true

    val isConnected: Boolean
        get() = socket != null && socket!!.isConnected && !socket!!.isClosed

    fun connect() {
        if (isConnected) return
        autoReconnect = true

        clientJob?.cancel()
        clientJob = scope.launch {
            while (isActive && autoReconnect) {
                try {
                    Log.d(tag, "Connecting to $peerAddress:$peerPort...")
                    val s = Socket()
                    s.tcpNoDelay = true
                    s.keepAlive = true
                    s.connect(InetSocketAddress(peerAddress, peerPort), 4500)
                    socket = s
                    Log.d(tag, "Successfully connected to $peerAddress:$peerPort")
                    onConnectionChanged(true, null)

                    val dis = DataInputStream(s.getInputStream())
                    while (isActive && !s.isClosed) {
                        val length = dis.readInt()
                        if (length <= 0 || length > 15 * 1024 * 1024) break
                        val buf = ByteArray(length)
                        dis.readFully(buf)
                        val json = String(buf, Charsets.UTF_8)
                        val packet = P2PPacket.fromJsonString(json)
                        if (packet != null) {
                            onPacketReceived(packet)
                        }
                    }
                } catch (e: Exception) {
                    if (autoReconnect && isActive) {
                        Log.w(tag, "Connection to $peerAddress interrupted: ${e.message}")
                    }
                    onConnectionChanged(false, e.message)
                } finally {
                    try {
                        socket?.close()
                    } catch (_: Exception) {}
                    socket = null
                }

                if (autoReconnect && isActive) {
                    delay(3000)
                }
            }
        }
    }

    suspend fun send(packet: P2PPacket): Boolean = withContext(Dispatchers.IO) {
        var s = socket
        // If socket is still connecting, allow up to 3000ms grace period
        if (s == null || !s.isConnected || s.isClosed) {
            val start = System.currentTimeMillis()
            while ((s == null || !s.isConnected || s.isClosed) && (System.currentTimeMillis() - start < 3000) && isActive) {
                delay(80)
                s = socket
            }
        }

        if (s == null || !s.isConnected || s.isClosed) {
            Log.w(tag, "Send failed: socket to $peerAddress not connected")
            return@withContext false
        }

        try {
            val jsonBytes = packet.toJsonString().toByteArray(Charsets.UTF_8)
            synchronized(s) {
                val dos = DataOutputStream(s.getOutputStream())
                dos.writeInt(jsonBytes.size)
                dos.write(jsonBytes)
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
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        onConnectionChanged(false, null)
    }
}
