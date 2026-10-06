/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: PrivacyPolicyScreen.kt
 *
 * Commentary / Architectural Overview:
 * In-app Privacy Policy disclosure screen.
 * Details the application's Zero-Cloud, Zero-Tracking, Peer-to-Peer architecture,
 * Android runtime permission purposes (Camera, Mic, Storage, Nearby Wi-Fi/Bluetooth, Screen Share),
 * local cryptographic key handling (AES-256-GCM, ECDH), on-device AI inference,
 * and user data ownership.
 *
 * Visual Tokens & Theming:
 * - Implemented in the Cyberpunk Dark system tokens (CyberBackground, CyberCard, CyberBorder).
 * - Exclusively utilizes Material Icons (no emojis) adhering to interface guidelines.
 */

package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberSectionHeader
import com.example.ui.components.CyberStatBoxes
import com.example.ui.components.CyberStatItem
import com.example.ui.theme.CyberAccentCyan
import com.example.ui.theme.CyberAccentGreen
import com.example.ui.theme.CyberAccentPurple
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = CyberBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(CyberBackground)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Bar
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(CyberCardElevated, RoundedCornerShape(10.dp))
                            .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CyberAccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PRIVACY POLICY",
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                color = CyberTextPrimary,
                                fontFamily = FontFamily.SansSerif
                            )
                        )
                        Text(
                            text = "Zero-Cloud // Local Mesh // E2E Encrypted",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                                color = CyberAccentGreen
                            )
                        )
                    }

                    CyberBadge(
                        text = "VERIFIED",
                        tint = CyberAccentGreen
                    )
                }
            }

            // Stat Summary Boxes
            item {
                val stats = listOf(
                    CyberStatItem("CLOUD HARVEST", "0 BYTES"),
                    CyberStatItem("TRACKING SDKs", "0 FOUND"),
                    CyberStatItem("CIPHER SUITE", "AES-256"),
                    CyberStatItem("AI INFERENCE", "100% LOCAL")
                )
                CyberStatBoxes(stats = stats)
            }

            // Executive Summary Card
            item {
                CyberCard(
                    borderColor = CyberAccentGreen.copy(alpha = 0.6f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(CyberAccentGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = CyberAccentGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Zero-Cloud Privacy Guarantee",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = CyberTextPrimary
                            )
                            Text(
                                text = "Effective: October 6, 2026 // v2.6 Mesh",
                                fontSize = 11.sp,
                                color = CyberTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "PeerLink is engineered from the ground up on an offline, peer-to-peer mesh architecture. " +
                                "We operate zero central servers, zero cloud databases, and zero tracking intermediaries. " +
                                "All messages, calls, media streams, and files remain strictly between your device and authenticated peers over local Wi-Fi or Wi-Fi Direct.",
                        fontSize = 13.sp,
                        color = CyberTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            // Permission Transparency Section
            item {
                CyberSectionHeader(title = "ANDROID PERMISSIONS & USAGE TRANSPARENCY")
            }

            item {
                PermissionPolicyCard(
                    title = "Camera (CAMERA)",
                    icon = Icons.Filled.CameraAlt,
                    accentColor = CyberAccentCyan,
                    purpose = "Used strictly in real time for peer-to-peer video calls and scanning QR codes for zero-knowledge key exchange. Video frames are streamed directly to the peer over encrypted local sockets; no feeds or photos are captured or transmitted to third parties."
                )
            }

            item {
                PermissionPolicyCard(
                    title = "Microphone (RECORD_AUDIO)",
                    icon = Icons.Filled.Mic,
                    accentColor = CyberAccentGreen,
                    purpose = "Used exclusively for real-time voice/video calling and user-initiated local call recording. Audio frames are Opus-encoded, encrypted with AES-256-GCM, and sent directly to the connected peer node. Local recordings save only to user-accessible downloads on-device."
                )
            }

            item {
                PermissionPolicyCard(
                    title = "Storage & Downloads Access",
                    icon = Icons.Filled.Folder,
                    accentColor = CyberAccentPurple,
                    purpose = "Used solely to read files selected by the user for P2P sending, write received files to /Download/PeerLink/, store offline GGUF AI models, and retain local chat history in Android Room SQLite. No unselected personal documents are accessed."
                )
            }

            item {
                PermissionPolicyCard(
                    title = "Nearby Devices & Wi-Fi Sockets",
                    icon = Icons.Filled.Wifi,
                    accentColor = CyberAccentCyan,
                    purpose = "Used to advertise and discover peer nodes over mDNS (_peerlink._tcp), UDP beacons, and Wi-Fi Direct. Used strictly for local socket handshakes without requiring internet connectivity."
                )
            }

            item {
                PermissionPolicyCard(
                    title = "Screen Sharing (MediaProjection)",
                    icon = Icons.Filled.ScreenShare,
                    accentColor = CyberAccentPurple,
                    purpose = "Used only when you explicitly tap 'Share Screen' during a session. Display mirror frames are encoded in real time and piped directly to the authenticated peer. Streaming terminates immediately upon user stop or call end."
                )
            }

            item {
                PermissionPolicyCard(
                    title = "System Notifications",
                    icon = Icons.Filled.Notifications,
                    accentColor = CyberAccentGreen,
                    purpose = "Used exclusively on-device to post high-priority alerts for incoming calls, incoming file transfers, and reconnection alerts when an offline peer returns to the local mesh."
                )
            }

            item {
                PermissionPolicyCard(
                    title = "Shizuku Privileged Access (Optional)",
                    icon = Icons.Filled.Code,
                    accentColor = CyberAccentCyan,
                    purpose = "Used only if enabled by the user via the Shizuku API to tune Wi-Fi scan throttling, toggle low-latency mode, and diagnose network interfaces. No private user data is accessed or extracted."
                )
            }

            // Cryptographic Guarantees Section
            item {
                CyberSectionHeader(title = "CRYPTOGRAPHIC PROTOCOLS")
            }

            item {
                CyberCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = CyberAccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "End-to-End Cryptography",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = CyberTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• Authenticated Encryption: AES-256-GCM with 128-bit authentication tags prevents eavesdropping and tampering.\n" +
                                "• Key Exchange: Elliptic-Curve Diffie-Hellman (ECDH secp256r1) derives unique symmetric session keys per peer connection.\n" +
                                "• Local Key Storage: Private identity keys are generated on-device, protected in Android secure storage, and never leave your hardware.\n" +
                                "• Anti-Replay Defense: Monotonic packet sequences and timestamp validation reject spoofed or repeated frames.",
                        fontSize = 12.sp,
                        color = CyberTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            // On-Device AI Section
            item {
                CyberSectionHeader(title = "ON-DEVICE AI NEURAL PROCESSING")
            }

            item {
                CyberCard(
                    borderColor = CyberAccentPurple.copy(alpha = 0.5f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = CyberAccentPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Local GGUF Models // Zero Remote API",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = CyberTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "The integrated AI assistant operates using local quantized models (e.g., Qwen GGUF) executed directly on your device CPU/GPU. " +
                                "Zero prompts, queries, code snippets, or AI outputs are transmitted across the internet to any cloud server or LLM provider.",
                        fontSize = 12.sp,
                        color = CyberTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            // Data Retention & User Rights Section
            item {
                CyberSectionHeader(title = "DATA OWNERSHIP & PURGE")
            }

            item {
                CyberCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.DeleteForever,
                            contentDescription = null,
                            tint = CyberAccentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Full User Control & Instant Erase",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = CyberTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• Local Storage Only: All message logs reside solely in your local SQLite/Room database.\n" +
                                "• Instant Delete: Deleting a chat thread permanently removes all messages from local disk.\n" +
                                "• Complete Wipe: Clearing app data in Android System Settings permanently purges all databases, identity keys, and cached files.",
                        fontSize = 12.sp,
                        color = CyberTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            // Developer & Compliance Section
            item {
                CyberSectionHeader(title = "COMPLIANCE & DEVELOPER CONTACT")
            }

            item {
                CyberCard {
                    Text(
                        text = "Developer Contact",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CyberTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "For privacy questions, audits, or support inquiries regarding PeerLink's offline architecture:",
                        fontSize = 12.sp,
                        color = CyberTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = null,
                                tint = CyberAccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ri96414693@gmail.com",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberAccentCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString("ri96414693@gmail.com"))
                                Toast.makeText(context, "Contact email copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy Email",
                                tint = CyberTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val summary = "PeerLink Privacy Policy:\n" +
                                    "Architecture: 100% Offline P2P Mesh\n" +
                                    "Cloud Tracking: None (Zero Telemetry)\n" +
                                    "Encryption: E2E AES-256-GCM / ECDH\n" +
                                    "AI: 100% On-Device GGUF Inference\n" +
                                    "Contact: ri96414693@gmail.com"
                            clipboardManager.setText(AnnotatedString(summary))
                            Toast.makeText(context, "Privacy summary copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberAccentCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = null,
                            tint = Color(0xFF00363D),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Copy Privacy Summary",
                            color = Color(0xFF00363D),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onBack,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = CyberAccentGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Close Privacy Policy",
                            color = CyberTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun PermissionPolicyCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    purpose: String
) {
    CyberCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = CyberTextPrimary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = purpose,
            fontSize = 12.sp,
            color = CyberTextSecondary,
            lineHeight = 16.sp
        )
    }
}
