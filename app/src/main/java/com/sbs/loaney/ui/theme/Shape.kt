package com.sbs.loaney.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ── MaterialTheme Shapes (generous, abstract) ──
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

// ── Custom Abstract Shapes ──
val PillShape = RoundedCornerShape(50)                     // Capsule / pill
val CardShape = RoundedCornerShape(24.dp)                  // Standard card
val BigCardShape = RoundedCornerShape(28.dp)               // Hero / feature card
val SheetTopShape = RoundedCornerShape(                    // Bottom-sheet style
    topStart = 28.dp, topEnd = 28.dp,
    bottomStart = 0.dp, bottomEnd = 0.dp
)
val AsymmetricCardShape = RoundedCornerShape(              // Asymmetric abstract card
    topStart = 28.dp, topEnd = 28.dp,
    bottomStart = 8.dp, bottomEnd = 28.dp
)
val SoftChipShape = RoundedCornerShape(14.dp)              // Tags / chips
val ButtonShape = RoundedCornerShape(16.dp)                // Action buttons
val SmallCardShape = RoundedCornerShape(18.dp)             // Inner / nested cards
