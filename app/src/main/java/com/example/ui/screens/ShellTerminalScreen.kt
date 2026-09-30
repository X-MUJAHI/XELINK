package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shizuku.ShizukuStatus
import com.example.ui.components.SystemCard
import com.example.ui.components.SystemHeading
import com.example.ui.components.SystemSubtitle
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentRed
import com.example.ui.theme.SystemBg
import com.example.ui.theme.SystemBorder
import com.example.ui.theme.SystemCard
import com.example.ui.theme.SystemElevated
import com.example.ui.theme.SystemTextMuted
import com.example.ui.theme.SystemTextSecondary
import com.example.ui.theme.SystemTextWhite
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class TerminalLog(
    val command: String,
    val output: String,
    val isError: Boolean = false
)

@Composable
fun ShellTerminalScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val shizukuStatus by viewModel.shizukuManager.status.collectAsState()

    var inputCommand by remember { mutableStateOf("") }
    val terminalLogs = remember {
        mutableStateListOf(
            TerminalLog(
                command = "shizuku-status",
                output = "System Controller Privileged Shell v2.4\nShizuku Binder: ${if (shizukuStatus == ShizukuStatus.AUTHORIZED) "AUTHORIZED (UID 2000)" else "STANDBY"}\nType command or select preset below."
            )
        )
    }

    val listState = rememberLazyListState()

    fun runCommand(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    if (shizukuStatus == ShizukuStatus.AUTHORIZED) {
                        viewModel.shizukuManager.executeShizukuCommand(trimmed)
                    } else {
                        // Standard local shell execution
                        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", trimmed))
                        val reader = BufferedReader(InputStreamReader(process.inputStream))
                        val sb = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            sb.append(line).append("\n")
                        }
                        process.waitFor()
                        reader.close()
                        if (sb.isEmpty()) "(Command executed with no output)" else sb.toString().trim()
                    }
                } catch (e: Exception) {
                    "Error: ${e.message}"
                }
            }

            terminalLogs.add(TerminalLog(command = trimmed, output = result, isError = result.startsWith("Error:")))
            inputCommand = ""
        }
    }

    LaunchedEffect(terminalLogs.size) {
        if (terminalLogs.isNotEmpty()) {
            listState.animateScrollToItem(terminalLogs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SystemBg)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                SystemHeading("SHELL TERMINAL", fontSize = 20, color = AccentCyan)
                SystemSubtitle("Privileged Shizuku & Local Linux Execution Engine")
            }

            Row {
                IconButton(onClick = {
                    val fullLog = terminalLogs.joinToString("\n\n") { "$ ${it.command}\n${it.output}" }
                    clipboardManager.setText(AnnotatedString(fullLog))
                    viewModel.postToast("Terminal console logs copied to clipboard")
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = AccentCyan)
                }
                IconButton(onClick = { terminalLogs.clear() }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SystemTextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Preset command chips
        val presets = listOf(
            "getprop ro.product.model",
            "cmd wifi status",
            "settings get system peak_refresh_rate",
            "dumpsys battery",
            "cat /proc/version"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SystemElevated)
                        .border(BorderStroke(1.dp, SystemBorder), RoundedCornerShape(8.dp))
                        .clickable { runCommand(preset) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = preset,
                        color = AccentCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Terminal Console Window
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SystemBg)
                .border(BorderStroke(1.dp, SystemBorder), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(terminalLogs) { log ->
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "shell@syscontroller:~$ ",
                                color = AccentCyan,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = log.command,
                                color = SystemTextWhite,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = log.output,
                            color = if (log.isError) AccentRed else AccentGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Command Input Field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 85.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SystemCard)
                    .border(BorderStroke(1.dp, SystemBorder), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$ ",
                    color = AccentCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                BasicTextField(
                    value = inputCommand,
                    onValueChange = { inputCommand = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        color = SystemTextWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    cursorBrush = SolidColor(AccentCyan),
                    decorationBox = { inner ->
                        if (inputCommand.isEmpty()) {
                            Text("Enter shell command...", color = SystemTextMuted, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        }
                        inner()
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentCyan)
                        .clickable { runCommand(inputCommand) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Execute",
                        tint = SystemBg,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
