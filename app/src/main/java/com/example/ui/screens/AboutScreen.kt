package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SystemCard
import com.example.ui.components.SystemHeading
import com.example.ui.components.SystemSubtitle
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.SystemBg
import com.example.ui.theme.SystemBorder
import com.example.ui.theme.SystemElevated
import com.example.ui.theme.SystemTextMuted
import com.example.ui.theme.SystemTextSecondary
import com.example.ui.theme.SystemTextWhite

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SystemBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            SystemHeading("ABOUT SYSTEM CONTROLLER", fontSize = 20, color = AccentCyan)
            Spacer(modifier = Modifier.height(4.dp))
            SystemSubtitle("High-performance privileged subsystem manager & telemetry engine")
        }

        // App Identity Card
        item {
            SystemCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(AccentCyan.copy(alpha = 0.15f))
                            .border(BorderStroke(1.5.dp, AccentCyan), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        SystemHeading("SYSTEM CONTROLLER", fontSize = 16)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Release v2.4.0 (Build 3624) • Production Dark",
                            color = AccentGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "minSdk 24 • targetSdk 36 • Android 16 Ready",
                            color = SystemTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Core Capabilities Card
        item {
            SystemCard {
                SystemHeading("ARCHITECTURE CAPABILITIES", fontSize = 14)
                Spacer(modifier = Modifier.height(12.dp))

                CapabilityItem(
                    title = "Shizuku Privileged Execution",
                    description = "Direct Binder connection to Rikka Shizuku v13.1.5 for elevated shell commands without persistent root."
                )

                CapabilityItem(
                    title = "Zero-Jitter Game Booster",
                    description = "Suppresses Wi-Fi scan throttling, forces 120Hz display refresh lock, and deploys p2p_gaming_boost.cfg to Downloads."
                )

                CapabilityItem(
                    title = "Privileged Shell Terminal",
                    description = "Real-time interactive command line for system queries (dumpsys, getprop, cmd wifi, top)."
                )

                CapabilityItem(
                    title = "Material 3 Dark Palette",
                    description = "Pure cyber dark environment with #0B0E14 background, #161D2A cards, 1dp #24324D borders, and cyan/green accents."
                )
            }
        }

        // Technical Specs Card
        item {
            SystemCard {
                SystemHeading("BUILD & SUBSYSTEM DETAILS", fontSize = 14)
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("Framework", "Kotlin 2.2.10 + Jetpack Compose")
                    DetailRow("ViewBinding", "Enabled in Gradle Toolchain")
                    DetailRow("ABI Support", Build.SUPPORTED_ABIS.joinToString(", "))
                    DetailRow("Shizuku API Version", "13.1.5 (dev.rikka.shizuku)")
                    DetailRow("License", "Apache 2.0 Open System Controller")
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun CapabilityItem(title: String, description: String) {
    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(AccentCyan)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = SystemTextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            color = SystemTextSecondary,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(start = 14.dp)
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = SystemTextMuted, fontSize = 12.sp)
        Text(text = value, color = SystemTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
