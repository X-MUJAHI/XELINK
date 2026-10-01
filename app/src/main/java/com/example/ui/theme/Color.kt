/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: Color.kt
 *
 * Commentary / Architectural Overview:
 * Defines the complete Cyberpunk Dark color token system:
 * - Background: #0B0E14
 * - Surface: #151A22
 * - Card: #161D2A
 * - Elevated Card: #1E2738
 * - Border: #24324D
 * - Text Primary: #FFFFFF, Secondary: #94A3B8, Muted: #64748B
 * - Accents:
 *   - Cyan (Primary): #00E5FF
 *   - Green (Success/On): #00E676
 *   - Amber (Warning): #FFB300
 *   - Purple (Secondary): #7C4DFF
 *   - Red (Error/Destructive): #FF5252
 */

package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Exact Cyberpunk Dark Theme Tokens
val CyberBackground = Color(0xFF0B0E14)
val CyberSurface = Color(0xFF151A22)
val CyberCard = Color(0xFF161D2A)
val CyberCardElevated = Color(0xFF1E2738)
val CyberBorder = Color(0xFF24324D)
val CyberTextPrimary = Color(0xFFFFFFFF)
val CyberTextSecondary = Color(0xFF94A3B8)
val CyberTextMuted = Color(0xFF64748B)

// Accents
val CyberAccentCyan = Color(0xFF00E5FF)       // Primary
val CyberAccentGreen = Color(0xFF00E676)      // Success / On
val CyberAccentAmber = Color(0xFFFFB300)      // Warning
val CyberAccentPurple = Color(0xFF7C4DFF)     // Secondary
val CyberAccentRed = Color(0xFFFF5252)        // Error / Destructive

// Compatibility aliases
val CyberCyan = CyberAccentCyan
val CyberCyanDark = Color(0xFF00B0FF)
val ElectricViolet = CyberAccentPurple
val ElectricVioletDark = Color(0xFF651FFF)
val NeonEmerald = CyberAccentGreen
val AmberWarning = CyberAccentAmber
val CrimsonError = CyberAccentRed

val AccentAmber = CyberAccentAmber
val AccentCyan = CyberAccentCyan
val AccentGreen = CyberAccentGreen
val AccentPurple = CyberAccentPurple
val AccentRed = CyberAccentRed
val SystemBg = CyberBackground
val SystemBorder = CyberBorder
val SystemCard = CyberCard
val SystemElevated = CyberCardElevated
val SystemSurface = CyberSurface
val SystemTextMuted = CyberTextMuted
val SystemTextSecondary = CyberTextSecondary
val SystemTextWhite = CyberTextPrimary

val DarkBg = CyberBackground
val DarkSurface = CyberSurface
val DarkSurfaceElevated = CyberCardElevated
val DarkSurfaceVariant = CyberCard
val DarkBorder = CyberBorder

val TextPrimaryDark = CyberTextPrimary
val TextSecondaryDark = CyberTextSecondary
val TextMutedDark = CyberTextMuted

// Light Theme equivalents
val LightBg = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFF1F5F9)
val LightSurfaceVariant = Color(0xFFE2E8F0)
val LightBorder = Color(0xFFCBD5E1)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)

