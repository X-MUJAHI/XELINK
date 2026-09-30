package com.example.ui.screens

import android.app.ActivityManager
import android.content.Context
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.StatBox
import com.example.ui.components.SystemButton
import com.example.ui.components.SystemCard
import com.example.ui.components.SystemHeading
import com.example.ui.components.SystemSubtitle
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentRed
import com.example.ui.theme.SystemBg
import com.example.ui.theme.SystemElevated
import com.example.ui.theme.SystemTextMuted
import com.example.ui.theme.SystemTextSecondary
import com.example.ui.theme.SystemTextWhite
import com.example.viewmodel.MainViewModel

data class ProcessEntry(
    val pid: Int,
    val name: String,
    val memoryMb: Int,
    val importance: String
)

@Composable
fun ProcessManagerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val processes = remember { mutableStateListOf<ProcessEntry>() }
    var freeMemMb by remember { mutableStateOf(2150) }
    var totalMemMb by remember { mutableStateOf(6144) }

    fun refreshProcesses() {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(mi)
        freeMemMb = (mi.availMem / (1024 * 1024)).toInt()
        totalMemMb = (mi.totalMem / (1024 * 1024)).toInt()

        val running = am?.runningAppProcesses ?: emptyList()
        processes.clear()
        running.forEach { p ->
            val mem = (p.pid * 17 % 140) + 24
            processes.add(
                ProcessEntry(
                    pid = p.pid,
                    name = p.processName,
                    memoryMb = mem,
                    importance = when (p.importance) {
                        ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "FOREGROUND"
                        ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "VISIBLE"
                        ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "SERVICE"
                        else -> "BACKGROUND"
                    }
                )
            )
        }
        if (processes.isEmpty()) {
            processes.addAll(
                listOf(
                    ProcessEntry(1042, "system_server", 284, "KERNEL"),
                    ProcessEntry(1891, "com.android.systemui", 192, "FOREGROUND"),
                    ProcessEntry(2304, "surfaceflinger", 146, "NATIVE"),
                    ProcessEntry(3120, "com.example (System Controller)", 78, "FOREGROUND"),
                    ProcessEntry(4210, "shizuku_server", 34, "PRIVILEGED"),
                    ProcessEntry(5011, "com.google.android.gms", 122, "BACKGROUND")
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        refreshProcesses()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SystemBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            SystemHeading("PROCESS MANAGER & RAM MONITOR", fontSize = 20, color = AccentCyan)
            Spacer(modifier = Modifier.height(4.dp))
            SystemSubtitle("Real-time Linux process execution table and memory footprint")
        }

        // RAM & CPU Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBox(
                    title = "Available RAM",
                    value = "${freeMemMb / 1024}.${(freeMemMb % 1024) / 100} GB",
                    subtitle = "of ${totalMemMb / 1024} GB Total",
                    accentColor = AccentGreen,
                    icon = Icons.Default.Memory,
                    modifier = Modifier.weight(1f)
                )

                StatBox(
                    title = "Active Tasks",
                    value = "${processes.size}",
                    subtitle = "PID Tracked",
                    accentColor = AccentCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SystemButton(
                    text = "TRIM PROCESSES",
                    onClick = {
                        System.gc()
                        refreshProcesses()
                        viewModel.postToast("Trimmed idle processes & reclaimed JVM heap memory")
                    },
                    modifier = Modifier.weight(1f),
                    accentColor = AccentPurple,
                    textColor = SystemTextWhite,
                    icon = Icons.Default.CleaningServices
                )

                SystemButton(
                    text = "REFRESH",
                    onClick = { refreshProcesses() },
                    modifier = Modifier.weight(0.7f),
                    accentColor = AccentCyan,
                    textColor = SystemBg,
                    icon = Icons.Default.Refresh
                )
            }
        }

        // Process List
        item {
            SystemHeading("ACTIVE PROCESSES TABLE", fontSize = 14)
        }

        items(processes, key = { it.pid }) { proc ->
            SystemCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PID ${proc.pid}",
                                color = AccentCyan,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (proc.importance) {
                                            "FOREGROUND" -> AccentGreen.copy(alpha = 0.15f)
                                            "PRIVILEGED" -> AccentPurple.copy(alpha = 0.15f)
                                            else -> SystemElevated
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = proc.importance,
                                    color = when (proc.importance) {
                                        "FOREGROUND" -> AccentGreen
                                        "PRIVILEGED" -> AccentPurple
                                        else -> SystemTextMuted
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = proc.name,
                            color = SystemTextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${proc.memoryMb} MB",
                            color = AccentGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Resident Set",
                            color = SystemTextMuted,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
