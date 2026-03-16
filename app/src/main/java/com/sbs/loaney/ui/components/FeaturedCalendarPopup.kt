package com.sbs.loaney.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbs.loaney.data.model.CalendarEvent
import com.sbs.loaney.data.model.CalendarEventType
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturedCalendarPopup(
    visible: Boolean,
    onDismiss: () -> Unit,
    allEvents: Map<String, List<CalendarEvent>>,
    currencySymbol: String,
    onNavigateToDetail: (Long) -> Unit
) {
    if (!visible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        CalendarContent(
            allEvents = allEvents,
            currencySymbol = currencySymbol,
            onNavigateToDetail = onNavigateToDetail,
            onClose = onDismiss
        )
    }
}

@Composable
fun CalendarContent(
    allEvents: Map<String, List<CalendarEvent>>,
    currencySymbol: String,
    onNavigateToDetail: (Long) -> Unit,
    onClose: () -> Unit
) {
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Loan Tracker Calendar",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "Deadlines, Initiations & Payments",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- MONTH SELECTOR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                currentMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev")
            }
            Text(
                text = monthFormat.format(currentMonth.time).uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            IconButton(onClick = {
                currentMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- MONTHLY GRID ---
        MonthGrid(
            currentMonth = currentMonth,
            selectedDate = selectedDate,
            allEvents = allEvents,
            onDateSelected = { selectedDate = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- SELECTED DATE EVENTS ---
        val selectedDateStr = dateFormat.format(selectedDate.time)
        val dayEvents = allEvents[selectedDateStr] ?: emptyList()

        Text(
            text = "Activity for ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate.time)}",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (dayEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No tracks scheduled for today",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(dayEvents) { event ->
                    CalendarEventCard(
                        event = event,
                        currencySymbol = currencySymbol,
                        onClick = { onNavigateToDetail(event.loanId) }
                    )
                }
            }
        }
    }
}

@Composable
fun MonthGrid(
    currentMonth: Calendar,
    selectedDate: Calendar,
    allEvents: Map<String, List<CalendarEvent>>,
    onDateSelected: (Calendar) -> Unit
) {
    val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (currentMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK)
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val rows = (daysInMonth + firstDayOfWeek - 2) / 7 + 1
        for (i in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (j in 1..7) {
                    val dayOfMonth = i * 7 + j - firstDayOfWeek + 1
                    if (dayOfMonth in 1..daysInMonth) {
                        val dayCal = (currentMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, dayOfMonth) }
                        val dateStr = dateFormat.format(dayCal.time)
                        val isSelected = dateFormat.format(dayCal.time) == dateFormat.format(selectedDate.time)
                        val dayEvents = allEvents[dateStr] ?: emptyList()

                        DayCell(
                            day = dayOfMonth.toString(),
                            isSelected = isSelected,
                            events = dayEvents,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            onClick = { onDateSelected(dayCal) }
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(
    day: String,
    isSelected: Boolean,
    events: List<CalendarEvent>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) CyberIndigo else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                )
            )
            if (events.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    // Maximum 3 dots
                    events.take(3).forEach { event ->
                        val color = when (event.type) {
                            CalendarEventType.LOAN_INITIATION -> VibrantTeal
                            CalendarEventType.DEADLINE -> CoralRose
                            CalendarEventType.PARTIAL_PAYMENT -> Color(0xFF3B82F6) // Bright Blue
                        }
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(if (isSelected) Color.White else color, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarEventCard(
    event: CalendarEvent,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val (color, icon, label) = when (event.type) {
        CalendarEventType.LOAN_INITIATION -> Triple(VibrantTeal, "Initiated", if (event.loanType == LoanType.LEND) "Lent to" else "Borrowed from")
        CalendarEventType.DEADLINE -> Triple(CoralRose, "Deadline", "Payback due from")
        CalendarEventType.PARTIAL_PAYMENT -> Triple(Color(0xFF3B82F6), "Payment", "Partial Payment for")
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = event.personName.firstOrNull()?.toString()?.uppercase() ?: "",
                    style = MaterialTheme.typography.titleMedium.copy(color = color, fontWeight = FontWeight.Black)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$label ${event.personName}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                )
                Text(
                    text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(event.date),
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }

            Text(
                text = "$currencySymbol${String.format("%,.0f", event.amount)}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = if (event.loanType == LoanType.LEND) Color(0xFF10B981) else Color(0xFFF43F5E)
                )
            )
        }
    }
}
