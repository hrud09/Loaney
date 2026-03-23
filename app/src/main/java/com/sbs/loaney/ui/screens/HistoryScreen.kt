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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbs.loaney.R
import com.sbs.loaney.data.local.dao.LoanWithPayments
import com.sbs.loaney.ui.theme.*
import com.sbs.loaney.ui.viewmodel.HistoryViewModel
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
                            "History",
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
            com.sbs.loaney.ui.components.AnimatedLoadingScreen(modifier = Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
            AnimatedContent(
                targetState = uiState.deletedLoans.isEmpty(),
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "HistoryContentAnimation"
            ) { isEmpty ->
                if (isEmpty) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = "No history",
                                modifier = Modifier.size(80.dp),
                                tint = AlimDark.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No history found",
                                style = MaterialTheme.typography.titleLarge,
                                color = AlimDark.copy(alpha = 0.4f)
                            )
                            Text(
                                "Deleted loans will appear here",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AlimDark.copy(alpha = 0.3f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.deletedLoans, key = { it.loan.id }) { item ->
                            Box(modifier = Modifier.animateItem()) {
                                SwipeableHistoryCard(
                                    item = item,
                                    dateFormat = dateFormat,
                                    currencySymbol = uiState.currencySymbol,
                                    onRestore = { loanToRestore = item },
                                    onDeletePermanently = { loanToDeletePermanently = item },
                                    onClick = { onNavigateToDetail(item.loan.id) }
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }

    if (loanToRestore != null) {
        AlertDialog(
            onDismissRequest = { loanToRestore = null },
            title = { Text("Restore Loan?") },
            text = { Text("Do you want to restore the loan for ${loanToRestore?.loan?.personName}?") },
            confirmButton = {
                TextButton(onClick = {
                    loanToRestore?.loan?.id?.let { viewModel.restoreLoan(it) }
                    loanToRestore = null
                }) {
                    Text("Restore", fontWeight = FontWeight.Bold, color = AlimGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { loanToRestore = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (loanToDeletePermanently != null) {
        AlertDialog(
            onDismissRequest = { loanToDeletePermanently = null },
            title = { Text("Delete Permanently?") },
            text = { Text("Are you sure you want to permanently delete the loan for ${loanToDeletePermanently?.loan?.personName}? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    loanToDeletePermanently?.loan?.let { viewModel.deletePermanently(it) }
                    loanToDeletePermanently = null
                }) {
                    Text("Delete Permanently", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { loanToDeletePermanently = null }) {
                    Text("Cancel")
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
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
    onClick: () -> Unit
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
                SwipeToDismissBoxValue.StartToEnd -> "Restore"
                SwipeToDismissBoxValue.EndToStart -> "Delete"
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
        ManageLoanCard(
            item = item,
            dateFormat = dateFormat,
            currencySymbol = currencySymbol,
            onClick = onClick,
            onProfilePhotoClick = {}
        )
    }
}
