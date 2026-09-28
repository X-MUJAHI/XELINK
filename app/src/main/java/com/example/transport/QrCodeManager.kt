package com.example.transport

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import java.util.EnumMap

data class QrConnectInfo(
    val peerId: String,
    val peerName: String,
    val ip: String,
    val port: Int = 8988
)

object QrCodeManager {

    private val qrReader = MultiFormatReader().apply {
        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
        hints[DecodeHintType.POSSIBLE_FORMATS] = listOf(BarcodeFormat.QR_CODE)
        hints[DecodeHintType.TRY_HARDER] = java.lang.Boolean.TRUE
        setHints(hints)
    }

    /**
     * Generates a high-contrast QR code bitmap for off-grid pairing.
     */
    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Creates connection payload URI: xelink://connect?ip=...&port=...&id=...&name=...
     */
    fun createConnectionPayload(ip: String, port: Int, deviceId: String, deviceName: String): String {
        return Uri.Builder()
            .scheme("xelink")
            .authority("connect")
            .appendQueryParameter("ip", ip)
            .appendQueryParameter("port", port.toString())
            .appendQueryParameter("id", deviceId)
            .appendQueryParameter("name", deviceName)
            .build()
            .toString()
    }

    /**
     * Parses scanned text payload into QrConnectInfo.
     */
    fun parseConnectionPayload(rawText: String): QrConnectInfo? {
        return try {
            val trimmed = rawText.trim()
            if (trimmed.startsWith("xelink://connect")) {
                val uri = Uri.parse(trimmed)
                val ip = uri.getQueryParameter("ip") ?: return null
                val port = uri.getQueryParameter("port")?.toIntOrNull() ?: 8988
                val id = uri.getQueryParameter("id") ?: "peer_${System.currentTimeMillis()}"
                val name = uri.getQueryParameter("name") ?: "Peer-$ip"
                return QrConnectInfo(peerId = id, peerName = name, ip = ip, port = port)
            }
            // Support raw IP:Port format as fallback
            if (trimmed.contains(":") && !trimmed.contains("http")) {
                val parts = trimmed.split(":")
                val ip = parts[0].trim()
                val port = parts.getOrNull(1)?.toIntOrNull() ?: 8988
                return QrConnectInfo(peerId = "peer_$ip", peerName = "Peer-$ip", ip = ip, port = port)
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Decodes QR code directly from CameraX ImageProxy in YUV format.
     */
    fun decodeQrFromImageProxy(imageProxy: ImageProxy): String? {
        return try {
            val planes = imageProxy.planes
            val yBuffer = planes[0].buffer
            val ySize = yBuffer.remaining()
            val yBytes = ByteArray(ySize)
            yBuffer.get(yBytes)
            val width = imageProxy.width
            val height = imageProxy.height
            val source = PlanarYUVLuminanceSource(
                yBytes,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = qrReader.decodeWithState(binaryBitmap)
            result.text
        } catch (_: Exception) {
            null
        } finally {
            qrReader.reset()
        }
    }
}
