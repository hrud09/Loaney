package com.sbs.loaney.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sbs.loaney.ui.theme.VibrantTeal
import com.sbs.loaney.ui.theme.CoralRose
import com.sbs.loaney.ui.theme.CyberIndigo
import kotlin.math.max
import com.sbs.loaney.ui.theme.SoftSlate
import androidx.compose.ui.res.stringResource
import com.sbs.loaney.R

@Composable
fun DonutChart(
    totalLent: Double,
    totalBorrowed: Double,
    modifier: Modifier = Modifier,
    currencySymbol: String = "৳"
) {
    val total = totalLent + totalBorrowed
    val lentRatio = if (total > 0) (totalLent / total).toFloat() else 0f
    
    val sweepAngle = remember { Animatable(0f) }
    
    LaunchedEffect(totalLent, totalBorrowed) {
        val targetSweep = 360f // The total sweep angle for the animation
        sweepAngle.animateTo(
            targetValue = max(targetSweep, 5f),
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val strokeWidth = 16.dp.toPx()
            val size = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // Background Track
            drawArc(
                color = Color.White.copy(alpha = 0.05f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Lent Arc (Teal)
            if (lentRatio > 0f) {
                drawArc(
                    color = VibrantTeal,
                    startAngle = -90f,
                    sweepAngle = sweepAngle.value * lentRatio,
                    useCenter = false,
                    topLeft = topLeft,
                    size = size,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Borrowed Arc (Coral)
            if (lentRatio < 1f && total > 0) {
                drawArc(
                    color = CoralRose,
                    startAngle = -90f + (360f * lentRatio),
                    sweepAngle = sweepAngle.value * (1f - lentRatio),
                    useCenter = false,
                    topLeft = topLeft,
                    size = size,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Center Text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(id = R.string.net_balance),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF64748B)
            )
            val netBalance = totalLent - totalBorrowed
            val color = if (netBalance >= 0) VibrantTeal else CoralRose
            Text(
                text = "${if (netBalance >= 0) "+" else "-"}${currencySymbol}${String.format("%.0f", Math.abs(netBalance))}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
fun LineChart(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(id = R.string.no_activity), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    val maxVal = dataPoints.maxOrNull() ?: 1f
    val minVal = dataPoints.minOrNull() ?: 0f
    val range = (maxVal - minVal).coerceAtLeast(1f)
    
    val animationProgress = remember { Animatable(0f) }
    
    LaunchedEffect(dataPoints) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    val path = remember { Path() }
    val fillPath = remember { Path() }

    Surface(
        modifier = modifier.fillMaxWidth().height(120.dp),
        color = Color.White.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val width = size.width
            val height = size.height
            val stepX = if (dataPoints.size > 1) width / (dataPoints.size - 1) else width

            path.reset()
            fillPath.reset()

            var previousX = 0f
            var previousY = height - ((dataPoints.first() - minVal) / range) * height

            path.moveTo(previousX, previousY)
            fillPath.moveTo(previousX, height)
            fillPath.lineTo(previousX, previousY)

            for (i in 1 until dataPoints.size) {
                val x = i * stepX
                val y = height - ((dataPoints[i] - minVal) / range) * height
                
                // Calculate control points for smooth bezier curve
                val controlPointX = (x + previousX) / 2
                
                path.cubicTo(
                    controlPointX, previousY,
                    controlPointX, y,
                    x, y
                )
                
                fillPath.cubicTo(
                    controlPointX, previousY,
                    controlPointX, y,
                    x, y
                )

                previousX = x
                previousY = y
            }
            
            fillPath.lineTo(previousX, height)
            fillPath.close()

            clipRect(right = width * animationProgress.value) {
                // Draw gradient fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(CyberIndigo.copy(alpha = 0.2f), Color.Transparent),
                        startY = 0f,
                        endY = height
                    )
                )

                // Draw line stroke
                drawPath(
                    path = path,
                    color = CyberIndigo,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            
            // Draw end point dot if animation is complete
            if (animationProgress.value == 1f && dataPoints.isNotEmpty()) {
                val lastX = (dataPoints.size - 1) * stepX
                val lastY = height - ((dataPoints.last() - minVal) / range) * height
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(lastX, lastY)
                )
                drawCircle(
                    color = CyberIndigo,
                    radius = 2.dp.toPx(),
                    center = Offset(lastX, lastY)
                )
            }
        }
    }
}
