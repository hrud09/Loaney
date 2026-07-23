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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.sbs.loaney.R
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
                if (targetRect != null) {
                    addRoundRect(
                        RoundRect(
                            rect = targetRect,
                            cornerRadius = CornerRadius(20.dp.toPx())
                        )
                    )
                }
            }

            clipPath(spotlight, clipOp = ClipOp.Difference) {
                drawRect(color = Color.Black.copy(alpha = 0.75f))
            }

            if (targetRect != null) {
                drawRoundRect(
                    color = AlimGreen,
                    topLeft = Offset(targetRect.left, targetRect.top),
                    size = Size(targetRect.width, targetRect.height),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            if (targetRect == null) {
                TutorialCard(
                    step = currentStep,
                    stepNumber = stepIndex + 1,
                    totalSteps = steps.size,
                    modifier = Modifier.align(Alignment.Center),
                    onNext = ::goNext,
                    onBack = ::goBack,
                    onSkip = onComplete
                )
            } else {
                val targetCenterY = (targetRect.top + targetRect.bottom) / 2f
                val screenHeightPx = with(density) { screenHeight.toPx() }
                val showBelow = targetCenterY < screenHeightPx * 0.55f

                if (showBelow) {
                    val topMarginDp = with(density) { (targetRect.bottom + 12f).toDp() }.coerceIn(20.dp, screenHeight - 200.dp)
                    TutorialCard(
                        step = currentStep,
                        stepNumber = stepIndex + 1,
                        totalSteps = steps.size,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = topMarginDp),
                        onNext = ::goNext,
                        onBack = ::goBack,
                        onSkip = onComplete
                    )
                } else {
                    val bottomMarginDp = with(density) { (screenHeightPx - targetRect.top + 12f).toDp() }.coerceIn(20.dp, screenHeight - 200.dp)
                    TutorialCard(
                        step = currentStep,
                        stepNumber = stepIndex + 1,
                        totalSteps = steps.size,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = bottomMarginDp),
                        onNext = ::goNext,
                        onBack = ::goBack,
                        onSkip = onComplete
                    )
                }
            }
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
                        text = if (isLast) stringResource(id = R.string.tutorial_done) else stringResource(id = R.string.tutorial_next),
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
                        stringResource(id = R.string.tutorial_skip_tour),
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
