package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonEmerald

enum class BadgeType {
    ONLINE,
    OFFLINE,
    CONNECTED,
    CONNECTING,
    ENCRYPTED,
    SHIZUKU_ACTIVE,
    SHIZUKU_INACTIVE,
    WARNING
}

@Composable
fun StatusBadge(
    type: BadgeType,
    text: String? = null,
    modifier: Modifier = Modifier
) {
    val (bgColor, dotColor, label) = when (type) {
        BadgeType.ONLINE -> Triple(NeonEmerald.copy(alpha = 0.15f), NeonEmerald, text ?: "Online")
        BadgeType.CONNECTED -> Triple(CyberCyan.copy(alpha = 0.15f), CyberCyan, text ?: "Connected")
        BadgeType.CONNECTING -> Triple(AmberWarning.copy(alpha = 0.15f), AmberWarning, text ?: "Connecting...")
        BadgeType.OFFLINE -> Triple(Color.Gray.copy(alpha = 0.15f), Color.Gray, text ?: "Offline")
        BadgeType.ENCRYPTED -> Triple(ElectricViolet.copy(alpha = 0.15f), ElectricViolet, text ?: "E2EE Secured")
        BadgeType.SHIZUKU_ACTIVE -> Triple(NeonEmerald.copy(alpha = 0.15f), NeonEmerald, text ?: "Shizuku Authorized")
        BadgeType.SHIZUKU_INACTIVE -> Triple(AmberWarning.copy(alpha = 0.15f), AmberWarning, text ?: "Shizuku Off")
        BadgeType.WARNING -> Triple(CrimsonError.copy(alpha = 0.15f), CrimsonError, text ?: "Notice")
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = dotColor
        )
    }
}
