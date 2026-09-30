package com.example.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeChildScope
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

/**
 * CompositionLocal for HazeState, allowing nested glass components to sample the blurred backdrop.
 */
val LocalHazeState = compositionLocalOf<HazeState?> { null }

/**
 * Glass Tokens: Centralized configuration for the entire Liquid / Frosted Glass design system.
 */
object GlassTokens {
    // Transparency Levels (Alphas)
    val AlphaSubtle = 0.10f
    val AlphaNormal = 0.18f
    val AlphaStrong = 0.28f
    val AlphaModal = 0.38f

    // Blur Radii
    val BlurSubtle = 12.dp
    val BlurNormal = 20.dp
    val BlurElevated = 28.dp
    val BlurModal = 36.dp

    // Corner Radii
    val RadiusSmall = 16.dp
    val RadiusButton = 18.dp
    val RadiusCard = 24.dp
    val RadiusPanel = 28.dp
    val RadiusSheet = 32.dp
    val RadiusCircular = 999.dp

    // Borders
    val BorderSubtle = 0.8.dp
    val BorderStandard = 1.2.dp
    val BorderProminent = 1.5.dp

    // Spacing Rhythm (4dp/8dp)
    val SpacingTiny = 4.dp
    val SpacingSmall = 8.dp
    val SpacingMedium = 12.dp
    val SpacingStandard = 16.dp
    val SpacingLarge = 20.dp
    val SpacingXLarge = 24.dp
    val SpacingHuge = 32.dp
}

enum class GlassLevel {
    LEVEL_1_SUBTLE,    // Small controls, chips, secondary items
    LEVEL_2_STANDARD,  // Cards, standard buttons, navigation bars
    LEVEL_3_ELEVATED   // Dialogs, bottom sheets, floating active panels
}

@Immutable
data class GlassColors(
    val containerTint: Color,
    val surfaceGradientTop: Color,
    val surfaceGradientBottom: Color,
    val borderGradientStart: Color,
    val borderGradientMiddle: Color,
    val borderGradientEnd: Color,
    val specularTopStart: Color,
    val specularTopEnd: Color,
    val shadowColor: Color
)

@Composable
fun rememberGlassColors(
    level: GlassLevel = GlassLevel.LEVEL_2_STANDARD,
    tint: Color? = null,
    isDark: Boolean = isSystemInDarkTheme()
): GlassColors {
    return remember(level, tint, isDark) {
        if (isDark) {
            when (level) {
                GlassLevel.LEVEL_1_SUBTLE -> GlassColors(
                    containerTint = tint?.copy(alpha = 0.08f) ?: Color(0xFF10192A).copy(alpha = 0.25f),
                    surfaceGradientTop = Color.White.copy(alpha = 0.10f),
                    surfaceGradientBottom = Color(0xFF090E1A).copy(alpha = 0.35f),
                    borderGradientStart = Color.White.copy(alpha = 0.40f),
                    borderGradientMiddle = tint?.copy(alpha = 0.30f) ?: Color.White.copy(alpha = 0.15f),
                    borderGradientEnd = Color.White.copy(alpha = 0.08f),
                    specularTopStart = Color.White.copy(alpha = 0.50f),
                    specularTopEnd = Color.Transparent,
                    shadowColor = Color.Black.copy(alpha = 0.20f)
                )
                GlassLevel.LEVEL_2_STANDARD -> GlassColors(
                    containerTint = tint?.copy(alpha = 0.14f) ?: Color(0xFF0E1728).copy(alpha = 0.42f),
                    surfaceGradientTop = Color.White.copy(alpha = 0.15f),
                    surfaceGradientBottom = Color(0xFF070C18).copy(alpha = 0.58f),
                    borderGradientStart = Color.White.copy(alpha = 0.70f),
                    borderGradientMiddle = tint?.copy(alpha = 0.45f) ?: Color(0xFF38BDF8).copy(alpha = 0.30f),
                    borderGradientEnd = Color.White.copy(alpha = 0.12f),
                    specularTopStart = Color.White.copy(alpha = 0.75f),
                    specularTopEnd = Color.Transparent,
                    shadowColor = Color.Black.copy(alpha = 0.35f)
                )
                GlassLevel.LEVEL_3_ELEVATED -> GlassColors(
                    containerTint = tint?.copy(alpha = 0.22f) ?: Color(0xFF111C33).copy(alpha = 0.65f),
                    surfaceGradientTop = Color.White.copy(alpha = 0.20f),
                    surfaceGradientBottom = Color(0xFF060912).copy(alpha = 0.75f),
                    borderGradientStart = Color.White.copy(alpha = 0.85f),
                    borderGradientMiddle = tint?.copy(alpha = 0.60f) ?: Color(0xFF818CF8).copy(alpha = 0.50f),
                    borderGradientEnd = Color.White.copy(alpha = 0.20f),
                    specularTopStart = Color.White.copy(alpha = 0.90f),
                    specularTopEnd = Color.Transparent,
                    shadowColor = Color.Black.copy(alpha = 0.50f)
                )
            }
        } else {
            // Light Theme Glass
            when (level) {
                GlassLevel.LEVEL_1_SUBTLE -> GlassColors(
                    containerTint = tint?.copy(alpha = 0.08f) ?: Color.White.copy(alpha = 0.45f),
                    surfaceGradientTop = Color.White.copy(alpha = 0.70f),
                    surfaceGradientBottom = Color(0xFFF1F5F9).copy(alpha = 0.40f),
                    borderGradientStart = Color.White.copy(alpha = 0.80f),
                    borderGradientMiddle = Color(0xFFCBD5E1).copy(alpha = 0.40f),
                    borderGradientEnd = Color.White.copy(alpha = 0.30f),
                    specularTopStart = Color.White.copy(alpha = 0.90f),
                    specularTopEnd = Color.Transparent,
                    shadowColor = Color(0x1A0F172A)
                )
                GlassLevel.LEVEL_2_STANDARD -> GlassColors(
                    containerTint = tint?.copy(alpha = 0.12f) ?: Color.White.copy(alpha = 0.65f),
                    surfaceGradientTop = Color.White.copy(alpha = 0.85f),
                    surfaceGradientBottom = Color(0xFFE2E8F0).copy(alpha = 0.55f),
                    borderGradientStart = Color.White,
                    borderGradientMiddle = Color(0xFF94A3B8).copy(alpha = 0.50f),
                    borderGradientEnd = Color.White.copy(alpha = 0.40f),
                    specularTopStart = Color.White,
                    specularTopEnd = Color.Transparent,
                    shadowColor = Color(0x2E0F172A)
                )
                GlassLevel.LEVEL_3_ELEVATED -> GlassColors(
                    containerTint = tint?.copy(alpha = 0.18f) ?: Color.White.copy(alpha = 0.82f),
                    surfaceGradientTop = Color.White,
                    surfaceGradientBottom = Color(0xFFCBD5E1).copy(alpha = 0.70f),
                    borderGradientStart = Color.White,
                    borderGradientMiddle = tint?.copy(alpha = 0.40f) ?: Color(0xFF64748B).copy(alpha = 0.40f),
                    borderGradientEnd = Color.White.copy(alpha = 0.50f),
                    specularTopStart = Color.White,
                    specularTopEnd = Color.Transparent,
                    shadowColor = Color(0x450F172A)
                )
            }
        }
    }
}
