package com.example.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Defines the UI Presentation style selectable by the user in Settings.
 * - DEFAULT: Standard high-contrast cyber dark interface with solid and semi-solid surfaces.
 * - GLASSMORPHISM: Ultra-modern frosted glass aesthetic with ambient mesh glow,
 *   translucent light-refracting cards, and luminous specular edge sheen.
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
        subtitle = "Frosted Glass & Ambient Glow",
        description = "Ultra-modern frosted glass surfaces, ambient mesh glow, iridescent specular borders & depth"
    );

    companion object {
        fun fromId(id: String?): UiThemeStyle {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}

val LocalUiThemeStyle = staticCompositionLocalOf { UiThemeStyle.DEFAULT }
