package com.sbs.loaney.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ── MaterialTheme Shapes ──
// ── MaterialTheme Shapes ──
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// ── Custom Shapes ──
val PillShape           = RoundedCornerShape(50)
val CardShape           = RoundedCornerShape(16.dp)
val BigCardShape        = RoundedCornerShape(24.dp)
val SheetTopShape       = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
val AsymmetricCardShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 24.dp)
val SoftChipShape       = RoundedCornerShape(12.dp)
val ButtonShape         = RoundedCornerShape(16.dp)
val SmallCardShape      = RoundedCornerShape(12.dp)
val ActionIconShape     = RoundedCornerShape(16.dp) 
