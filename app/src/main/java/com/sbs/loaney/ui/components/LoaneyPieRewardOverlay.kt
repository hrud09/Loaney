package com.sbs.loaney.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val GoldPie    = Color(0xFFFFC107)
private val GoldDeep   = Color(0xFFFF8F00)
private val OverlayBg  = Color(0x99000000)  // 60% black scrim

/**
 * Full-screen semi-transparent overlay that pops up whenever the user earns
 * Loaney Pie points. Self-dismisses after 2.2 seconds.
 *
 * @param pointsEarned   Points to display in the reward message.
 * @param isVisible      Drive visibility from the parent.
 * @param onAnimationComplete Called after the auto-dismiss delay; use it to reset
 *                            [isVisible] to false in the parent.
 */
@Composable
fun LoaneyPieRewardOverlay(
    pointsEarned: Int,
    isVisible: Boolean,
    onAnimationComplete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope  = rememberCoroutineScope()

    // ── Animatable scale for the card pop ─────────────────────────
    val cardScale = remember { Animatable(0f) }

    // ── Icon secondary "heartbeat" pulse once settled ──────────────
    val iconPulse = remember { Animatable(1f) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            // 1. Haptic burst — triple tap for rewarding feel
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(80)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(80)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

            // 2. Scale card in with overshoot bounce
            cardScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMedium
                )
            )

            // 3. Subtle icon pulse
            repeat(2) {
                iconPulse.animateTo(1.18f, tween(160, easing = FastOutSlowInEasing))
                iconPulse.animateTo(1.00f, tween(160, easing = FastOutSlowInEasing))
            }

            // 4. Wait, then collapse and signal parent
            delay(1_600)
            cardScale.animateTo(
                targetValue = 0f,
                animationSpec = tween(260, easing = FastOutLinearInEasing)
            )
            onAnimationComplete()
        } else {
            // Reset if hidden externally
            cardScale.snapTo(0f)
            iconPulse.snapTo(1f)
        }
    }

    if (isVisible || cardScale.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OverlayBg),
            contentAlignment = Alignment.Center
        ) {
            // ── Reward card ────────────────────────────────────────
            Card(
                modifier = Modifier
                    .scale(cardScale.value)
                    .widthIn(max = 300.dp)
                    .padding(horizontal = 32.dp),
                shape  = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D2E)),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF252840), Color(0xFF1A1D2E))
                            )
                        )
                        .padding(vertical = 36.dp, horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // Glowing gold icon
                    Box(
                        modifier = Modifier
                            .scale(iconPulse.value)
                            .size(80.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        GoldPie.copy(alpha = 0.35f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Stars,
                            contentDescription = "Loaney Pie",
                            tint               = GoldPie,
                            modifier           = Modifier.size(56.dp)
                        )
                    }

                    // Points text
                    Text(
                        text       = "+$pointsEarned",
                        fontSize   = 52.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = GoldPie,
                        lineHeight = 52.sp
                    )

                    Text(
                        text       = "Loaney Pies!",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )

                    // Sub-label
                    Text(
                        text      = "Great job paying on time 🎉",
                        fontSize  = 13.sp,
                        color     = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(4.dp))

                    // Thin gold divider accent
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(3.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, GoldPie, Color.Transparent)
                                ),
                                RoundedCornerShape(50)
                            )
                    )
                }
            }
        }
    }
}
