package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.LocalUiThemeStyle
import com.example.ui.theme.UiThemeStyle

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    containerColor: Color? = null,
    borderColor: Color = DarkBorder.copy(alpha = 0.6f),
    content: @Composable BoxScope.() -> Unit
) {
    val uiStyle = LocalUiThemeStyle.current
    val isGlassmorphism = uiStyle == UiThemeStyle.GLASSMORPHISM

    // In Glassmorphism mode: translucent frosted acrylic; In Default mode: solid/semi-solid cyber surface
    val resolvedContainerColor = containerColor ?: if (isGlassmorphism) {
        Color(0xFF0F1A2F).copy(alpha = 0.58f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    }

    val resolvedBorder = if (isGlassmorphism) {
        if (borderColor == DarkBorder.copy(alpha = 0.6f)) {
            // Luminous iridescent glass border
            BorderStroke(
                1.2.dp,
                Brush.linearGradient(
                    colors = listOf(
                        CyberCyan.copy(alpha = 0.65f),
                        Color.White.copy(alpha = 0.35f),
                        ElectricViolet.copy(alpha = 0.55f),
                        Color(0xFF38BDF8).copy(alpha = 0.35f)
                    )
                )
            )
        } else {
            // Specular border reflecting caller's specific accent color (e.g. CrimsonError, NeonEmerald)
            BorderStroke(
                1.2.dp,
                Brush.linearGradient(
                    colors = listOf(
                        borderColor.copy(alpha = 0.85f),
                        Color.White.copy(alpha = 0.40f),
                        borderColor.copy(alpha = 0.55f)
                    )
                )
            )
        }
    } else {
        BorderStroke(1.dp, borderColor)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = resolvedContainerColor),
        border = resolvedBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isGlassmorphism) 6.dp else 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Specular top highlight line in Glassmorphism mode (simulates physical light reflection on glass edge)
            if (isGlassmorphism) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.45f),
                                    CyberCyan.copy(alpha = 0.55f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Box(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}
