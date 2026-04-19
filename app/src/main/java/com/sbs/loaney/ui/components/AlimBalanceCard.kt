package com.sbs.loaney.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbs.loaney.R
import com.sbs.loaney.ui.theme.*

@Composable
fun AlimBalanceCard(
    balance: Double,
    currencySymbol: String,
    onNavigateToAddLoan: (String) -> Unit,
    onNavigateToHistory: (String?) -> Unit,
    onNavigateToHistoryScreen: () -> Unit,
    onReportClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onPositionedCalendar: (LayoutCoordinates) -> Unit = {},
    onPositionedQuickActions: (LayoutCoordinates) -> Unit = {},
    onPositionedReport: (LayoutCoordinates) -> Unit = {}
) {
    var isBalanceVisible by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxWidth()) {
        // Background Split Layer: Top half black (AlimDark), bottom half cream (AlimCream)
        Column(modifier = Modifier.matchParentSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(AlimDark))
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(AlimCream))
        }

        // Foreground Content Layer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // The Emerald Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AlimGreen, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(id = R.string.current_balance),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = AlimWhite.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Balance",
                                tint = AlimWhite.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { isBalanceVisible = !isBalanceVisible }
                            )
                        }
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "Calendar",
                            tint = AlimWhite,
                            modifier = Modifier
                                .size(24.dp)
                                .onGloballyPositioned { onPositionedCalendar(it) }
                                .clickable { onCalendarClick() }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    AnimatedContent(
                        targetState = isBalanceVisible,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                                .togetherWith(fadeOut(animationSpec = tween(90)))
                        },
                        label = "BalanceAnimation"
                    ) { visible ->
                        Text(
                            text = if (visible) "$currencySymbol${String.format("%,.0f", balance)}" else "****",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = AlimWhite,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 32.sp
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Quick Actions Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { onPositionedQuickActions(it) },
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AlimCardAction(Icons.Default.Add, stringResource(id = R.string.lend), onClick = { onNavigateToAddLoan("LEND") })
                        AlimCardAction(Icons.Default.Remove, stringResource(id = R.string.borrow), onClick = { onNavigateToAddLoan("BORROW") })
                        AlimCardAction(Icons.Default.History, stringResource(id = R.string.history), onClick = onNavigateToHistoryScreen)
                        Box(modifier = Modifier.onGloballyPositioned { onPositionedReport(it) }) {
                            AlimCardAction(Icons.Default.BarChart, stringResource(id = R.string.report), onClick = onReportClick)
                        }
                    }
                }
            }
            
            // Bottom spacing on the cream background
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AlimCardAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.bounceClick { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(AlimWhite.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = AlimWhite,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = AlimWhite,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
