package com.example.transport.model

import android.util.Base64
import org.json.JSONObject
import java.util.UUID

enum class PacketType {
    HANDSHAKE,
    HANDSHAKE_ACK,
    MESSAGE,
    MESSAGE_ACK,
    CALL_OFFER,
    CALL_ANSWER,
    CALL_HANGUP,
    CALL_REJECT,
    AUDIO_CHUNK,
    VIDEO_FRAME,
    SCREEN_FRAME,
    FILE_START,
    FILE_CHUNK,
    FILE_ACK,
    PING,
    PONG
}

data class P2PPacket(
    val packetId: String = UUID.randomUUID().toString(),
    val type: PacketType,
    val senderId: String,
    val senderName: String,
    val targetId: String = "",
    val sequenceNumber: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val payload: String = "",
    val binaryPayload: ByteArray? = null,
    val extraData: Map<String, String> = emptyMap()
) {
    fun toJsonString(): String {
        val json = JSONObject()
        json.put("packetId", packetId)
        json.put("type", type.name)
        json.put("senderId", senderId)
        json.put("senderName", senderName)
        json.put("targetId", targetId)
        json.put("sequenceNumber", sequenceNumber)
        json.put("timestamp", timestamp)
        json.put("payload", payload)
        if (binaryPayload != null) {
            json.put("binaryPayload", Base64.encodeToString(binaryPayload, Base64.NO_WRAP))
        }
        if (extraData.isNotEmpty()) {
            val extraObj = JSONObject()
            extraData.forEach { (k, v) -> extraObj.put(k, v) }
            json.put("extraData", extraObj)
        }
        return json.toString()
    }

    companion object {
        fun fromJsonString(jsonStr: String): P2PPacket? {
            return try {
                val json = JSONObject(jsonStr)
                val binStr = if (json.has("binaryPayload")) json.getString("binaryPayload") else null
                val binBytes = if (!binStr.isNullOrEmpty()) Base64.decode(binStr, Base64.NO_WRAP) else null

                val extraMap = mutableMapOf<String, String>()
                if (json.has("extraData")) {
                    val extraObj = json.getJSONObject("extraData")
                    val keys = extraObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        extraMap[key] = extraObj.getString(key)
                    }
                }

                P2PPacket(
                    packetId = json.optString("packetId", UUID.randomUUID().toString()),
                    type = PacketType.valueOf(json.getString("type")),
                    senderId = json.getString("senderId"),
                    senderName = json.getString("senderName"),
                    targetId = json.optString("targetId", ""),
                    sequenceNumber = json.optLong("sequenceNumber", System.currentTimeMillis()),
                    timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                    payload = json.optString("payload", ""),
                    binaryPayload = binBytes,
                    extraData = extraMap
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as P2PPacket
        return packetId == other.packetId
    }

    override fun hashCode(): Int = packetId.hashCode()
}
