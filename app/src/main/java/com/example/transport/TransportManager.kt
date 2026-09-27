package com.example.transport

import android.content.Context
import android.util.Log
import com.example.security.CryptoManager
import com.example.security.DeviceIdentity
import com.example.transport.model.P2PPacket
import com.example.transport.model.PacketType
import com.example.transport.model.PeerDevice
import com.example.transport.model.PeerStatus
import com.example.transport.model.TransportType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.ConcurrentHashMap

class TransportManager(
    val context: Context,
    val deviceIdentity: DeviceIdentity,
    val cryptoManager: CryptoManager
) {
    private val tag = "TransportManager"
    private val scope = CoroutineScope(Dispatchers.IO)

    // Discovered devices map: deviceId -> PeerDevice (Guaranteed to NOT include own device)
    private val _discoveredDevices = MutableStateFlow<Map<String, PeerDevice>>(emptyMap())
    val discoveredDevices: StateFlow<Map<String, PeerDevice>> = _discoveredDevices.asStateFlow()

    // Connected clients pool: peerAddress/deviceId -> PeerSocketClient
    private val clients = ConcurrentHashMap<String, PeerSocketClient>()

    // Incoming packets stream for ViewModels & Feature Managers
    private val _incomingPackets = MutableSharedFlow<Pair<P2PPacket, String>>(extraBufferCapacity = 64)
    val incomingPackets: SharedFlow<Pair<P2PPacket, String>> = _incomingPackets.asSharedFlow()

    // Audio streaming packets
    private val _incomingAudio = MutableSharedFlow<ByteArray>(extraBufferCapacity = 128)
    val incomingAudio: SharedFlow<ByteArray> = _incomingAudio.asSharedFlow()

    // Status state
    private val _isBroadcasting = MutableStateFlow(false)
    val isBroadcasting: StateFlow<Boolean> = _isBroadcasting.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _localIp = MutableStateFlow(getLocalIpAddress())
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    val serverPort = 8988

    // Underlying Managers
    private val socketServer = PeerSocketServer(
        port = serverPort,
        onPacketReceived = { packet, remoteAddr ->
            handleIncomingPacket(packet, remoteAddr)
        },
        onClientConnected = { remoteAddr ->
            Log.d(tag, "Client connected: $remoteAddr")
            refreshLocalIp()
        },
        onClientDisconnected = { remoteAddr ->
            Log.d(tag, "Client disconnected: $remoteAddr")
        }
    )

    private val nsdManager = NsdDiscoveryManager(
        context = context,
        onDeviceFound = { peer ->
            if (!isSelf(peer)) {
                updateDevice(peer)
            }
        },
        onDeviceLost = { deviceId ->
            removeDevice(deviceId)
        }
    )

    private val wifiDirectManager = WifiDirectManager(
        context = context,
        onPeersUpdated = { list ->
            list.forEach { p ->
                if (!isSelf(p)) {
                    updateDevice(p)
                }
            }
        },
        onConnected = { ownerIp, isOwner ->
            Log.d(tag, "Wi-Fi Direct connected! Owner IP: $ownerIp, isOwner: $isOwner")
            refreshLocalIp()
            if (!isOwner && ownerIp.isNotBlank() && !isSelfAddress(ownerIp)) {
                connectToPeer(ownerIp, serverPort, "Wi-Fi Direct Host")
            }
        },
        onDisconnected = {
            Log.d(tag, "Wi-Fi Direct disconnected")
        }
    )

    private val udpStreamer = PeerUdpStreamer(
        localPort = 8990,
        onAudioChunkReceived = { data ->
            _incomingAudio.tryEmit(data)
        }
    )

    fun initialize() {
        nsdManager.setOwnIdentity(
            deviceId = deviceIdentity.deviceId,
            deviceName = deviceIdentity.deviceName,
            fingerprint = deviceIdentity.keyFingerprint
        )
        socketServer.start()
        udpStreamer.start()
        wifiDirectManager.start()
        refreshLocalIp()
        filterOutSelfDevices()
    }

    fun isSelf(device: PeerDevice): Boolean {
        if (device.id == deviceIdentity.deviceId) return true
        if (device.id.startsWith("PL-${deviceIdentity.deviceId}")) return true
        if (device.id.contains(deviceIdentity.deviceId)) return true
        if (device.fingerprint.isNotBlank() && device.fingerprint == deviceIdentity.keyFingerprint) return true
        if (device.name.isNotBlank() && device.name.equals(deviceIdentity.deviceName, ignoreCase = true)) return true
        if (isSelfAddress(device.address)) return true
        return false
    }

    fun isSelfAddress(address: String?): Boolean {
        if (address.isNullOrBlank()) return false
        val cleanAddr = address.substringBefore('%').trim()
        if (cleanAddr == "127.0.0.1" || cleanAddr == "localhost" || cleanAddr == "0.0.0.0" || cleanAddr == "::1") return true
        if (cleanAddr == _localIp.value) return true
        return getAllLocalIpAddresses().contains(cleanAddr)
    }

    fun getAllLocalIpAddresses(): Set<String> {
        val ips = mutableSetOf("127.0.0.1", "localhost", "0.0.0.0", "::1")
        val currentLocal = _localIp.value
        if (currentLocal.isNotBlank()) ips.add(currentLocal)

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    val host = addr.hostAddress?.substringBefore('%')
                    if (!host.isNullOrBlank()) {
                        ips.add(host)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to get local IP interfaces: ${e.message}")
        }
        return ips
    }

    private fun filterOutSelfDevices() {
        val current = _discoveredDevices.value.toMutableMap()
        val filtered = current.filterNot { (_, device) -> isSelf(device) }
        if (filtered.size != current.size) {
            _discoveredDevices.value = filtered
        }
    }

    fun startAdvertisingAndDiscovery() {
        startBroadcast()
        startDiscovery()
    }

    fun startBroadcast() {
        nsdManager.startAdvertising(
            deviceId = deviceIdentity.deviceId,
            deviceName = deviceIdentity.deviceName,
            port = serverPort,
            fingerprint = deviceIdentity.keyFingerprint
        )
        _isBroadcasting.value = true
    }

    fun stopBroadcast() {
        nsdManager.stopAdvertising()
        _isBroadcasting.value = false
    }

    fun startDiscovery() {
        nsdManager.setOwnIdentity(
            deviceId = deviceIdentity.deviceId,
            deviceName = deviceIdentity.deviceName,
            fingerprint = deviceIdentity.keyFingerprint
        )
        nsdManager.startDiscovery()
        wifiDirectManager.discoverPeers()
        _isScanning.value = true
        refreshLocalIp()
        filterOutSelfDevices()
    }

    fun stopDiscovery() {
        nsdManager.stopDiscovery()
        _isScanning.value = false
    }

    fun connectToPeer(ip: String, port: Int = serverPort, nameHint: String = "Peer"): PeerSocketClient? {
        if (isSelfAddress(ip)) {
            Log.w(tag, "Refusing connection to own device address ($ip)")
            return null
        }

        val existing = clients[ip]
        if (existing != null && existing.isConnected) {
            return existing
        }

        val client = PeerSocketClient(
            peerAddress = ip,
            peerPort = port,
            onPacketReceived = { packet ->
                handleIncomingPacket(packet, ip)
            },
            onConnectionChanged = { connected, error ->
                Log.d(tag, "Connection to $ip changed: connected=$connected, error=$error")
                updateDeviceStatusByAddress(ip, if (connected) PeerStatus.CONNECTED else PeerStatus.DISCONNECTED)
                if (connected) {
                    sendHandshake(ip)
                }
            }
        )
        clients[ip] = client
        client.connect()

        val id = "ip-$ip"
        if (!_discoveredDevices.value.containsKey(id)) {
            val candidate = PeerDevice(
                id = id,
                name = nameHint,
                address = ip,
                port = port,
                transportType = TransportType.DIRECT_IP,
                status = PeerStatus.CONNECTING
            )
            if (!isSelf(candidate)) {
                updateDevice(candidate)
            }
        }
        return client
    }

    fun disconnectPeer(ip: String) {
        clients[ip]?.disconnect()
        clients.remove(ip)
        updateDeviceStatusByAddress(ip, PeerStatus.DISCONNECTED)
    }

    private fun sendHandshake(targetIp: String) {
        if (isSelfAddress(targetIp)) return

        val handshakePacket = P2PPacket(
            type = PacketType.HANDSHAKE,
            senderId = deviceIdentity.deviceId,
            senderName = deviceIdentity.deviceName,
            payload = deviceIdentity.keyFingerprint,
            binaryPayload = deviceIdentity.publicKeyBytes,
            extraData = mapOf("ip" to (_localIp.value), "port" to serverPort.toString())
        )
        scope.launch {
            sendPacketToIp(targetIp, handshakePacket)
        }
    }

    suspend fun sendPacketToIp(ip: String, packet: P2PPacket): Boolean {
        if (isSelfAddress(ip)) {
            Log.w(tag, "Aborting packet sending to self IP: $ip")
            return false
        }

        // First try client connection
        var client = clients[ip]
        if (client != null && client.isConnected) {
            return client.send(packet)
        }
        // Try server socket active client connection
        val sentViaServer = socketServer.sendToClient(ip, packet)
        if (sentViaServer) return true

        // Try connecting client
        client = connectToPeer(ip, serverPort) ?: return false
        return client.send(packet)
    }

    fun sendAudio(peerIp: String, data: ByteArray) {
        if (!isSelfAddress(peerIp)) {
            udpStreamer.sendAudio(peerIp, 8990, data)
        }
    }

    private fun handleIncomingPacket(packet: P2PPacket, remoteAddr: String) {
        // Discard any packet from own device
        if (packet.senderId == deviceIdentity.deviceId || isSelfAddress(remoteAddr)) {
            Log.d(tag, "Discarding loopback packet from self (${packet.senderId}, $remoteAddr)")
            return
        }

        Log.d(tag, "Incoming packet ${packet.type} from ${packet.senderName} (${packet.senderId})")

        // Auto-reply to Handshake
        if (packet.type == PacketType.HANDSHAKE) {
            val peerId = packet.senderId
            val peerName = packet.senderName
            val fp = packet.payload
            val pubBytes = packet.binaryPayload

            if (pubBytes != null) {
                cryptoManager.deriveSharedKey(peerId, pubBytes)
            }

            val peer = PeerDevice(
                id = peerId,
                name = peerName,
                address = remoteAddr,
                port = serverPort,
                transportType = TransportType.WIFI_NSD,
                status = PeerStatus.CONNECTED,
                publicKey = pubBytes,
                fingerprint = fp
            )
            if (!isSelf(peer)) {
                updateDevice(peer)
            }

            val ackPacket = P2PPacket(
                type = PacketType.HANDSHAKE_ACK,
                senderId = deviceIdentity.deviceId,
                senderName = deviceIdentity.deviceName,
                targetId = peerId,
                payload = deviceIdentity.keyFingerprint,
                binaryPayload = deviceIdentity.publicKeyBytes
            )
            scope.launch {
                socketServer.sendToClient(remoteAddr, ackPacket)
            }
        } else if (packet.type == PacketType.HANDSHAKE_ACK) {
            val peerId = packet.senderId
            val pubBytes = packet.binaryPayload
            if (pubBytes != null) {
                cryptoManager.deriveSharedKey(peerId, pubBytes)
            }
            updateDeviceStatus(peerId, PeerStatus.CONNECTED)
        }

        // Auto-ACK for normal messages
        if (packet.type == PacketType.MESSAGE) {
            val ack = P2PPacket(
                type = PacketType.MESSAGE_ACK,
                senderId = deviceIdentity.deviceId,
                senderName = deviceIdentity.deviceName,
                targetId = packet.senderId,
                payload = packet.packetId
            )
            scope.launch {
                sendPacketToIp(remoteAddr, ack)
            }
        }

        _incomingPackets.tryEmit(Pair(packet, remoteAddr))
    }

    private fun updateDevice(device: PeerDevice) {
        if (isSelf(device)) {
            Log.d(tag, "Skipping addition of own device to discovered list: ${device.name}")
            return
        }
        val current = _discoveredDevices.value.toMutableMap()
        current[device.id] = device
        _discoveredDevices.value = current
    }

    private fun updateDeviceStatus(deviceId: String, status: PeerStatus) {
        val current = _discoveredDevices.value.toMutableMap()
        current[deviceId]?.let {
            current[deviceId] = it.copy(status = status)
            _discoveredDevices.value = current
        }
    }

    private fun updateDeviceStatusByAddress(address: String, status: PeerStatus) {
        val current = _discoveredDevices.value.toMutableMap()
        for ((k, v) in current) {
            if (v.address == address) {
                current[k] = v.copy(status = status)
            }
        }
        _discoveredDevices.value = current
    }

    private fun removeDevice(deviceId: String) {
        val current = _discoveredDevices.value.toMutableMap()
        current.remove(deviceId)
        _discoveredDevices.value = current
    }

    fun refreshLocalIp() {
        _localIp.value = getLocalIpAddress()
        filterOutSelfDevices()
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: ""
                        if (!host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to get local IP: ${e.message}")
        }
        return "127.0.0.1"
    }

    fun shutdown() {
        stopBroadcast()
        stopDiscovery()
        wifiDirectManager.stop()
        socketServer.stop()
        udpStreamer.stop()
        clients.values.forEach { it.disconnect() }
        clients.clear()
    }
}
