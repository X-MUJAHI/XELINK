// PeerLink Production Sync - Active
package com.example.calling

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import com.example.diagnostic.AppDiagnostics
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED,
    STOPPED
}

data class CallRecordingInfo(
    val state: RecordingState = RecordingState.IDLE,
    val durationSeconds: Long = 0L,
    val filePath: String? = null,
    val peerName: String = "",
    val errorMessage: String? = null
)

class CallRecordingManager(private val context: Context) {
    private val tag = "CallRecordingManager"
    private val prefs = context.getSharedPreferences("peerlink_settings", Context.MODE_PRIVATE)

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        AppDiagnostics.log("CallRecordingManager", "Uncaught exception: ${throwable.message}", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineExceptionHandler)

    private val _isAutoRecordEnabled = MutableStateFlow(
        prefs.getBoolean("pref_auto_record_calls", false)
    )
    val isAutoRecordEnabled: StateFlow<Boolean> = _isAutoRecordEnabled.asStateFlow()

    private val _recordingInfo = MutableStateFlow(CallRecordingInfo())
    val recordingInfo: StateFlow<CallRecordingInfo> = _recordingInfo.asStateFlow()

    // Video Encoder Configuration
    private val videoWidth = 320
    private val videoHeight = 240
    private val frameRate = 15
    private val bitRate = 800_000

    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var videoTrackIndex = -1
    private var isMuxerStarted = false
    private var outputFile: File? = null
    private var timerJob: Job? = null

    private var startTimeUs = 0L
    private var totalPausedDurationUs = 0L
    private var pauseStartUs = 0L
    private var frameIndex = 0L

    private val bufferInfo = MediaCodec.BufferInfo()
    private val yuvBuffer = ByteArray(videoWidth * videoHeight * 3 / 2)
    private val argbPixels = IntArray(videoWidth * videoHeight)

    fun setAutoRecordEnabled(enabled: Boolean) {
        _isAutoRecordEnabled.value = enabled
        prefs.edit().putBoolean("pref_auto_record_calls", enabled).apply()
        AppDiagnostics.log(tag, "Auto-record video calls setting changed to: $enabled")
    }

    @Synchronized
    fun startRecording(peerName: String): Boolean {
        if (_recordingInfo.value.state == RecordingState.RECORDING || _recordingInfo.value.state == RecordingState.PAUSED) {
            AppDiagnostics.log(tag, "Cannot start recording: already recording or paused")
            return false
        }

        try {
            val safePeerName = peerName.ifBlank { "Peer" }.replace("[^a-zA-Z0-9_]".toRegex(), "_")
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "PeerLink_VideoCall_${safePeerName}_$timestamp.mp4"

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val peerlinkFolder = File(downloadsDir, "PeerLink")
            if (!peerlinkFolder.exists()) {
                peerlinkFolder.mkdirs()
            }

            outputFile = if (peerlinkFolder.exists() && peerlinkFolder.canWrite()) {
                File(peerlinkFolder, fileName)
            } else {
                val fallbackFolder = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "PeerLink")
                fallbackFolder.mkdirs()
                File(fallbackFolder, fileName)
            }

            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, videoWidth, videoHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            mediaCodec = codec

            val muxer = MediaMuxer(outputFile!!.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            mediaMuxer = muxer
            isMuxerStarted = false
            videoTrackIndex = -1

            startTimeUs = System.nanoTime() / 1000
            totalPausedDurationUs = 0L
            pauseStartUs = 0L
            frameIndex = 0L

            _recordingInfo.value = CallRecordingInfo(
                state = RecordingState.RECORDING,
                durationSeconds = 0L,
                filePath = outputFile!!.absolutePath,
                peerName = peerName
            )
            startDurationTimer()
            AppDiagnostics.log(tag, "Video recording started. Saving to ${outputFile!!.absolutePath}")
            return true
        } catch (e: Throwable) {
            AppDiagnostics.log(tag, "Failed to start video recording: ${e.message}", e)
            cleanupEncoder()
            _recordingInfo.value = CallRecordingInfo(
                state = RecordingState.IDLE,
                errorMessage = "Failed to start recording: ${e.message}"
            )
            return false
        }
    }

    @Synchronized
    fun pauseRecording() {
        if (_recordingInfo.value.state != RecordingState.RECORDING) return
        pauseStartUs = System.nanoTime() / 1000
        _recordingInfo.value = _recordingInfo.value.copy(state = RecordingState.PAUSED)
        AppDiagnostics.log(tag, "Video recording paused")
    }

    @Synchronized
    fun resumeRecording() {
        if (_recordingInfo.value.state != RecordingState.PAUSED) return
        if (pauseStartUs > 0) {
            val pausedDuration = (System.nanoTime() / 1000) - pauseStartUs
            totalPausedDurationUs += maxOf(0L, pausedDuration)
            pauseStartUs = 0L
        }
        _recordingInfo.value = _recordingInfo.value.copy(state = RecordingState.RECORDING)
        AppDiagnostics.log(tag, "Video recording resumed")
    }

