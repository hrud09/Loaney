package com.sbs.loaney.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.sbs.loaney.ui.theme.AlimDark
import com.sbs.loaney.ui.theme.AlimGreen

data class TutorialStep(
    val title: String,
    val description: String,
    val targetCoordinates: LayoutCoordinates? = null,
    val padding: Float = 16f
)

@Composable
fun TutorialOverlay(
    steps: List<TutorialStep>,
    isVisible: Boolean,
    onComplete: () -> Unit
) {
    if (!isVisible || steps.isEmpty()) return

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val currentStep = steps.getOrNull(currentStepIndex) ?: return
    
    val density = LocalDensity.current

    // Animate target position for smooth transition between steps
    val targetRect by remember(currentStep) {
        derivedStateOf {
            currentStep.targetCoordinates?.let { coords ->
                val pos = coords.positionInRoot()
                val size = coords.size
                Rect(
                    pos.x - currentStep.padding,
                    pos.y - currentStep.padding,
                    pos.x + size.width + currentStep.padding,
                    pos.y + size.height + currentStep.padding
                )
            }
        }
    }

    val animatedLeft by animateFloatAsState(targetValue = targetRect?.left ?: 0f, label = "left")
    val animatedTop by animateFloatAsState(targetValue = targetRect?.top ?: 0f, label = "top")
    val animatedRight by animateFloatAsState(targetValue = targetRect?.right ?: 0f, label = "right")
    val animatedBottom by animateFloatAsState(targetValue = targetRect?.bottom ?: 0f, label = "bottom")

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
    ) {
        val screenHeight = maxHeight
        
        // Spotlight Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spotlightPath = Path().apply {
                if (targetRect != null) {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(animatedLeft, animatedTop, animatedRight, animatedBottom),
                            cornerRadius = CornerRadius(16.dp.toPx())
                        )
                    )
                }
            }

            // Draw overlay with hole
            clipPath(spotlightPath, clipOp = ClipOp.Difference) {
                drawRect(color = Color.Black.copy(alpha = 0.7f))
            }
            
            // Draw spotlight border
            if (targetRect != null) {
                drawRoundRect(
                    color = AlimGreen,
                    topLeft = Offset(animatedLeft, animatedTop),
                    size = Size(animatedRight - animatedLeft, animatedBottom - animatedTop),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Instruction Card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            val boxModifier = if (targetRect != null) {
                val isTargetInBottomHalf = animatedTop > with(density) { screenHeight.toPx() } / 2
                Modifier.align(if (isTargetInBottomHalf) Alignment.TopCenter else Alignment.BottomCenter)
            } else {
                Modifier.align(Alignment.Center)
            }

            TutorialCard(
                title = currentStep.title,
                description = currentStep.description,
                currentStep = currentStepIndex + 1,
                totalSteps = steps.size,
                modifier = boxModifier,
                onNext = {
                    if (currentStepIndex < steps.size - 1) {
                        currentStepIndex++
                    } else {
                        onComplete()
                    }
                },
                onSkip = onComplete
            )
        }
    }
}

@Composable
fun TutorialCard(
    title: String,
    description: String,
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Step $currentStep of $totalSteps",
                        style = MaterialTheme.typography.labelMedium,
                        color = AlimGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = AlimDark,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                IconButton(onClick = onSkip) {
                    Icon(Icons.Default.Close, contentDescription = "Skip", tint = AlimDark.copy(alpha = 0.4f))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = AlimDark.copy(alpha = 0.7f),
                lineHeight = 22.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AlimGreen)
            ) {
                Text(
                    text = if (currentStep == totalSteps) "Got it!" else "Next",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
