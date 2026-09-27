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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as P2PPacket
        return packetId == other.packetId
    }

    override fun hashCode(): Int = packetId.hashCode()

    // Single companion object holds both the existing JSON (de)serialization and the
    // newer binary file-chunk framing — Kotlin only allows one companion object per class.
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

        // 4-byte marker placed at the start of a frame's payload to identify it as a raw
        // binary file-chunk frame instead of a JSON packet. Chosen so it can never collide
        // with the first byte of a JSON string (always '{' = 0x7B). Only used by the
        // file-transfer fast path; every other packet type is unaffected.
        const val FRAME_MAGIC: Int = -0x46494C45 // "FILE" marker, negative so it can't be a valid JSON-string length coincidence

        /**
         * True if the 4 bytes at the start of a raw frame identify it as a binary file-chunk
         * frame (written by [encodeChunkFrame]) rather than a JSON-encoded P2PPacket.
         */
        fun isBinaryFrame(firstFourBytes: Int): Boolean = firstFourBytes == FRAME_MAGIC

        /**
         * Encodes a file chunk as a compact binary frame instead of a JSON+Base64 packet:
         *
         *   [MAGIC:4][transferIdLen:2][transferId bytes][chunkIndex:4][isLast:1][data...]
         *
         * This avoids Base64 (33% size overhead) and JSON string building/parsing per chunk,
         * which is the dominant cost of transferring many small chunks.
         */
        fun encodeChunkFrame(transferId: String, chunkIndex: Int, isLast: Boolean, data: ByteArray, offset: Int, length: Int): ByteArray {
            val idBytes = transferId.toByteArray(Charsets.UTF_8)
            val header = 4 + 2 + idBytes.size + 4 + 1
            val out = ByteArray(header + length)
            var pos = 0

            fun putInt(v: Int) {
                out[pos] = (v ushr 24).toByte(); out[pos + 1] = (v ushr 16).toByte()
                out[pos + 2] = (v ushr 8).toByte(); out[pos + 3] = v.toByte()
                pos += 4
            }
            fun putShort(v: Int) {
                out[pos] = (v ushr 8).toByte(); out[pos + 1] = v.toByte()
                pos += 2
            }

            putInt(FRAME_MAGIC)
            putShort(idBytes.size)
            System.arraycopy(idBytes, 0, out, pos, idBytes.size); pos += idBytes.size
            putInt(chunkIndex)
            out[pos] = if (isLast) 1 else 0; pos += 1
            System.arraycopy(data, offset, out, pos, length)
            return out
        }
    }
}

/** Parsed result of a binary file-chunk frame decoded by [decodeChunkFrame]. */
data class FileChunkFrame(
    val transferId: String,
    val chunkIndex: Int,
    val isLast: Boolean,
    val data: ByteArray
)

/**
 * Decodes a frame body previously produced by [P2PPacket.encodeChunkFrame]. `body` must be
 * everything after the outer 4-byte length prefix (i.e. it starts with the 4-byte magic).
 */
fun decodeChunkFrame(body: ByteArray): FileChunkFrame {
    var pos = 4 // skip magic, already checked by caller
    fun getInt(): Int {
        val v = ((body[pos].toInt() and 0xFF) shl 24) or ((body[pos + 1].toInt() and 0xFF) shl 16) or
                ((body[pos + 2].toInt() and 0xFF) shl 8) or (body[pos + 3].toInt() and 0xFF)
        pos += 4
        return v
    }
    fun getShort(): Int {
        val v = ((body[pos].toInt() and 0xFF) shl 8) or (body[pos + 1].toInt() and 0xFF)
        pos += 2
        return v
    }

    val idLen = getShort()
    val transferId = String(body, pos, idLen, Charsets.UTF_8); pos += idLen
    val chunkIndex = getInt()
    val isLast = body[pos] == 1.toByte(); pos += 1
    val data = body.copyOfRange(pos, body.size)
    return FileChunkFrame(transferId, chunkIndex, isLast, data)
}
