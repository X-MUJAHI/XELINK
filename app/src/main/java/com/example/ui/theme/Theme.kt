package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SystemDarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = SystemBg,
    primaryContainer = SystemElevated,
    onPrimaryContainer = AccentCyan,
    secondary = AccentPurple,
    onSecondary = SystemTextWhite,
    secondaryContainer = SystemElevated,
    onSecondaryContainer = AccentPurple,
    tertiary = AccentGreen,
    onTertiary = SystemBg,
    tertiaryContainer = SystemElevated,
    onTertiaryContainer = AccentGreen,
    background = SystemBg,
    onBackground = SystemTextWhite,
    surface = SystemSurface,
    onSurface = SystemTextWhite,
    surfaceVariant = SystemCard,
    onSurfaceVariant = SystemTextSecondary,
    outline = SystemBorder,
    error = AccentRed,
    onError = SystemTextWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // System Controller is dark theme by default
    content: @Composable () -> Unit
) {
    val colorScheme = SystemDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = SystemBg.toArgb()
                window.navigationBarColor = SystemBg.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
