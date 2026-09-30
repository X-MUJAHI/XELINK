package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// System Controller Exact Theme Palette:
// background #0B0E14, surface #151A22, card #161D2A, elevated #1E2738,
// border #24324D, text #FFFFFF / #94A3B8 / #64748B,
// accents cyan #00E5FF, green #00E676, amber #FFB300, purple #7C4DFF, red #FF5252.
// =========================================================================

val SystemBg = Color(0xFF0B0E14)          // background #0B0E14
val SystemSurface = Color(0xFF151A22)     // surface #151A22
val SystemCard = Color(0xFF161D2A)        // card #161D2A
val SystemElevated = Color(0xFF1E2738)    // elevated #1E2738
val SystemBorder = Color(0xFF24324D)      // border #24324D

// Text
val SystemTextWhite = Color(0xFFFFFFFF)       // text primary #FFFFFF
val SystemTextSecondary = Color(0xFF94A3B8)   // text secondary #94A3B8
val SystemTextMuted = Color(0xFF64748B)       // text muted #64748B

// Accents
val AccentCyan = Color(0xFF00E5FF)        // cyan #00E5FF
val AccentGreen = Color(0xFF00E676)       // green #00E676
val AccentAmber = Color(0xFFFFB300)       // amber #FFB300
val AccentPurple = Color(0xFF7C4DFF)      // purple #7C4DFF
val AccentRed = Color(0xFFFF5252)         // red #FF5252

// Compatibility aliases
val CyberCyan = AccentCyan
val CyberCyanDark = Color(0xFF00B0FF)
val ElectricViolet = AccentPurple
val ElectricVioletDark = Color(0xFF651FFF)
val NeonEmerald = AccentGreen
val AmberWarning = AccentAmber
val CrimsonError = AccentRed

val DarkBg = SystemBg
val DarkSurface = SystemSurface
val DarkSurfaceElevated = SystemElevated
val DarkSurfaceVariant = SystemCard
val DarkBorder = SystemBorder

val TextPrimaryDark = SystemTextWhite
val TextSecondaryDark = SystemTextSecondary
val TextMutedDark = SystemTextMuted

// Light Theme equivalents (clean dark-optimized fallbacks)
val LightBg = Color(0xFF0B0E14)
val LightSurface = Color(0xFF151A22)
val LightSurfaceElevated = Color(0xFF1E2738)
val LightSurfaceVariant = Color(0xFF161D2A)
val LightBorder = Color(0xFF24324D)
val TextPrimaryLight = Color(0xFFFFFFFF)
val TextSecondaryLight = Color(0xFF94A3B8)
