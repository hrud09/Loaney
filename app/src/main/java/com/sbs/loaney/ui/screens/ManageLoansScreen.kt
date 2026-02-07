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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
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
import com.sbs.loaney.ui.theme.PrimaryLime
import com.sbs.loaney.ui.theme.SecondaryOrange
import com.sbs.loaney.ui.theme.TertiaryRed
import com.sbs.loaney.ui.viewmodel.LoanTrackerViewModel
import com.sbs.loaney.ui.viewmodel.ManageLoansViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageLoansScreen(
    onNavigateBack: () -> Unit,
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Manage Loans", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddLoan,
                containerColor = PrimaryLime,
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Loan")
            }
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
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .padding(4.dp)
            ) {
                listOf(LoanType.LEND to "LEND", LoanType.BORROW to "BORROW").forEach { (type, text) ->
                    val selected = uiState.selectedType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No loans found.", 
                        style = MaterialTheme.typography.bodyLarge, 
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.loans) { item ->
                        SwipeableManageLoanCard(
                            item = item,
                            dateFormat = dateFormat,
                            onClick = { onNavigateToDetail(item.loan.id) },
                            onSwipeLeft = { selectedLoanIdForPayment = item.loan.id },
                            onSwipeRight = { loanToDelete = item }
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
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = Color.White,
            textContentColor = Color.Gray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableManageLoanCard(
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
    val progress = if (loan.amount > 0) (paid / loan.amount).toFloat() else 0f
    
    val cardColor = when (loan.status) {
        LoanStatus.OVERDUE -> TertiaryRed
        LoanStatus.FULLY_PAID -> PrimaryLime
        else -> SecondaryOrange
    }
    
    val textColor = Color.Black

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = loan.personName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Icon(Icons.Default.DateRange, contentDescription = null, tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                         Spacer(modifier = Modifier.width(4.dp))
                         Text(
                            text = dateFormat.format(loan.promisedReturnDate),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    }
                }
                Text(
                    text = "৳${String.format("%.0f", loan.amount)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Progress", style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.7f))
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = textColor)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = Color.Black,
                    trackColor = Color.Black.copy(alpha = 0.1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Footer with Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.height(32.dp)
                ) {
                     Box(
                         contentAlignment = Alignment.Center,
                         modifier = Modifier.padding(horizontal = 12.dp)
                     ) {
                         Text(
                            text = loan.status.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                         )
                     }
                }
                
                // Avatars Mock
                 Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = loan.personName.take(1), 
                        fontWeight = FontWeight.Bold, 
                        color = textColor
                    )
                }
            }
        }
    }
}