    @Synchronized
    fun stopRecording(): String? {
        if (_recordingInfo.value.state == RecordingState.IDLE || _recordingInfo.value.state == RecordingState.STOPPED) {
            return null
        }
        val path = outputFile?.absolutePath
        AppDiagnostics.log(tag, "Stopping video recording...")
        try {
            timerJob?.cancel()
            drainEncoder(endOfStream = true)
        } catch (e: Throwable) {
            AppDiagnostics.log(tag, "Error during encoder drain on stop: ${e.message}")
        } finally {
            cleanupEncoder()
        }

        if (path != null) {
            try {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(path),
                    arrayOf("video/mp4"),
                    null
                )
            } catch (_: Throwable) {}
        }

        _recordingInfo.value = _recordingInfo.value.copy(
            state = RecordingState.STOPPED,
            filePath = path
        )
        AppDiagnostics.log(tag, "Video recording successfully saved: $path")
        return path
    }

    @Synchronized
    fun feedVideoFrame(bitmap: Bitmap) {
        if (_recordingInfo.value.state != RecordingState.RECORDING) return
        if (bitmap.isRecycled) return
        val codec = mediaCodec ?: return

        try {
            val nowUs = (System.nanoTime() / 1000) - startTimeUs - totalPausedDurationUs
            if (nowUs < 0) return

            val scaled = if (bitmap.width == videoWidth && bitmap.height == videoHeight) {
                bitmap
            } else {
                Bitmap.createScaledBitmap(bitmap, videoWidth, videoHeight, true)
            }

            convertBitmapToNv12(scaled, videoWidth, videoHeight, yuvBuffer, argbPixels)
            if (scaled !== bitmap && !scaled.isRecycled) {
                try { scaled.recycle() } catch (_: Throwable) {}
            }

            val inputBufferIndex = codec.dequeueInputBuffer(10_000)
            if (inputBufferIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                inputBuffer?.clear()
                inputBuffer?.put(yuvBuffer)
                codec.queueInputBuffer(inputBufferIndex, 0, yuvBuffer.size, nowUs, 0)
                frameIndex++
            }
            drainEncoder(endOfStream = false)
        } catch (oom: OutOfMemoryError) {
            AppDiagnostics.log(tag, "OOM in recording frame feeder, skipped frame")
            System.gc()
        } catch (e: Throwable) {
            Log.w(tag, "Error feeding frame to recorder: ${e.message}")
        }
    }

    private fun drainEncoder(endOfStream: Boolean) {
        val codec = mediaCodec ?: return
        val muxer = mediaMuxer ?: return

        if (endOfStream) {
            try {
                val inputIndex = codec.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    codec.queueInputBuffer(
                        inputIndex, 0, 0,
                        (System.nanoTime() / 1000) - startTimeUs - totalPausedDurationUs,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                }
            } catch (_: Throwable) {}
        }

        while (true) {
            val outputBufferIndex = try {
                codec.dequeueOutputBuffer(bufferInfo, 10_000)
            } catch (e: Throwable) {
                break
            }

            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (isMuxerStarted) {
                    AppDiagnostics.log(tag, "Warning: MediaFormat changed twice")
                } else {
                    val newFormat = codec.outputFormat
                    videoTrackIndex = muxer.addTrack(newFormat)
                    muxer.start()
                    isMuxerStarted = true
                    AppDiagnostics.log(tag, "MediaMuxer started with track index $videoTrackIndex")
                }
            } else if (outputBufferIndex >= 0) {
                val encodedData = codec.getOutputBuffer(outputBufferIndex)
                if (encodedData != null) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size > 0 && isMuxerStarted && videoTrackIndex >= 0) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                }
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
    }

    private fun convertBitmapToNv12(
        src: Bitmap,
        width: Int,
        height: Int,
        outNv12: ByteArray,
        argbTemp: IntArray
    ) {
        src.getPixels(argbTemp, 0, width, 0, 0, width, height)
        var yIndex = 0
        var uvIndex = width * height
        for (j in 0 until height) {
            for (i in 0 until width) {
                val c = argbTemp[j * width + i]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                outNv12[yIndex++] = y.coerceIn(0, 255).toByte()
                if (j % 2 == 0 && i % 2 == 0) {
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    outNv12[uvIndex++] = u.coerceIn(0, 255).toByte()
                    outNv12[uvIndex++] = v.coerceIn(0, 255).toByte()
                }
            }
        }
    }

    private fun startDurationTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            var seconds = 0L
            while (isActive && (_recordingInfo.value.state == RecordingState.RECORDING || _recordingInfo.value.state == RecordingState.PAUSED)) {
                delay(1000)
                if (_recordingInfo.value.state == RecordingState.RECORDING) {
                    seconds++
                    _recordingInfo.value = _recordingInfo.value.copy(durationSeconds = seconds)
                }
            }
        }
    }

    private fun cleanupEncoder() {
        try {
            mediaCodec?.stop()
            mediaCodec?.release()
        } catch (_: Throwable) {}
        mediaCodec = null
        try {
            if (isMuxerStarted) {
                mediaMuxer?.stop()
            }
            mediaMuxer?.release()
        } catch (_: Throwable) {}
        mediaMuxer = null
        isMuxerStarted = false
        videoTrackIndex = -1
    }
}
