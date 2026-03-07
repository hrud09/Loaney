package com.sbs.loaney.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ── MaterialTheme Shapes ──
// ── MaterialTheme Shapes ──
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

// ── Custom Shapes ──
val PillShape           = RoundedCornerShape(50)
val CardShape           = RoundedCornerShape(12.dp)
val BigCardShape        = RoundedCornerShape(16.dp)
val SheetTopShape       = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
val AsymmetricCardShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
val SoftChipShape       = RoundedCornerShape(8.dp)
val ButtonShape         = RoundedCornerShape(8.dp)
val SmallCardShape      = RoundedCornerShape(8.dp)
val ActionIconShape     = RoundedCornerShape(8.dp) // Changed from Circle for Brutalist look
