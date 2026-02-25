package com.sbs.loaney.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbs.loaney.data.local.dao.LoanWithPayments
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.ui.components.DonutChart
import com.sbs.loaney.ui.theme.CoralRed
import com.sbs.loaney.ui.theme.NeonLime
import com.sbs.loaney.ui.theme.SkyBlue
import com.sbs.loaney.ui.theme.SurfaceDark
import com.sbs.loaney.ui.theme.SurfaceElevated
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
    var loanToDelete by remember { mutableStateOf<Long?>(null) }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val allLoans = (uiState.lentLoans + uiState.borrowedLoans).sortedByDescending { it.loan.loanDate }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp) // Increased spacing
        ) {
            item {
                Text(
                    text = "Welcome Back!",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // --- New Graph Header section ---
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = SurfaceDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Financial Overview",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Pie Chart
                        DonutChart(
                            totalLent = uiState.totalLent,
                            totalBorrowed = uiState.totalBorrowed,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        
                        // Mini Legend
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NeonLime))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lent", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CoralRed))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Borrowed", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // --- Summary Cards ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCardDark(
                        title = "Total Lent",
                        amount = uiState.totalLent,
                        accentColor = NeonLime,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCardDark(
                        title = "Total Borrowed",
                        amount = uiState.totalBorrowed,
                        accentColor = SkyBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                // Add Loan Button
                Button(
                    onClick = onNavigateToAddLoan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonLime,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Loan", style = MaterialTheme.typography.titleMedium)
                }
            }

            item {
                // Section: Recent Activity
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }

            if (allLoans.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No loans yet. Tap + to add one.", color = Color.Gray)
                    }
                }
            } else {
                items(allLoans.take(10), key = { it.loan.id }) { item ->
                    val accentColor = if (item.loan.type == LoanType.LEND) NeonLime else SkyBlue
                    
                    SwipeableLoanItem(
                        item = item,
                        accentColor = accentColor,
                        onClick = { onNavigateToDetail(item.loan.id) },
                        onSwipeLeft = { selectedLoanIdForPayment = item.loan.id },
                        onSwipeRight = { loanToDelete = item.loan.id }
                    )
                }
            }
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
            containerColor = SurfaceDark,
            titleContentColor = Color.White,
            textContentColor = Color.Gray,
            onDismissRequest = { loanToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = CoralRed) },
            title = { Text("Delete Loan?") },
            text = { Text("Are you sure you want to delete this loan? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val loan = (uiState.lentLoans + uiState.borrowedLoans).find { it.loan.id == loanToDelete }
                        loan?.loan?.let { trackerViewModel.deleteLoan(it) }
                        loanToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = CoralRed)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { loanToDelete = null }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableLoanItem(
    item: LoanWithPayments,
    accentColor: Color,
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
        }
    )

    SwipeToDismissBox(
        state = swipeState,
        enableDismissFromStartToEnd = true,
        backgroundContent = {
            val direction = swipeState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> CoralRed
                SwipeToDismissBoxValue.EndToStart -> NeonLime
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color),
                contentAlignment = alignment
            ) {
                Icon(
                    icon, 
                    contentDescription = null, 
                    tint = if (direction == SwipeToDismissBoxValue.StartToEnd) Color.White else Color.Black,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    ) {
        HomeLoanItemDark(
            item = item,
            accentColor = accentColor,
            onClick = onClick
        )
    }
}

@Composable
fun SummaryCardDark(
    title: String,
    amount: Double,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(130.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Accent Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(accentColor)
            )
            
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(accentColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.AttachMoney, contentDescription = null, tint = accentColor)
                }
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = "৳${String.format("%.0f", amount)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun HomeLoanItemDark(
    item: LoanWithPayments,
    accentColor: Color,
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Left Accent Stripe
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .background(accentColor)
            )

            Row(
                modifier = Modifier
                    .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(SurfaceElevated, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.loan.personName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = accentColor
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.loan.personName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (item.loan.type == LoanType.LEND) "Lent" else "Borrowed",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor
                        )
                        Text(
                            text = " • Due: ${dateFormat.format(dueDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDueSoon) CoralRed else Color.Gray
                        )
                    }
                }
                
                // Trailing amount
                Text(
                    text = "৳${String.format("%.0f", balance)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }

            // Status Badge (Overdue)
            if (isOverdue) {
                Surface(
                    color = CoralRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(bottomStart = 12.dp, topEnd = 20.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                     Text(
                        text = "Overdue",
                        style = MaterialTheme.typography.labelSmall,
                        color = CoralRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                     )
                }
            } else if (item.loan.status == LoanStatus.FULLY_PAID) {
                Surface(
                    color = NeonLime.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(bottomStart = 12.dp, topEnd = 20.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                     Text(
                        text = "Paid",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonLime,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                     )
                }
            }
        }
    }
}
