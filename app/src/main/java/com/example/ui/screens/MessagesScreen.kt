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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SystemCard
import com.example.ui.components.SystemHeading
import com.example.ui.components.SystemSubtitle
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.SystemBg
import com.example.ui.theme.SystemBorder
import com.example.ui.theme.SystemCard
import com.example.ui.theme.SystemElevated
import com.example.ui.theme.SystemTextMuted
import com.example.ui.theme.SystemTextSecondary
import com.example.ui.theme.SystemTextWhite
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SystemMessageItem(
    val id: String,
    val sender: String,
    val content: String,
    val timestamp: String,
    val type: MessageType
)

enum class MessageType {
    KERNEL,
    BOOSTER,
    NETWORK,
    USER
}

@Composable
fun MessagesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val messages = remember {
        mutableStateListOf(
            SystemMessageItem(
                id = "1",
                sender = "KERNEL SUBSYSTEM",
                content = "System Controller initialization completed. Tier-0 shell permissions bound.",
                timestamp = "09:42",
                type = MessageType.KERNEL
            ),
            SystemMessageItem(
                id = "2",
                sender = "GAME BOOSTER",
                content = "Config profile deployed. Display peak refresh locked to 120Hz.",
                timestamp = "09:43",
                type = MessageType.BOOSTER
            ),
            SystemMessageItem(
                id = "3",
                sender = "WI-FI PHY ENGINE",
                content = "Wi-Fi Power Save suppressed. Low-latency socket buffer set to 16MB.",
                timestamp = "09:45",
                type = MessageType.NETWORK
            ),
            SystemMessageItem(
                id = "4",
                sender = "SHIZUKU DAEMON",
                content = "Binder service connected. UID 2000 privileged access verified.",
                timestamp = "09:46",
                type = MessageType.KERNEL
            )
        )
    }

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SystemBg)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        SystemHeading(text = "SYSTEM MESSAGES & DISPATCH", fontSize = 20, color = AccentCyan)
        Spacer(modifier = Modifier.height(4.dp))
        SystemSubtitle(text = "Real-time kernel telemetry logs and operator broadcast feed")
        Spacer(modifier = Modifier.height(12.dp))

        // Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val accentColor = when (msg.type) {
                    MessageType.KERNEL -> AccentGreen
                    MessageType.BOOSTER -> AccentPurple
                    MessageType.NETWORK -> AccentAmber
                    MessageType.USER -> AccentCyan
                }
                val icon: ImageVector = when (msg.type) {
                    MessageType.KERNEL -> Icons.Default.Shield
                    MessageType.BOOSTER -> Icons.Default.Bolt
                    MessageType.NETWORK -> Icons.Default.Wifi
                    MessageType.USER -> Icons.Default.Info
                }

                SystemCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (msg.type == MessageType.USER) AccentCyan.copy(alpha = 0.5f) else SystemBorder
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg.sender,
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Text(
                            text = msg.timestamp,
                            color = SystemTextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = msg.content,
                        color = SystemTextWhite,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Input & SEND Row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 85.dp) // Clearance for floating pill bar
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SystemCard)
                    .border(BorderStroke(1.dp, SystemBorder), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        color = SystemTextWhite,
                        fontSize = 13.sp
                    ),
                    cursorBrush = SolidColor(AccentCyan),
                    decorationBox = { innerTextField ->
                        if (inputText.isEmpty()) {
                            Text(
                                text = "Type command or message...",
                                color = SystemTextMuted,
                                fontSize = 13.sp
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentCyan)
                        .clickable {
                            if (inputText.isNotBlank()) {
                                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                messages.add(
                                    SystemMessageItem(
                                        id = System.currentTimeMillis().toString(),
                                        sender = "OPERATOR BROADCAST",
                                        content = inputText.trim(),
                                        timestamp = timeFormat.format(Date()),
                                        type = MessageType.USER
                                    )
                                )
                                inputText = ""
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SEND",
                            color = SystemBg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = SystemBg,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}
