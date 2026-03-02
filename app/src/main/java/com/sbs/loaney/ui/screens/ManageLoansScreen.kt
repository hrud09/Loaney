package com.sbs.loaney.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.launch
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
import com.sbs.loaney.ui.components.FullScreenImageViewer
import com.sbs.loaney.ui.components.bounceClick
import com.sbs.loaney.ui.theme.*
import com.sbs.loaney.ui.viewmodel.LoanTrackerViewModel
import com.sbs.loaney.ui.viewmodel.ManageLoansViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.sbs.loaney.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ManageLoansScreen(
    initialType: String? = null,
    onNavigateToAddLoan: (String) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: ManageLoansViewModel = hiltViewModel(),
    trackerViewModel: LoanTrackerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })

    var selectedLoanIdForPayment by remember { mutableStateOf<Long?>(null) }
    var loanToDelete by remember { mutableStateOf<LoanWithPayments?>(null) }

    var expandedImageUri by remember { mutableStateOf<String?>(null) }
    var isImageExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(initialType) {
        if (initialType != null) {
            try {
                val parsedType = LoanType.valueOf(initialType)
                if (parsedType == LoanType.LEND) {
                    pagerState.scrollToPage(0)
                } else {
                    pagerState.scrollToPage(1)
                }
            } catch (e: Exception) {
                // Ignore parsing errors and keep current state
            }
        }
    }
    
    val currentType = if (pagerState.currentPage == 0) LoanType.LEND else LoanType.BORROW

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        stringResource(id = R.string.transactions), 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddLoan(currentType.name) },
                containerColor = if (currentType == LoanType.LEND) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                contentColor = if (currentType == LoanType.LEND) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Loan")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            com.sbs.loaney.ui.components.AnimatedLoadingScreen(modifier = Modifier.padding(padding))
        } else {
            Column(modifier = Modifier
                .padding(padding)
                .fillMaxSize()) {
            
            // Segmented Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .padding(4.dp)
            ) {
                listOf(0 to stringResource(id = R.string.lent), 1 to stringResource(id = R.string.borrowed)).forEach { (pageIndex, text) ->
                    val selected = pagerState.currentPage == pageIndex
                    val activeColor = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(activeColor)
                            .bounceClick { coroutineScope.launch { pagerState.animateScrollToPage(pageIndex) } }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = text,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val loans = if (page == 0) uiState.lentLoans else uiState.borrowedLoans
                
                if (loans.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.HourglassEmpty,
                                contentDescription = "No loans",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            stringResource(id = R.string.no_transactions_yet),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(id = R.string.tap_to_start_tracking),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        items(loans, key = { it.loan.id }) { item ->
                            SwipeableManageLoanCard(
                                modifier = Modifier.animateItem(
                                    fadeInSpec = null, 
                                    placementSpec = androidx.compose.animation.core.tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing), 
                                    fadeOutSpec = null
                                ),
                                item = item,
                                dateFormat = dateFormat,
                                currencySymbol = uiState.currencySymbol,
                                onClick = { onNavigateToDetail(item.loan.id) },
                                onSwipeLeft = { selectedLoanIdForPayment = item.loan.id },
                                onSwipeRight = { loanToDelete = item },
                                onProfilePhotoClick = { uri ->
                                    expandedImageUri = uri
                                    isImageExpanded = true
                                }
                            )
                        }
                    }
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
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(id = R.string.delete_loan_title)) },
            text = { Text(stringResource(id = R.string.delete_loan_msg, loanToDelete?.loan?.personName ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        loanToDelete?.loan?.let { trackerViewModel.deleteLoan(it) }
                        loanToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(id = R.string.delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { loanToDelete = null }) {
                    Text(stringResource(id = R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    FullScreenImageViewer(
        visible = isImageExpanded,
        imageUri = expandedImageUri,
        onDismiss = { isImageExpanded = false }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableManageLoanCard(
    modifier: Modifier = Modifier,
    item: LoanWithPayments,
    dateFormat: SimpleDateFormat,
    currencySymbol: String,
    onClick: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onProfilePhotoClick: (String?) -> Unit
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
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.error
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primary
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
                SwipeToDismissBoxValue.StartToEnd -> stringResource(id = R.string.delete)
                SwipeToDismissBoxValue.EndToStart -> stringResource(id = R.string.add_payment)
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
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onError)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                    } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                        Text(label, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
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
            onProfilePhotoClick = onProfilePhotoClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageLoanCard(
    item: LoanWithPayments,
    dateFormat: SimpleDateFormat,
    currencySymbol: String,
    onClick: () -> Unit,
    onProfilePhotoClick: (String?) -> Unit
) {
    val loan = item.loan
    val paid = item.payments.sumOf { it.amount }
    val totalLoan = loan.amount + item.loanItems.sumOf { it.amount }
    val remainingBalance = (totalLoan - paid).coerceAtLeast(0.0)
    val progress = if (totalLoan > 0) (paid / totalLoan).toFloat() else 0f

    val accentColor = if (loan.type == LoanType.LEND) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

    Surface(
        modifier = Modifier
            .bounceClick(onClick)
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp) // Airy layout
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Profile Photo
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onProfilePhotoClick(loan.profilePhotoUri) },
                    contentAlignment = Alignment.Center
                ) {
                    if (loan.profilePhotoUri != null) {
                        AsyncImage(
                            model = loan.profilePhotoUri,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val isLent = loan.type == LoanType.LEND
                        val bgColor = if (isLent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                        val textColor = if (isLent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        Box(
                            modifier = Modifier.fillMaxSize().background(bgColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = loan.personName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = loan.personName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.due_by, dateFormat.format(loan.promisedReturnDate)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${currencySymbol}${String.format(Locale.getDefault(), "%,.0f", remainingBalance)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Progress Section
            Column {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp) 
                        .clip(RoundedCornerShape(4.dp)), 
                    color = accentColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.paid_amount, "${currencySymbol}${String.format(Locale.getDefault(), "%,.0f", paid)}"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val statusText = when {
                loan.status == LoanStatus.FULLY_PAID -> stringResource(id = R.string.status_paid)
                loan.status == LoanStatus.OVERDUE -> stringResource(id = R.string.status_overdue)
                else -> stringResource(id = R.string.status_active)
            }
            
            val tonalButtonColors = when (loan.status) {
                LoanStatus.OVERDUE -> ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.error
                )
                LoanStatus.FULLY_PAID -> ButtonDefaults.filledTonalButtonColors(
                    containerColor = accentColor.copy(alpha = 0.1f),
                    contentColor = accentColor
                )
                else -> ButtonDefaults.filledTonalButtonColors( // Active
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { /* Non-interactive */ },
                shape = CircleShape,
                colors = tonalButtonColors,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(28.dp),
                enabled = true
            ) {
                 Text(text = statusText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}
