package com.sbs.loaney.ui.theme

import androidx.compose.ui.graphics.Color

// ── bKash Brand Palette ──────────────────────────────────────────────────────
val BkashPink        = Color(0xFFE2136E)   // Primary brand pink/magenta
val BkashPinkDark    = Color(0xFFC01060)   // Pressed / darker variant
val BkashPinkLight   = Color(0xFFFF4B96)   // Lighter variant / hover
val BkashPinkSurface = Color(0xFFFCE4EF)   // Pink tinted surface / chips
val BkashHeroStart   = Color(0xFFE2136E)   // Hero gradient start
val BkashHeroEnd     = Color(0xFFC4006A)   // Hero gradient end

// ── Light (default) neutrals ─────────────────────────────────────────────────
val BkashWhite          = Color(0xFFFFFFFF)
val BkashOffWhite       = Color(0xFFF7F7F7)    // Screen background
val BkashSurface        = Color(0xFFFFFFFF)    // Card / sheet surface
val BkashSurfaceVariant = Color(0xFFF2F2F2)    // Input backgrounds
val BkashOutline        = Color(0xFFE0E0E0)    // Borders
val BkashOutlineVariant = Color(0xFFEEEEEE)    // Subtle dividers
val BkashOnSurface      = Color(0xFF1A1A1A)    // Primary text
val BkashOnSurfaceVar   = Color(0xFF757575)    // Secondary / hint text

// ── Dark fallback (kept for dark theme option) ────────────────────────────────
val DeepDarkBg          = Color(0xFF0F172A)
val DarkSurface         = Color(0xFF1E293B)
val DarkSurfaceVariant  = Color(0xFF334155)
val DarkOutline         = Color(0xFF475569)
val GlassBorder         = Color(0x0DFFFFFF)
val MutedText           = Color(0xFF94A3B8)

// ── Semantic / Financial Colors ───────────────────────────────────────────────
val EmeraldGreen = Color(0xFF00875A)   // Positive / lent / income (bKash-compatible green)
val CoralRose    = Color(0xFFE53935)   // Negative / borrowed / expense
val SkyBlue      = Color(0xFF1565C0)   // Accent blue
val AmberWarn    = Color(0xFFF59E0B)   // Warning / overdue

// ── Legacy compat (mapped so existing references compile) ─────────────────────
val SoftViolet         = BkashPink
val SoftVioletDark     = BkashPinkDark
val SoftVioletLight    = BkashPinkLight
val CoralPink          = CoralRose
val CoralPinkDark      = Color(0xFFB71C1C)
val CoralPinkLight     = Color(0xFFFFEBEE)
val CoralRed           = CoralRose
val NeonLime           = EmeraldGreen
val DarkCard           = DarkSurface
val LightGrayBg        = BkashOffWhite
val SubtleGray         = BkashOutline
val SurfaceElevated    = BkashSurfaceVariant
val DarkCardBg         = DarkSurface
val TextSubtextDark    = MutedText
val TextSubtextLight   = Color(0xFFE2E8F0)
val TextMainDark       = BkashOnSurface
val TextMainLight      = Color(0xFFF1F5F9)
val DashboardBg        = BkashOffWhite
val SubtleBorder       = BkashOutline
val ThemeGreen         = EmeraldGreen
val ThemeLightGreen    = Color(0xFFD1FAE5)
val PrimaryLime        = EmeraldGreen
val SecondaryOrange    = Color(0xFFF59E0B)
val FabGlow            = BkashPink

// ── Colorful/Neon Accents (kept for colorful theme option) ───────────────────
val SoftNeonPurple   = Color(0xFF9B72FF)
val SoftNeonPink     = Color(0xFFFF6B9D)
val SoftNeonCyan     = Color(0xFF4DD9E8)
val SoftNeonBg       = Color(0xFF1A1428)
val SoftNeonSurface  = Color(0xFF241E36)
val SoftNeonSurfaceVar = Color(0xFF2E2644)
val SoftNeonError    = Color(0xFFFF6B6B)
val SoftNeonOnSurface = Color(0xFFE8DFFF)
val SoftNeonOutline  = Color(0xFF4A3D6B)

// ── Hero Card Gradient (kept for dark mode hero) ──────────────────────────────
val HeroCardDark1  = Color(0xFF0A2E1F)
val HeroCardDark2  = Color(0xFF0F3B2A)
val HeroCardDark3  = Color(0xFF152E3D)
val HeroCardAccent = Color(0xFF1A4030)

// ── Gradient Avatar Palette ───────────────────────────────────────────────────
val AvatarGradients = listOf(
    Color(0xFFE2136E) to Color(0xFFC4006A),  // bKash Pink
    Color(0xFF667EEA) to Color(0xFF764BA2),  // Indigo → Purple
    Color(0xFF4FACFE) to Color(0xFF00F2FE),  // Blue → Cyan
    Color(0xFF43E97B) to Color(0xFF38F9D7),  // Green → Teal
    Color(0xFFF6D365) to Color(0xFFFDA085),  // Gold → Peach
    Color(0xFFA18CD1) to Color(0xFFFBC2EB),  // Lavender → Rose
    Color(0xFF30CFD0) to Color(0xFF330867),  // Teal → Midnight
    Color(0xFFFF9A9E) to Color(0xFFFECFEF),  // Salmon → Blush
)
