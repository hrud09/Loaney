package com.sbs.loaney.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import com.sbs.loaney.ui.components.OnboardingIllustration
import com.sbs.loaney.ui.components.OnboardingIllustrationType
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sbs.loaney.R
import com.sbs.loaney.data.local.entity.BankAccountEntity
import com.sbs.loaney.data.local.dao.LoanWithPayments
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.ui.components.CustomLightTextField
import com.sbs.loaney.ui.components.FullScreenImageViewer
import com.sbs.loaney.ui.components.WalletCardHolder
import com.sbs.loaney.ui.components.bounceClick
import com.sbs.loaney.ui.theme.*
import com.sbs.loaney.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToAddLoan: (String) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToHistory: (String?) -> Unit, // Replaces Manage navigation
    onNavigateToSettings: () -> Unit, // New
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAddBankSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }

    var expandedImageUri by remember { mutableStateOf<String?>(null) }
    var isImageExpanded by remember { mutableStateOf(false) }
    var showLentSummary by remember { mutableStateOf(false) }
    var showBorrowedSummary by remember { mutableStateOf(false) }

    val balance = uiState.totalLent - uiState.totalBorrowed
    val bankAccounts = uiState.bankAccounts
    val allLoans = (uiState.lentLoans + uiState.borrowedLoans).sortedByDescending { it.loan.loanDate }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        if (uiState.isLoading) {
             com.sbs.loaney.ui.components.AnimatedLoadingScreen()
        } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .animateContentSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── PINK HERO HEADER ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .neubrutalistCard(
                        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                        backgroundColor = NbCoral,
                        shadowOffset = 6.dp
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                    // Top row: greeting + notification
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(id = R.string.greeting_hi, uiState.userName),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NbPureBlack
                                )
                            )
                            Text(
                                text = stringResource(id = R.string.greeting_morning),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = NbPureBlack.copy(alpha = 0.6f)
                                )
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .neubrutalistButton(
                                        shape = ActionIconShape,
                                        backgroundColor = NbPureWhite
                                    )
                                    .bounceClick { onNavigateToSettings() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = NbPureBlack, modifier = Modifier.size(20.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .neubrutalistButton(
                                        shape = ActionIconShape,
                                        backgroundColor = NbPureWhite
                                    )
                                    .bounceClick { showNotificationsSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = NbPureBlack, modifier = Modifier.size(20.dp))
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(10.dp)
                                        .background(NbPureBlack, CircleShape)
                                        .border(1.5.dp, NbPureWhite, CircleShape)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Net Balance
                    Text(
                        text = stringResource(id = R.string.net_balance),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NbPureBlack.copy(alpha = 0.6f),
                            letterSpacing = 1.sp
                        )
                    )
                    val netPrefix = if (balance >= 0) "+" else "-"
                    Text(
                        text = "$netPrefix${uiState.currencySymbol}${String.format("%,.0f", kotlin.math.abs(balance))}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = NbPureBlack,
                            letterSpacing = (-1).sp
                        ),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Lent / Borrowed sub-row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Lent chip
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .neubrutalistCard(
                                    shape = RoundedCornerShape(12.dp),
                                    backgroundColor = NbGreen,
                                    shadowOffset = 3.dp
                                )
                                .bounceClick { onNavigateToHistory("LEND") }
                                .combinedClickable(
                                    onClick = { onNavigateToHistory("LEND") },
                                    onLongClick = { showLentSummary = true }
                                )
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.ArrowOutward, null, tint = NbPureBlack, modifier = Modifier.size(12.dp))
                                    Text(stringResource(R.string.total_lent), style = MaterialTheme.typography.labelSmall.copy(color = NbPureBlack.copy(0.7f)))
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "${uiState.currencySymbol}${String.format("%,.0f", uiState.totalLent)}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, color = NbPureBlack)
                                )
                            }
                        }
                        // Borrowed chip
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .neubrutalistCard(
                                    shape = RoundedCornerShape(12.dp),
                                    backgroundColor = NbSkyBlue,
                                    shadowOffset = 3.dp
                                )
                                .bounceClick { onNavigateToHistory("BORROW") }
                                .combinedClickable(
                                    onClick = { onNavigateToHistory("BORROW") },
                                    onLongClick = { showBorrowedSummary = true }
                                )
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.CallReceived, null, tint = NbPureBlack, modifier = Modifier.size(12.dp))
                                    Text(stringResource(R.string.total_borrowed), style = MaterialTheme.typography.labelSmall.copy(color = NbPureBlack.copy(0.7f)))
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "${uiState.currencySymbol}${String.format("%,.0f", uiState.totalBorrowed)}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, color = NbPureBlack)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (allLoans.isEmpty()) {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    HomeZeroState(onNavigateToAddLoan = { onNavigateToAddLoan("LEND") })
                }
            } else {
                // ── QUICK ACTION GRID ──────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .neubrutalistCard(
                            shape = CardShape,
                            backgroundColor = NbPureWhite
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuickActionItem(
                            icon = Icons.Default.CallMade,
                            label = stringResource(R.string.lend),
                            onClick = { onNavigateToAddLoan("LEND") }
                        )
                        QuickActionItem(
                            icon = Icons.Default.CallReceived,
                            label = stringResource(R.string.borrow),
                            onClick = { onNavigateToAddLoan("BORROW") }
                        )
                        QuickActionItem(
                            icon = Icons.Default.Payments,
                            label = stringResource(R.string.pay),
                            onClick = { onNavigateToHistory(null) }
                        )
                        QuickActionItem(
                            icon = Icons.Default.FormatListBulleted,
                            label = "View All",
                            onClick = { onNavigateToHistory(null) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── RECENT TRANSACTIONS ────────────────────────────────────────
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Transactions",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = NbPureBlack
                            )
                        )
                        Text(
                            text = "See All",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = NbCoral,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onNavigateToHistory(null) }
                        )
                    }

                    // Show up to 5 most recent
                    allLoans.take(5).forEach { item ->
                        BkashTransactionRow(
                            item = item,
                            currencySymbol = uiState.currencySymbol,
                            onClick = { onNavigateToDetail(item.loan.id) }
                        )
                    }
                }
