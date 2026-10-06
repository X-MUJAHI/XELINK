/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: TurboBoostCard.kt
 *
 * Responsibilities:
 * - Cyberpunk styled Turbo Boost & Rewarded Ads Card and Dialog.
 * - Voluntary, opt-in interaction to empower P2P throughput and offline AI acceleration.
 * - Fully compliant with "Use icons instead emojis" rule: uses Material Icons exclusively.
 */

package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ads.RewardedAdManager
import com.example.ui.theme.CyberAccentAmber
import com.example.ui.theme.CyberAccentCyan
import com.example.ui.theme.CyberAccentGreen
import com.example.ui.theme.CyberAccentPurple
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.NeonEmerald
import com.example.viewmodel.MainViewModel

/**
 * Safely resolves the nearest Activity from a Context.
 */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Full-width Cyberpunk Card displaying Turbo Boost status, perks, and
 * rewarded ad activation trigger.
 */
@Composable
fun TurboBoostCard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val adManager = viewModel.rewardedAdManager

    val isTurboActive by adManager.isTurboActive.collectAsState()
    val remainingSeconds by adManager.remainingBoostSeconds.collectAsState()
    val isAdLoaded by adManager.isAdLoaded.collectAsState()
    val isLoadingAd by adManager.isLoading.collectAsState()
    val turboTokens by adManager.turboTokens.collectAsState()

    val cardBorderColor = if (isTurboActive) NeonEmerald.copy(alpha = 0.6f) else CyberBorder
    val containerBg = if (isTurboActive) CyberSurface else CyberCard

    CyberCard(
        modifier = modifier,
        borderColor = cardBorderColor,
        containerColor = containerBg
    ) {
        // Header Row: Title & Status Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isTurboActive) NeonEmerald.copy(alpha = 0.2f) else CyberAccentCyan.copy(alpha = 0.15f))
                        .border(1.dp, if (isTurboActive) NeonEmerald else CyberAccentCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Turbo Boost",
                        tint = if (isTurboActive) NeonEmerald else CyberCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "P2P TURBO BOOST",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isTurboActive) "High-Throughput Acceleration Active" else "Voluntary Speed & AI Optimization",
                        fontSize = 11.sp,
                        color = if (isTurboActive) NeonEmerald else CyberTextSecondary
                    )
                }
            }

            // Status Pill
            if (isTurboActive) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = NeonEmerald.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, NeonEmerald)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = NeonEmerald,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = adManager.formatRemainingTime(remainingSeconds),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = NeonEmerald
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CyberCardElevated,
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Text(
                        text = "STANDBY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyberTextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Perks Feature Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CyberBackground.copy(alpha = 0.6f))
                .border(1.dp, DarkBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TurboPerkRow(
                icon = Icons.Default.Speed,
                iconTint = CyberAccentCyan,
                title = "Max P2P Throughput",
                description = if (isTurboActive) "Uncapped 110 MB/s pipelining & expanded buffer window" else "Unlocks deep socket pipelining for faster transfers",
                isActive = isTurboActive
            )
            TurboPerkRow(
                icon = Icons.Default.NetworkCheck,
                iconTint = CyberAccentGreen,
                title = "Priority Stream Quality",
                description = if (isTurboActive) "Low-latency packet queue for screen share & voice calls" else "Reduces Wi-Fi jitter during concurrent transfers",
                isActive = isTurboActive
            )
            TurboPerkRow(
                icon = Icons.Default.AutoAwesome,
                iconTint = CyberAccentPurple,
                title = "Offline AI Acceleration",
                description = if (isTurboActive) "High-priority CPU & RAM scheduling for local Qwen models" else "Accelerates local neural inference token generation",
                isActive = isTurboActive
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "REWARD TOKENS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberTextMuted,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "$turboTokens Tokens",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyberCyan
                )
            }

            Button(
                onClick = {
                    val activity = context.findActivity()
                    if (activity != null) {
                        adManager.showRewardedAd(
                            activity = activity,
                            onRewardEarned = { reward ->
                                viewModel.postToast("Turbo Boost Activated! +${reward.amount} ${reward.type}")
                            },
                            onAdDismissed = {
                                // Ad completed/closed
                            },
                            onAdFailed = { error ->
                                viewModel.postToast(error)
                            }
                        )
                    } else {
                        viewModel.postToast("Unable to present ad: Activity context unavailable")
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTurboActive) CyberAccentGreen else CyberCyan,
                    disabledContainerColor = CyberCardElevated
                ),
                shape = RoundedCornerShape(10.dp),
                enabled = isAdLoaded && !isLoadingAd
            ) {
                if (isLoadingAd) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = CyberCyan,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LOADING AD...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextSecondary
                    )
                } else {
                    Icon(
                        imageVector = if (isTurboActive) Icons.Default.Bolt else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isTurboActive) Color(0xFF042111) else Color(0xFF00363D),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isTurboActive) "EXTEND BOOST (+30M)" else "WATCH AD (+30M BOOST)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTurboActive) Color(0xFF042111) else Color(0xFF00363D)
                    )
                }
            }
        }
    }
}

@Composable
private fun TurboPerkRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    isActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) iconTint else iconTint.copy(alpha = 0.5f),
            modifier = Modifier
                .size(18.dp)
                .padding(top = 1.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyberTextPrimary
                )
                if (isActive) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = NeonEmerald,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Text(
                text = description,
                fontSize = 11.sp,
                color = CyberTextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}

/**
 * Cyberpunk Modal Dialog for Turbo Boost & Rewarded Ads.
 * Can be invoked from side drawers, header pills, or quick links.
 */
@Composable
fun TurboBoostDialog(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = CyberSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, CyberBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(CyberAccentCyan.copy(alpha = 0.15f))
                                .border(1.dp, CyberAccentCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "TURBO BOOST HUB",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberTextPrimary,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = CyberTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Embed the TurboBoostCard inside the dialog
                TurboBoostCard(viewModel = viewModel)

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Watching a short video directly supports offline P2P development and activates 30 minutes of high-priority socket buffers and local AI acceleration.",
                    fontSize = 11.sp,
                    color = CyberTextMuted,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
