package com.sbs.loaney.ui.theme

import androidx.compose.ui.graphics.Color

// ── Cyber-Vibrant Core Palette ───────────────────────────────────────────────
val CyberIndigo   = Color(0xFF6366F1)   // Primary
val VibrantTeal   = Color(0xFF2DD4BF)   // Secondary
val CoralRose     = Color(0xFFFB7185)   // Accent / Debt
val SoftSlate     = Color(0xFFF8FAFC)   // Background
val PureWhite     = Color(0xFFFFFFFF)
val PureBlack     = Color(0xFF000000)

// ── Glassmorphism Helpers ────────────────────────────────────────────────────
val GlassWhite    = Color(0xCCFFFFFF)   // 80% White
val GlassBorder   = Color(0x4DFFFFFF)   // 30% White

// ── Theme Mapping (Compat with existing code) ───────────────────────────────
val NbPureBlack   = PureBlack 
val NbPureWhite   = PureWhite
val NbCoral       = CoralRose
val NbSkyBlue     = CyberIndigo
val NbYellow      = VibrantTeal
val NbGreen       = VibrantTeal
val NbCream       = SoftSlate

// Light Mode Mapping
val NbLightBg             = SoftSlate
val NbLightSurface        = PureWhite
val NbLightOnSurface      = PureBlack
val NbLightOutline        = PureBlack.copy(alpha = 0.1f)
val NbLightShadow         = Color(0x0D000000)

// Dark Mode Mapping
val NbDarkBg              = Color(0xFF0F172A) // Deep Slate
val NbDarkSurface         = Color(0xFF1E293B)
val NbDarkOnSurface       = PureWhite
val NbDarkOutline         = Color(0x33FFFFFF)
val NbDarkShadow          = Color(0x66000000)

// ── bKash/Legacy Mapping (Updated for Cyber-Vibrant) ─────────────────────────
val BkashPink        = CyberIndigo
val BkashPinkDark    = CyberIndigo.copy(alpha = 0.8f)
val BkashPinkLight   = CyberIndigo.copy(alpha = 0.1f)
val BkashPinkSurface = SoftSlate
val BkashHeroStart   = CyberIndigo
val BkashHeroEnd     = Color(0xFF818CF8)

// ── Neutrals ─────────────────────────────────────────────────────────────────
val BkashWhite          = PureWhite
val BkashOffWhite       = SoftSlate
val BkashSurface        = PureWhite
val BkashSurfaceVariant = Color(0xFFF1F5F9)
val BkashOutline        = Color(0xFFE2E8F0)
val BkashOutlineVariant = Color(0xFF94A3B8).copy(alpha = 0.2f)
val BkashOnSurface      = PureBlack
val BkashOnSurfaceVar   = Color(0xFF475569)

// ── Semantic ─────────────────────────────────────────────────────────────────
val EmeraldGreen = VibrantTeal
val AmberWarn    = Color(0xFFFBBF24)

// ── Avatar Gradients (Cyber-Vibrant Edition) ─────────────────────────────────
val AvatarGradients = listOf(
    CyberIndigo to Color(0xFF818CF8),
    VibrantTeal to Color(0xFF5EEAD4),
    CoralRose to Color(0xFFFDA4AF),
    Color(0xFFA855F7) to Color(0xFFC084FC), 
    CyberIndigo to VibrantTeal,
    VibrantTeal to CoralRose,
    CoralRose to CyberIndigo,
    Color(0xFFF472B6) to Color(0xFFFB7185), 
)

// Comprehensive Legacy Mapping for Build Compatibility
val CoralRed         = CoralRose
val CoralPink        = CoralRose.copy(alpha = 0.6f)
val SkyBlue          = CyberIndigo
val SoftViolet       = CyberIndigo
val SurfaceElevated  = PureWhite
val SecondaryOrange  = VibrantTeal
val DarkSurface      = Color(0xFF1E293B)
val DarkOutline      = Color(0x33FFFFFF)
val MutedText        = Color(0xFF94A3B8)
val DashboardBg      = SoftSlate
val DeepDarkBg       = Color(0xFF0F172A)
val DarkSurfaceVariant = Color(0xFF1E293B)
val SubtleBorder     = Color(0xFFE2E8F0)
