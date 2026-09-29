package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.LocalUiThemeStyle
import com.example.ui.theme.UiThemeStyle

/**
 * Shared surface primitive used by existing screens.
 * Modern mode delegates to the new Haze-backed surface system; the other two
 * presentation styles retain their existing visual behaviour.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    containerColor: Color? = null,
    borderColor: Color = DarkBorder.copy(alpha = 0.6f),
    content: @Composable BoxScope.() -> Unit
) {
    val uiStyle = LocalUiThemeStyle.current

    if (uiStyle == UiThemeStyle.MODERN) {
        ModernGlassSurface(
            modifier = modifier.fillMaxWidth(),
            level = when {
                cornerRadius >= 28.dp -> ModernGlassLevel.Elevated
                cornerRadius <= 16.dp -> ModernGlassLevel.Subtle
                else -> ModernGlassLevel.Standard
            },
            shape = RoundedCornerShape(cornerRadius),
            tint = if (borderColor == DarkBorder.copy(alpha = 0.6f)) {
                MaterialTheme.colorScheme.primary
            } else {
                borderColor
            },
            contentPadding = 16.dp,
            content = content,
        )
        return
    }

    val isGlassmorphism = uiStyle == UiThemeStyle.GLASSMORPHISM
    val resolvedContainerColor = containerColor ?: if (isGlassmorphism) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
    }

    val resolvedBorder = if (isGlassmorphism) {
        if (borderColor == DarkBorder.copy(alpha = 0.6f)) {
            BorderStroke(
                1.5.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        CyberCyan.copy(alpha = 0.65f),
                        ElectricViolet.copy(alpha = 0.50f),
                        Color.White.copy(alpha = 0.20f)
                    )
                )
            )
        } else {
            BorderStroke(
                1.5.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.90f),
                        borderColor.copy(alpha = 0.90f),
                        borderColor.copy(alpha = 0.60f),
                        Color.White.copy(alpha = 0.30f)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isGlassmorphism) {
                        Modifier.background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.14f),
                                    Color(0xFF101C33).copy(alpha = 0.45f),
                                    Color(0xFF09101F).copy(alpha = 0.62f)
                                )
                            )
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            if (isGlassmorphism) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.85f),
                                    CyberCyan.copy(alpha = 0.70f),
                                    Color.White.copy(alpha = 0.50f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(32.dp)
                        .align(Alignment.TopStart)
                        .padding(top = 4.dp, start = 4.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.60f),
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
