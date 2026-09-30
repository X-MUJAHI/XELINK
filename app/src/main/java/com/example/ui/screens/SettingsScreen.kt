package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diagnostic.AppDiagnostics
import com.example.shizuku.GamingProfile
import com.example.shizuku.ShizukuStatus
import com.example.ui.components.SystemButton
import com.example.ui.components.SystemCard
import com.example.ui.components.SystemHeading
import com.example.ui.components.SystemSubtitle
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.SystemBg
import com.example.ui.theme.SystemBorder
import com.example.ui.theme.SystemElevated
import com.example.ui.theme.SystemTextMuted
import com.example.ui.theme.SystemTextSecondary
import com.example.ui.theme.SystemTextWhite
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shizukuStatus by viewModel.shizukuManager.status.collectAsState()
    val activeProfile by viewModel.shizukuManager.activeProfile.collectAsState()
    val isProfilePlaced by viewModel.shizukuManager.isBoosterProfilePlaced.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SystemBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            SystemHeading("SYSTEM SETTINGS", fontSize = 20, color = AccentCyan)
            Spacer(modifier = Modifier.height(4.dp))
            SystemSubtitle("Privileged Execution, Performance Profiles & Subsystem Management")
        }

        // Shizuku Privilege Deck Card
        item {
            SystemCard(
                borderColor = if (shizukuStatus == ShizukuStatus.AUTHORIZED) AccentGreen else AccentAmber
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        SystemHeading("SHIZUKU PRIVILEGED HOOK", fontSize = 14)
                        Text(
                            text = "Status: ${shizukuStatus.name}",
                            color = if (shizukuStatus == ShizukuStatus.AUTHORIZED) AccentGreen else AccentAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentCyan)
                            .clickable {
                                if (shizukuStatus == ShizukuStatus.AUTHORIZED) {
                                    viewModel.postToast("Shizuku is already authorized with UID 2000")
                                } else {
                                    viewModel.shizukuManager.requestAuthorization()
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (shizukuStatus == ShizukuStatus.AUTHORIZED) "AUTHORIZED" else "CONNECT",
                            color = SystemBg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enables system settings injection, 120Hz display locking, and zero-drop Wi-Fi power save suppression without needing persistent root.",
                    color = SystemTextMuted,
                    fontSize = 11.sp
                )
            }
        }

        // Active Gaming Profile Selector Card
        item {
            SystemCard {
                SystemHeading("GAMING BOOSTER PROFILE", fontSize = 14)
                Spacer(modifier = Modifier.height(10.dp))

                GamingProfile.values().forEach { profile ->
                    val isSelected = profile == activeProfile
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) SystemElevated else SystemBg)
                            .border(
                                BorderStroke(1.dp, if (isSelected) AccentCyan else SystemBorder),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                viewModel.shizukuManager.setGamingProfile(profile)
                                scope.launch {
                                    viewModel.shizukuManager.placeGameBoosterProfile()
                                    viewModel.postToast("Active Profile updated: ${profile.title}")
                                }
                            }
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = profile.title,
                                    color = if (isSelected) AccentCyan else SystemTextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = profile.refreshRate,
                                    color = if (isSelected) AccentGreen else SystemTextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = profile.subtitle,
                                color = SystemTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Configuration Cleanup
        item {
            SystemCard {
                SystemHeading("DATA & FILE MANAGEMENT", fontSize = 14)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Downloads Booster File", color = SystemTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = if (isProfilePlaced) "p2p_gaming_boost.cfg present in Download/" else "No file in Download/",
                            color = SystemTextMuted,
                            fontSize = 11.sp
                        )
                    }

                    if (isProfilePlaced) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentAmber.copy(alpha = 0.15f))
                                .border(BorderStroke(1.dp, AccentAmber), RoundedCornerShape(8.dp))
                                .clickable {
                                    scope.launch {
                                        viewModel.shizukuManager.deleteBoosterProfile()
                                        viewModel.postToast("Booster file removed from Downloads")
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("DELETE FILE", color = AccentAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Diagnostics Reset
        item {
            SystemButton(
                text = "CLEAR CRASH & DIAGNOSTIC LOGS",
                onClick = {
                    AppDiagnostics.clearSavedCrashLog(context)
                    viewModel.postToast("Diagnostic crash logs cleared")
                },
                accentColor = SystemElevated,
                textColor = SystemTextWhite,
                icon = Icons.Default.CleaningServices
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
