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

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Default to our custom cyber dark/light palette for consistent branding
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
