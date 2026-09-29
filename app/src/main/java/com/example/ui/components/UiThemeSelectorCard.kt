package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.UiThemeManager
import com.example.ui.theme.UiThemeStyle

@Composable
fun UiThemeSelectorCard(
    uiThemeManager: UiThemeManager,
    modifier: Modifier = Modifier
) {
    val currentStyle by uiThemeManager.currentStyle.collectAsState()
    val isModern = currentStyle == UiThemeStyle.MODERN
    val isGlassmorphism = currentStyle == UiThemeStyle.GLASSMORPHISM

    GlassCard(
        borderColor = when {
            isModern -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            isGlassmorphism -> ElectricViolet.copy(alpha = 0.6f)
            else -> CyberCyan.copy(alpha = 0.4f)
        },
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        if (isModern) CyberCyan.copy(alpha = 0.20f) else ElectricViolet.copy(alpha = 0.25f),
                                        if (isModern) ElectricViolet.copy(alpha = 0.12f) else CyberCyan.copy(alpha = 0.24f)
                                    )
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Interface Presentation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Choose the visual system used across PeerLink",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            when {
                                isModern -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                isGlassmorphism -> ElectricViolet.copy(alpha = 0.20f)
                                else -> CyberCyan.copy(alpha = 0.12f)
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                isModern -> MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
                                isGlassmorphism -> ElectricViolet.copy(alpha = 0.48f)
                                else -> CyberCyan.copy(alpha = 0.38f)
                            },
                            RoundedCornerShape(50.dp)
                        )
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = currentStyle.title.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Choose UI Mode",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeButton(
                    label = "Default",
                    icon = Icons.Default.Widgets,
                    selected = currentStyle == UiThemeStyle.DEFAULT,
                    selectedColor = CyberCyan,
                    onClick = { uiThemeManager.setUiStyle(UiThemeStyle.DEFAULT) },
                    modifier = Modifier.weight(1f)
                )
                ModeButton(
                    label = "Glass",
                    icon = Icons.Default.AutoAwesome,
                    selected = isGlassmorphism,
                    selectedColor = ElectricViolet,
                    onClick = { uiThemeManager.setUiStyle(UiThemeStyle.GLASSMORPHISM) },
                    modifier = Modifier.weight(1f)
                )
                ModeButton(
                    label = "Modern",
                    icon = Icons.Default.CheckCircle,
                    selected = isModern,
                    selectedColor = MaterialTheme.colorScheme.primary,
                    onClick = { uiThemeManager.setUiStyle(UiThemeStyle.MODERN) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StyleOptionTile(
                    title = "Default UI",
                    badge = "STANDARD",
                    badgeColor = CyberCyan,
                    description = "Original high-contrast cyber interface with solid surfaces and crisp borders.",
                    icon = Icons.Default.Widgets,
                    isSelected = currentStyle == UiThemeStyle.DEFAULT,
                    onSelect = { uiThemeManager.setUiStyle(UiThemeStyle.DEFAULT) }
                )
                StyleOptionTile(
                    title = "Glassmorphism UI",
                    badge = "FROSTED",
                    badgeColor = ElectricViolet,
                    description = "Existing luminous glass presentation with ambient mesh glow and stronger specular edges.",
                    icon = Icons.Default.AutoAwesome,
                    isSelected = isGlassmorphism,
                    onSelect = { uiThemeManager.setUiStyle(UiThemeStyle.GLASSMORPHISM) }
                )
                StyleOptionTile(
                    title = "Modern UI",
                    badge = "RECOMMENDED STYLE",
                    badgeColor = MaterialTheme.colorScheme.primary,
                    description = "New adaptive frosted-glass system with Haze-backed blur, restrained lighting, floating navigation, and calmer hierarchy.",
                    icon = Icons.Default.CheckCircle,
                    isSelected = isModern,
                    onSelect = { uiThemeManager.setUiStyle(UiThemeStyle.MODERN) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            ModernPreview(isModern = isModern, isGlassmorphism = isGlassmorphism)
        }
    }
}

@Composable
private fun ModeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) selectedColor.copy(alpha = 0.86f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f),
            contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        ),
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ModernPreview(
    isModern: Boolean,
    isGlassmorphism: Boolean,
) {
    val title = when {
        isModern -> "Modern Glass Preview"
        isGlassmorphism -> "Frosted Glass Preview"
        else -> "Default Preview"
    }
    val description = when {
        isModern -> "Soft atmospheric lighting, subtle blur, calm borders, and floating depth."
        isGlassmorphism -> "Ambient neon light shines through stronger frosted surfaces."
        else -> "Solid cyber surfaces prioritize immediate contrast and clarity."
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (isModern) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.06f),
                        if (isGlassmorphism) ElectricViolet.copy(alpha = 0.08f) else Color.Transparent,
                        Color.Transparent,
                    )
                )
            )
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                    currentStyleLabel(isModern, isGlassmorphism),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
        }
    }
}

private fun currentStyleLabel(isModern: Boolean, isGlassmorphism: Boolean): String = when {
    isModern -> "Haze + M3"
    isGlassmorphism -> "Legacy Glass"
    else -> "Material Base"
}

@Composable
private fun StyleOptionTile(
    title: String,
    badge: String,
    badgeColor: Color,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) badgeColor.copy(alpha = 0.75f) else DarkBorder.copy(alpha = 0.38f),
        animationSpec = tween(180),
        label = "style_border"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) badgeColor.copy(alpha = 0.11f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.23f),
        animationSpec = tween(180),
        label = "style_background"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onSelect)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(badgeColor.copy(alpha = 0.13f))
                .border(1.dp, badgeColor.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(18.dp))
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(badgeColor.copy(alpha = 0.13f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(badge, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                }
            }
            Text(description, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp)
        }

        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = if (isSelected) "Selected" else "Select",
            tint = if (isSelected) badgeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            modifier = Modifier.size(20.dp)
        )
    }
}