// ... (rest of the file update)

                Spacer(modifier = Modifier.height(20.dp))

                // ── UPCOMING DEADLINES ─────────────────────────────────────────
                if (uiState.upcomingDeadlines.isNotEmpty()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        var selectedDate by remember {
                            mutableStateOf(
                                uiState.upcomingDeadlines.firstOrNull()?.loan?.promisedReturnDate ?:
                                Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                }.time
                            )
                        }
                        UpcomingDeadlineSection(
                            deadlines = uiState.upcomingDeadlines,
                            currencySymbol = uiState.currencySymbol,
                            selectedDate = selectedDate,
                            onDateSelected = { selectedDate = it },
                            onNavigateToDetail = onNavigateToDetail
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ── BANK ACCOUNTS ──────────────────────────────────────────────
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.bank_accounts),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = stringResource(id = R.string.add),
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.clickable { showAddBankSheet = true }
                        )
                    }

                    if (bankAccounts.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(id = R.string.no_bank_accounts),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { showAddBankSheet = true },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Link Account", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        val lazyListState = rememberLazyListState()
                        val snapBehavior = rememberSnapFlingBehavior(lazyListState)
                        LazyRow(
                            state = lazyListState,
                            flingBehavior = snapBehavior,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(bankAccounts, key = { it.id }) { account ->
                                BankAccountCard(
                                    account = account,
                                    context = context,
                                    onDelete = { viewModel.deleteBankAccount(it) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
        }
        }
    }


    if (showAddBankSheet) {
        AddBankAccountBottomSheet(
            onDismiss = { showAddBankSheet = false },
            onAdd = { request ->
                viewModel.addBankAccount(
                    accountName = request.accountName,
                    accountNumber = request.accountNumber,
                    bankName = request.bankName,
                    branchName = request.branchName,
                    swiftCode = request.swiftCode,
                    coverImageUri = request.coverImageUri,
                    isCard = request.isCard,
                    isMfs = request.isMfs,
                    mfsProvider = request.mfsProvider,
                    qrCodeUri = request.qrCodeUri
                )
                showAddBankSheet = false
            }
        )
    }

    if (showNotificationsSheet) {
        NotificationsBottomSheet(
            onDismiss = { showNotificationsSheet = false }
        )
    }

    FullScreenImageViewer(
        visible = isImageExpanded,
        imageUri = expandedImageUri,
        onDismiss = { isImageExpanded = false }
    )

    if (showLentSummary) {
        LoanSummaryDialog(
            title = stringResource(id = R.string.total_lent),
            loans = uiState.lentLoans,
            currencySymbol = uiState.currencySymbol,
            accentColor = MaterialTheme.colorScheme.primary,
            onDismiss = { showLentSummary = false }
        )
    }

    if (showBorrowedSummary) {
        LoanSummaryDialog(
            title = stringResource(id = R.string.total_borrowed),
            loans = uiState.borrowedLoans,
            currencySymbol = uiState.currencySymbol,
            accentColor = MaterialTheme.colorScheme.error,
            onDismiss = { showBorrowedSummary = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsBottomSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Mark all read",
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Icon(
                Icons.Outlined.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "No new notifications",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "You're all caught up!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.bounceClick(onClick)
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        )
    }
}

// ── bKash Quick Action Grid Item ────────────────────────────────────────────
@Composable
fun QuickActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(72.dp)
            .bounceClick(onClick)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .neubrutalistButton(
                    shape = ActionIconShape,
                    backgroundColor = NbYellow,
                    shadowOffset = 2.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = NbPureBlack,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = NbPureBlack,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
    }
}

// ── bKash Transaction Row item ───────────────────────────────────────────────
@Composable
fun BkashTransactionRow(
    item: com.sbs.loaney.data.local.dao.LoanWithPayments,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val isLent = item.loan.type == com.sbs.loaney.data.model.LoanType.LEND
    val amountColor = if (isLent) NbGreen else NbCoral
    val amountPrefix = if (isLent) "+" else "-"
    val avatarColor = AvatarGradients[0].first // Using a simplified solid color for Brutalist look

    val totalLoan = item.loan.amount + item.loanItems.sumOf { it.amount }
    val paid = item.payments.sumOf { it.amount }
    val remaining = (totalLoan - paid).coerceAtLeast(0.0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .neubrutalistCard(
                shape = SmallCardShape,
                backgroundColor = NbPureWhite,
                shadowOffset = 2.dp
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar circle -> now Avatar Square/Rounded for Brutalist look
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .neubrutalistCard(
                        shape = RoundedCornerShape(8.dp),
                        backgroundColor = if (isLent) NbGreen else NbCoral,
                        shadowOffset = 0.dp // No shadow for small avatar
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.loan.personName.firstOrNull()?.toString()?.uppercase() ?: "",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = NbPureBlack,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }

            // Name + type
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.loan.personName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = NbPureBlack
                    ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = if (isLent) "Lent" else "Borrowed",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = NbPureBlack.copy(alpha = 0.6f)
                    )
                )
            }

            // Amount
            Text(
                text = "$amountPrefix$currencySymbol${String.format("%,.0f", remaining)}",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = NbPureBlack
                )
            )
        }
    }
}

