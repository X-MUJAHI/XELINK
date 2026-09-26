package com.example.security

import android.content.Context
import android.os.Build
import java.security.KeyPair
import java.security.MessageDigest
import java.util.UUID

/**
 * Local device identity for offline peer-to-peer identification and cryptographic authentication.
 */
data class DeviceIdentity(
    val deviceId: String,
    val deviceName: String,
    val publicKeyBytes: ByteArray,
    val keyFingerprint: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DeviceIdentity
        return deviceId == other.deviceId
    }

    override fun hashCode(): Int {
        return deviceId.hashCode()
    }

    companion object {
        private const val PREFS_NAME = "peerlink_identity"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_NAME = "device_name"

        fun getOrCreate(context: Context, keyPair: KeyPair): DeviceIdentity {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            var id = prefs.getString(KEY_DEVICE_ID, null)
            if (id == null) {
                id = UUID.randomUUID().toString()
                prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            }

            var name = prefs.getString(KEY_DEVICE_NAME, null)
            if (name == null) {
                val model = Build.MODEL.replace(" ", "-")
                val shortId = id.take(4).uppercase()
                name = "$model-$shortId"
                prefs.edit().putString(KEY_DEVICE_NAME, name).apply()
            }

            val pubBytes = keyPair.public.encoded
            val fingerprint = computeFingerprint(pubBytes)

            return DeviceIdentity(
                deviceId = id,
                deviceName = name,
                publicKeyBytes = pubBytes,
                keyFingerprint = fingerprint
            )
        }

        fun updateDeviceName(context: Context, newName: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_DEVICE_NAME, newName.trim()).apply()
        }

        fun computeFingerprint(publicKeyBytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(publicKeyBytes)
            return digest.take(8).joinToString(":") { "%02X".format(it) }
        }
    }
}
