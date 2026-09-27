true
        if (javaClass != other?.javaClass) return false
        other as P2PPacket
        return packetId == other.packetId
    }