// --- BANK ACCOUNTS COMPONENTS ---

@Composable
fun BankAccountCard(
    account: BankAccountEntity,
    context: Context,
    onDelete: (BankAccountEntity) -> Unit
) {
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val cardWidth = screenWidth * 0.85f

    // MFS header color (only for the top section)
    val mfsHeaderColor = if (account.isMfs) {
        when (account.mfsProvider?.lowercase()) {
            "bkash" -> Color(0xFFE2136E) // Pink
            "nagad" -> Color(0xFFED4D36) // Orange
            "rocket" -> Color(0xFF8C158C) // Purple
            "upay" -> Color(0xFF005BAC) // Blue
            else -> MaterialTheme.colorScheme.primary
        }
    } else {
        Color.Unspecified
    }

    // Content area always uses standard theme colors
    val contentColor = MaterialTheme.colorScheme.onBackground
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier
            .width(cardWidth)
            .padding(4.dp)
            .neubrutalistCard(
                shape = BigCardShape,
                backgroundColor = NbPureWhite
            )
    ) {
        Column {
            // Header Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
                if (!account.coverImageUri.isNullOrBlank() && !account.isMfs) {
                    AsyncImage(
                        model = account.coverImageUri,
                        contentDescription = "Cover Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = if (account.isMfs) mfsHeaderColor else NbYellow
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val headerIcon = if (account.isMfs) Icons.Default.PhoneIphone else Icons.Default.AccountBalance
                        val headerTint = if (account.isMfs) NbPureWhite.copy(alpha = 0.4f) else NbPureBlack.copy(alpha = 0.1f)
                        Icon(headerIcon, contentDescription = null, tint = headerTint, modifier = Modifier.size(48.dp))
                    }
                }
                
                // Delete overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .clickable { onDelete(account) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(16.dp))
                }

                // Copy All overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .clickable {
                            val text = buildString {
                                append("${if (account.isMfs) "Provider" else "Bank"}: ${account.bankName}\n")
                                append("${if (account.isMfs) "Holder" else "Account Name"}: ${account.accountName}\n")
                                append("${if (account.isMfs) "Mobile No" else "Account No"}.: ${account.accountNumber}\n")
                                if (!account.branchName.isNullOrBlank()) append("Branch: ${account.branchName}\n")
                                if (!account.swiftCode.isNullOrBlank()) append("SWIFT: ${account.swiftCode}\n")
                            }
                            clipboardManager.setPrimaryClip(ClipData.newPlainText(if (account.isMfs) "MFS Account" else "Bank Account", text))
                            Toast.makeText(context, "All Details Copied!", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy All", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            HorizontalDivider(color = NbPureBlack, thickness = 2.dp)

            // Details Section
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = account.bankName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = contentColor
                        ),
                        maxLines = 1,
                        modifier = Modifier.weight(1f).clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            clipboardManager.setPrimaryClip(ClipData.newPlainText("Bank Name", account.bankName))
                            Toast.makeText(context, "Name Copied!", Toast.LENGTH_SHORT).show()
                        }
                    )
                    
                    if (account.isMfs && !account.qrCodeUri.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(2.dp)
                        ) {
                            AsyncImage(
                                model = account.qrCodeUri,
                                contentDescription = "QR Code",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            clipboardManager.setPrimaryClip(ClipData.newPlainText(if (account.isCard) "Card Number" else "Account Number", account.accountNumber))
                            Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    val numberLabel = when {
                        account.isCard -> "CARD NUMBER"
                        account.isMfs -> "MOBILE NUMBER"
                        else -> stringResource(id = R.string.account_number)
                    }
                    Text(numberLabel, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.sp), color = labelColor)
                    
                    val displayNum = when {
                        account.isCard -> account.accountNumber.chunked(4).joinToString(" ")
                        else -> account.accountNumber
                    }
                    Text(
                        text = displayNum,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.5.sp
                        ),
                        maxLines = 1
                    )
                }

                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                val holderLabel = when {
                    account.isCard -> "Cardholder Name"
                    account.isMfs -> "Account Holder"
                    else -> stringResource(id = R.string.account_holder)
                }
                BankField(holderLabel, account.accountName, clipboardManager, context, labelColor, contentColor)

                if (!account.isMfs && (!account.branchName.isNullOrBlank() || !account.swiftCode.isNullOrBlank())) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!account.branchName.isNullOrBlank()) {
                            Box(modifier = Modifier.weight(1f)) {
                                BankField(stringResource(id = R.string.branch), account.branchName, clipboardManager, context, labelColor, contentColor)
                            }
                        }
                        if (!account.swiftCode.isNullOrBlank()) {
                            Box(modifier = Modifier.weight(1f)) {
                                BankField(stringResource(id = R.string.swift), account.swiftCode, clipboardManager, context, labelColor, contentColor)
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun BankField(label: String, value: String, clipboardManager: ClipboardManager, context: Context, labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant, contentColor: Color = MaterialTheme.colorScheme.onBackground) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    clipboardManager.setPrimaryClip(ClipData.newPlainText(label, value))
                    Toast.makeText(context, "$label Copied!", Toast.LENGTH_SHORT).show()
                }
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.5.sp), color = labelColor)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = contentColor),
                maxLines = 1
            )
        }
    }
}

