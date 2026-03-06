package com.sbs.loaney.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ── MaterialTheme Shapes ──
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

// ── Custom Shapes ──
val PillShape           = RoundedCornerShape(50)
val CardShape           = RoundedCornerShape(16.dp)            // Softer than before
val BigCardShape        = RoundedCornerShape(20.dp)
val SheetTopShape       = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
val AsymmetricCardShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 8.dp, bottomEnd = 28.dp)
val SoftChipShape       = RoundedCornerShape(10.dp)
val ButtonShape         = RoundedCornerShape(12.dp)
val SmallCardShape      = RoundedCornerShape(12.dp)
val ActionIconShape     = CircleShape                          // bKash circular action icons
