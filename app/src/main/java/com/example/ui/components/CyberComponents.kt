package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CyberAccentAmber
import com.example.ui.theme.CyberAccentCyan
import com.example.ui.theme.CyberAccentGreen
import com.example.ui.theme.CyberAccentPurple
import com.example.ui.theme.CyberAccentRed
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

/**
 * CyberCard: 14-16dp rounded corners, 1dp border, card-color fill (#161D2A),
 * 16dp padding, consistent 12dp spacing.
 */
@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderColor: Color = CyberBorder,
    borderWidth: Dp = 1.dp,
    containerColor: Color = CyberCard,
    padding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val clickModifier = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else Modifier

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(clickModifier),
        shape = shape,
        color = containerColor,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            content = content
        )
    }
}

/**
 * Section header: small, bold, uppercase, letter-spaced, in muted or accent color.
 */
@Composable
fun CyberSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false,
    color: Color = if (isAccent) CyberAccentCyan else CyberTextMuted
) {
    Text(
        text = text.uppercase(),
        style = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp,
            color = color,
            fontFamily = FontFamily.SansSerif
        ),
        modifier = modifier.padding(vertical = 4.dp)
    )
}

/**
 * Primary button: full width, ~52dp tall, 12dp radius, accent fill with dark text.
 * Button text is bold and uppercase.
 */
@Composable
fun CyberPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    accentColor: Color = CyberAccentCyan,
    textColor: Color = CyberBackground
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "CyberButtonScale"
    )

    val shape = RoundedCornerShape(12.dp)
    val bgColor = if (enabled) accentColor else accentColor.copy(alpha = 0.35f)
    val finalTextColor = if (enabled) textColor else textColor.copy(alpha = 0.6f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale)
            .clip(shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = shape,
        color = bgColor
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = finalTextColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = finalTextColor,
                    fontFamily = FontFamily.SansSerif
                )
            )
        }
    }
}

/**
 * Secondary button: outlined with an accent border and accent text, uppercase, ~52dp tall.
 */
@Composable
fun CyberSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    accentColor: Color = CyberAccentCyan,
    height: Dp = 52.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "CyberSecondaryScale"
    )

    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (enabled) accentColor else accentColor.copy(alpha = 0.3f)
    val textColor = if (enabled) accentColor else accentColor.copy(alpha = 0.4f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .scale(scale)
            .clip(shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = shape,
        color = CyberSurface,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = textColor,
                    fontFamily = FontFamily.SansSerif
                )
            )
        }
    }
}

/**
 * Destructive button: uses red (#FF5252), bold uppercase.
 */
@Composable
fun CyberDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    isOutlined: Boolean = false
) {
    if (isOutlined) {
        CyberSecondaryButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            leadingIcon = leadingIcon,
            accentColor = CyberAccentRed
        )
    } else {
        CyberPrimaryButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            leadingIcon = leadingIcon,
            accentColor = CyberAccentRed,
            textColor = Color.White
        )
    }
}

/**
 * Stat boxes: a row of equal-width small tiles, each with a tiny muted label
 * above a large bold value, on an elevated fill (#1E2738) with a border (#24324D).
 */
data class CyberStatItem(
    val label: String,
    val value: String,
    val valueColor: Color = CyberTextPrimary,
    val subtext: String? = null
)

@Composable
fun CyberStatBoxes(
    stats: List<CyberStatItem>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        stats.forEach { stat ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = CyberCardElevated,
                border = BorderStroke(1.dp, CyberBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = stat.label.uppercase(),
                        style = TextStyle(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = CyberTextMuted
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stat.value,
                        style = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = stat.valueColor
                        ),
                        maxLines = 1
                    )
                    if (stat.subtext != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stat.subtext,
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = CyberTextSecondary
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Status indicator: a rounded card with a colored dot or badge (green, amber or red)
 * and a short status line.
 */
@Composable
fun CyberStatusIndicator(
    statusText: String,
    subText: String? = null,
    statusColor: Color = CyberAccentGreen,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = CyberCard,
        border = BorderStroke(1.dp, CyberBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Colored status dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = statusText,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberTextPrimary
                        )
                    )
                    if (subText != null) {
                        Text(
                            text = subText,
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = CyberTextSecondary
                            )
                        )
                    }
                }
            }
            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingContent()
            }
        }
    }
}

