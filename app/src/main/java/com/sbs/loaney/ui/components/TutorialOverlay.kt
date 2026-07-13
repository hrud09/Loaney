package com.sbs.loaney.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.sbs.loaney.ui.theme.AlimGreen
import com.sbs.loaney.ui.theme.AlimWhite

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
    // Guard against the step list shrinking underneath us.
    val stepIndex = currentStepIndex.coerceIn(0, steps.lastIndex)
    val currentStep = steps[stepIndex]

    val density = LocalDensity.current

    val targetRect: Rect? = currentStep.targetCoordinates?.let { coords ->
        val pos = coords.positionInRoot()
        val size = coords.size
        Rect(
            pos.x - currentStep.padding,
            pos.y - currentStep.padding,
            pos.x + size.width + currentStep.padding,
            pos.y + size.height + currentStep.padding
        )
    }

    // Animatables rather than animateFloatAsState: the first target must be snapped to,
    // otherwise the spotlight visibly flies in from the top-left corner on the first step.
    val left = remember { Animatable(targetRect?.left ?: 0f) }
    val top = remember { Animatable(targetRect?.top ?: 0f) }
    val right = remember { Animatable(targetRect?.right ?: 0f) }
    val bottom = remember { Animatable(targetRect?.bottom ?: 0f) }
    var hasPositioned by remember { mutableStateOf(targetRect != null) }

    LaunchedEffect(targetRect) {
        val r = targetRect ?: return@LaunchedEffect
        if (!hasPositioned) {
            left.snapTo(r.left); top.snapTo(r.top)
            right.snapTo(r.right); bottom.snapTo(r.bottom)
            hasPositioned = true
        } else {
            val spec = tween<Float>(durationMillis = 320)
            left.animateTo(r.left, spec); top.animateTo(r.top, spec)
            right.animateTo(r.right, spec); bottom.animateTo(r.bottom, spec)
        }
    }

    fun goNext() {
        if (stepIndex < steps.lastIndex) currentStepIndex = stepIndex + 1 else onComplete()
    }

    fun goBack() {
        if (stepIndex > 0) currentStepIndex = stepIndex - 1
    }

    BackHandler(enabled = true) {
        if (stepIndex > 0) goBack() else onComplete()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            // Swallow every tap. Without this the scrim is purely cosmetic and the user can
            // tap the buttons underneath it while the tutorial is up.
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        val screenHeight = maxHeight

        Canvas(modifier = Modifier.fillMaxSize()) {
            val spotlight = Path().apply {
                if (targetRect != null && hasPositioned) {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(left.value, top.value, right.value, bottom.value),
                            cornerRadius = CornerRadius(20.dp.toPx())
                        )
                    )
                }
            }

            clipPath(spotlight, clipOp = ClipOp.Difference) {
                drawRect(color = Color.Black.copy(alpha = 0.75f))
            }

            if (targetRect != null && hasPositioned) {
                drawRoundRect(
                    color = AlimGreen,
                    topLeft = Offset(left.value, top.value),
                    size = Size(right.value - left.value, bottom.value - top.value),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Keep the card clear of the thing it is describing.
            val alignment = if (targetRect == null) {
                Alignment.Center
            } else {
                val targetIsLow = top.value > with(density) { screenHeight.toPx() } / 2
                if (targetIsLow) Alignment.TopCenter else Alignment.BottomCenter
            }

            TutorialCard(
                step = currentStep,
                stepNumber = stepIndex + 1,
                totalSteps = steps.size,
                modifier = Modifier.align(alignment),
                onNext = ::goNext,
                onBack = ::goBack,
                onSkip = onComplete
            )
        }
    }
}

@Composable
private fun TutorialCard(
    step: TutorialStep,
    stepNumber: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    val isFirst = stepNumber == 1
    val isLast = stepNumber == totalSteps

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepDots(
                    total = totalSteps,
                    current = stepNumber - 1,
                    modifier = Modifier.weight(1f)
                )

                if (!isFirst) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous step",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }

                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlimGreen,
                        contentColor = AlimWhite
                    )
                ) {
                    Text(
                        text = if (isLast) "Done" else "Next",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!isLast) {
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        "Skip tour",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StepDots(total: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val active = index == current
            Box(
                modifier = Modifier
                    .size(width = if (active) 20.dp else 8.dp, height = 8.dp)
                    .background(
                        color = if (active) AlimGreen else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    )
            )
        }
    }
}
