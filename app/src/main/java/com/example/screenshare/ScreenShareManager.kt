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
import android.os.HandlerThread
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
import java.nio.ByteBuffer

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
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null

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
                        } catch (e: Throwable) {
                            Log.w(tag, "Failed to decode screen frame: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    fun createScreenCaptureIntent(): Intent? {
        return try {
            projectionManager?.createScreenCaptureIntent()
        } catch (t: Throwable) {
            Log.e(tag, "Failed to create screen capture intent: ${t.message}", t)
            null
        }
    }

    fun startScreenCapture(resultCode: Int, data: Intent, targetPeerIp: String, targetPeerName: String) {
        if (resultCode != Activity.RESULT_OK) {
            Log.e(tag, "User cancelled screen capture permission")
            return
        }

        try {
            // Stop any existing capture session first
            stopScreenCaptureInternal(notifyService = false)

            _sharingTargetPeer.value = targetPeerName
            _isSharing.value = true

            // Supply in-memory fallback for immediate zero-IPC availability
            ScreenCaptureService.pendingData = data
            ScreenCaptureService.pendingResultCode = resultCode

            // Set up callback from ScreenCaptureService once startForeground is active
            ScreenCaptureService.onMediaProjectionReadyListener = { mp, ip, name ->
                scope.launch {
                    setupVirtualDisplayAndCapture(mp, ip, name)
                }
            }

            ScreenCaptureService.onServiceStoppedListener = {
                stopScreenCaptureInternal(notifyService = false)
            }

            // Start Foreground Service first (Strictly required on Android 14+ before getMediaProjection)
            val serviceIntent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_START
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
                putExtra(ScreenCaptureService.EXTRA_TARGET_IP, targetPeerIp)
                putExtra(ScreenCaptureService.EXTRA_TARGET_NAME, targetPeerName)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
            Log.d(tag, "Foreground service launch requested with projection data")
        } catch (t: Throwable) {
            Log.e(tag, "Failed to start screen capture: ${t.message}", t)
            stopScreenCapture()
        }
    }

    private fun setupVirtualDisplayAndCapture(mp: MediaProjection, targetPeerIp: String, targetPeerName: String) {
        try {
            mediaProjection = mp

            // Dedicated background handler thread for screen frame acquisition and JPEG encoding
            // Completely isolates heavy graphics processing from the Compose UI thread
            val thread = HandlerThread("ScreenCaptureThread").apply { start() }
            captureThread = thread
            val handler = Handler(thread.looper)
            captureHandler = handler

            // Register callback BEFORE createVirtualDisplay (Mandatory requirement on Android 14+)
            mp.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d(tag, "MediaProjection stopped by system")
                    stopScreenCapture()
                }
            }, handler)

            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val (rawWidth, rawHeight, densityDpi) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = windowManager.currentWindowMetrics.bounds
                val density = context.resources.displayMetrics.densityDpi
                Triple(bounds.width(), bounds.height(), density)
            } else {
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRealMetrics(metrics)
                Triple(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
            }

            // Downscale to 50% for smooth high-framerate real-time streaming
            val scale = 0.5f
            var width = (rawWidth * scale).toInt()
            var height = (rawHeight * scale).toInt()
            if (width <= 0) width = 540
            if (height <= 0) height = 960
            // Ensure even dimensions
            if (width % 2 != 0) width++
            if (height % 2 != 0) height++
            val density = if (densityDpi > 0) (densityDpi * scale).toInt() else DisplayMetrics.DENSITY_DEFAULT

            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            imageReader = reader

            reader.setOnImageAvailableListener({ ir ->
                val now = System.currentTimeMillis()
                // Throttle to ~15 FPS (66ms) for optimal balance of smooth viewing and socket throughput
                if (now - lastFrameTime < 66) {
                    try {
                        ir.acquireLatestImage()?.close()
                    } catch (_: Throwable) {}
                    return@setOnImageAvailableListener
                }
                lastFrameTime = now

                var image: android.media.Image? = null
                try {
                    image = ir.acquireLatestImage() ?: return@setOnImageAvailableListener
                    val planes = image.planes
                    if (planes.isEmpty()) return@setOnImageAvailableListener

                    val plane = planes[0]
                    val buffer = plane.buffer
                    val pixelStride = plane.pixelStride
                    val rowStride = plane.rowStride
                    val rowPadding = rowStride - pixelStride * width

                    val bitmap: Bitmap = if (rowPadding == 0) {
                        val bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bm.copyPixelsFromBuffer(buffer)
                        bm
                    } else {
                        val effectiveWidth = rowStride / pixelStride
                        val fullBm = Bitmap.createBitmap(effectiveWidth, height, Bitmap.Config.ARGB_8888)
                        val requiredBytes = fullBm.byteCount
                        if (buffer.remaining() >= requiredBytes) {
                            fullBm.copyPixelsFromBuffer(buffer)
                        } else {
                            val directBuf = ByteBuffer.allocateDirect(requiredBytes)
                            directBuf.put(buffer)
                            while (directBuf.hasRemaining()) {
                                directBuf.put(0.toByte())
                            }
                            directBuf.rewind()
                            fullBm.copyPixelsFromBuffer(directBuf)
                        }
                        val croppedBm = Bitmap.createBitmap(fullBm, 0, 0, width, height)
                        fullBm.recycle()
                        croppedBm
                    }

                    val out = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 65, out)
                    bitmap.recycle()
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
                } catch (t: Throwable) {
                    Log.w(tag, "Screen capture frame processing error: ${t.message}")
                } finally {
                    try {
                        image?.close()
                    } catch (_: Throwable) {}
                }
            }, handler)

            virtualDisplay = mp.createVirtualDisplay(
                "PeerLinkScreenShare",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                handler
            )

            startDurationTimer()
            Log.d(tag, "Screen sharing successfully started towards $targetPeerIp ($width x $height)")
        } catch (t: Throwable) {
            Log.e(tag, "Failed to setup VirtualDisplay: ${t.message}", t)
            stopScreenCapture()
        }
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
        stopScreenCaptureInternal(notifyService = true)
    }

    private fun stopScreenCaptureInternal(notifyService: Boolean) {
        durationJob?.cancel()
        _shareDurationSeconds.value = 0L
        _isSharing.value = false
        _sharingTargetPeer.value = null

        try {
            virtualDisplay?.release()
        } catch (_: Throwable) {}
        virtualDisplay = null

        try {
            imageReader?.close()
        } catch (_: Throwable) {}
        imageReader = null

        try {
            mediaProjection?.stop()
        } catch (_: Throwable) {}
        mediaProjection = null

        try {
            captureThread?.quitSafely()
        } catch (_: Throwable) {}
        captureThread = null
        captureHandler = null

        if (notifyService) {
            try {
                val serviceIntent = Intent(context, ScreenCaptureService::class.java).apply {
                    action = ScreenCaptureService.ACTION_STOP
                }
                context.startService(serviceIntent)
            } catch (_: Throwable) {}
        }
        Log.d(tag, "Screen sharing stopped")
    }

    fun clearRemoteScreen() {
        _remoteScreenBitmap.value = null
        _remotePeerName.value = null
    }
}
