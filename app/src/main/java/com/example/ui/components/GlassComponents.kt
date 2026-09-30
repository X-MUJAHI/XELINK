package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassColors
import com.example.ui.theme.GlassLevel
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.LocalHazeState
import com.example.ui.theme.rememberGlassColors
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

/**
 * Fundamental GlassSurface: Combines Haze backdrop blur, translucent gradient,
 * specular edge reflection highlight, and soft floating shadow.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(GlassTokens.RadiusCard),
    level: GlassLevel = GlassLevel.LEVEL_2_STANDARD,
    tint: Color? = null,
    borderWidth: Dp = GlassTokens.BorderStandard,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val hazeState = LocalHazeState.current
    val colors = rememberGlassColors(level = level, tint = tint)
    val blurRadius = when (level) {
        GlassLevel.LEVEL_1_SUBTLE -> GlassTokens.BlurSubtle
        GlassLevel.LEVEL_2_STANDARD -> GlassTokens.BlurNormal
        GlassLevel.LEVEL_3_ELEVATED -> GlassTokens.BlurElevated
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = when (level) {
                    GlassLevel.LEVEL_1_SUBTLE -> 2.dp
                    GlassLevel.LEVEL_2_STANDARD -> 6.dp
                    GlassLevel.LEVEL_3_ELEVATED -> 12.dp
                },
                shape = shape,
                ambientColor = colors.shadowColor,
                spotColor = colors.shadowColor
            )
            .clip(shape)
            .then(
                if (hazeState != null) {
                    Modifier.hazeChild(
                        state = hazeState,
                        style = HazeStyle(
                            backgroundColor = colors.containerTint,
                            tints = listOf(HazeTint(colors.containerTint)),
                            blurRadius = blurRadius
                        )
                    )
                } else {
                    Modifier
                }
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colors.surfaceGradientTop,
                        colors.surfaceGradientBottom
                    )
                )
            )
            .border(
                BorderStroke(
                    width = borderWidth,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            colors.borderGradientStart,
                            colors.borderGradientMiddle,
                            colors.borderGradientEnd
                        )
                    )
                ),
                shape = shape
            )
    ) {
        // Specular top highlight reflection line (simulating light striking cut glass edge)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            colors.specularTopStart,
                            colors.borderGradientMiddle,
                            colors.specularTopEnd
                        )
                    )
                )
        )

        Box(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * GlassContainer: General-purpose flex container with Level 1/2/3 configuration.
 */
@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(GlassTokens.RadiusCard),
    level: GlassLevel = GlassLevel.LEVEL_2_STANDARD,
    tint: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier,
        shape = shape,
        level = level,
        tint = tint,
        contentPadding = contentPadding,
        content = content
    )
}

/**
 * GlassSection: Reusable section wrapper with header and glass container body.
 */
@Composable
fun GlassSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GlassTokens.SpacingSmall)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (action != null) {
                action()
            }
        }

        GlassContainer(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(14.dp)
        ) {
            content()
        }
    }
}

/**
 * GlassButton: Liquid/frosted glass button with smooth spring press scale animation.
 */
