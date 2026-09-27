package com.example.transport.udp

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.transport.model.PeerDevice
import com.example.transport.model.PeerStatus
import com.example.transport.model.TransportType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.ConcurrentHashMap

/**
 * Fast, zero-configuration UDP subnet broadcast peer discovery.
 * Runs in parallel with NSD and provides instant discovery across local Wi-Fi and Hotspots.
 */
class UdpBeaconDiscoveryManager(
    private val context: Context,
    private val onDeviceFound: (PeerDevice) -> Unit,
    private val onDeviceLost: (String) -> Unit
) {
    private val tag = "UdpBeaconDiscovery"
    private val beaconPort = 8992
    private val scope = CoroutineScope(Dispatchers.IO)

    private var broadcastJob: Job? = null
    private var listenerJob: Job? = null
    private var cleanupJob: Job? = null

    private var serverSocket: DatagramSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    var ownDeviceId: String = ""
    var ownDeviceName: String = ""
    var ownFingerprint: String = ""
    var ownTcpPort: Int = 8988

    private val lastSeenPeers = ConcurrentHashMap<String, Long>()

    fun setOwnIdentity(deviceId: String, deviceName: String, fingerprint: String = "", tcpPort: Int = 8988) {
        this.ownDeviceId = deviceId
        this.ownDeviceName = deviceName
        this.ownFingerprint = fingerprint
        this.ownTcpPort = tcpPort
    }

    fun start() {
        acquireMulticastLock()
        startListener()
        startBroadcaster()
        startPruningJob()
    }

    private fun acquireMulticastLock() {
        try {
            if (multicastLock == null) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                multicastLock = wifiManager?.createMulticastLock("peerlink_multicast_lock")?.apply {
                    setReferenceCounted(true)
                    acquire()
                }
                Log.d(tag, "Wi-Fi MulticastLock acquired")
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to acquire MulticastLock: ${e.message}")
        }
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
            multicastLock = null
        } catch (e: Exception) {
            Log.w(tag, "Error releasing MulticastLock: ${e.message}")
        }
    }

    private fun startListener() {
        listenerJob?.cancel()
        listenerJob = scope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(beaconPort).apply {
                    broadcast = true
                    reuseAddress = true
                }
                serverSocket = socket
                Log.d(tag, "UDP Beacon listener active on port $beaconPort")

                val buffer = ByteArray(2048)
                while (isActive && !socket.isClosed) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)

                    val rawSenderIp = packet.address.hostAddress ?: ""
                    val cleanSenderIp = cleanIp(rawSenderIp)
                    val message = String(packet.data, packet.offset, packet.length, Charsets.UTF_8).trim()

                    if (message.startsWith("PL_BEACON:")) {
                        handleBeacon(message.removePrefix("PL_BEACON:"), cleanSenderIp)
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.w(tag, "UDP Beacon listener error: ${e.message}")
                }
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) {}
            }
        }
    }

    private fun startBroadcaster() {
        broadcastJob?.cancel()
        broadcastJob = scope.launch {
            while (isActive) {
                if (ownDeviceId.isNotBlank()) {
                    sendBeaconPacket()
                }
                delay(2500)
            }
        }
    }

    private fun sendBeaconPacket() {
        try {
            val json = JSONObject().apply {
                put("id", ownDeviceId)
                put("name", ownDeviceName)
                put("fp", ownFingerprint)
                put("port", ownTcpPort)
            }
            val payload = "PL_BEACON:${json}".toByteArray(Charsets.UTF_8)

            DatagramSocket().use { sendSocket ->
                sendSocket.broadcast = true

                // Broadcast to standard global broadcast
                try {
                    val globalBroadcast = InetAddress.getByName("255.255.255.255")
                    sendSocket.send(DatagramPacket(payload, payload.size, globalBroadcast, beaconPort))
                } catch (_: Exception) {}

                // Broadcast to all active network interface broadcast addresses (Hotspot / Wi-Fi subnets)
                try {
                    val interfaces = NetworkInterface.getNetworkInterfaces()
                    while (interfaces.hasMoreElements()) {
                        val iface = interfaces.nextElement()
                        if (iface.isLoopback || !iface.isUp) continue

                        for (interfaceAddress in iface.interfaceAddresses) {
                            val broadcast = interfaceAddress.broadcast
                            if (broadcast != null) {
                                try {
                                    sendSocket.send(DatagramPacket(payload, payload.size, broadcast, beaconPort))
                                } catch (_: Exception) {}
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(tag, "Error sending UDP beacon: ${e.message}")
        }
    }

    private fun handleBeacon(jsonStr: String, senderIp: String) {
        try {
            val json = JSONObject(jsonStr)
            val peerId = json.optString("id", "")
            val peerName = json.optString("name", "Peer")
            val peerFp = json.optString("fp", "")
            val tcpPort = json.optInt("port", 8988)

            if (peerId.isBlank()) return
            // Ignore own beacon
            if (peerId == ownDeviceId || peerId.contains(ownDeviceId)) return
            if (ownFingerprint.isNotBlank() && peerFp == ownFingerprint) return
            if (ownDeviceName.isNotBlank() && peerName.equals(ownDeviceName, ignoreCase = true)) return
            if (isLocalHostAddress(senderIp)) return

            lastSeenPeers[peerId] = System.currentTimeMillis()

            val peer = PeerDevice(
                id = peerId,
                name = peerName,
                address = senderIp,
                port = tcpPort,
                transportType = TransportType.WIFI_NSD,
                fingerprint = peerFp,
                status = PeerStatus.DISCOVERED
            )
            onDeviceFound(peer)
        } catch (e: Exception) {
            Log.w(tag, "Failed to parse beacon: ${e.message}")
        }
    }

    private fun startPruningJob() {
        cleanupJob?.cancel()
        cleanupJob = scope.launch {
            while (isActive) {
                delay(6000)
                val now = System.currentTimeMillis()
                val iterator = lastSeenPeers.entries.iterator()
                while (iterator.hasNext()) {
                    val (peerId, lastSeen) = iterator.next()
                    if (now - lastSeen > 20000) { // 20s timeout without beacon
                        iterator.remove()
                        onDeviceLost(peerId)
                    }
                }
            }
        }
    }

    private fun cleanIp(raw: String): String {
        return raw.trim().removePrefix("/").removePrefix("::ffff:").substringBefore('%')
    }

    private fun isLocalHostAddress(address: String): Boolean {
        val clean = cleanIp(address)
        if (clean.isBlank()) return true
        if (clean == "127.0.0.1" || clean == "localhost" || clean == "0.0.0.0" || clean == "::1") return true

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    val host = cleanIp(addr.hostAddress ?: "")
                    if (host == clean) return true
                }
            }
        } catch (_: Exception) {}
        return false
    }

    fun stop() {
        broadcastJob?.cancel()
        listenerJob?.cancel()
        cleanupJob?.cancel()
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        releaseMulticastLock()
        lastSeenPeers.clear()
    }
}
