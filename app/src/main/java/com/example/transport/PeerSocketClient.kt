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
    val peerAddress: String,
    val peerPort: Int = 8988,
    private val onPacketReceived: (packet: P2PPacket) -> Unit,
    private val onConnectionChanged: (isConnected: Boolean, error: String?) -> Unit
) {
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
                    s.connect(InetSocketAddress(peerAddress, peerPort), 5000)
                    socket = s
                    Log.d(tag, "Connected to $peerAddress:$peerPort")
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
                    Log.w(tag, "Connection to $peerAddress error: ${e.message}")
                    onConnectionChanged(false, e.message)
                } finally {
                    try {
                        socket?.close()
                    } catch (_: Exception) {}
                    socket = null
                }

                if (autoReconnect && isActive) {
                    delay(3000) // Wait before auto-reconnect retry
                }
            }
        }
    }

    suspend fun send(packet: P2PPacket): Boolean = withContext(Dispatchers.IO) {
        val s = socket ?: return@withContext false
        if (!s.isConnected || s.isClosed) return@withContext false

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
            Log.e(tag, "Error sending packet: ${e.message}")
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
