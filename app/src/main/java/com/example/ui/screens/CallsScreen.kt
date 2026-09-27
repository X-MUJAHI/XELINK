package com.example.ui.screens

import android.Manifest
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.example.ui.components.GlassCard
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonEmerald
import com.example.viewmodel.MainViewModel

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
    val isFrontCamera by viewModel.callManager.videoCallManager.isFrontCamera.collectAsState()
    val videoFps by viewModel.callManager.videoCallManager.fps.collectAsState()
    val targetFps by viewModel.callManager.videoCallManager.targetFps.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions handled
    }

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

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF070B14))
        ) {
            if (call.callType == CallType.VIDEO) {
                // Video Call Screen
                Box(modifier = Modifier.fillMaxSize()) {
                    // Remote Video (Full Screen / Background)
                    if (remoteVideoBitmap != null) {
                        Image(
                            bitmap = remoteVideoBitmap!!.asImageBitmap(),
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
                                    viewModel.callManager.videoCallManager.bindCamera(
                                        lifecycleOwner = lifecycleOwner,
                                        surfaceProvider = previewView.surfaceProvider
                                    )
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

                    // Audio Waveform visualizer
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

                if (call.callType == CallType.VIDEO && call.callState == CallState.CONNECTED) {
                    Text(
                        text = "FPS: $videoFps",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyberCyan
                    )
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
                // Custom Frame Rate Selector Bar (Video Call Only)
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
                    Spacer(modifier = Modifier.height(14.dp))
                }

                if (call.callState == CallState.INCOMING_RINGING) {
                    // Incoming Ringing Controls
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
                    // Connected / Outgoing In-Call Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mic Mute
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

                        // Speaker toggle
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

                        // If video call: camera switch & camera toggle
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

                        // End Call
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
        // No Active Call -> Show Quick Call Launchpad & Available Nearby Peers
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
