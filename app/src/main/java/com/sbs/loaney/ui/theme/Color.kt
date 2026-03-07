package com.sbs.loaney.ui.theme

import androidx.compose.ui.graphics.Color

// ── Neubrutalism Core Palette ────────────────────────────────────────────────
val NbCoral      = Color(0xFFFC6A4D)   // Vibrant Coral/Orange
val NbSkyBlue    = Color(0xFF608BC1)   // Soft Sky Blue
val NbYellow     = Color(0xFFFFE66D)   // Bright Yellow
val NbGreen      = Color(0xFF00C896)   // Minty Green
val NbCream      = Color(0xFFF8F4E1)   // Soft Cream Background
val NbPureBlack  = Color(0xFF000000)   // Solid Black for borders/shadows
val NbPureWhite  = Color(0xFFFFFFFF)   // Pure White

// ── Theme Mapping ────────────────────────────────────────────────────────────
// Light Mode
val NbLightBg             = NbCream
val NbLightSurface        = NbPureWhite
val NbLightOnSurface      = NbPureBlack
val NbLightOutline        = NbPureBlack
val NbLightShadow         = NbPureBlack

// Dark Mode
val NbDarkBg              = Color(0xFF121212)
val NbDarkSurface         = Color(0xFF1E1E1E)
val NbDarkOnSurface       = NbPureWhite
val NbDarkOutline         = NbPureWhite
val NbDarkShadow          = Color(0xFF000000).copy(alpha = 0.8f)

// ── bKash Brand Palette (Re-mapped to Neubrutalism) ──────────────────────────
val BkashPink        = NbCoral
val BkashPinkDark    = NbCoral.copy(alpha = 0.8f)
val BkashPinkLight   = NbCoral.copy(alpha = 0.6f)
val BkashPinkSurface = NbCream
val BkashHeroStart   = NbCoral
val BkashHeroEnd     = Color(0xFFFF8E72)

// ── Neutrals (Re-mapped) ─────────────────────────────────────────────────────
val BkashWhite          = NbPureWhite
val BkashOffWhite       = NbCream
val BkashSurface        = NbPureWhite
val BkashSurfaceVariant = NbCream
val BkashOutline        = NbPureBlack
val BkashOutlineVariant = NbPureBlack.copy(alpha = 0.4f)
val BkashOnSurface      = NbPureBlack
val BkashOnSurfaceVar   = Color(0xFF333333)

// ── Dark fallback (Re-mapped) ────────────────────────────────────────────────
val DeepDarkBg          = NbDarkBg
val DarkSurface         = NbDarkSurface
val DarkSurfaceVariant  = Color(0xFF2C2C2C)
val DarkOutline         = NbDarkOutline
val GlassBorder         = Color(0x33FFFFFF)
val MutedText           = Color(0xFFAAAAAA)

// ── Semantic / Financial Colors (Re-mapped) ───────────────────────────────────
val EmeraldGreen = NbGreen
val CoralRose    = NbCoral
val SkyBlue      = NbSkyBlue
val AmberWarn    = NbYellow

// ── Legacy compat ─────────────────────
val SoftViolet         = BkashPink
val SoftVioletDark     = BkashPinkDark
val SoftVioletLight    = BkashPinkLight
val CoralPink          = CoralRose
val CoralPinkDark      = NbCoral
val CoralPinkLight     = NbCream
val CoralRed           = NbCoral
val NeonLime           = NbGreen
val DarkCard           = DarkSurface
val LightGrayBg        = NbCream
val SubtleGray         = NbPureBlack
val SurfaceElevated    = NbPureWhite
val DarkCardBg         = DarkSurface
val TextSubtextDark    = MutedText
val TextSubtextLight   = Color(0xFFCCCCCC)
val TextMainDark       = NbPureWhite
val TextMainLight      = NbPureBlack
val DashboardBg        = NbCream
val SubtleBorder       = NbPureBlack
val ThemeGreen         = NbGreen
val ThemeLightGreen    = NbGreen.copy(alpha = 0.2f)
val PrimaryLime        = NbGreen
val SecondaryOrange    = NbYellow
val FabGlow            = NbCoral

// ── Colorful/Neon Accents (Re-mapped) ─────────────────────────────────────────
val SoftNeonPurple   = NbSkyBlue
val SoftNeonPink     = NbCoral
val SoftNeonCyan     = NbSkyBlue
val SoftNeonBg       = NbDarkBg
val SoftNeonSurface  = NbDarkSurface
val SoftNeonSurfaceVar = Color(0xFF2C2C2C)
val SoftNeonError    = NbCoral
val SoftNeonOnSurface = NbPureWhite
val SoftNeonOutline  = NbPureWhite

// ── Hero Card Gradient ──────────────────────────────────────────────
val HeroCardDark1  = Color(0xFF1E1E1E)
val HeroCardDark2  = Color(0xFF2C2C2C)
val HeroCardDark3  = Color(0xFF333333)
val HeroCardAccent = NbCoral

// ── Gradient Avatar Palette (Updated for Neubrutalism vibe) ───────────────────
val AvatarGradients = listOf(
    NbCoral to Color(0xFFFF8E72),
    NbSkyBlue to Color(0xFF8AB6D6),
    NbYellow to Color(0xFFFFED90),
    NbGreen to Color(0xFF4DD9B6),
    NbSkyBlue to NbCoral,
    NbCoral to NbYellow,
    NbGreen to NbSkyBlue,
    NbYellow to NbGreen,
)
