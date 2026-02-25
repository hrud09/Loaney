package com.sbs.loaney.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbs.loaney.data.local.dao.LoanWithPayments
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.ui.theme.PrimaryLime
import com.sbs.loaney.ui.theme.TertiaryRed
import com.sbs.loaney.ui.viewmodel.LoanTrackerViewModel
import com.sbs.loaney.ui.viewmodel.ManageLoansViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ManageLoansScreen(
    onNavigateToAddLoan: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: ManageLoansViewModel = hiltViewModel(),
    trackerViewModel: LoanTrackerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    var selectedLoanIdForPayment by remember { mutableStateOf<Long?>(null) }
    var loanToDelete by remember { mutableStateOf<LoanWithPayments?>(null) }

    Scaffold(
        containerColor = Color(0xFF0B0E11),
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Manage Loans", 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddLoan,
                containerColor = Color(0xFFC3FF4D),
                contentColor = Color.Black,
                text = { Text("New Loan", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add New Loan") },
                shape = CircleShape
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {
            
            // Segmented Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color(0xFF1C2024), CircleShape)
                    .padding(4.dp)
            ) {
                listOf(LoanType.LEND to "LENT", LoanType.BORROW to "BORROWED").forEach { (type, text) ->
                    val selected = uiState.selectedType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (selected) Color(0xFFC3FF4D) else Color.Transparent)
                            .clickable { viewModel.setLoanType(type) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = text,
                            color = if (selected) Color.Black else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (uiState.loans.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HourglassEmpty, // A more visually engaging icon
                        contentDescription = "No loans",
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No loans... yet!",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tap the '+' button to add a new loan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.loans, key = { it.loan.id }) { item ->
                        SwipeableManageLoanCard(
                            modifier = Modifier.animateItemPlacement(), // Add smooth animations
                            item = item,
                            dateFormat = dateFormat,
                            onClick = { onNavigateToDetail(item.loan.id) },
                            onSwipeLeft = { selectedLoanIdForPayment = item.loan.id },
                            onSwipeRight = { loanToDelete = item },
                        )
                    }
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
            containerColor = Color(0xFF1C2024),
            titleContentColor = Color.White,
            textContentColor = Color.Gray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableManageLoanCard(
    modifier: Modifier = Modifier,
    item: LoanWithPayments,
    dateFormat: SimpleDateFormat,
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
        modifier = modifier,
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
                    } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                        Text(label, color = Color.Black, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(icon, contentDescription = null, tint = Color.Black)
                    }
                }
            }
        }
    ) {
        ManageLoanCard(
            item = item,
            dateFormat = dateFormat,
            onClick = onClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageLoanCard(
    item: LoanWithPayments,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    val loan = item.loan
    val paid = item.payments.sumOf { it.amount }
    val totalLoan = loan.amount + item.loanItems.sumOf { it.amount }
    val progress = if (totalLoan > 0) (paid / totalLoan).toFloat() else 0f

    val primaryColor = Color(0xFFC3FF4D)
    val surfaceColor = Color(0xFF1C2024)

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        color = surfaceColor.copy(alpha = 0.8f), // Glassmorphism style
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp) // Airy layout
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = loan.personName,
                        style = MaterialTheme.typography.titleMedium, // High-quality typography
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Due by: ${dateFormat.format(loan.promisedReturnDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = "৳${String.format(Locale.getDefault(), "%,.0f", totalLoan)}",
                    style = MaterialTheme.typography.headlineSmall, // High-quality typography
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Section
            Column {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp) // Thick progress indicator
                        .clip(RoundedCornerShape(4.dp)), // Rounded
                    color = primaryColor,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Paid: ৳${String.format(Locale.getDefault(), "%,.0f", paid)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val statusText = when {
                loan.status == LoanStatus.FULLY_PAID -> "Paid"
                loan.status == LoanStatus.OVERDUE -> "Overdue"
                else -> "Active"
            }
            
            val tonalButtonColors = when (loan.status) {
                LoanStatus.OVERDUE -> ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFFB00020).copy(alpha = 0.3f),
                    contentColor = Color(0xFFFFB8B8)
                )
                LoanStatus.FULLY_PAID -> ButtonDefaults.filledTonalButtonColors(
                    containerColor = primaryColor.copy(alpha = 0.2f),
                    contentColor = primaryColor
                )
                else -> ButtonDefaults.filledTonalButtonColors( // Active
                    containerColor = Color.White.copy(alpha = 0.1f),
                    contentColor = Color.White.copy(alpha = 0.8f)
                )
            }

            Button(
                onClick = { /* Non-interactive */ },
                shape = CircleShape,
                colors = tonalButtonColors,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.height(36.dp),
                enabled = false
            ) {
                 Text(text = statusText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
