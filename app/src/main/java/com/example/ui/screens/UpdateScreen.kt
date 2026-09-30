package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SystemButton
import com.example.ui.components.SystemCard
import com.example.ui.components.SystemHeading
import com.example.ui.components.SystemSubtitle
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.SystemBg
import com.example.ui.theme.SystemTextMuted
import com.example.ui.theme.SystemTextSecondary
import com.example.ui.theme.SystemTextWhite
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UpdateScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var lastCheckedText by remember { mutableStateOf("Checked today at 09:45") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SystemBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            SystemHeading("SYSTEM UPDATES", fontSize = 20, color = AccentCyan)
            Spacer(modifier = Modifier.height(4.dp))
            SystemSubtitle("Firmware, Subsystem Packages & Release Channel Management")
        }

        // Current Version Card
        item {
            SystemCard(borderColor = AccentGreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(AccentGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        SystemHeading("SYSTEM IS UP TO DATE", fontSize = 15, color = AccentGreen)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Installed Version: v2.4.0 (Build 3624 - Production)",
                            color = SystemTextWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = lastCheckedText,
                            color = SystemTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Check Action Button
        item {
            SystemButton(
                text = if (isChecking) "CHECKING REPOSITORIES..." else "CHECK FOR UPDATES NOW",
                onClick = {
                    scope.launch {
                        isChecking = true
                        delay(1200)
                        isChecking = false
                        lastCheckedText = "Checked just now"
                        viewModel.postToast("System Controller is on the latest release (v2.4.0)")
                    }
                },
                enabled = !isChecking,
                accentColor = AccentCyan,
                textColor = SystemBg,
                icon = Icons.Default.Refresh
            )
        }

        // Changelog Card
        item {
            SystemCard {
                SystemHeading("CHANGELOG v2.4.0", fontSize = 14)
                Spacer(modifier = Modifier.height(10.dp))

                ChangelogItem(
                    tag = "NEW",
                    title = "Single-Activity DrawerLayout + Floating Bottom Pill Bar",
                    desc = "Modern cyber dark environment (#0B0E14) with four equal-width tabs: HOME, MSG, PROFILE, ABOUT."
                )

                ChangelogItem(
                    tag = "SHIZUKU",
                    title = "Kernel Latency & Wi-Fi Low-Latency Engine",
                    desc = "Direct integration with Shizuku v13.1.5 for 120Hz display lock and scan throttling suppression."
                )

                ChangelogItem(
                    tag = "BOOSTER",
                    title = "Downloads Folder Game Booster Config",
                    desc = "Automated deployment of p2p_gaming_boost.cfg to public Download directory for zero-overhead game performance."
                )

                ChangelogItem(
                    tag = "DIAG",
                    title = "Privileged Shell Terminal & Process Manager",
                    desc = "Real-time interactive terminal executing dumpsys, getprop, cmd wifi, and process tree inspect."
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ChangelogItem(tag: String, title: String, desc: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                    .background(AccentCyan.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(tag, color = AccentCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, color = SystemTextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(desc, color = SystemTextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
    }
}
