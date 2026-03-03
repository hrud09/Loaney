package com.sbs.loaney.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.sbs.loaney.ui.theme.*

enum class OnboardingIllustrationType {
    WELCOME, HISTORY, CLARITY, ZERO_STATE
}

@Composable
fun OnboardingIllustration(
    type: OnboardingIllustrationType,
    modifier: Modifier = Modifier.size(240.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "illustration")
    
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating"
    )

    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaling"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2f, height / 2f)

        withTransform({
            translate(top = floatAnim)
            scale(scaleAnim, scaleAnim, center)
        }) {
            when (type) {
                OnboardingIllustrationType.WELCOME -> {
                    // Central "Loaney Pie" Coin
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFD54F), Color(0xFFFFC107)),
                            center = center
                        ),
                        radius = width * 0.25f
                    )
                    // Coin texture / pie slice
                    drawPath(
                        path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(center.x, center.y)
                            lineTo(center.x, center.y - width * 0.25f)
                            arcTo(
                                rect = androidx.compose.ui.geometry.Rect(
                                    center.x - width * 0.25f,
                                    center.y - width * 0.25f,
                                    center.x + width * 0.25f,
                                    center.y + width * 0.25f
                                ),
                                startAngleDegrees = -90f,
                                sweepAngleDegrees = 60f,
                                forceMoveTo = false
                            )
                            close()
                        },
                        color = DeepDarkBg.copy(alpha = 0.2f)
                    )
                    // Decorative orbits
                    drawCircle(
                        color = SoftViolet.copy(alpha = 0.3f),
                        radius = width * 0.4f,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = SkyBlue.copy(alpha = 0.3f),
                        radius = width * 0.45f,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                OnboardingIllustrationType.HISTORY -> {
                    // Stylized Transaction List
                    val cardWidth = width * 0.5f
                    val cardHeight = height * 0.15f
                    
                    for (i in 0 until 3) {
                        val offset = Offset(width * 0.25f, height * 0.3f + (i * height * 0.2f))
                        drawRoundRect(
                            color = if (i == 1) SoftViolet else DarkSurfaceVariant,
                            topLeft = offset,
                            size = Size(cardWidth, cardHeight),
                            cornerRadius = CornerRadius(12.dp.toPx())
                        )
                        // Line placeholder
                        drawLine(
                            color = Color.White.copy(alpha = 0.2f),
                            start = offset.copy(x = offset.x + 20.dp.toPx(), y = offset.y + cardHeight/2),
                            end = offset.copy(x = offset.x + cardWidth - 40.dp.toPx(), y = offset.y + cardHeight/2),
                            strokeWidth = 4.dp.toPx()
                        )
                    }
                }
                OnboardingIllustrationType.CLARITY -> {
                    // Stylized Chart / Clarity
                    val points = listOf(
                        Offset(width * 0.2f, height * 0.7f),
                        Offset(width * 0.4f, height * 0.4f),
                        Offset(width * 0.6f, height * 0.5f),
                        Offset(width * 0.8f, height * 0.2f)
                    )
                    
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(points[0].x, points[0].y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    
                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(listOf(EmeraldGreen, SkyBlue)),
                        style = Stroke(width = 6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                    
                    // Circles at points
                    points.forEach {
                        drawCircle(color = Color.White, radius = 6.dp.toPx(), center = it)
                        drawCircle(color = SkyBlue, radius = 4.dp.toPx(), center = it)
                    }
                }
                OnboardingIllustrationType.ZERO_STATE -> {
                    // Minimalist "Empty Wallet" or "Start"
                    drawCircle(
                        color = DarkSurfaceVariant,
                        radius = width * 0.3f,
                        style = Stroke(width = 2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f))
                    )
                    drawCircle(
                        brush = Brush.linearGradient(listOf(SoftViolet, SkyBlue)),
                        radius = width * 0.15f
                    )
                    // Plus sign
                    val plusSize = width * 0.08f
                    drawLine(
                        color = Color.White,
                        start = center.copy(x = center.x - plusSize),
                        end = center.copy(x = center.x + plusSize),
                        strokeWidth = 4.dp.toPx()
                    )
                    drawLine(
                        color = Color.White,
                        start = center.copy(y = center.y - plusSize),
                        end = center.copy(y = center.y + plusSize),
                        strokeWidth = 4.dp.toPx()
                    )
                }
            }
        }
    }
}
