package com.example.transport.model

enum class TransportType {
    WIFI_NSD,
    WIFI_DIRECT,
    BLUETOOTH,
    DIRECT_IP
}

enum class PeerStatus {
    DISCOVERED,
    CONNECTING,
    CONNECTED,
    DISCONNECTED
}

data class PeerDevice(
    val id: String,
    val name: String,
    val address: String,
    val port: Int = 8988,
    val transportType: TransportType = TransportType.WIFI_NSD,
    val rssi: Int? = null,
    val status: PeerStatus = PeerStatus.DISCOVERED,
    val publicKey: ByteArray? = null,
    val fingerprint: String = "",
    val lastSeen: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PeerDevice
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
