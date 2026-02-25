package com.sbs.loaney.ui.theme

import androidx.compose.ui.graphics.Color

// === Dark Finance Palette (Screenshot Match) ===

// Backgrounds
val DeepCharcoal    = Color(0xFF12141A)   // Main background
val SurfaceDark     = Color(0xFF1E2028)   // Card / bottom bar
val SurfaceElevated = Color(0xFF252830)   // Elevated card / input bg
val SurfaceBorder   = Color(0xFF2E3140)   // Subtle card border

// Accents
val NeonLime        = Color(0xFFC6FF00)   // Primary — Lent, positive, CTA
val SkyBlue         = Color(0xFF4DC9FF)   // Secondary — charts, active input
val CoralRed        = Color(0xFFFF6B6B)   // Tertiary — Borrowed, negative, delete
val AccentPurple    = Color(0xFF9B7FFF)   // Quaternary — misc chart, tags
val AccentOrange    = Color(0xFFFF9F45)   // Quinary — overdue, warn

// Semantic aliases
val PrimaryLime     = NeonLime
val SecondaryBlue   = SkyBlue
val TertiaryRed     = CoralRed
val BlackBG         = DeepCharcoal
val ChipGrey        = SurfaceDark

// Legacy aliases kept so existing screens compile
val SecondaryOrange = AccentOrange
val AccentYellow    = NeonLime