/**
 * Input: dark fill, 1dp border that turns cyan on focus, muted hint text.
 */
@Composable
fun CyberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor = if (isFocused) CyberAccentCyan else CyberBorder
    val shape = RoundedCornerShape(12.dp)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(CyberSurface)
            .border(BorderStroke(1.dp, borderColor), shape),
        interactionSource = interactionSource,
        singleLine = singleLine,
        textStyle = TextStyle(
            color = CyberTextPrimary,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif
        ),
        cursorBrush = SolidColor(CyberAccentCyan),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = if (isFocused) CyberAccentCyan else CyberTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            style = TextStyle(
                                color = CyberTextMuted,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        )
                    }
                    innerTextField()
                }
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailingIcon()
                }
            }
        }
    )
}

/**
 * Toggles and switches: accent-colored when on (#00E5FF), muted gray when off.
 */
@Composable
fun CyberSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = CyberAccentCyan
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = CyberBackground,
            checkedTrackColor = accentColor,
            uncheckedThumbColor = CyberTextMuted,
            uncheckedTrackColor = CyberCardElevated,
            uncheckedBorderColor = CyberBorder,
            checkedBorderColor = accentColor
        )
    )
}

/**
 * List rows: card-style items with a leading icon, primary and secondary text lines,
 * and a trailing action or badge.
 */
@Composable
fun CyberListRow(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    iconTint: Color = CyberAccentCyan,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val clickMod = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else Modifier

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(clickMod),
        shape = RoundedCornerShape(14.dp),
        color = CyberCard,
        border = BorderStroke(1.dp, CyberBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberCardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column {
                    Text(
                        text = title,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyberTextPrimary
                        )
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = CyberTextSecondary
                            )
                        )
                    }
                }
            }
            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingContent()
            }
        }
    }
}

/**
 * Badges: small pill shapes with a tinted background.
 */
@Composable
fun CyberBadge(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = CyberAccentCyan,
    textColor: Color = tint
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(50.dp)),
        shape = RoundedCornerShape(50.dp),
        color = tint.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.45f))
    ) {
        Text(
            text = text.uppercase(),
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = textColor,
                fontFamily = FontFamily.SansSerif
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/**
 * Dialog: rounded, dark card background, accent border, centered loading spinner or message.
 */
@Composable
fun CyberDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String? = null,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable ColumnScope.() -> Unit
) {
    if (!visible) return

    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = CyberCard,
            border = BorderStroke(1.dp, CyberAccentCyan.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                content = {
                    if (title != null) {
                        Text(
                            text = title,
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberTextPrimary,
                                letterSpacing = 0.6.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    content()
                }
            )
        }
    }
}

/**
 * Two small square icon buttons floating at the top corners (menu on the left, settings on the right),
 * with rounded borders and accent-colored icons.
 */
@Composable
fun CyberCornerIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    iconTint: Color = CyberAccentCyan
) {
    Surface(
        modifier = modifier
            .size(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                this.role = Role.Button
            },
        shape = RoundedCornerShape(10.dp),
        color = CyberSurface,
        border = BorderStroke(1.dp, CyberBorder)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Floating pill-shaped bottom bar with 3-4 equal-width tabs.
 * The active tab has a slightly lighter filled background and accent-colored text.
 * Inactive tabs are transparent with muted text. Labels are short, bold, uppercase, 12sp.
 */
data class CyberTabItem(
    val id: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun CyberFloatingBottomBar(
    tabs: List<CyberTabItem>,
    selectedTabId: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        color = CyberCard,
        border = BorderStroke(1.dp, CyberBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = tab.id == selectedTabId
                val activeBg = if (isSelected) CyberCardElevated else Color.Transparent
                val activeText = if (isSelected) CyberAccentCyan else CyberTextMuted
                val activeBorder = if (isSelected) BorderStroke(1.dp, CyberBorder) else null

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .clickable { onTabSelected(tab.id) },
                    shape = RoundedCornerShape(26.dp),
                    color = activeBg,
                    border = activeBorder
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = activeText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.label.uppercase(),
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeText,
                                fontFamily = FontFamily.SansSerif,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
