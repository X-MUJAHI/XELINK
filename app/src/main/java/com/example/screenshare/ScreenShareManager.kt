package com.example.screenshare

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.example.service.ScreenCaptureService
import com.example.transport.TransportManager
import com.example.transport.model.P2PPacket
import com.example.transport.model.PacketType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class ScreenShareManager(
    private val context: Context,
    private val transportManager: TransportManager
) {
    private val tag = "ScreenShareManager"
    private val scope = CoroutineScope(Dispatchers.Default)
    private val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val backgroundHandler = Handler(Looper.getMainLooper())

    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

    private val _sharingTargetPeer = MutableStateFlow<String?>(null)
    val sharingTargetPeer: StateFlow<String?> = _sharingTargetPeer.asStateFlow()

    private val _remoteScreenBitmap = MutableStateFlow<Bitmap?>(null)
    val remoteScreenBitmap: StateFlow<Bitmap?> = _remoteScreenBitmap.asStateFlow()

    private val _remotePeerName = MutableStateFlow<String?>(null)
    val remotePeerName: StateFlow<String?> = _remotePeerName.asStateFlow()

    private val _shareDurationSeconds = MutableStateFlow(0L)
    val shareDurationSeconds: StateFlow<Long> = _shareDurationSeconds.asStateFlow()

    private var durationJob: Job? = null
    private var lastFrameTime = 0L

    init {
        // Listen for SCREEN_FRAME packets from peer
        scope.launch {
            transportManager.incomingPackets.collect { (packet, _) ->
                if (packet.type == PacketType.SCREEN_FRAME) {
                    val bytes = packet.binaryPayload
                    if (bytes != null) {
                        try {
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bitmap != null) {
                                _remoteScreenBitmap.value = bitmap
                                _remotePeerName.value = packet.senderName
                            }
                        } catch (e: Exception) {
                            Log.w(tag, "Failed to decode screen frame: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    fun createScreenCaptureIntent(): Intent? {
        return projectionManager?.createScreenCaptureIntent()
    }

    fun startScreenCapture(resultCode: Int, data: Intent, targetPeerIp: String, targetPeerName: String) {
        if (resultCode != Activity.RESULT_OK) {
            Log.e(tag, "User cancelled screen capture permission")
            return
        }

        // Start Foreground Service first (mandatory on Android 10+)
        val serviceIntent = Intent(context, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_START
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        val mp = projectionManager?.getMediaProjection(resultCode, data) ?: return
        mediaProjection = mp
        _sharingTargetPeer.value = targetPeerName
        _isSharing.value = true

        mp.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Log.d(tag, "MediaProjection stopped by system")
                stopScreenCapture()
            }
        }, backgroundHandler)

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        // Downscale slightly for smooth real-time offline streaming (e.g. max width 720)
        val scale = 0.5f
        val width = (metrics.widthPixels * scale).toInt()
        val height = (metrics.heightPixels * scale).toInt()
        val density = (metrics.densityDpi * scale).toInt()

        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader

        reader.setOnImageAvailableListener({ ir ->
            val now = System.currentTimeMillis()
            // Throttle to ~12-15 FPS for optimal socket bandwidth
            if (now - lastFrameTime < 70) {
                ir.acquireLatestImage()?.close()
                return@setOnImageAvailableListener
            }
            lastFrameTime = now

            val image = ir.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width

                val bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)

                // Crop row padding if present
                val croppedBitmap = if (rowPadding != 0) {
                    Bitmap.createBitmap(bitmap, 0, 0, width, height)
                } else {
                    bitmap
                }

                val out = ByteArrayOutputStream()
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 60, out)
                val jpegBytes = out.toByteArray()

                val packet = P2PPacket(
                    type = PacketType.SCREEN_FRAME,
                    senderId = transportManager.deviceIdentity.deviceId,
                    senderName = transportManager.deviceIdentity.deviceName,
                    binaryPayload = jpegBytes
                )

                scope.launch(Dispatchers.IO) {
                    transportManager.sendPacketToIp(targetPeerIp, packet)
                }
            } catch (e: Exception) {
                Log.w(tag, "Screen capture frame error: ${e.message}")
            } finally {
                image.close()
            }
        }, backgroundHandler)

        virtualDisplay = mp.createVirtualDisplay(
            "PeerLinkScreenShare",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            backgroundHandler
        )

        startDurationTimer()
        Log.d(tag, "Screen sharing started towards $targetPeerIp ($width x $height)")
    }

    private fun startDurationTimer() {
        durationJob?.cancel()
        durationJob = scope.launch {
            var s = 0L
            while (isActive && _isSharing.value) {
                delay(1000)
                s++
                _shareDurationSeconds.value = s
            }
        }
    }

    fun stopScreenCapture() {
        durationJob?.cancel()
        _shareDurationSeconds.value = 0L
        _isSharing.value = false
        _sharingTargetPeer.value = null

        try {
            virtualDisplay?.release()
        } catch (_: Exception) {}
        virtualDisplay = null

        try {
            imageReader?.close()
        } catch (_: Exception) {}
        imageReader = null

        try {
            mediaProjection?.stop()
        } catch (_: Exception) {}
        mediaProjection = null

        // Stop foreground service
        val serviceIntent = Intent(context, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_STOP
        }
        context.startService(serviceIntent)
        Log.d(tag, "Screen sharing stopped")
    }

    fun clearRemoteScreen() {
        _remoteScreenBitmap.value = null
        _remotePeerName.value = null
    }
}