enum class GlassButtonVariant {
    PRIMARY,    // CyberCyan / ElectricViolet subtle tinted glass
    SECONDARY,  // Neutral frosted glass
    DESTRUCTIVE // Subtle crimson red glass
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: GlassButtonVariant = GlassButtonVariant.PRIMARY,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(GlassTokens.RadiusButton),
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.75f),
        label = "press_scale"
    )

    val tint = when (variant) {
        GlassButtonVariant.PRIMARY -> CyberCyan
        GlassButtonVariant.SECONDARY -> null
        GlassButtonVariant.DESTRUCTIVE -> Color(0xFFFF5252)
    }

    val contentColor = when (variant) {
        GlassButtonVariant.PRIMARY -> if (isSystemInDarkTheme()) CyberCyan else Color(0xFF005662)
        GlassButtonVariant.SECONDARY -> MaterialTheme.colorScheme.onSurface
        GlassButtonVariant.DESTRUCTIVE -> Color(0xFFFF5252)
    }

    val alpha = if (enabled) 1f else 0.45f

    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
    ) {
        GlassSurface(
            shape = shape,
            level = if (variant == GlassButtonVariant.PRIMARY) GlassLevel.LEVEL_2_STANDARD else GlassLevel.LEVEL_1_SUBTLE,
            tint = tint,
            contentPadding = contentPadding
        ) {
            CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides contentColor.copy(alpha = alpha)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}

/**
 * GlassIconButton: Circular or rounded glass icon control with smooth feedback.
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String?,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    shape: Shape = CircleShape
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = 600f, dampingRatio = 0.7f),
        label = "icon_press_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            shape = shape,
            level = GlassLevel.LEVEL_1_SUBTLE,
            tint = tint,
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier.size(size),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = if (enabled) tint else tint.copy(alpha = 0.4f),
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

/**
 * GlassFloatingActionButton: Circular glass FAB with high blur and floating luminous edge.
 */
@Composable
fun GlassFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String?,
    tint: Color = CyberCyan
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.75f),
        label = "fab_press_scale"
    )

    Box(
        modifier = modifier
            .size(58.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            shape = CircleShape,
            level = GlassLevel.LEVEL_3_ELEVATED,
            tint = tint,
            borderWidth = GlassTokens.BorderProminent,
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier.size(58.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

/**
 * GlassTextField: Liquid glass input field with animated focus glow and clean placeholder.
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "",
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val focusBorderColor by animateColorAsState(
        targetValue = if (isFocused) CyberCyan.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.25f),
        animationSpec = tween(200),
        label = "input_border"
    )

    val hazeState = LocalHazeState.current
    val isDark = isSystemInDarkTheme()
    val containerBg = if (isDark) {
        if (isFocused) Color(0xFF142036).copy(alpha = 0.65f) else Color(0xFF0C1322).copy(alpha = 0.45f)
    } else {
        if (isFocused) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.60f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassTokens.RadiusButton))
            .then(
                if (hazeState != null) {
                    Modifier.hazeChild(
                        state = hazeState,
                        style = HazeStyle(
                            backgroundColor = containerBg,
                            tints = listOf(HazeTint(containerBg)),
                            blurRadius = GlassTokens.BlurNormal
                        )
                    )
                } else {
                    Modifier
                }
            )
            .background(containerBg)
            .border(
                BorderStroke(
                    if (isFocused) 1.5.dp else 1.dp,
                    if (isFocused) Brush.horizontalGradient(listOf(CyberCyan, ElectricViolet)) else SolidColor(focusBorderColor)
                ),
                shape = RoundedCornerShape(GlassTokens.RadiusButton)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(10.dp))
            }

            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholderText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = singleLine,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    interactionSource = interactionSource,
                    enabled = enabled,
                    cursorBrush = SolidColor(CyberCyan)
                )
            }

            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(10.dp))
                trailingIcon()
            }
        }
    }
}

/**
 * GlassChip: Filter/status pill in subtle frosted glass with animated selection state.
 */
@Composable
fun GlassChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = CyberCyan
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = 600f, dampingRatio = 0.7f),
        label = "chip_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
    ) {
        GlassSurface(
            shape = RoundedCornerShape(GlassTokens.RadiusSmall),
            level = if (selected) GlassLevel.LEVEL_2_STANDARD else GlassLevel.LEVEL_1_SUBTLE,
            tint = if (selected) accentColor else null,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) {
                        if (isSystemInDarkTheme()) Color.White else accentColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

/**
 * GlassTopBar: Floating translucent top bar with backdrop blur and subtle lower boundary.
 */
@Composable
fun GlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = GlassTokens.RadiusCard, bottomEnd = GlassTokens.RadiusCard),
        level = GlassLevel.LEVEL_2_STANDARD,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (navigationIcon != null) {
                    navigationIcon()
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (actions != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    actions()
                }
            }
        }
    }
}

/**
 * GlassDialog: Modal glass dialog with Level 3 elevated frosted glass, high blur, and specular rim.
 */
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            GlassSurface(
                shape = RoundedCornerShape(GlassTokens.RadiusSheet),
                level = GlassLevel.LEVEL_3_ELEVATED,
                borderWidth = GlassTokens.BorderProminent,
                contentPadding = PaddingValues(24.dp),
                modifier = modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        GlassIconButton(
                            onClick = onDismissRequest,
                            icon = Icons.Default.Close,
                            contentDescription = "Close",
                            size = 32.dp,
                            iconSize = 16.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    content()

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dismissButton != null) {
                            dismissButton()
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        confirmButton()
                    }
                }
            }
        }
    }
}
