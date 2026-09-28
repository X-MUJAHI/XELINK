package com.example.ui.screens

import android.Manifest
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.calling.CallState
import com.example.calling.CallType
import com.example.calling.RecordingState
import com.example.diagnostic.AppDiagnostics
import com.example.ui.components.GlassCard
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonEmerald
import com.example.viewmodel.MainViewModel
import java.util.Locale

@Composable
fun CallsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val callInfo by viewModel.callManager.callInfo.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val isMuted by viewModel.callManager.audioCallManager.isMuted.collectAsState()
    val isSpeakerOn by viewModel.callManager.audioCallManager.isSpeakerOn.collectAsState()
    val audioAmp by viewModel.callManager.audioCallManager.audioAmplitude.collectAsState()
    val remoteVideoBitmap by viewModel.callManager.videoCallManager.remoteVideoBitmap.collectAsState()
    val isCameraEnabled by viewModel.callManager.videoCallManager.isCameraEnabled.collectAsState()
    val videoFps by viewModel.callManager.videoCallManager.fps.collectAsState()
    val targetFps by viewModel.callManager.videoCallManager.targetFps.collectAsState()
    val recordingInfo by viewModel.callManager.callRecordingManager.recordingInfo.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            )
        )
    }

    if (callInfo != null && callInfo?.callState != CallState.IDLE) {
        val call = callInfo!!
        BackHandler {
            viewModel.callManager.endCall()
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF070B14))
        ) {
            if (call.callType == CallType.VIDEO) {
                // Video Call Screen
                Box(modifier = Modifier.fillMaxSize()) {
                    val safeRemoteBitmap = remoteVideoBitmap?.takeIf { !it.isRecycled }
                    if (safeRemoteBitmap != null) {
                        val safeImage = remember(safeRemoteBitmap) {
                            try { safeRemoteBitmap.asImageBitmap() } catch (_: Throwable) { null }
                        }
                        if (safeImage != null) {
                            Image(
                                bitmap = safeImage,
                                contentDescription = "Remote Video Stream",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF131B2E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Rendering video...", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF131B2E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (call.callState == CallState.CONNECTED) "Waiting for remote video frames..." else "Calling ${call.peerName}...",
                                    color = Color.LightGray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Local Camera PIP (Picture-In-Picture in top corner)
                    if (isCameraEnabled) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 40.dp, end = 16.dp)
                                .size(width = 110.dp, height = 150.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberCyan)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx)
                                    try {
                                        viewModel.callManager.videoCallManager.bindCamera(
                                            lifecycleOwner = lifecycleOwner,
                                            surfaceProvider = previewView.surfaceProvider
                                        )
                                    } catch (t: Throwable) {
                                        AppDiagnostics.log("CallsScreen", "Failed to bind camera: ${t.message}", t)
                                    }
                                    previewView
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            } else {
                // Voice Call Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = call.peerName.take(2).uppercase(),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = call.peerName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = when (call.callState) {
                            CallState.CONNECTED -> "In Call • ${call.durationSeconds}s"
                            CallState.OUTGOING_RINGING -> "Ringing nearby peer..."
                            CallState.INCOMING_RINGING -> "Incoming peer call..."
                            else -> "Connecting..."
                        },
                        fontSize = 14.sp,
                        color = NeonEmerald
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    if (call.callState == CallState.CONNECTED) {
                        WaveformVisualizer(
                            amplitude = audioAmp,
                            isMuted = isMuted,
                            barCount = 13,
                            maxHeight = 60.dp
                        )
                    }
                }
            }

            // Top Info Bar overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 20.dp, end = 20.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = call.peerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        text = "${call.callType.name} • ${if (call.callState == CallState.CONNECTED) "${call.durationSeconds}s" else "Ringing..."}",
                        fontSize = 12.sp,
                        color = NeonEmerald
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (call.callType == CallType.VIDEO && call.callState == CallState.CONNECTED) {
                        if (recordingInfo.state == RecordingState.RECORDING) {
                            val mins = recordingInfo.durationSeconds / 60
                            val secs = recordingInfo.durationSeconds % 60
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CrimsonError.copy(alpha = 0.25f))
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(CrimsonError)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "REC ${String.format(Locale.US, "%02d:%02d", mins, secs)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CrimsonError
                                )
                            }
                        } else if (recordingInfo.state == RecordingState.PAUSED) {
                            Text(
                                text = "⏸ PAUSED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB300)
                            )
                        }

                        Text(
                            text = "FPS: $videoFps",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyberCyan
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            AppDiagnostics.copyReportToClipboard(
                                context,
                                mapOf(
                                    "In-Call Peer" to call.peerName,
                                    "Peer IP" to call.peerIp,
                                    "Call Type" to call.callType.name,
                                    "Call State" to call.callState.name,
                                    "Duration" to "${call.durationSeconds}s",
                                    "Target FPS" to "$targetFps",
                                    "Actual FPS" to "$videoFps",
                                    "Audio Muted" to "$isMuted",
                                    "Speakerphone" to "$isSpeakerOn",
                                    "Recording State" to recordingInfo.state.name,
                                    "Recording File" to (recordingInfo.filePath ?: "None")
                                )
                            )
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Log",
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Log", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Bottom Call Controls Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // FPS selector and recording bar for video
                if (call.callType == CallType.VIDEO && call.callState == CallState.CONNECTED) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xDD0D1322))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "FPS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            listOf(5, 10, 15, 24, 30, 60).forEach { fpsOption ->
                                val isSelected = targetFps == fpsOption
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) CyberCyan else Color.White.copy(alpha = 0.12f))
                                        .clickable { viewModel.callManager.videoCallManager.setTargetFps(fpsOption) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$fpsOption",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFF00363D) else Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Video Call Recording Control Bar
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xEE0B111F))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (recordingInfo.state) {
                                RecordingState.RECORDING -> {
                                    val mins = recordingInfo.durationSeconds / 60
                                    val secs = recordingInfo.durationSeconds % 60
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(CrimsonError)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "REC ${String.format(Locale.US, "%02d:%02d", mins, secs)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CrimsonError
                                        )
                                    }
                                    Button(
                                        onClick = { viewModel.callManager.callRecordingManager.pauseRecording() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color.Black, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Pause", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { viewModel.callManager.callRecordingManager.stopRecording() },
                                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonError),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Stop", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                RecordingState.PAUSED -> {
                                    Text(
                                        text = "⏸ PAUSED",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFB300)
                                    )
                                    Button(
                                        onClick = { viewModel.callManager.callRecordingManager.resumeRecording() },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = Color(0xFF00363D), modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Resume", color = Color(0xFF00363D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { viewModel.callManager.callRecordingManager.stopRecording() },
                                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonError),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Stop", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                else -> {
                                    Row(
                                        modifier = Modifier
                                            .clickable { viewModel.callManager.callRecordingManager.startRecording(call.peerName) }
                                            .padding(horizontal = 4.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(CrimsonError)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Record Video (/Download/PeerLink)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                if (call.callState == CallState.INCOMING_RINGING) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = { viewModel.callManager.acceptCall() },
                            modifier = Modifier.size(64.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = NeonEmerald, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Accept Call", modifier = Modifier.size(30.dp))
                        }
                        FilledIconButton(
                            onClick = { viewModel.callManager.declineCall() },
                            modifier = Modifier.size(64.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = CrimsonError, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "Decline Call", modifier = Modifier.size(30.dp))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = { viewModel.callManager.audioCallManager.toggleMute() },
                            modifier = Modifier.size(52.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isMuted) CrimsonError else Color.White.copy(alpha = 0.2f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = "Mute Mic")
                        }

                        FilledIconButton(
                            onClick = { viewModel.callManager.audioCallManager.toggleSpeaker() },
                            modifier = Modifier.size(52.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isSpeakerOn) CyberCyan else Color.White.copy(alpha = 0.2f),
                                contentColor = if (isSpeakerOn) Color(0xFF00363D) else Color.White
                            )
                        ) {
                            Icon(if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown, contentDescription = "Speaker")
                        }

                        if (call.callType == CallType.VIDEO) {
                            FilledIconButton(
                                onClick = { viewModel.callManager.videoCallManager.switchCamera(lifecycleOwner) },
                                modifier = Modifier.size(52.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.2f),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Camera")
                            }

                            FilledIconButton(
                                onClick = { viewModel.callManager.videoCallManager.toggleCamera(lifecycleOwner) },
                                modifier = Modifier.size(52.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (!isCameraEnabled) CrimsonError else Color.White.copy(alpha = 0.2f),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(if (isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff, contentDescription = "Toggle Camera")
                            }
                        }

                        FilledIconButton(
                            onClick = { viewModel.callManager.endCall() },
                            modifier = Modifier.size(58.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = CrimsonError,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "End Call", modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Column {
                    Text(
                        text = "Voice & Video Calls",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Direct peer-to-peer audio & video streaming without internet",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = null,
                            tint = NeonEmerald,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Zero-Cloud Calling",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Voice flows directly through local UDP datagrams (Port 8990) and Video through encrypted P2P sockets.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                GlassCard(borderColor = CyberCyan.copy(alpha = 0.5f)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Call Diagnostics & Logs", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Button(
                                onClick = {
                                    AppDiagnostics.copyReportToClipboard(
                                        context,
                                        mapOf(
                                            "Local IP" to viewModel.transportManager.localIp.value,
                                            "Online Peers" to "${discoveredDevices.size}",
                                            "Shizuku Status" to viewModel.shizukuManager.status.value.name,
                                            "WakeLock Active" to "${viewModel.wakeLockManager.isWakeLockActive.value}"
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF00363D), modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Logs", color = Color(0xFF00363D), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "If you encounter any call issue, crash, or one-sided voice, tap 'Copy Logs' and send the output to the developer to diagnose instantly.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Online Peers Ready to Call (${discoveredDevices.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (discoveredDevices.isEmpty()) {
                item {
                    GlassCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No peers detected yet",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Open PeerLink on another device to start a voice or video call.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(discoveredDevices.values.toList(), key = { it.id }) { peer ->
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = peer.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${peer.address} • ${peer.transportType.name}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.startVoiceCall(peer) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Voice Call", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Voice")
                                }
                                Button(
                                    onClick = { viewModel.startVideoCall(peer) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = Color(0xFF00363D), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Video", color = Color(0xFF00363D))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
