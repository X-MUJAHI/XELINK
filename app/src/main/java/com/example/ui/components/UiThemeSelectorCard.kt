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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
    val isGlassmorphism = currentStyle == UiThemeStyle.GLASSMORPHISM

    GlassCard(
        borderColor = if (isGlassmorphism) ElectricViolet.copy(alpha = 0.6f) else CyberCyan.copy(alpha = 0.4f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isGlassmorphism) {
                                    Brush.linearGradient(listOf(ElectricViolet.copy(alpha = 0.35f), CyberCyan.copy(alpha = 0.35f)))
                                } else {
                                    Brush.linearGradient(listOf(CyberCyan.copy(alpha = 0.2f), CyberCyan.copy(alpha = 0.2f)))
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = if (isGlassmorphism) CyberCyan else CyberCyan
                        )
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
                            text = "Switch between Default and Glassmorphism UI",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Active Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isGlassmorphism) ElectricViolet.copy(alpha = 0.25f) else CyberCyan.copy(alpha = 0.15f)
                        )
                        .border(
                            1.dp,
                            if (isGlassmorphism) ElectricViolet.copy(alpha = 0.6f) else CyberCyan.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isGlassmorphism) "✨ GLASS" else "DEFAULT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGlassmorphism) CyberCyan else CyberCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Choice Button Bar (Explicitly implements user requirement for buttons)
            Text(
                text = "Choose UI Mode",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button 1: Default
                val defaultSelected = currentStyle == UiThemeStyle.DEFAULT
                Button(
                    onClick = { uiThemeManager.setUiStyle(UiThemeStyle.DEFAULT) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (defaultSelected) CyberCyan else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        contentColor = if (defaultSelected) Color(0xFF00363D) else MaterialTheme.colorScheme.onSurface
                    ),
                    border = if (!defaultSelected) androidx.compose.foundation.BorderStroke(1.dp, DarkBorder.copy(alpha = 0.6f)) else null
                ) {
                    Icon(
                        imageVector = if (defaultSelected) Icons.Default.CheckCircle else Icons.Default.Widgets,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "i) Default",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Button 2: Glassmorphism
                val glassSelected = currentStyle == UiThemeStyle.GLASSMORPHISM
                Button(
                    onClick = { uiThemeManager.setUiStyle(UiThemeStyle.GLASSMORPHISM) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (glassSelected) ElectricViolet else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        contentColor = if (glassSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    border = if (!glassSelected) androidx.compose.foundation.BorderStroke(1.dp, DarkBorder.copy(alpha = 0.6f)) else null
                ) {
                    Icon(
                        imageVector = if (glassSelected) Icons.Default.CheckCircle else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ii) Liquid Glass",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Detailed Interactive Option Cards
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Option 1: Default UI Card
                StyleOptionTile(
                    title = "i) Default UI",
                    badge = "STANDARD",
                    badgeColor = CyberCyan,
                    description = "Original high-contrast cyber dark interface with solid cards, crisp borders, and clean typography. Fast and minimal.",
                    icon = Icons.Default.Widgets,
                    isSelected = currentStyle == UiThemeStyle.DEFAULT,
                    onSelect = { uiThemeManager.setUiStyle(UiThemeStyle.DEFAULT) }
                )

                // Option 2: Glassmorphism UI Card
                StyleOptionTile(
                    title = "ii) Modern Liquid Glass UI",
                    badge = "✨ LIQUID GLASS",
                    badgeColor = ElectricViolet,
                    description = "Ultra-modern frosted & liquid glass with backdrop blur, specular top-edge refraction, floating glass navigation, and ambient mesh glow.",
                    icon = Icons.Default.AutoAwesome,
                    isSelected = currentStyle == UiThemeStyle.GLASSMORPHISM,
                    onSelect = { uiThemeManager.setUiStyle(UiThemeStyle.GLASSMORPHISM) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Interactive Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        if (isGlassmorphism) {
                            Modifier.background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF060A14),
                                        Color(0xFF0C1326)
                                    )
                                )
                            )
                        } else {
                            Modifier.background(Color(0xFF0A0F1A))
                        }
                    )
                    .border(
                        1.dp,
                        if (isGlassmorphism) {
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.6f),
                                    CyberCyan.copy(alpha = 0.6f),
                                    ElectricViolet.copy(alpha = 0.6f),
                                    Color.White.copy(alpha = 0.2f)
                                )
                            )
                        } else {
                            SolidColor(DarkBorder.copy(alpha = 0.8f))
                        },
                        RoundedCornerShape(14.dp)
                    )
            ) {
                if (isGlassmorphism) {
                    // Mini background glow orbs inside preview box to demonstrate true glass refraction
                    androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(CyberCyan.copy(alpha = 0.45f), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.25f),
                                radius = size.width * 0.40f
                            )
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(ElectricViolet.copy(alpha = 0.45f), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.75f),
                                radius = size.width * 0.45f
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isGlassmorphism) {
                                Modifier
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.16f),
                                                Color(0xFF101B30).copy(alpha = 0.40f)
                                            )
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.8f),
                                                CyberCyan.copy(alpha = 0.5f),
                                                Color.White.copy(alpha = 0.2f)
                                            )
                                        ),
                                        RoundedCornerShape(10.dp)
                                    )
                            } else {
                                Modifier
                            }
                        )
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isGlassmorphism) "✨ Live Frosted Glass Preview" else "Live Default Preview",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isGlassmorphism) CyberCyan else CyberCyan
                            )
                            Text(
                                text = if (isGlassmorphism) "Frosted Acrylic Sheen" else "Solid & High-Contrast",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = if (isGlassmorphism) {
                                "Luminous neon orbs shine through translucent frosted cards with top specular reflection lines and light refraction."
                            } else {
                                "Solid cyber dark surfaces with clean sharp borders and instant high-contrast readability."
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
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
        targetValue = if (isSelected) badgeColor else DarkBorder.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "border"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) badgeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        animationSpec = tween(200),
        label = "bg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) badgeColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) badgeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.2f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (isSelected) "Selected" else "Unselected",
                tint = if (isSelected) badgeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
