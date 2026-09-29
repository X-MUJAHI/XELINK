package com.example.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Defines the UI Presentation style selectable by the user in Settings.
 * - DEFAULT: Standard high-contrast cyber dark interface with solid and semi-solid surfaces.
 * - GLASSMORPHISM: Legacy luminous glass presentation retained for compatibility.
 * - MODERN: New restrained frosted-glass system using Haze-backed blur, soft depth,
 *   adaptive Material 3 surfaces, and a calmer premium visual hierarchy.
 */
enum class UiThemeStyle(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String
) {
    DEFAULT(
        id = "default",
        title = "Default",
        subtitle = "Cyber Dark Solid",
        description = "High-contrast cyber dark interface with crisp solid cards and clean borders"
    ),
    GLASSMORPHISM(
        id = "glassmorphism",
        title = "Glassmorphism",
        subtitle = "Legacy Frosted Glass",
        description = "Existing luminous frosted presentation with ambient glow and refracted edge styling"
    ),
    MODERN(
        id = "modern",
        title = "Modern",
        subtitle = "Adaptive Frosted Glass",
        description = "Premium Android glass system with Haze blur, restrained lighting, floating surfaces, and smoother interaction"
    );

    companion object {
        fun fromId(id: String?): UiThemeStyle {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}

val LocalUiThemeStyle = staticCompositionLocalOf { UiThemeStyle.DEFAULT }
