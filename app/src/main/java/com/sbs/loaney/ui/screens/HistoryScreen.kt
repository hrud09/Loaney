package com.sbs.loaney.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbs.loaney.R
import com.sbs.loaney.data.local.dao.LoanWithPayments
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.ui.theme.*
import com.sbs.loaney.ui.viewmodel.HistoryViewModel
import com.sbs.loaney.ui.components.bounceClick
import com.sbs.loaney.ui.components.AnimatedLoadingScreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())

    var loanToRestore by remember { mutableStateOf<LoanWithPayments?>(null) }
    var loanToDeletePermanently by remember { mutableStateOf<LoanWithPayments?>(null) }

    Scaffold(
        containerColor = AlimCream,
        topBar = {
            Column(modifier = Modifier.background(AlimDark)) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            stringResource(id = R.string.history_title),
                            fontWeight = FontWeight.SemiBold,
                            color = AlimWhite
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = AlimWhite
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = AlimDark,
                        titleContentColor = AlimWhite
                    )
                )
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            AnimatedLoadingScreen(modifier = Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // Info Section
                if (uiState.deletedLoans.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = AlimDark.copy(alpha = 0.05f),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.History,
                                contentDescription = null,
                                tint = AlimGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                stringResource(id = R.string.history_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = AlimDark.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                AnimatedContent(
                    targetState = uiState.deletedLoans.isEmpty(),
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "HistoryContentAnimation"
                ) { isEmpty ->
                    if (isEmpty) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = "No history",
                                modifier = Modifier.size(64.dp),
                                tint = AlimDark.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                stringResource(id = R.string.no_history_found),
                                style = MaterialTheme.typography.titleMedium,
                                color = AlimDark.copy(alpha = 0.4f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.deletedLoans, key = { it.loan.id }) { item ->
                                SwipeableHistoryCard(
                                    item = item,
                                    dateFormat = dateFormat,
                                    currencySymbol = uiState.currencySymbol,
                                    onClick = { onNavigateToDetail(item.loan.id) },
                                    onRestore = { loanToRestore = item },
                                    onDeletePermanently = { loanToDeletePermanently = item }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (loanToRestore != null) {
        AlertDialog(
            onDismissRequest = { loanToRestore = null },
            title = { Text(stringResource(id = R.string.restore_loan_title)) },
            text = { Text(stringResource(id = R.string.restore_loan_msg, loanToRestore?.loan?.personName ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.restoreLoan(loanToRestore!!.loan.id)
                        loanToRestore = null
                    }
                ) {
                    Text(stringResource(id = R.string.restore), color = AlimGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { loanToRestore = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    if (loanToDeletePermanently != null) {
        AlertDialog(
            onDismissRequest = { loanToDeletePermanently = null },
            title = { Text(stringResource(id = R.string.delete_permanently_title)) },
            text = { Text(stringResource(id = R.string.delete_permanently_msg, loanToDeletePermanently?.loan?.personName ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePermanently(loanToDeletePermanently!!.loan)
                        loanToDeletePermanently = null
                    }
                ) {
                    Text(stringResource(id = R.string.delete), color = CoralRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { loanToDeletePermanently = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableHistoryCard(
    item: LoanWithPayments,
    dateFormat: SimpleDateFormat,
    currencySymbol: String,
    onClick: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onRestore()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDeletePermanently()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = swipeState,
        backgroundContent = {
            val direction = swipeState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> AlimGreen
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                else -> Color.Transparent
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Restore
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.DeleteForever
                else -> Icons.Default.Restore
            }
            val label = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> stringResource(id = R.string.restore)
                SwipeToDismissBoxValue.EndToStart -> stringResource(id = R.string.delete)
                else -> ""
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .background(color, RoundedCornerShape(24.dp)),
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
                        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(icon, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    ) {
        HistoryLoanCard(
            item = item,
            dateFormat = dateFormat,
            currencySymbol = currencySymbol,
            onClick = onClick
        )
    }
}

@Composable
fun HistoryLoanCard(
    item: LoanWithPayments,
    dateFormat: SimpleDateFormat,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val loan = item.loan
    val totalLoan = loan.amount + item.loanItems.sumOf { it.amount }
    
    val isDeleted = loan.deleted
    val isForgiven = loan.status == LoanStatus.FORGIVEN
    val isFullyPaid = loan.status == LoanStatus.FULLY_PAID

    val statusText = when {
        isDeleted -> stringResource(id = R.string.status_deleted)
        isForgiven -> stringResource(id = R.string.status_forgiven)
        isFullyPaid -> stringResource(id = R.string.status_completed)
        else -> stringResource(id = R.string.status_active)
    }

    val statusColor = when {
        isDeleted -> MaterialTheme.colorScheme.error
        isForgiven -> CoralRose
        isFullyPaid -> AlimGreen
        else -> AlimGreen
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .bounceClick(onClick),
        shape = RoundedCornerShape(24.dp),
        color = AlimWhite,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(AlimDark.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = loan.personName.firstOrNull()?.toString()?.uppercase() ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AlimDark
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = loan.personName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = AlimDark,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${currencySymbol}${String.format(Locale.getDefault(), "%,.0f", totalLoan)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AlimDark
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dateToShow = loan.removedAt ?: loan.createdAt
                    Text(
                        text = dateFormat.format(Date(dateToShow)),
                        style = MaterialTheme.typography.bodySmall,
                        color = AlimDark.copy(alpha = 0.5f)
                    )

                    Surface(
                        color = statusColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
