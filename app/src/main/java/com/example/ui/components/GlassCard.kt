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
    cornerRadius: Dp = 18.dp,
    containerColor: Color? = null,
    borderColor: Color = DarkBorder.copy(alpha = 0.6f),
    content: @Composable BoxScope.() -> Unit
) {
    val uiStyle = LocalUiThemeStyle.current
    val isGlassmorphism = uiStyle == UiThemeStyle.GLASSMORPHISM

    // In Glassmorphism mode: true frosted glass with high translucency and white-tinted acrylic frost
    // In Default mode: solid/semi-solid high-contrast cyber dark surface
    val resolvedContainerColor = containerColor ?: if (isGlassmorphism) {
        Color.Transparent // Background is rendered via frosted gradient inside Box to allow pure transparency
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
    }

    val resolvedBorder = if (isGlassmorphism) {
        if (borderColor == DarkBorder.copy(alpha = 0.6f)) {
            // Luminous frosted glass edge: bright top-left specular white highlight catching ambient light,
            // blending into cyber cyan & violet neon refraction, fading to soft translucent white at bottom
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
            // Specular border reflecting caller's specific accent color (e.g. CrimsonError, NeonEmerald)
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
        colors = CardDefaults.cardColors(
            containerColor = resolvedContainerColor
        ),
        border = resolvedBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Avoid M3 dark tonal elevation greyout
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
                // 1. Top specular reflection line (physical light refraction across top cut edge of glass)
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

                // 2. Subtle diagonal specular gloss highlight band
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
