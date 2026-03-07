package com.sbs.loaney.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applies a glassmorphism effect with a semi-transparent background and 
 * subtle white border. Note: We avoid using .blur() here as it affects children;
 * for background blur, use a separate layer.
 */
fun Modifier.glassCard(
    shape: Shape,
    backgroundColor: Color = Color.White.copy(alpha = 0.7f),
    borderColor: Color = Color.White.copy(alpha = 0.3f),
    borderWidth: Dp = 0.5.dp
): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            colors = listOf(
                backgroundColor,
                backgroundColor.copy(alpha = 0.6f)
            )
        )
    )
    .border(borderWidth, borderColor, shape)

/**
 * A simpler version of the glass effect without active blur for better performance
 * on older devices or for lists.
 */
fun Modifier.simpleGlass(
    shape: Shape,
    backgroundColor: Color = Color.White.copy(alpha = 0.85f),
    borderColor: Color = Color.White.copy(alpha = 0.1f)
): Modifier = this
    .clip(shape)
    .background(backgroundColor)
    .border(0.5.dp, borderColor, shape)
