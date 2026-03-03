package com.sbs.loaney.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.abs

/**
 * Animated currency text that smoothly counts up/down to the target value
 * like an odometer / rolling counter effect.
 */
@Composable
fun AnimatedCurrencyText(
    targetValue: Double,
    currencySymbol: String = "৳",
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineLarge,
    color: Color = MaterialTheme.colorScheme.onBackground,
    fontWeight: FontWeight = FontWeight.ExtraBold,
    prefix: String = "",
    durationMillis: Int = 800
) {
    val animatedValue = remember { Animatable(0f) }

    LaunchedEffect(targetValue) {
        animatedValue.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = tween(durationMillis = durationMillis)
        )
    }

    val displayValue = animatedValue.value.toDouble()
    val formattedValue = String.format("%,.0f", abs(displayValue))

    Text(
        text = "$prefix$currencySymbol$formattedValue",
        style = style.copy(
            fontWeight = fontWeight,
            color = color
        ),
        modifier = modifier
    )
}
