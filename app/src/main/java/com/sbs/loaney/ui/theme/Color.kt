package com.sbs.loaney.ui.theme

import androidx.compose.ui.graphics.Color

// ── Premium Slate Palette ──
val SoftViolet = Color(0xFF7C6EF6)           // Primary accent (luminous indigo)
val SoftVioletDark = Color(0xFF6A5CE0)       // Pressed/darker variant
val SoftVioletLight = Color(0xFF9B8FFF)      // Lighter variant
val DeepDarkBg = Color(0xFF0F172A)           // Deep slate background
val DarkSurface = Color(0xFF1E293B)          // Elevated surface / cards
val DarkSurfaceVariant = Color(0xFF334155)   // Input fields, icon containers
val DarkOutline = Color(0xFF475569)          // Subtle borders
val GlassBorder = Color(0x0DFFFFFF)          // ~5% white for glassmorphic edges
val MutedText = Color(0xFF94A3B8)            // Slate-gray secondary text

// ── Semantic Colors ──
val EmeraldGreen = Color(0xFF10B981)         // Positive / lent / income
val CoralRose = Color(0xFFF43F5E)            // Negative / borrowed / expense
val SkyBlue = Color(0xFF5AC8FA)              // Accent blue
val AmberWarn = Color(0xFFF59E0B)            // Warning / overdue

// ── Legacy backward compat (mapped to new palette) ──
val CoralPink = CoralRose
val CoralPinkDark = Color(0xFFE11D48)
val CoralPinkLight = Color(0xFFFDE8EB)
val CoralRed = CoralRose
val NeonLime = EmeraldGreen
val DarkCard = DarkSurface
val LightGrayBg = Color(0xFFF1F5F9)
val SubtleGray = Color(0xFFCBD5E1)
val SurfaceElevated = Color(0xFF334155)
val DarkCardBg = DarkSurface
val TextSubtextDark = MutedText
val TextSubtextLight = Color(0xFFE2E8F0)
val TextMainDark = Color(0xFF0F172A)
val TextMainLight = Color(0xFFF1F5F9)
val DashboardBg = Color(0xFFF1F5F9)
val SubtleBorder = DarkOutline
val ThemeGreen = EmeraldGreen
val ThemeLightGreen = Color(0xFFD1FAE5)
val PrimaryLime = EmeraldGreen
val SecondaryOrange = Color(0xFFF59E0B)

// ── Colorful Theme - Soft Neon Palette ──
val SoftNeonPurple = Color(0xFF9B72FF)
val SoftNeonPink = Color(0xFFFF6B9D)
val SoftNeonCyan = Color(0xFF4DD9E8)
val SoftNeonBg = Color(0xFF1A1428)
val SoftNeonSurface = Color(0xFF241E36)
val SoftNeonSurfaceVar = Color(0xFF2E2644)
val SoftNeonError = Color(0xFFFF6B6B)
val SoftNeonOnSurface = Color(0xFFE8DFFF)
val SoftNeonOutline = Color(0xFF4A3D6B)

// ── Hero Card Gradient ──
val HeroCardDark1 = Color(0xFF0A2E1F)      // Deep emerald-black
val HeroCardDark2 = Color(0xFF0F3B2A)      // Forest green
val HeroCardDark3 = Color(0xFF152E3D)      // Dark slate-teal
val HeroCardAccent = Color(0xFF1A4030)      // Subtle green tint

// ── Gradient Avatar Palette (vibrant pairs) ──
val AvatarGradients = listOf(
    Color(0xFF667EEA) to Color(0xFF764BA2),  // Indigo → Purple
    Color(0xFFF093FB) to Color(0xFFF5576C),  // Pink → Coral
    Color(0xFF4FACFE) to Color(0xFF00F2FE),  // Blue → Cyan
    Color(0xFF43E97B) to Color(0xFF38F9D7),  // Green → Teal
    Color(0xFFF6D365) to Color(0xFFFDA085),  // Gold → Peach
    Color(0xFFA18CD1) to Color(0xFFFBC2EB),  // Lavender → Rose
    Color(0xFF30CFD0) to Color(0xFF330867),  // Teal → Midnight
    Color(0xFFFF9A9E) to Color(0xFFFECFEF),  // Salmon → Blush
)

// ── FAB Glow ──
val FabGlow = Color(0xFF7C6EF6)             // Matches primary violet
