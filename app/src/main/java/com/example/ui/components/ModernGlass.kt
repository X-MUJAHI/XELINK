package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import com.example.ui.theme.LocalUiThemeStyle
import com.example.ui.theme.UiThemeStyle

val LocalModernHazeState = staticCompositionLocalOf<HazeState?> { null }

enum class ModernGlassLevel(
    val fillAlpha: Float,
    val tintAlpha: Float,
    val borderAlpha: Float,
    val blurRadius: Dp,
    val shadowElevation: Dp,
    val cornerRadius: Dp,
) {
    Subtle(0.09f, 0.045f, 0.18f, 9.dp, 3.dp, 16.dp),
    Standard(0.15f, 0.065f, 0.24f, 15.dp, 6.dp, 22.dp),
    Elevated(0.22f, 0.085f, 0.32f, 22.dp, 10.dp, 30.dp),
}

@Composable
fun ModernGlassSurface(
    modifier: Modifier = Modifier,
    level: ModernGlassLevel = ModernGlassLevel.Standard,
    shape: Shape = RoundedCornerShape(level.cornerRadius),
    tint: Color = Color.White,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val uiStyle = LocalUiThemeStyle.current
    if (uiStyle != UiThemeStyle.MODERN) {
        Box(modifier = modifier, content = content)
        return
    }

    val hazeState = LocalModernHazeState.current
    val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = level.fillAlpha)
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = level.borderAlpha + 0.10f),
            tint.copy(alpha = level.borderAlpha * 0.75f),
            Color.White.copy(alpha = level.borderAlpha * 0.45f),
        )
    )

    Box(
        modifier = modifier
            .shadow(level.shadowElevation, shape, clip = false)
            .clip(shape)
            .then(
                if (hazeState != null) {
                    Modifier.hazeBlur(
                        input = HazeInput.Sources(hazeState),
                        style = HazeBlurStyle {
                            blurEnabled(true)
                            blurRadius(level.blurRadius)
                            colorEffects(
                                listOf(
                                    HazeColorEffect.tint(
                                        tint.copy(alpha = level.tintAlpha)
                                    )
                                )
                            )
                        },
                        performanceMode = HazePerformanceMode.Adaptive,
                        expandLayerBounds = false,
                    )
                } else {
                    Modifier
                }
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = level.fillAlpha + 0.045f),
                        surfaceColor,
                        MaterialTheme.colorScheme.surface.copy(alpha = level.fillAlpha * 0.82f),
                    )
                )
            )
            .border(BorderStroke(1.dp, borderBrush), shape),
    ) {
        // Soft directional lighting, deliberately restrained so the material reads as frosted rather than glossy.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.075f),
                            Color.Transparent,
                            tint.copy(alpha = 0.025f),
                        )
                    )
                )
        )

        Box(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun ModernGlassCard(
    modifier: Modifier = Modifier,
    level: ModernGlassLevel = ModernGlassLevel.Standard,
    tint: Color = MaterialTheme.colorScheme.primary,
    content: @Composable BoxScope.() -> Unit,
) = ModernGlassSurface(
    modifier = modifier.fillMaxWidth(),
    level = level,
    shape = RoundedCornerShape(level.cornerRadius),
    tint = tint,
    content = content,
)

@Composable
fun ModernGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable (() -> Unit))? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(stiffness = 900f),
        label = "modern_button_scale",
    )

    ModernGlassSurface(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        level = ModernGlassLevel.Standard,
        tint = MaterialTheme.colorScheme.primary,
        contentPadding = 0.dp,
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            interactionSource = interactionSource,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            ),
            contentPadding = ButtonDefaults.ContentPadding,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leadingIcon?.invoke()
                Text(text = text, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun ModernGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    icon: @Composable () -> Unit,
) {
    ModernGlassSurface(
        modifier = modifier
            .size(48.dp)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            }
            .clickable(role = Role.Button, onClick = onClick),
        level = ModernGlassLevel.Subtle,
        shape = CircleShape,
        tint = MaterialTheme.colorScheme.primary,
        contentPadding = 0.dp,
    ) {
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
            icon()
        }
    }
}

@Composable
fun ModernGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
) {
    ModernGlassSurface(
        modifier = modifier,
        level = ModernGlassLevel.Subtle,
        shape = RoundedCornerShape(18.dp),
        tint = MaterialTheme.colorScheme.primary,
        contentPadding = 0.dp,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            label = label?.let { { Text(it) } },
            placeholder = placeholder?.let { { Text(it) } },
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f),
            ),
        )
    }
}

@Composable
fun ModernGlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    ModernGlassSurface(
        modifier = modifier.fillMaxWidth(),
        level = ModernGlassLevel.Subtle,
        shape = RoundedCornerShape(0.dp),
        contentPadding = 8.dp,
        tint = MaterialTheme.colorScheme.primary,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            navigationIcon?.invoke()
            ColumnScopeTitle(title = title, subtitle = subtitle, modifier = Modifier.weight(1f))
            actions()
        }
    }
}

@Composable
private fun ColumnScopeTitle(
    title: String,
    subtitle: String?,
    modifier: Modifier,
) {
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ModernGlassSection(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        content()
    }
}

@Composable
fun ModernGlassFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Action",
    content: @Composable () -> Unit,
) {
    ModernGlassSurface(
        modifier = modifier
            .size(56.dp)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            }
            .clickable(role = Role.Button, onClick = onClick),
        level = ModernGlassLevel.Elevated,
        shape = CircleShape,
        tint = MaterialTheme.colorScheme.primary,
        contentPadding = 0.dp,
    ) {
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
fun ModernGlassDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!visible) return
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        ModernGlassSurface(
            modifier = modifier.fillMaxWidth(),
            level = ModernGlassLevel.Elevated,
            shape = RoundedCornerShape(32.dp),
            tint = MaterialTheme.colorScheme.primary,
            contentPadding = 22.dp,
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernGlassBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        dragHandle = null,
    ) {
        ModernGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            level = ModernGlassLevel.Elevated,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            tint = MaterialTheme.colorScheme.primary,
            contentPadding = 20.dp,
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxWidth(),
                content = content,
            )
        }
    }
}

@Composable
fun ModernGlassChip(
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    ModernGlassSurface(
        modifier = modifier,
        level = ModernGlassLevel.Subtle,
        shape = RoundedCornerShape(50.dp),
        tint = tint,
        contentPadding = 0.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
        )
    }
}

