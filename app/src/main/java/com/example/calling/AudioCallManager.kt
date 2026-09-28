package com.example.calling

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import com.example.diagnostic.AppDiagnostics
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class AudioCallManager(
    private val context: Context,
    private val onAudioChunkReady: (ByteArray) -> Unit
) {
    private val tag = "AudioCallManager"
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        AppDiagnostics.log("AudioCallManager", "Uncaught audio coroutine exception: ${throwable.message}", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineExceptionHandler)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val sampleRate = 16000
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    private var recordJob: Job? = null
    private var isPlaying = false

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(true)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startCall() {
        try {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = _isSpeakerOn.value

            val minRecordBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat)
            val recordBufferSize = if (minRecordBufferSize > 0) maxOf(minRecordBufferSize * 2, 4096) else 8192

            // Cascade to find working AudioRecord source
            audioRecord = createSafeAudioRecord(recordBufferSize)

            val minTrackBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioFormat)
            val trackBufferSize = if (minTrackBufferSize > 0) maxOf(minTrackBufferSize * 2, 4096) else 8192

            // Cascade to find working AudioTrack
            audioTrack = createSafeAudioTrack(trackBufferSize)

            // Start AudioTrack playback
            try {
                if (audioTrack != null && audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
                    audioTrack?.play()
                    isPlaying = true
                    AppDiagnostics.log("AudioCallManager", "AudioTrack initialized and playing")
                } else {
                    AppDiagnostics.log("AudioCallManager", "Warning: AudioTrack is not initialized")
                }
            } catch (t: Throwable) {
                AppDiagnostics.log("AudioCallManager", "AudioTrack play error: ${t.message}", t)
            }

            // Start AudioRecord recording
            try {
                if (audioRecord != null && audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord?.startRecording()
                    AppDiagnostics.log("AudioCallManager", "AudioRecord initialized and recording")
                } else {
                    AppDiagnostics.log("AudioCallManager", "Warning: AudioRecord is not initialized")
                }
            } catch (t: Throwable) {
                AppDiagnostics.log("AudioCallManager", "AudioRecord startRecording error: ${t.message}", t)
            }

            // Continuous audio recording loop
            recordJob?.cancel()
            recordJob = scope.launch {
                val buffer = ByteArray(1024)
                while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    try {
                        val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (read > 0) {
                            var maxVal = 0
                            for (i in 0 until read step 2) {
                                if (i + 1 < read) {
                                    val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
                                    val shortSample = sample.toShort()
                                    if (abs(shortSample.toInt()) > maxVal) {
                                        maxVal = abs(shortSample.toInt())
                                    }
                                }
                            }
                            _audioAmplitude.value = (maxVal / 32768f).coerceIn(0f, 1f)

                            if (!_isMuted.value) {
                                val chunk = ByteArray(read)
                                System.arraycopy(buffer, 0, chunk, 0, read)
                                onAudioChunkReady(chunk)
                            }
                        }
                    } catch (e: Throwable) {
                        AppDiagnostics.log("AudioCallManager", "AudioRecord read error: ${e.message}")
                    }
                }
            }
            AppDiagnostics.log("AudioCallManager", "AudioCallManager session successfully initialized")
        } catch (e: Throwable) {
            AppDiagnostics.log("AudioCallManager", "Failed to start AudioCall: ${e.message}", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun createSafeAudioRecord(bufferSize: Int): AudioRecord? {
        val sources = listOf(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT
        )

        for (source in sources) {
            try {
                val record = AudioRecord(source, sampleRate, channelConfigIn, audioFormat, bufferSize)
                if (record.state == AudioRecord.STATE_INITIALIZED) {
                    return record
                }
                record.release()
            } catch (t: Throwable) {
                AppDiagnostics.log("AudioCallManager", "AudioRecord source $source failed: ${t.message}")
            }
        }
        return null
    }

    private fun createSafeAudioTrack(bufferSize: Int): AudioTrack? {
        // 1. Try M3 / modern AudioAttributes with VOICE_COMMUNICATION
        try {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(audioFormat)
                .setChannelMask(channelConfigOut)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            if (track.state == AudioTrack.STATE_INITIALIZED) return track
            track.release()
        } catch (t: Throwable) {
            AppDiagnostics.log("AudioCallManager", "AudioTrack.Builder VOICE failed: ${t.message}")
        }

        // 2. Try modern AudioAttributes with USAGE_MEDIA
        try {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(audioFormat)
                .setChannelMask(channelConfigOut)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            if (track.state == AudioTrack.STATE_INITIALIZED) return track
            track.release()
        } catch (t: Throwable) {
            AppDiagnostics.log("AudioCallManager", "AudioTrack.Builder MEDIA failed: ${t.message}")
        }

        // 3. Fallback to legacy AudioTrack
        return try {
            @Suppress("DEPRECATION")
            AudioTrack(
                AudioManager.STREAM_VOICE_CALL,
                sampleRate,
                channelConfigOut,
                audioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM
            )
        } catch (t: Throwable) {
            AppDiagnostics.log("AudioCallManager", "Legacy AudioTrack failed: ${t.message}")
            null
        }
    }

    fun playAudioChunk(data: ByteArray) {
        val track = audioTrack ?: return
        if (!isPlaying) return
        try {
            if (track.state == AudioTrack.STATE_INITIALIZED) {
                if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    track.play()
                }
                track.write(data, 0, data.size)
            }
        } catch (e: Throwable) {
            Log.w(tag, "AudioTrack write error: ${e.message}")
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        AppDiagnostics.log("AudioCallManager", "Mic mute state: ${_isMuted.value}")
    }

    fun toggleSpeaker() {
        val newState = !_isSpeakerOn.value
        _isSpeakerOn.value = newState
        try {
            audioManager?.isSpeakerphoneOn = newState
        } catch (_: Throwable) {}
        AppDiagnostics.log("AudioCallManager", "Speakerphone state: $newState")
    }

    fun stopCall() {
        recordJob?.cancel()
        recordJob = null
        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
        } catch (_: Throwable) {}
        audioRecord = null

        try {
            if (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack?.stop()
            }
            audioTrack?.release()
        } catch (_: Throwable) {}
        audioTrack = null
        isPlaying = false

        try {
            audioManager?.mode = AudioManager.MODE_NORMAL
        } catch (_: Throwable) {}

        _audioAmplitude.value = 0f
        AppDiagnostics.log("AudioCallManager", "AudioCallManager session stopped")
    }
}
