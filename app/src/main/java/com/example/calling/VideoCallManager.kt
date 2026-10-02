// PeerLink Production Sync - Active
package com.example.calling

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.diagnostic.AppDiagnostics
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

class VideoCallManager(
    private val context: Context,
    private val onVideoFrameReady: (jpegBytes: ByteArray) -> Unit,
    var onRemoteFrameDecoded: ((Bitmap) -> Unit)? = null
) {
    private val tag = "VideoCallManager"
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        AppDiagnostics.log("VideoCallManager", "Uncaught coroutine exception: ${throwable.message}", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + coroutineExceptionHandler)

    private val _isCameraEnabled = MutableStateFlow(true)
    val isCameraEnabled: StateFlow<Boolean> = _isCameraEnabled.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val _remoteVideoBitmap = MutableStateFlow<Bitmap?>(null)
    val remoteVideoBitmap: StateFlow<Bitmap?> = _remoteVideoBitmap.asStateFlow()

    private val _fps = MutableStateFlow(0)
    val fps: StateFlow<Int> = _fps.asStateFlow()

    private val _targetFps = MutableStateFlow(15)
    val targetFps: StateFlow<Int> = _targetFps.asStateFlow()

    private var cameraProvider: ProcessCameraProvider? = null
    private var lastFrameTime = 0L
    private var frameCount = 0
    private var lastFpsUpdateTime = 0L

    fun setTargetFps(fps: Int) {
        _targetFps.value = fps.coerceIn(5, 60)
        AppDiagnostics.log("VideoCallManager", "Target video frame rate set to ${_targetFps.value} FPS")
    }

    fun bindCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider? = null) {
        val hasPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            AppDiagnostics.log("VideoCallManager", "bindCamera skipped: CAMERA permission not yet granted")
            return
        }

        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    cameraProvider = cameraProviderFuture.get()
                    rebindCamera(lifecycleOwner, surfaceProvider)
                } catch (e: Throwable) {
                    AppDiagnostics.log("VideoCallManager", "Failed to initialize CameraX: ${e.message}", e)
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (t: Throwable) {
            AppDiagnostics.log("VideoCallManager", "ProcessCameraProvider.getInstance failed: ${t.message}", t)
        }
    }

    fun rebindCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider? = null) {
        val provider = cameraProvider ?: return
        try {
            provider.unbindAll()
        } catch (_: Throwable) {}

        if (!_isCameraEnabled.value) return

        val preferredSelector = if (_isFrontCamera.value) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        // Safely determine which camera to use so it never throws IllegalArgumentException
        val cameraSelector = try {
            if (provider.hasCamera(preferredSelector)) {
                preferredSelector
            } else {
                val alternate = if (_isFrontCamera.value) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                if (provider.hasCamera(alternate)) {
                    _isFrontCamera.value = !_isFrontCamera.value
                    alternate
                } else {
                    provider.availableCameraInfos.firstOrNull()?.cameraSelector ?: preferredSelector
                }
            }
        } catch (e: Throwable) {
            AppDiagnostics.log("VideoCallManager", "Error querying camera existence: ${e.message}")
            preferredSelector
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            processCameraImage(imageProxy)
        }

        val preview = Preview.Builder().build()
        if (surfaceProvider != null) {
            preview.setSurfaceProvider(surfaceProvider)
        }

        try {
            provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
            AppDiagnostics.log("VideoCallManager", "Camera bound to lifecycle successfully (front=${_isFrontCamera.value})")
        } catch (e: Throwable) {
            AppDiagnostics.log("VideoCallManager", "Failed to bind camera use cases: ${e.message}", e)
        }
    }

    private fun processCameraImage(imageProxy: ImageProxy) {
        try {
            val now = System.currentTimeMillis()
            val targetFpsVal = _targetFps.value.coerceIn(5, 60)
            val minInterval = 1000L / targetFpsVal

            if (now - lastFrameTime < minInterval) {
                imageProxy.close()
                return
            }
            lastFrameTime = now

            val bitmap = try {
                imageProxy.toBitmap()
            } catch (e: Throwable) {
                null
            }

            if (bitmap != null) {
                val rotation = imageProxy.imageInfo.rotationDegrees
                val rotated = if (rotation != 0) {
                    val matrix = android.graphics.Matrix()
                    matrix.postRotate(rotation.toFloat())
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                } else {
                    bitmap
                }

                // Downsample slightly for silky smooth performance and minimal memory
                val maxWidth = 320
                val scaled = if (rotated.width > maxWidth) {
                    val scale = maxWidth.toFloat() / rotated.width
                    val targetHeight = (rotated.height * scale).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(rotated, maxWidth, targetHeight, true)
                } else {
                    rotated
                }

                val out = ByteArrayOutputStream()
                val quality = when {
                    targetFpsVal <= 10 -> 55
                    targetFpsVal <= 20 -> 40
                    targetFpsVal <= 30 -> 32
                    else -> 28
                }
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                val jpegBytes = out.toByteArray()

                onVideoFrameReady(jpegBytes)

                frameCount++
                if (now - lastFpsUpdateTime > 1000) {
                    _fps.value = frameCount
                    frameCount = 0
                    lastFpsUpdateTime = now
                }
            }
        } catch (oom: OutOfMemoryError) {
            AppDiagnostics.log("VideoCallManager", "OOM in camera frame capture, running GC", oom)
            System.gc()
        } catch (e: Throwable) {
            Log.w(tag, "Frame processing error: ${e.message}")
        } finally {
            try {
                imageProxy.close()
            } catch (_: Throwable) {}
        }
    }

    fun onRemoteFrameReceived(jpegBytes: ByteArray) {
        scope.launch {
            try {
                val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
                if (bitmap != null) {
                    _remoteVideoBitmap.value = bitmap
                    try {
                        onRemoteFrameDecoded?.invoke(bitmap)
                    } catch (_: Throwable) {}
                }
            } catch (oom: OutOfMemoryError) {
                AppDiagnostics.log("VideoCallManager", "OOM decoding remote video frame, dropped frame", oom)
                System.gc()
            } catch (e: Throwable) {
                AppDiagnostics.log("VideoCallManager", "Error decoding remote frame: ${e.message}", e)
            }
        }
    }

    fun toggleCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider? = null) {
        _isCameraEnabled.value = !_isCameraEnabled.value
        rebindCamera(lifecycleOwner, surfaceProvider)
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider? = null) {
        _isFrontCamera.value = !_isFrontCamera.value
        rebindCamera(lifecycleOwner, surfaceProvider)
    }

    fun stop() {
        try {
            cameraProvider?.unbindAll()
        } catch (_: Throwable) {}
        val old = _remoteVideoBitmap.value
        _remoteVideoBitmap.value = null
        if (old != null && !old.isRecycled) {
            try { old.recycle() } catch (_: Throwable) {}
        }
        _fps.value = 0
    }
}