data class AddBankAccountRequest(
    val accountName: String,
    val accountNumber: String,
    val bankName: String,
    val branchName: String?,
    val swiftCode: String?,
    val coverImageUri: String?,
    val isCard: Boolean,
    val isMfs: Boolean,
    val mfsProvider: String?,
    val qrCodeUri: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBankAccountBottomSheet(
    onDismiss: () -> Unit,
    onAdd: (AddBankAccountRequest) -> Unit
) {
    var accountName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var branchName by remember { mutableStateOf("") }
    var swiftCode by remember { mutableStateOf("") }
    var proofUri by remember { mutableStateOf<Uri?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Bank, 1: Card, 2: MFS
    var mfsProvider by remember { mutableStateOf("bKash") }
    var qrCodeUri by remember { mutableStateOf<Uri?>(null) }
    
    val mfsProviders = listOf("bKash", "Nagad", "Rocket", "Upay")

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> 
        if (selectedTab == 2) {
            qrCodeUri = uri
        } else {
            proofUri = uri 
        }
    }

    val context = LocalContext.current
    val contactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                )
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        
                        if (numberIndex >= 0) {
                            accountNumber = cursor.getString(numberIndex)?.replace(Regex("[^0-9]"), "")?.take(15) ?: ""
                        }
                        if (nameIndex >= 0 && accountName.isBlank()) {
                            accountName = cursor.getString(nameIndex) ?: ""
                        }
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(id = R.string.add_bank_account), 
                style = MaterialTheme.typography.headlineSmall, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.onBackground
            )

            // Segmented Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(0 to "Bank Account", 1 to "Card", 2 to "MFS").forEach { (index, title) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == index) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(title, color = if (selectedTab == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // MFS Provider Chips
            if (selectedTab == 2) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(mfsProviders) { provider ->
                        val isSelected = mfsProvider == provider
                        FilterChip(
                            selected = isSelected,
                            onClick = { 
                                mfsProvider = provider 
                                bankName = provider // Sync bank name to provider automatically
                            },
                            label = { Text(provider) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Color.Transparent,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                enabled = true,
                                selected = isSelected
                            ),
                            shape = CircleShape
                        )
                    }
                }
            }

            // Image Picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                val imageUri = if (selectedTab == 2) qrCodeUri else proofUri
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Upload",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val icon = if (selectedTab == 2) Icons.Default.QrCode else Icons.Default.Image
                        val textStr = if (selectedTab == 2) "Upload My QR Code" else stringResource(id = R.string.tap_custom_cover)
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(textStr, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            
            if (selectedTab != 2) {
                CustomLightTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = if (selectedTab == 1) "Card Issuer (e.g. Visa, Mastercard)" else stringResource(id = R.string.bank_name_hint),
                    leadingIcon = if (selectedTab == 1) Icons.Default.CreditCard else Icons.Default.AccountBalance
                )
            }

            CustomLightTextField(
                value = accountName,
                onValueChange = { accountName = it },
                label = when (selectedTab) {
                    1 -> "Cardholder Name"
                    2 -> "Account Holder Name"
                    else -> stringResource(id = R.string.account_holder_name_hint)
                },
                leadingIcon = Icons.Default.Person
            )

            if (selectedTab == 2) {
                // MFS Mobile Number with Contact Picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomLightTextField(
                        value = accountNumber,
                        onValueChange = {
                            accountNumber = it.filter { char -> char.isDigit() }.take(15)
                        },
                        label = "Mobile Number",
                        leadingIcon = Icons.Default.Phone,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    FilledIconButton(
                        onClick = { 
                            val intent = android.content.Intent(android.content.Intent.ACTION_PICK).apply {
                                type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                            }
                            contactLauncher.launch(intent) 
                        },
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Contacts, contentDescription = "Pick from Contacts", modifier = Modifier.size(24.dp))
                    }
                }
            } else {
                CustomLightTextField(
                    value = accountNumber,
                    onValueChange = {
                        if (selectedTab == 1) {
                            accountNumber = it.take(16)
                        } else {
                            accountNumber = it
                        }
                    },
                    label = if (selectedTab == 1) "Card Number" else stringResource(id = R.string.account_number_hint),
                    leadingIcon = Icons.Default.DateRange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = if (selectedTab == 1) CardNumberVisualTransformation() else VisualTransformation.None
                )
            }

            if (selectedTab == 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CustomLightTextField(
                        value = branchName,
                        onValueChange = { branchName = it },
                        label = stringResource(id = R.string.branch_optional),
                        leadingIcon = Icons.Default.LocationOn,
                        modifier = Modifier.weight(1f)
                    )

                    CustomLightTextField(
                        value = swiftCode,
                        onValueChange = { swiftCode = it },
                        label = stringResource(id = R.string.swift_optional),
                        leadingIcon = Icons.Default.Info,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Button(
                onClick = {
                    onAdd(AddBankAccountRequest(
                        accountName = accountName,
                        accountNumber = accountNumber,
                        bankName = if (selectedTab == 2) mfsProvider else bankName,
                        branchName = if (selectedTab == 0) branchName.ifBlank { null } else null,
                        swiftCode = if (selectedTab == 0) swiftCode.ifBlank { null } else null,
                        coverImageUri = if (selectedTab == 2) null else proofUri?.toString(),
                        isCard = selectedTab == 1,
                        isMfs = selectedTab == 2,
                        mfsProvider = if (selectedTab == 2) mfsProvider else null,
                        qrCodeUri = if (selectedTab == 2) qrCodeUri?.toString() else null
                    ))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                enabled = accountName.isNotBlank() && accountNumber.isNotBlank() && (selectedTab == 2 || bankName.isNotBlank())
            ) {
                val actionLabel = when (selectedTab) {
                    1 -> "Save Card"
                    2 -> "Save MFS Account"
                    else -> stringResource(id = R.string.save_bank_account)
                }
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HomeZeroState(onNavigateToAddLoan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OnboardingIllustration(
            type = OnboardingIllustrationType.ZERO_STATE,
            modifier = Modifier.size(140.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Your dashboard is empty",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Track your first lent or borrowed amount to see it here.",
            style = MaterialTheme.typography.bodyMedium.copy(color = MutedText),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNavigateToAddLoan,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SoftViolet),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Transaction", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LoanSummaryDialog(
    title: String,
    loans: List<LoanWithPayments>,
    currencySymbol: String,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val sortedEntries = remember(loans) {
        loans.map { item ->
            val totalLoan = item.loan.amount + item.loanItems.sumOf { it.amount }
            val paid = item.payments.sumOf { it.amount }
            val remaining = (totalLoan - paid).coerceAtLeast(0.0)
            item.loan.personName to remaining
        }.sortedByDescending { it.second }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text(title, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (sortedEntries.isEmpty()) {
                    Text("No entries", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    sortedEntries.forEachIndexed { index, (name, amount) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.width(28.dp)
                                )
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    ),
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = "$currencySymbol${String.format("%,.0f", amount)}",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            )
                        }
                        if (index < sortedEntries.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    )
}

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
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
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

                // Deadlines for this date
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
                        .neubrutalistButton(
                            shape = RoundedCornerShape(12.dp),
                            backgroundColor = if (isSelected) NbCoral else NbPureWhite,
                            shadowOffset = if (isSelected) 3.dp else 2.dp
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
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (isSelected) NbPureWhite else NbPureBlack.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dateFormat.format(date),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) NbPureWhite else NbPureBlack
                        )

                        // Dot indicators
                        if (dayDeadlines.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (hasLent) {
                                    Box(modifier = Modifier.size(6.dp).background(if (isSelected) NbPureWhite else NbGreen, CircleShape).border(1.dp, NbPureBlack, CircleShape))
                                }
                                if (hasBorrowed) {
                                    Box(modifier = Modifier.size(6.dp).background(if (isSelected) NbPureWhite else NbCoral, CircleShape).border(1.dp, NbPureBlack, CircleShape))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Content for the selected date
        if (filteredDeadlines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
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
                        selectedDate = selectedDate,
                        onClick = { onNavigateToDetail(item.loan.id) }
                    )
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
    val percentPaid = (progressFraction * 100).toInt()

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
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().bounceClick { onClick() },
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Type icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isLent) Icons.Default.CallReceived else Icons.Default.ArrowOutward,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                // Name + note
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.loan.personName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.loan.purpose?.ifBlank { if (isLent) stringResource(id = R.string.lent) else stringResource(id = R.string.borrowed) }
                            ?: if (isLent) stringResource(id = R.string.lent) else stringResource(id = R.string.borrowed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                // Amount + chevron
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$currencySymbol${String.format("%,.0f", balance)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
                    // Urgency chip
                    Surface(
                        shape = CircleShape,
                        color = urgencyColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = urgencyLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = urgencyColor
                            ),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Repayment progress bar
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Repaid $percentPaid%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.15f)
            )
        }
    }
}

class CardNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Format as XXXX XXXX XXXX XXXX
        val original = text.text
        var formatted = ""
        for (i in original.indices) {
            formatted += original[i]
            if ((i + 1) % 4 == 0 && i != 15) {
                formatted += " "
            }
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return offset
                if (offset <= 4) return offset
                if (offset <= 8) return offset + 1
                if (offset <= 12) return offset + 2
                if (offset <= 16) return offset + 3
                return 19
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return offset
                if (offset <= 4) return offset
                if (offset <= 9) return offset - 1
                if (offset <= 14) return offset - 2
                if (offset <= 19) return offset - 3
                return 16
            }
        }
        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}
