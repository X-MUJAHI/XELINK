package com.example.transport

import android.util.Log
import com.example.transport.model.P2PPacket
import com.example.transport.model.decodeChunkFrame
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
    private val onConnectionChanged: (isConnected: Boolean, error: String?) -> Unit,
    // Optional: fast-path callback for raw binary file-chunk frames (see P2PPacket.encodeChunkFrame).
    // When null, incoming binary frames are simply dropped (no other packet type ever uses them).
    private val onFileChunkFrame: ((com.example.transport.model.FileChunkFrame) -> Unit)? = null
) {
    val peerAddress: String = rawAddress.trim().removePrefix("/").removePrefix("::ffff:").substringBefore('%')
    private val tag = "PeerSocketClient"
    private var socket: Socket? = null
    private var clientJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var autoReconnect = true

    // Persistent output stream for the lifetime of a connection, instead of re-wrapping
    // the socket's OutputStream on every send() call. Recreated only when a new socket
    // connects. Guarded by `writeLock` since sends can come from multiple coroutines.
    private var sharedOut: DataOutputStream? = null
    private val writeLock = Any()

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
                try {
                    Log.d(tag, "Connecting to $peerAddress:$peerPort (Attempt $attempt)...")
                    val s = Socket()
                    s.tcpNoDelay = true
                    s.keepAlive = true
                    s.sendBufferSize = 1024 * 1024
                    s.receiveBufferSize = 1024 * 1024
                    s.setPerformancePreferences(0, 1, 2)
                    s.connect(InetSocketAddress(peerAddress, peerPort), 2500)
                    socket = s
                    synchronized(writeLock) {
                        sharedOut = DataOutputStream(BufferedOutputStream(s.getOutputStream(), 256 * 1024))
                    }
                    Log.d(tag, "Successfully connected to $peerAddress:$peerPort")
                    onConnectionChanged(true, null)

                    val dis = DataInputStream(BufferedInputStream(s.getInputStream(), 256 * 1024))
                    while (isActive && !s.isClosed) {
                        val length = dis.readInt()
                        if (length <= 0 || length > 32 * 1024 * 1024) break
                        val buf = ByteArray(length)
                        dis.readFully(buf)
                        if (length >= 4 && P2PPacket.isBinaryFrame(readIntBE(buf, 0))) {
                            onFileChunkFrame?.invoke(decodeChunkFrame(buf))
                        } else {
                            val json = String(buf, Charsets.UTF_8)
                            val packet = P2PPacket.fromJsonString(json)
                            if (packet != null) {
                                onPacketReceived(packet)
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (autoReconnect && isActive) {
                        Log.w(tag, "Connection to $peerAddress attempt $attempt failed: ${e.message}")
                    }
                    onConnectionChanged(false, e.message)
                } finally {
                    try {
                        socket?.close()
                    } catch (_: Exception) {}
                    socket = null
                    synchronized(writeLock) { sharedOut = null }
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
            val jsonBytes = packet.toJsonString().toByteArray(Charsets.UTF_8)
            synchronized(writeLock) {
                val dos = sharedOut ?: DataOutputStream(BufferedOutputStream(s.getOutputStream(), 256 * 1024)).also { sharedOut = it }
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

    /**
     * Fast path for file-chunk transfer: writes a pre-encoded raw binary frame
     * (see P2PPacket.encodeChunkFrame) directly, skipping JSON/Base64 entirely.
     * Same wire framing as send() (4-byte length prefix) so the receiver's single
     * read loop handles both transparently.
     */
    suspend fun sendRawFrame(frame: ByteArray): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected && (clientJob == null || !clientJob!!.isActive)) {
            connect()
        }

        var s = socket
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
            synchronized(writeLock) {
                val dos = sharedOut ?: DataOutputStream(BufferedOutputStream(s.getOutputStream(), 256 * 1024)).also { sharedOut = it }
                dos.writeInt(frame.size)
                dos.write(frame)
                dos.flush()
            }
            true
        } catch (e: Exception) {
            Log.e(tag, "Error sending frame to $peerAddress: ${e.message}")
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
        synchronized(writeLock) { sharedOut = null }
        onConnectionChanged(false, null)
    }
}

/** Reads a big-endian 4-byte int from `buf` at `offset`, matching the encoding used by encodeChunkFrame. */
private fun readIntBE(buf: ByteArray, offset: Int): Int {
    return ((buf[offset].toInt() and 0xFF) shl 24) or ((buf[offset + 1].toInt() and 0xFF) shl 16) or
            ((buf[offset + 2].toInt() and 0xFF) shl 8) or (buf[offset + 3].toInt() and 0xFF)
}
