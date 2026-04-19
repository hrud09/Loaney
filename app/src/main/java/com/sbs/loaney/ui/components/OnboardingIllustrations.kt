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
    modifier: Modifier = Modifier.size(280.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "illustration")
    
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating"
    )

    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaling"
    )

    val rotationAnim by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2f, height / 2f)

        withTransform({
            translate(top = floatAnim)
            scale(scaleAnim, scaleAnim, center)
            rotate(rotationAnim, center)
        }) {
            when (type) {
                OnboardingIllustrationType.WELCOME -> {
                    // Modern Glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(AlimGreen.copy(alpha = 0.2f), Color.Transparent),
                            center = center,
                            radius = width * 0.5f
                        )
                    )

                    // Central "Loaney Pie" Coin with Shadow
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.1f),
                        radius = width * 0.26f,
                        center = center.copy(y = center.y + 8.dp.toPx())
                    )
                    
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFFFEE58), Color(0xFFFBC02D)),
                            start = Offset(center.x - width*0.2f, center.y - width*0.2f),
                            end = Offset(center.x + width*0.2f, center.y + width*0.2f)
                        ),
                        radius = width * 0.25f
                    )

                    // Coin Highlights
                    drawCircle(
                        color = Color.White.copy(alpha = 0.3f),
                        radius = width * 0.22f,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Decorative orbits with glowing nodes
                    drawCircle(
                        color = AlimGreen.copy(alpha = 0.2f),
                        radius = width * 0.4f,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    
                    val orbitAngle = (rotationAnim * 10f).toDouble()
                    drawCircle(
                        color = AlimGreen,
                        radius = 6.dp.toPx(),
                        center = Offset(
                            center.x + (width * 0.4f) * kotlin.math.cos(Math.toRadians(orbitAngle)).toFloat(),
                            center.y + (width * 0.4f) * kotlin.math.sin(Math.toRadians(orbitAngle)).toFloat()
                        )
                    )
                }
                OnboardingIllustrationType.HISTORY -> {
                    // Background soft glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(SoftViolet.copy(alpha = 0.15f), Color.Transparent),
                            center = center,
                            radius = width * 0.6f
                        )
                    )

                    val cardWidth = width * 0.55f
                    val cardHeight = height * 0.18f
                    
                    for (i in 0 until 3) {
                        val verticalOffset = height * 0.25f + (i * height * 0.22f)
                        val horizontalShift = if (i == 1) 20.dp.toPx() else 0f
                        val offset = Offset(width * 0.2f + horizontalShift, verticalOffset)
                        
                        // Card Shadow
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.05f),
                            topLeft = offset.copy(y = offset.y + 4.dp.toPx()),
                            size = Size(cardWidth, cardHeight),
                            cornerRadius = CornerRadius(16.dp.toPx())
                        )

                        // Main Card
                        drawRoundRect(
                            brush = if (i == 1) {
                                Brush.linearGradient(listOf(AlimGreen, AlimGreen.copy(alpha = 0.8f)))
                            } else {
                                Brush.linearGradient(listOf(Color.White, Color(0xFFFAFAFA)))
                            },
                            topLeft = offset,
                            size = Size(cardWidth, cardHeight),
                            cornerRadius = CornerRadius(16.dp.toPx())
                        )
                        
                        // Content Lines
                        val lineColor = if (i == 1) Color.White.copy(alpha = 0.4f) else AlimDark.copy(alpha = 0.1f)
                        drawLine(
                            color = lineColor,
                            start = offset.copy(x = offset.x + 16.dp.toPx(), y = offset.y + cardHeight * 0.4f),
                            end = offset.copy(x = offset.x + cardWidth * 0.6f, y = offset.y + cardHeight * 0.4f),
                            strokeWidth = 6.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        drawLine(
                            color = lineColor,
                            start = offset.copy(x = offset.x + 16.dp.toPx(), y = offset.y + cardHeight * 0.65f),
                            end = offset.copy(x = offset.x + cardWidth * 0.4f, y = offset.y + cardHeight * 0.65f),
                            strokeWidth = 6.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }
                OnboardingIllustrationType.CLARITY -> {
                    // Stylized Chart with Gradient Fill
                    val points = listOf(
                        Offset(width * 0.15f, height * 0.75f),
                        Offset(width * 0.35f, height * 0.45f),
                        Offset(width * 0.55f, height * 0.55f),
                        Offset(width * 0.85f, height * 0.25f)
                    )
                    
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(points[0].x, points[0].y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }

                    val fillPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(points[0].x, points[0].y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, height * 0.85f)
                        lineTo(points.first().x, height * 0.85f)
                        close()
                    }

                    // Fill Gradient
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(AlimGreen.copy(alpha = 0.3f), Color.Transparent),
                            startY = points.last().y,
                            endY = height * 0.85f
                        )
                    )
                    
                    // Main Line
                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(listOf(AlimGreen, SkyBlue)),
                        style = Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                    
                    // Interaction Points
                    points.forEachIndexed { index, point ->
                        val pulse = if (index == points.size - 1) scaleAnim else 1f
                        drawCircle(color = Color.White, radius = 8.dp.toPx() * pulse, center = point)
                        drawCircle(color = AlimGreen, radius = 5.dp.toPx() * pulse, center = point)
                    }
                }
                OnboardingIllustrationType.ZERO_STATE -> {
                    drawCircle(
                        color = AlimDark.copy(alpha = 0.05f),
                        radius = width * 0.35f,
                        style = Stroke(width = 2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(30f, 30f), floatAnim))
                    )
                    
                    drawCircle(
                        brush = Brush.linearGradient(listOf(AlimGreen, SkyBlue)),
                        radius = width * 0.18f,
                        center = center
                    )

                    // Refined Plus sign
                    val plusSize = width * 0.07f
                    drawLine(
                        color = Color.White,
                        start = center.copy(x = center.x - plusSize),
                        end = center.copy(x = center.x + plusSize),
                        strokeWidth = 5.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White,
                        start = center.copy(y = center.y - plusSize),
                        end = center.copy(y = center.y + plusSize),
                        strokeWidth = 5.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }
    }
}
