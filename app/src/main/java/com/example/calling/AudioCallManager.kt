package com.example.calling

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private val scope = CoroutineScope(Dispatchers.IO)
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
            val recordBufferSize = maxOf(minRecordBufferSize, 4096)

            audioRecord = try {
                AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    sampleRate,
                    channelConfigIn,
                    audioFormat,
                    recordBufferSize
                )
            } catch (e: Exception) {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfigIn,
                    audioFormat,
                    recordBufferSize
                )
            }

            val minTrackBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioFormat)
            val trackBufferSize = maxOf(minTrackBufferSize, 4096)

            audioTrack = AudioTrack(
                AudioManager.STREAM_VOICE_CALL,
                sampleRate,
                channelConfigOut,
                audioFormat,
                trackBufferSize,
                AudioTrack.MODE_STREAM
            )

            audioTrack?.play()
            isPlaying = true

            audioRecord?.startRecording()

            recordJob = scope.launch {
                val buffer = ByteArray(1024)
                while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        // Calculate amplitude for audio visualizer
                        var maxVal = 0
                        for (i in 0 until read step 2) {
                            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
                            val shortSample = sample.toShort()
                            if (abs(shortSample.toInt()) > maxVal) {
                                maxVal = abs(shortSample.toInt())
                            }
                        }
                        _audioAmplitude.value = (maxVal / 32768f).coerceIn(0f, 1f)

                        if (!_isMuted.value) {
                            val chunk = ByteArray(read)
                            System.arraycopy(buffer, 0, chunk, 0, read)
                            onAudioChunkReady(chunk)
                        }
                    }
                }
            }
            Log.d(tag, "AudioCallManager started successfully")
        } catch (e: Exception) {
            Log.e(tag, "Failed to start AudioCall: ${e.message}")
        }
    }

    fun playAudioChunk(data: ByteArray) {
        if (!isPlaying || audioTrack == null) return
        try {
            audioTrack?.write(data, 0, data.size)
        } catch (e: Exception) {
            Log.w(tag, "AudioTrack write error: ${e.message}")
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun toggleSpeaker() {
        val newState = !_isSpeakerOn.value
        _isSpeakerOn.value = newState
        audioManager?.isSpeakerphoneOn = newState
    }

    fun stopCall() {
        recordJob?.cancel()
        isPlaying = false

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null

        audioManager?.mode = AudioManager.MODE_NORMAL
        _audioAmplitude.value = 0f
        Log.d(tag, "AudioCallManager stopped")
    }
}
