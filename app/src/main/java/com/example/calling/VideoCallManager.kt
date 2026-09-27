package com.example.calling

import android.content.Context
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

class VideoCallManager(
    private val context: Context,
    private val onVideoFrameReady: (jpegBytes: ByteArray) -> Unit
) {
    private val tag = "VideoCallManager"
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(Dispatchers.Default)

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
        Log.d(tag, "Target video frame rate set to ${_targetFps.value} FPS")
    }

    fun bindCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider? = null) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                rebindCamera(lifecycleOwner, surfaceProvider)
            } catch (e: Exception) {
                Log.e(tag, "Failed to initialize CameraX: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun rebindCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider? = null) {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        if (!_isCameraEnabled.value) return

        val cameraSelector = if (_isFrontCamera.value) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
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
        } catch (e: Exception) {
            Log.e(tag, "Failed to bind camera use cases: ${e.message}")
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

            // Use CameraX built-in safe toBitmap() which handles all YUV420 strides, formats, and device quirks without native crashes
            val bitmap = try {
                imageProxy.toBitmap()
            } catch (e: Exception) {
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

                // Scale down slightly (e.g. max width 360) for silky smooth network streaming
                val maxWidth = 360
                val scaled = if (rotated.width > maxWidth) {
                    val scale = maxWidth.toFloat() / rotated.width
                    val targetHeight = (rotated.height * scale).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(rotated, maxWidth, targetHeight, true)
                } else {
                    rotated
                }

                val out = ByteArrayOutputStream()
                val quality = when {
                    targetFpsVal <= 10 -> 60
                    targetFpsVal <= 20 -> 45
                    targetFpsVal <= 30 -> 38
                    else -> 32
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
                }
            } catch (e: Exception) {
                Log.w(tag, "Error decoding remote frame: ${e.message}")
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
        } catch (_: Exception) {}
        _remoteVideoBitmap.value = null
        _fps.value = 0
    }
}
