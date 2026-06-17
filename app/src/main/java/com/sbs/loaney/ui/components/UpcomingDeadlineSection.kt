package com.sbs.loaney.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbs.loaney.R
import com.sbs.loaney.data.local.dao.LoanWithPayments
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UpcomingDeadlineSection(
    deadlines: List<LoanWithPayments>,
    currencySymbol: String,
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
    // Generate dates for the strip (today + 14 days)
    val dates = remember {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val list = mutableListOf<Date>()
        for (i in 0 until 14) {
            val d = today.clone() as Calendar
            d.add(Calendar.DAY_OF_YEAR, i)
            list.add(d.time)
        }
        list
    }

    val today = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
    }

    // Count deadlines within next 7 days for the badge
    val weekDeadlineCount = remember(deadlines) {
        val limit = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 7)
        }.time
        deadlines.count { it.loan.promisedReturnDate.before(limit) }
    }

    val filteredDeadlines = deadlines.filter { item ->
        val loanCal = Calendar.getInstance().apply { time = item.loan.promisedReturnDate }
        val selCal = Calendar.getInstance().apply { time = selectedDate }
        loanCal.get(Calendar.YEAR) == selCal.get(Calendar.YEAR) &&
        loanCal.get(Calendar.DAY_OF_YEAR) == selCal.get(Calendar.DAY_OF_YEAR)
    }

    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dateFormat = SimpleDateFormat("dd", Locale.getDefault())
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header row with title + week-count badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(id = R.string.upcoming_deadlines),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp
                    )
                )
                if (weekDeadlineCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "$weekDeadlineCount this week",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Month label for the visible range
        Text(
            text = monthYearFormat.format(selectedDate),
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        )

        // Calendar Date Strip
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(dates) { date ->
                val dateCal = Calendar.getInstance().apply { time = date }
                val isSelected = dateCal.get(Calendar.DAY_OF_YEAR) == 
                    Calendar.getInstance().apply { time = selectedDate }.get(Calendar.DAY_OF_YEAR)
                val isToday = dateCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    dateCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

                val dayDeadlines = deadlines.filter { item ->
                    val loanCal = Calendar.getInstance().apply { time = item.loan.promisedReturnDate }
                    loanCal.get(Calendar.YEAR) == dateCal.get(Calendar.YEAR) &&
                    loanCal.get(Calendar.DAY_OF_YEAR) == dateCal.get(Calendar.DAY_OF_YEAR)
                }
                val hasLent = dayDeadlines.any { it.loan.type == LoanType.LEND }
                val hasBorrowed = dayDeadlines.any { it.loan.type == LoanType.BORROW }

                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(76.dp)
                        .background(
                            if (isSelected) CyberIndigo else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 0.5.dp,
                            color = if (isSelected) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onDateSelected(date) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isToday) "TODAY" else dayFormat.format(date).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = if (isSelected) PureWhite.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = dateFormat.format(date),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (isSelected) PureWhite else MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (dayDeadlines.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (hasLent) Box(modifier = Modifier.size(4.dp).background(if (isSelected) PureWhite else VibrantTeal, CircleShape))
                                if (hasBorrowed) Box(modifier = Modifier.size(4.dp).background(if (isSelected) PureWhite else CoralRose, CircleShape))
                            }
                        }
                    }
                }
            }
        }

        // Content for the selected date
        AnimatedContent(
            targetState = selectedDate to filteredDeadlines.isEmpty(),
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                    scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                    .togetherWith(fadeOut(animationSpec = tween(90)))
            },
            label = "DeadlineContentAnimation"
        ) { (currentDate, isEmpty) ->
            if (isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldGreen.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "All clear — no deadlines here!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    filteredDeadlines.forEach { item ->
                        UpcomingDeadlineCard(
                            item = item,
                            currencySymbol = currencySymbol,
                            selectedDate = currentDate,
                            onClick = { onNavigateToDetail(item.loan.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpcomingDeadlineCard(
    item: LoanWithPayments,
    currencySymbol: String,
    selectedDate: Date,
    onClick: () -> Unit
) {
    val isLent = item.loan.type == LoanType.LEND
    val totalLoan = item.loan.amount + item.loanItems.sumOf { it.amount }
    val paid = item.payments.sumOf { it.amount }
    val balance = (totalLoan - paid).coerceAtLeast(0.0)
    val progressFraction = if (totalLoan > 0) (paid / totalLoan).coerceIn(0.0, 1.0).toFloat() else 0f

    val accentColor = if (isLent) EmeraldGreen else CoralRose

    // Urgency: days until deadline from today
    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val deadlineCal = Calendar.getInstance().apply { time = item.loan.promisedReturnDate }
    val daysUntil = ((deadlineCal.timeInMillis - todayCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
    val urgencyLabel = when {
        daysUntil == 0 -> "Due Today"
        daysUntil == 1 -> "Due Tomorrow"
        daysUntil > 1 -> "Due in $daysUntil days"
        else -> "Overdue"
    }
    val urgencyColor = when {
        daysUntil == 0 -> CoralRose
        daysUntil == 1 -> MaterialTheme.colorScheme.tertiary
        daysUntil > 1 -> MaterialTheme.colorScheme.primary
        else -> CoralRose
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp, 
                shape = RoundedCornerShape(24.dp), 
                spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(accentColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isLent) Icons.AutoMirrored.Filled.CallReceived else Icons.Default.ArrowOutward,
                        null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.loan.personName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )
                    Text(
                        text = urgencyLabel,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = urgencyColor)
                    )
                }
                Text(
                    text = "$currencySymbol${String.format("%,.0f", balance)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (progressFraction >= 1f) VibrantTeal else accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
