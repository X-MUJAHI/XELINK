package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF9CF1FF),
    secondary = ElectricViolet,
    onSecondary = Color(0xFF280068),
    secondaryContainer = Color(0xFF3F009C),
    onSecondaryContainer = Color(0xFFE4D7FF),
    tertiary = NeonEmerald,
    onTertiary = Color(0xFF003919),
    tertiaryContainer = Color(0xFF005327),
    onTertiaryContainer = Color(0xFF72FBA0),
    background = DarkBg,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkBorder,
    error = CrimsonError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = CyberCyanDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBAF0F8),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = ElectricVioletDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6DEFF),
    onSecondaryContainer = Color(0xFF21005E),
    tertiary = Color(0xFF008947),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD5F6E3),
    onTertiaryContainer = Color(0xFF002112),
    background = LightBg,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightBorder,
    error = CrimsonError,
    onError = Color.White
)

private val ModernDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF002C33),
    primaryContainer = CyberCyan.copy(alpha = 0.18f),
    onPrimaryContainer = Color(0xFFB9F7FF),
    secondary = Color(0xFFC9B8FF),
    onSecondary = Color(0xFF24124C),
    secondaryContainer = ElectricViolet.copy(alpha = 0.18f),
    onSecondaryContainer = Color(0xFFEBDDFF),
    tertiary = Color(0xFF8BE8B4),
    onTertiary = Color(0xFF07351B),
    tertiaryContainer = NeonEmerald.copy(alpha = 0.16f),
    onTertiaryContainer = Color(0xFFC5F8D7),
    background = Color(0xFF09111D),
    onBackground = Color(0xFFF2F6FB),
    surface = Color.White.copy(alpha = 0.13f),
    onSurface = Color(0xFFF4F7FB),
    surfaceVariant = Color.White.copy(alpha = 0.085f),
    onSurfaceVariant = Color(0xFFB7C2D0),
    outline = Color.White.copy(alpha = 0.24f),
    error = Color(0xFFFF8A84),
    onError = Color(0xFF3F0806)
)

private val ModernLightColorScheme = lightColorScheme(
    primary = Color(0xFF007B8A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBDEDF2).copy(alpha = 0.82f),
    onPrimaryContainer = Color(0xFF002A31),
    secondary = Color(0xFF6650A8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7DFFF).copy(alpha = 0.84f),
    onSecondaryContainer = Color(0xFF25154C),
    tertiary = Color(0xFF147A49),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD4F2E1).copy(alpha = 0.84f),
    onTertiaryContainer = Color(0xFF062617),
    background = Color(0xFFEAF1F7),
    onBackground = Color(0xFF17212B),
    surface = Color.White.copy(alpha = 0.74f),
    onSurface = Color(0xFF18212B),
    surfaceVariant = Color.White.copy(alpha = 0.58f),
    onSurfaceVariant = Color(0xFF526171),
    outline = Color(0xFF758595).copy(alpha = 0.48f),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val uiStyle = LocalUiThemeStyle.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        uiStyle == UiThemeStyle.MODERN && darkTheme -> ModernDarkColorScheme
        uiStyle == UiThemeStyle.MODERN -> ModernLightColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
