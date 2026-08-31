package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ===== YAML minimal design - version alpha =====
// Light mode
val Primary = Color(0xFFFFFFFF) // #ffffff
val PrimaryHover = Color(0xFFF5F5F5) // #f5f5f5
val OnPrimary = Color(0xFF1A1A1A) // #1a1a1a
val Background = Color(0xFFFAFAFA) // #fafafa
val Surface = Color(0xFFFFFFFF) // #ffffff
val Border = Color(0xFFE5E5E5) // #e5e5e5
val Text = Color(0xFF1A1A1A) // #1a1a1a
val TextMuted = Color(0xFF666666) // #666666
val Accent = Color(0xFF0066CC) // #0066cc
val AccentHover = Color(0xFF0052A3)
val AccentPressed = Color(0xFF003D7A)
val Success = Color(0xFF10B981) // #10b981
val Warning = Color(0xFFF59E0B) // #f59e0b
val Danger = Color(0xFFEF4444) // #ef4444

// Dark mode (near-black #1a1a1a / #0f0f0f)
val DarkPrimary = Color(0xFF1A1A1A)
val DarkPrimaryHover = Color(0xFF2A2A2A)
val DarkOnPrimary = Color(0xFFFFFFFF)
val DarkBackground = Color(0xFF0F0F0F)
val DarkSurface = Color(0xFF252525)
val DarkBorder = Color(0xFF3A3A3A)
val DarkText = Color(0xFFFFFFFF)
val DarkTextMuted = Color(0xFF999999)
val DarkAccent = Color(0xFF4D94FF)
val DarkSuccess = Color(0xFF10B981)
val DarkWarning = Color(0xFFFBBF24)
val DarkDanger = Color(0xFFFF6B6B)

// Shadows (as color overlays, actual shadow uses elevation)
val ShadowCard = Color(0x14000000) // 0 1px 3px rgba(0,0,0,0.08)
val ShadowElevated = Color(0x1F000000) // 0 4px 12px rgba(0,0,0,0.12)

// ===== Backward compatibility aliases (old VoLTE theme) =====
val VoLtePrimary = Accent
val VoLtePrimaryLight = Color(0xFF4D94FF)
val VoLtePrimaryDark = AccentPressed
val VoLteSecondary = Color(0xFF21005D)
val VoLteTertiary = Color(0xFF006A6A)
val StatusSuccess = Success
val StatusSuccessContainer = Color(0xFFD1FAE5)
val StatusSuccessText = Color(0xFF064E3B)
val StatusWarning = Warning
val StatusWarningContainer = Color(0xFFFEF3C7)
val StatusWarningText = Color(0xFF92400E)
val StatusError = Danger
val StatusErrorContainer = Color(0xFFFEE2E2)
val StatusErrorText = Color(0xFF991B1B)
val StatusInfo = Accent
val StatusInfoContainer = Color(0xFFDBEAFE)
val StatusInfoText = Color(0xFF1E3A8A)
val LightBackground = Background
val LightSurface = Surface
val LightSurfaceVariant = Color(0xFFF9FAFB)
val LightOutline = Border
val LightOutlineVariant = Color(0xFFD1D5DB)
val LightOnSurface = Text
val LightOnSurfaceVariant = TextMuted
val DarkSurfaceVariant = Color(0xFF2A2A2A)
val DarkOutline = DarkBorder
val DarkOutlineVariant = Color(0xFF4B5563)
val DarkOnSurface = DarkText
val DarkOnSurfaceVariant = DarkTextMuted

// Spacing scale (8px base)
val Spacing4 = 4
val Spacing8 = 8
val Spacing12 = 12
val Spacing16 = 16
val Spacing24 = 24
val Spacing32 = 32
val Spacing48 = 48
val Spacing64 = 64
val Spacing96 = 96
val Spacing128 = 128

// Radius
val RadiusSm = 4
val RadiusMd = 8
val RadiusLg = 12
val RadiusXl = 16
val RadiusPill = 9999
