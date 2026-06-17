package com.sbs.loaney.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Neubrutulism extension for cards and containers.
 * Applies a thick black border and a hard offset shadow.
 */
@Composable
fun Modifier.neubrutalistCard(
    shape: Shape = CardShape,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.onBackground,
    borderWidth: Dp = 2.dp,
    shadowOffset: Dp = 4.dp
): Modifier = this
    .drawBehind {
        // Draw hard shadow
        drawRoundRect(
            color = borderColor,
            topLeft = androidx.compose.ui.geometry.Offset(shadowOffset.toPx(), shadowOffset.toPx()),
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                (shape as? androidx.compose.foundation.shape.RoundedCornerShape)?.bottomEnd?.toPx(size, this) ?: 0f
            )
        )
    }
    .background(backgroundColor, shape)
    .border(borderWidth, borderColor, shape)

/**
 * Neubrutulism extension for buttons.
 */
@Composable
fun Modifier.neubrutalistButton(
    shape: Shape = ButtonShape,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    borderColor: Color = MaterialTheme.colorScheme.onBackground,
    borderWidth: Dp = 2.dp,
    shadowOffset: Dp = 2.dp
): Modifier = this
    .drawBehind {
        drawRoundRect(
            color = borderColor,
            topLeft = androidx.compose.ui.geometry.Offset(shadowOffset.toPx(), shadowOffset.toPx()),
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                (shape as? androidx.compose.foundation.shape.RoundedCornerShape)?.bottomEnd?.toPx(size, this) ?: 0f
            )
        )
    }
    .background(backgroundColor, shape)
    .border(borderWidth, borderColor, shape)
