package com.sbs.loaney.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.data.model.calculateLoaneyPiePoints
import com.sbs.loaney.ui.components.FullScreenImageViewer
import com.sbs.loaney.ui.components.LoaneyPieRewardOverlay
import com.sbs.loaney.ui.viewmodel.LoanTrackerViewModel
import java.text.SimpleDateFormat
import java.util.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.sbs.loaney.R
import com.sbs.loaney.ui.theme.*
import com.sbs.loaney.util.PdfReceiptGenerator
import com.sbs.loaney.util.sendReminder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanTrackerScreen(
    loanId: Long,
    onNavigateBack: () -> Unit,
    viewModel: LoanTrackerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAddPaymentSheet by remember { mutableStateOf(false) }
    var showAddLoanSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showSettleConfirmation by remember { mutableStateOf(false) }
    var showForgiveConfirmation by remember { mutableStateOf(false) }

    var expandedImageUri by remember { mutableStateOf<String?>(null) }
    var isImageExpanded by remember { mutableStateOf(false) }

    // Loaney Pie reward overlay
    var showRewardOverlay by remember { mutableStateOf(false) }
    var rewardPoints by remember { mutableStateOf(0) }

    LaunchedEffect(loanId) {
        viewModel.selectLoan(loanId)
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(id = R.string.delete_loan_title)) },
            text = { Text(stringResource(id = R.string.delete_loan_msg, uiState.selectedLoan?.loan?.personName ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        uiState.selectedLoan?.loan?.let {
                            viewModel.deleteLoan(it)
                            onNavigateBack()
                        }
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(id = R.string.delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(id = R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showSettleConfirmation) {
        AlertDialog(
            onDismissRequest = { showSettleConfirmation = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(id = R.string.settle_loan_title)) },
            text = { Text(stringResource(id = R.string.settle_loan_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.markAsSettled()
                        showSettleConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(id = R.string.settle), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettleConfirmation = false }) {
                    Text(stringResource(id = R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // Forgive Debt Confirmation Dialog
    if (showForgiveConfirmation) {
        val remainingAmount = uiState.selectedLoan?.let { lwp ->
            val total = lwp.loan.amount + lwp.loanItems.sumOf { it.amount }
            val paid = lwp.payments.sumOf { it.amount }
            (total - paid).coerceAtLeast(0.0)
        } ?: 0.0
        AlertDialog(
            onDismissRequest = { showForgiveConfirmation = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarn) },
            title = { Text("Forgive Debt?") },
            text = { Text("Are you sure you want to write off the remaining ${uiState.currencySymbol}${String.format("%,.0f", remainingAmount)}? This will not log it as received cash.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.forgiveLoan()
                        showForgiveConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AmberWarn)
                ) {
                    Text("Forgive", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgiveConfirmation = false }) {
                    Text(stringResource(id = R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(id = R.string.loan_details), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    if (uiState.selectedLoan != null) {
                        // Share PDF receipt
                        IconButton(onClick = {
                            uiState.selectedLoan?.let { lwp ->
                                PdfReceiptGenerator.generateAndShare(
                                    context = context,
                                    loan = lwp.loan,
                                    payments = lwp.payments,
                                    loanItems = lwp.loanItems,
                                    currencySymbol = uiState.currencySymbol
                                )
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share Receipt", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            if (uiState.selectedLoan != null && uiState.selectedLoan?.loan?.status != LoanStatus.FULLY_PAID && uiState.selectedLoan?.loan?.status != LoanStatus.FORGIVEN) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 6.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                             Button(
                                 onClick = { showAddPaymentSheet = true },
                                 modifier = Modifier
                                     .weight(1f)
                                     .height(52.dp),
                                 shape = SoftChipShape,
                                 colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                             ) {
                                 Text(stringResource(id = R.string.pay), fontWeight = FontWeight.Bold)
                             }

                             Button(
                                 onClick = { showAddLoanSheet = true },
                                 modifier = Modifier
                                     .weight(1f)
                                     .height(52.dp),
                                 shape = RoundedCornerShape(12.dp),
                                 colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary)
                             ) {
                                 Text(stringResource(id = R.string.add_more), fontWeight = FontWeight.Bold)
                             }

                             FilledIconButton(
                                 onClick = { showSettleConfirmation = true },
                                 modifier = Modifier.size(52.dp),
                                 shape = RoundedCornerShape(12.dp),
                                 colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                             ) {
                                 Icon(Icons.Default.CheckCircle, contentDescription = "Settle", tint = MaterialTheme.colorScheme.primary)
                             }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Forgive Debt
                            TextButton(
                                onClick = { showForgiveConfirmation = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                            ) {
                                Text("Forgive Debt", style = MaterialTheme.typography.labelMedium)
                            }
                            // Send Reminder
                            TextButton(
                                onClick = {
                                    uiState.selectedLoan?.let { lwp ->
                                        val loan = lwp.loan
                                        val total = loan.amount + lwp.loanItems.sumOf { it.amount }
                                        val paid = lwp.payments.sumOf { it.amount }
                                        val remaining = (total - paid).coerceAtLeast(0.0)
                                        val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                                        sendReminder(
                                            context = context,
                                            contactName = loan.personName,
                                            amount = remaining,
                                            dueDate = dateFormat.format(loan.promisedReturnDate),
                                            phoneNumber = loan.phoneNumber.ifBlank { null },
                                            currencySymbol = uiState.currencySymbol
                                        )
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = SkyBlue)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Send Reminder", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.selectedLoan == null) {
             com.sbs.loaney.ui.components.AnimatedLoadingScreen(modifier = Modifier.padding(padding))
        } else {
            val loanWithPayments = uiState.selectedLoan!!
            val loan = loanWithPayments.loan
            val payments = loanWithPayments.payments
            val loanItems = loanWithPayments.loanItems

            val totalLoan = loan.amount + loanItems.sumOf { it.amount }
            val paid = payments.sumOf { it.amount }
            val remaining = (totalLoan - paid).coerceAtLeast(0.0)
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

            // Combine all events for history
            val historyEvents = (
                listOf(HistoryEvent(loan.loanDate, stringResource(id = R.string.initial_loan), "${uiState.currencySymbol}${String.format("%.0f", loan.amount)}", true)) +
                loanItems.map { HistoryEvent(it.date, stringResource(id = R.string.additional_loan), "+${uiState.currencySymbol}${String.format("%.0f", it.amount)}", true) } +
                payments.map { HistoryEvent(it.date, stringResource(id = R.string.payment_received), "-${uiState.currencySymbol}${String.format("%.0f", it.amount)}", false, it.method) }
            ).sortedByDescending { it.date }

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header Card - Clean White Card
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)), // 24.dp * 0.75
                    shape = SmallCardShape, // section card
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                     Row(
                        modifier = Modifier
                            .padding(15.dp) // 20.dp * 0.75
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Profile Photo 
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    if (loan.profilePhotoUri != null) {
                                        expandedImageUri = loan.profilePhotoUri
                                        isImageExpanded = true
                                    }
                                },
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
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(3.dp)) // 4.dp * 0.75
                            Text(
                                text = stringResource(id = R.string.due_date_format, dateFormat.format(loan.promisedReturnDate)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${uiState.currencySymbol}${String.format("%.0f", remaining)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(6.dp)) // 8.dp * 0.75
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { // 8.dp * 0.75
                                if (!loan.email.isNullOrBlank()) {
                                    FilledIconButton(
                                        onClick = {
                                             val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${loan.email}"))
                                             context.startActivity(intent)
                                        },
                                        modifier = Modifier.size(27.dp), // 36.dp * 0.75
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Icon(Icons.Default.Email, contentDescription = "Email", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(14.dp)) // 18.dp * 0.75
                                    }
                                }
                                FilledIconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${loan.phoneNumber}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.size(27.dp), // 36.dp * 0.75
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(14.dp)) // 18.dp * 0.75
                                }
                            }
                        }
                    }
                }

                // Info Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) { // 12.dp * 0.75
                    InfoTile(
                        title = stringResource(id = R.string.total_amount),
                        value = "${uiState.currencySymbol}${String.format("%.0f", totalLoan)}",
                        modifier = Modifier.weight(1f)
                    )
                    InfoTile(
                        title = stringResource(id = R.string.loan_type),
                        value = if (loan.type.name == "LEND") stringResource(id = R.string.lent) else stringResource(id = R.string.borrowed),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Detailed Information
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)), // 24.dp * 0.75
                    shape = SmallCardShape, // section card
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { // 16.dp * 0.75, 12.dp * 0.75
                        DetailRow(icon = Icons.Default.Info, label = stringResource(id = R.string.reason_for_loan), value = loan.purpose ?: stringResource(id = R.string.not_specified))
                        DetailRow(icon = Icons.Default.LocationOn, label = stringResource(id = R.string.location_optional), value = loan.address ?: stringResource(id = R.string.not_specified))
                        DetailRow(icon = Icons.AutoMirrored.Filled.Notes, label = stringResource(id = R.string.note_optional), value = loan.notes ?: stringResource(id = R.string.no_notes))

                        val statusColor = when (loan.status) {
                            LoanStatus.OVERDUE -> MaterialTheme.colorScheme.error
                            LoanStatus.FULLY_PAID -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onBackground
                        }
                        DetailRow(icon = Icons.Default.CheckCircle, label = stringResource(id = R.string.status), value = loan.status?.name ?: "", highlightColor = statusColor)
                    }
                }

                // Timeline / History
                Text(
                    text = stringResource(id = R.string.loan_history),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { // 16.dp * 0.75
                    if (historyEvents.isEmpty()) {
                        Text(stringResource(id = R.string.no_history), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                    historyEvents.forEach { event ->
                        TimelineItem(
                            date = dateFormat.format(event.date),
                            title = event.title,
                            subtitle = if (event.method != null) stringResource(id = R.string.via_method, event.method) else "",
                            amount = event.amount,
                            isLoan = event.isLoan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(75.dp)) // 100.dp * 0.75
            }
        }
    }

    if (showAddPaymentSheet) {
        AddPaymentBottomSheet(
            onDismiss = { showAddPaymentSheet = false },
            onAddPayment = { amount, method, note, proofUri ->
                viewModel.addPayment(amount, method, note, proofUri)
                showAddPaymentSheet = false

                // Calculate Loaney Pie reward
                val dueDate = uiState.selectedLoan?.loan?.promisedReturnDate
                val daysBeforeDue = if (dueDate != null) {
                    val diffMs = dueDate.time - System.currentTimeMillis()
                    (diffMs / (1000 * 60 * 60 * 24)).toInt()
                } else 0
                // Reward points for on-time or early payments
                val userXpLevel = 1 // TODO: pull from UserProfile ViewModel
                val pts = calculateLoaneyPiePoints(
                    amountPaid   = amount,
                    daysBeforeDue = daysBeforeDue,
                    userXpLevel  = userXpLevel
                )
                if (pts > 0 && daysBeforeDue >= 0) {
                    rewardPoints = pts
                    showRewardOverlay = true
                }
            }
        )
    }

    // Loaney Pie reward overlay — rendered last so it sits above everything
    LoaneyPieRewardOverlay(
        pointsEarned       = rewardPoints,
        isVisible          = showRewardOverlay,
        onAnimationComplete = { showRewardOverlay = false }
    )

    if (showAddLoanSheet) {
        AddMoreLoanBottomSheet(
            onDismiss = { showAddLoanSheet = false },
            onAddLoan = { amount, note, proofUri ->
                viewModel.addLoanItem(amount, note, proofUri)
                showAddLoanSheet = false
            }
        )
    }

    FullScreenImageViewer(
        visible = isImageExpanded,
        imageUri = expandedImageUri,
        onDismiss = { isImageExpanded = false }
    )
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String, highlightColor: Color? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp)) // 20.dp * 0.75
        Spacer(modifier = Modifier.width(9.dp)) // 12.dp * 0.75
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = highlightColor ?: MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

data class HistoryEvent(
    val date: Date,
    val title: String,
    val amount: String,
    val isLoan: Boolean,
    val method: String? = null
)

@Composable
fun InfoTile(title: String, value: String, highlight: Boolean = false, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(15.dp)), // 20.dp * 0.75
        shape = SmallCardShape, // item card
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) { // 16.dp * 0.75
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp)) // 8.dp * 0.75
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun TimelineItem(date: String, title: String, subtitle: String, isLoan: Boolean = false, amount: String? = null) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(45.dp)) { // 60.dp * 0.75
            Text(
                text = date.substringBefore(" "), // Day
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = date.split(" ").getOrNull(1) ?: "", // Month
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(12.dp)) // 16.dp * 0.75

        Card(
            modifier = Modifier.weight(1f).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)), // 16.dp * 0.75
            shape = SoftChipShape, // inner card
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(12.dp), // 16.dp * 0.75
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp) // 10.dp * 0.75
                        .background(if (isLoan) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(modifier = Modifier.width(9.dp)) // 12.dp * 0.75
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (amount != null) {
                    Text(amount, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (isLoan) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentBottomSheet(
    onDismiss: () -> Unit,
    onAddPayment: (Double, String, String?, String?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("Cash") }
    var proofUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> proofUri = uri }

    val methods = listOf("Cash", "Bank", "bKash", "Nagad", "Rocket")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp) // 24.dp * 0.75
                .padding(bottom = 36.dp) // 48.dp * 0.75
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(15.dp) // 20.dp * 0.75
        ) {
            Text(stringResource(id = R.string.add_payment), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it },
                label = { Text(stringResource(id = R.string.amount), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp), // 16.dp * 0.75
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Column {
                Text(stringResource(id = R.string.payment_method), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp)) // 8.dp * 0.75
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { // 8.dp * 0.75
                    methods.take(3).forEach { m ->
                        val selected = method == m
                        FilterChip(
                            selected = selected,
                            onClick = { method = m },
                            label = { Text(m) },
                             colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha=0.3f),
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Color.Transparent,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                enabled = true,
                                selected = selected
                            ),
                            shape = CircleShape
                        )
                    }
                }
                 Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { // 8.dp * 0.75
                    methods.drop(3).forEach { m ->
                        val selected = method == m
                        FilterChip(
                            selected = selected,
                            onClick = { method = m },
                            label = { Text(m) },
                             colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha=0.3f),
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Color.Transparent,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                enabled = true,
                                selected = selected
                            ),
                            shape = CircleShape
                        )
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(id = R.string.note_optional), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), // 16.dp * 0.75
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Button(
                onClick = {
                    amount.toDoubleOrNull()?.let {
                        onAddPayment(it, method, note.ifBlank { null }, proofUri?.toString())
                    }
                },
                modifier = Modifier.fillMaxWidth().height(42.dp), // 56.dp * 0.75
                shape = RoundedCornerShape(12.dp), // 16.dp * 0.75
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                enabled = amount.isNotBlank()
            ) {
                Text(stringResource(id = R.string.save_payment), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMoreLoanBottomSheet(
    onDismiss: () -> Unit,
    onAddLoan: (Double, String?, String?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var proofUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> proofUri = uri }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp) // 24.dp * 0.75
                .padding(bottom = 36.dp) // 48.dp * 0.75
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(15.dp) // 20.dp * 0.75
        ) {
            Text(stringResource(id = R.string.add_more_loan), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it },
                label = { Text(stringResource(id = R.string.amount), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp), // 16.dp * 0.75
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    cursorColor = MaterialTheme.colorScheme.secondary
                )
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(id = R.string.note_optional), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), // 16.dp * 0.75
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Button(
                onClick = {
                    amount.toDoubleOrNull()?.let {
                        onAddLoan(it, note.ifBlank { null }, proofUri?.toString())
                    }
                },
                modifier = Modifier.fillMaxWidth().height(42.dp), // 56.dp * 0.75
                shape = RoundedCornerShape(12.dp), // 16.dp * 0.75
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
                enabled = amount.isNotBlank()
            ) {
                Text(stringResource(id = R.string.add_loan), fontWeight = FontWeight.Bold)
            }
        }
    }
}
