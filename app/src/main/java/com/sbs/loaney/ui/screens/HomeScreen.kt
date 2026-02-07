package com.sbs.loaney.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
    var loanToDelete by remember { mutableStateOf<Long?>(null) }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Manage Loans",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Open Drawer */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { /* TODO: Profile */ },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                )
            )
        }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Summary Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SummaryCard(
                        title = "Total Lent",
                        amount = uiState.totalLent,
                        gradient = Brush.linearGradient(
                            colors = listOf(Color(0xFFE8F5E9), Color(0xFFC5E1A5))
                        ),
                        textColor = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Total Borrowed",
                        amount = uiState.totalBorrowed,
                        gradient = Brush.linearGradient(
                            colors = listOf(Color(0xFFFFF3E0), Color(0xFFFFB74D))
                        ),
                        textColor = Color.Black,
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
                        containerColor = PrimaryLime,
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
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            if (allLoans.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No loans yet. Tap + to add one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(allLoans.take(10), key = { it.loan.id }) { item ->
                    val cardColor = if (item.loan.type == LoanType.LEND) PrimaryLime else SecondaryOrange
                    
                    SwipeableLoanItem(
                        item = item,
                        backgroundColor = cardColor,
                        contentColor = Color.Black,
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
            onDismissRequest = { loanToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = TertiaryRed) },
            title = { Text("Delete Loan?") },
            text = { Text("Are you sure you want to delete this loan? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val loan = (uiState.lentLoans + uiState.borrowedLoans).find { it.loan.id == loanToDelete }
                        loan?.loan?.let { trackerViewModel.deleteLoan(it) }
                        loanToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = TertiaryRed)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { loanToDelete = null }) {
                    Text("Cancel")
                }
            }
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
        }
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
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
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
                        style = MaterialTheme.typography.titleLarge,
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
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
                            shape = CircleShape,
                            modifier = Modifier.height(18.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = if (item.loan.type == LoanType.LEND) "Lent" else "Borrowed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = contentColor
                                )
                            }
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Balance: ৳${String.format("%.0f", balance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.6f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Due: ${dateFormat.format(dueDate)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDueSoon) Color(0xFFD32F2F) else contentColor.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Status Chip
            val chipBgColor = if (isOverdue) Color(0xFFFFEBEE) else Color.Black.copy(alpha = 0.05f)
            val chipTextColor = if (isOverdue) Color(0xFFD32F2F) else contentColor.copy(alpha = 0.7f)
            val statusText = if (isOverdue) "Overdue" else if (item.loan.status == LoanStatus.FULLY_PAID) "Paid" else "Active"

            Surface(
                color = chipBgColor,
                shape = RoundedCornerShape(bottomStart = 12.dp, topEnd = 16.dp),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                 Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = chipTextColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                 )
            }
        }
    }
}
