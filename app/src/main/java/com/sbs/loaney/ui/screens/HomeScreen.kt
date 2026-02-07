package com.sbs.loaney.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbs.loaney.data.local.dao.LoanWithPayments
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.ui.theme.AccentYellow
import com.sbs.loaney.ui.theme.PrimaryLime
import com.sbs.loaney.ui.theme.SecondaryOrange
import com.sbs.loaney.ui.theme.TertiaryRed
import com.sbs.loaney.ui.viewmodel.HomeViewModel
import com.sbs.loaney.ui.viewmodel.LoanTrackerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToManage: () -> Unit,
    onNavigateToAddLoan: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    trackerViewModel: LoanTrackerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedLoanIdForPayment by remember { mutableStateOf<Long?>(null) }
    var loanToDelete by remember { mutableStateOf<LoanWithPayments?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
             // Custom Floating Bottom Bar handled in MainScreen, but we add spacing here
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header - Anchored Top Left, moved more upward
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp), // Reduced from 16.dp
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Good Morning,",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                    Text(
                        text = "Manage Loans",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = { /* TODO: Profile/Settings */ },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White)
                }
            }

            // Summary Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryCard(
                    title = "Total Lent",
                    amount = uiState.totalLent,
                    gradient = Brush.linearGradient(
                        colors = listOf(Color(0xFFE8F5E9), Color(0xFFC5E1A5)) // Light pastel green to herbal green
                    ),
                    textColor = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Total Borrowed",
                    amount = uiState.totalBorrowed,
                    gradient = Brush.linearGradient(
                        colors = listOf(Color(0xFFFFF3E0), Color(0xFFFFB74D)) // Light apricot to standard orange
                    ),
                    textColor = Color.Black,
                    modifier = Modifier.weight(1f)
                )
            }

            // Add Loan Button
            Button(
                onClick = onNavigateToAddLoan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryLime,
                    contentColor = Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Loan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            // Section: Recent Activity
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (uiState.lentLoans.isEmpty() && uiState.borrowedLoans.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No loans yet. Tap + to add one.", color = Color.Gray)
                }
            } else {
                // Combine and show
                val allLoans = (uiState.lentLoans + uiState.borrowedLoans).sortedByDescending { it.loan.loanDate }
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    allLoans.take(10).forEach { item ->
                        // Distinguish color by LoanType
                        val cardColor = if (item.loan.type == LoanType.LEND) PrimaryLime else SecondaryOrange
                        val textColor = Color.Black

                        SwipeableLoanItem(
                            item = item,
                            backgroundColor = cardColor,
                            contentColor = textColor,
                            onClick = { onNavigateToDetail(item.loan.id) },
                            onSwipeLeft = { selectedLoanIdForPayment = item.loan.id },
                            onSwipeRight = { loanToDelete = item }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (selectedLoanIdForPayment != null) {
        trackerViewModel.selectLoan(selectedLoanIdForPayment!!)
        AddPaymentBottomSheet(
            onDismiss = { selectedLoanIdForPayment = null },
            onAddPayment = { amount, method, note, proofUri ->
                trackerViewModel.addPayment(amount, method, note, proofUri)
                selectedLoanIdForPayment = null
            }
        )
    }

    if (loanToDelete != null) {
        AlertDialog(
            onDismissRequest = { loanToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = TertiaryRed) },
            title = { Text("Delete Loan?") },
            text = { Text("Are you sure you want to delete the loan for ${loanToDelete?.loan?.personName}? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        loanToDelete?.loan?.let { trackerViewModel.deleteLoan(it) }
                        loanToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = TertiaryRed)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { loanToDelete = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = Color.White,
            textContentColor = Color.Gray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableLoanItem(
    item: LoanWithPayments,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onSwipeLeft()
                    false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onSwipeRight()
                    false
                }
                else -> false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.4f }
    )

    SwipeToDismissBox(
        state = swipeState,
        enableDismissFromStartToEnd = true,
        backgroundContent = {
            val direction = swipeState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> TertiaryRed
                SwipeToDismissBoxValue.EndToStart -> PrimaryLime
                else -> Color.Transparent
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Delete
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Add
                else -> Icons.Default.Add
            }
            val label = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> "Delete"
                SwipeToDismissBoxValue.EndToStart -> "Add Payment"
                else -> ""
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(color),
                contentAlignment = alignment
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (direction == SwipeToDismissBoxValue.StartToEnd) {
                        Icon(icon, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Text(label, color = Color.Black, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(icon, contentDescription = null, tint = Color.Black)
                    }
                }
            }
        }
    ) {
        HomeLoanItem(
            item = item,
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            onClick = onClick
        )
    }
}

@Composable
fun SummaryCard(
    title: String,
    amount: Double,
    gradient: Brush,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.AttachMoney, contentDescription = null, tint = textColor)
                }
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "৳${String.format("%.0f", amount)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun HomeLoanItem(
    item: LoanWithPayments,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    val paid = item.payments.sumOf { it.amount }
    val balance = item.loan.amount - paid
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    val currentTime = System.currentTimeMillis()
    val dueDate = item.loan.promisedReturnDate
    val threeDaysInMillis = 3 * 24 * 60 * 60 * 1000L
    
    val isOverdue = currentTime > dueDate.time && item.loan.status != LoanStatus.FULLY_PAID
    val isDueSoon = (dueDate.time - currentTime) in 0..threeDaysInMillis
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.loan.personName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.loan.personName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Type Tag
                        Surface(
                            color = Color.Black.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.height(20.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = if (item.loan.type == LoanType.LEND) "Lent" else "Borrowed",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = contentColor
                                )
                            }
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = contentColor.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Balance: ৳${String.format("%.0f", balance)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (isDueSoon) Color(0xFFFF9800) else contentColor.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Due: ${dateFormat.format(dueDate)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isDueSoon) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (isDueSoon) Color(0xFFFF9800) else contentColor.copy(alpha = 0.6f)
                        )
                    }
                }

                // Placeholder for alignment if needed, though Box covers TopEnd
                Spacer(modifier = Modifier.width(60.dp))
            }

            // Status Chip - Dynamic based on state
            val chipBgColor = if (isOverdue) TertiaryRed.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f)
            val chipTextColor = if (isOverdue) TertiaryRed else if (item.loan.status == LoanStatus.FULLY_PAID) Color(0xFF4CAF50) else Color(0xFF2E7D32)
            val statusText = if (isOverdue) "Overdue" else if (item.loan.status == LoanStatus.FULLY_PAID) "Paid" else "Active"

            Surface(
                color = chipBgColor,
                shape = RoundedCornerShape(bottomStart = 16.dp, topEnd = 24.dp), // Styled to fit corner
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                 Box(
                     contentAlignment = Alignment.Center,
                     modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                 ) {
                     Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = chipTextColor
                     )
                 }
            }
        }
    }
}
