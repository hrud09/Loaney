package com.sbs.loaney.ui.theme

import androidx.compose.ui.graphics.Color

// === Unified Consumer-Ready Palette (Light Dashboard Style) ===

// Backgrounds
val DashboardBg     = Color(0xFFF7F7F7)   // Clean light grey background
val DarkCardBg      = Color(0xFF1E1E22)   // Premium sleek dark card
val SurfaceElevated = Color(0xFFFFFFFF)   // White cards / popups
val SubtleBorder    = Color(0xFFE5E5EA)   // Light subtle borders

// Accents
val NeonLime        = Color(0xFFC6FF00)   // Primary — Action items (Deposit/Send/Pay)
val SkyBlue         = Color(0xFF4DC9FF)   // Secondary — active input, links
val CoralRed        = Color(0xFFFF6B6B)   // Tertiary — Destructive, withdraw
val AccentPurple    = Color(0xFF9B7FFF)   // Misc chart accents
val AccentOrange    = Color(0xFFFF9F45)   // Warning, overdue

// Text Colors
val TextMainDark    = Color(0xFF1C1C1E)   // Headers on light background
val TextSubtextDark = Color(0xFF8E8E93)   // Subtext on light background
val TextMainLight   = Color(0xFFFFFFFF)   // Text on dark cards
val TextSubtextLight= Color(0xFFA0A0A5)   // Subtext on dark cards

// Semantic aliases (For legacy mapping)
val PrimaryLime     = NeonLime
val SecondaryBlue   = SkyBlue
val TertiaryRed     = CoralRed
val BlackBG         = DarkCardBg
val ChipGrey        = Color(0xFFE5E5EA)
val SecondaryOrange = AccentOrange
val AccentYellow    = NeonLime
