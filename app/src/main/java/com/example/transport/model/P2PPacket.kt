package com.example.transport.model

import android.util.Base64
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
    /**
     * Legacy JSON representation. This remains the wire representation for
     * every packet type except FILE_CHUNK.
     */
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

    /**
     * Produces the bytes stored inside the existing 4-byte length-prefixed
     * transport frame. Only FILE_CHUNK gets a binary payload format.
     * All other packets are byte-for-byte on the same JSON path as before.
     */
    fun toTransportBytes(): ByteArray {
        return if (type == PacketType.FILE_CHUNK && binaryPayload != null) {
            toBinaryFileChunkFrame()
        } else {
            toJsonString().toByteArray(Charsets.UTF_8)
        }
    }

    private fun toBinaryFileChunkFrame(): ByteArray {
        val transferId = extraData["transferId"] ?: payload
        require(transferId.isNotEmpty()) { "FILE_CHUNK requires a transferId" }

        val transferIdBytes = transferId.toByteArray(Charsets.UTF_8)
        require(transferIdBytes.size <= MAX_TRANSFER_ID_LENGTH) {
            "FILE_CHUNK transferId is too long"
        }

        val chunkIndex = extraData["chunkIndex"]?.toIntOrNull() ?: 0
        require(chunkIndex >= 0) { "FILE_CHUNK chunkIndex must be non-negative" }

        val isLast = extraData["isLast"]?.toBooleanStrictOrNull() ?: false
        val payloadBytes = binaryPayload ?: error("FILE_CHUNK requires binaryPayload")
        val headerSize = BINARY_FIXED_HEADER_SIZE + transferIdBytes.size
        val frame = ByteBuffer.allocate(headerSize + payloadBytes.size)
            .order(ByteOrder.BIG_ENDIAN)

        frame.putInt(FILE_BINARY_MAGIC)
        frame.putShort(transferIdBytes.size.toShort())
        frame.put(transferIdBytes)
        frame.putInt(chunkIndex)
        frame.put(if (isLast) 1.toByte() else 0.toByte())
        frame.put(payloadBytes)
        return frame.array()
    }

    companion object {
        /** Binary marker for FILE_CHUNK bodies. */
        private const val FILE_BINARY_MAGIC: Int = -1179208773 // 0xB9B6B3BB
        private const val MAX_TRANSFER_ID_LENGTH = 0x7FFF
        private const val BINARY_FIXED_HEADER_SIZE = 4 + 2 + 4 + 1

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

        /**
         * Parses the body stored inside the existing 4-byte length-prefixed
         * socket frame. Binary detection is O(1) and falls back to JSON.
         */
        fun fromTransportBytes(bytes: ByteArray): P2PPacket? {
            if (isBinaryFileChunk(bytes)) {
                return fromBinaryFileChunkFrame(bytes)
            }
            return fromJsonString(String(bytes, Charsets.UTF_8))
        }

        fun isBinaryFileChunk(bytes: ByteArray): Boolean {
            if (bytes.size < 4) return false
            val magic = ByteBuffer.wrap(bytes, 0, 4)
                .order(ByteOrder.BIG_ENDIAN)
                .int
            return magic == FILE_BINARY_MAGIC
        }

        private fun fromBinaryFileChunkFrame(bytes: ByteArray): P2PPacket? {
            return try {
                if (bytes.size < BINARY_MIN_FRAME_SIZE) return null

                val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
                val magic = buffer.int
                if (magic != FILE_BINARY_MAGIC) return null

                val transferIdLength = buffer.short.toInt()
                if (transferIdLength <= 0 || transferIdLength > MAX_TRANSFER_ID_LENGTH) return null
                if (buffer.remaining() < transferIdLength + 4 + 1) return null

                val transferIdBytes = ByteArray(transferIdLength)
                buffer.get(transferIdBytes)
                val transferId = String(transferIdBytes, Charsets.UTF_8)

                val chunkIndex = buffer.int
                if (chunkIndex < 0 || buffer.remaining() < 1) return null

                val isLast = buffer.get().toInt() != 0
                val chunkBytes = ByteArray(buffer.remaining())
                buffer.get(chunkBytes)

                P2PPacket(
                    // FILE_CHUNK metadata is carried by FILE_START; the binary
                    // frame intentionally contains only transfer-specific data.
                    packetId = UUID.randomUUID().toString(),
                    type = PacketType.FILE_CHUNK,
                    senderId = "",
                    senderName = "",
                    targetId = "",
                    payload = transferId,
                    binaryPayload = chunkBytes,
                    extraData = mapOf(
                        "transferId" to transferId,
                        "chunkIndex" to chunkIndex.toString(),
                        "isLast" to isLast.toString()
                    )
                )
            } catch (_: Exception) {
                null
            }
        }

        private const val BINARY_MIN_FRAME_SIZE = 4 + 2 + 1 + 4 + 1
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as P2PPacket
        return packetId == other.packetId
    }

    override fun hashCode(): Int = packetId.hashCode()
}
